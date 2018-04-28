
package ScanVmManager;

import ScanVmManager.VmController.SCANVM_STATE;
import ScanVmManager.VmController.VM_INFO;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import scanBalancer.Config;
import scanBalancer.ScanQueue;
import scanBalancer.ScanQueue.CHECK_SERVER;
import scanBalancer.Utils;


public class ScanVMManager implements Runnable
{
    private static final            Logger log           = Logger.getLogger(ScanVMManager.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();

    private String getAvsVersions(String servIp,int port) throws Exception
    {
        InputStream in = null;
        StringBuilder retData = new StringBuilder();
        byte[] buff = new byte[1024];
        int readb;
        
        try
        {
            URL url = new URL("http://",servIp,port,"/AvsVersions");
            URLConnection httpCon = url.openConnection();
            in = httpCon.getInputStream();
            while(true)
            {
                readb = in.read(buff);
                if(readb == -1) break;
                retData.append(new String(buff,0,readb));
            }
        }
        finally
        {
            if(in != null) in.close();
        }
        
        return retData.toString();
    }
    
    private String getAvsVersionsHash(String servIp,int port) throws Exception
    {
        String updServAvsVerHash = "";
        String xmlUpdSerAvs = getAvsVersions(servIp,port);
        if(!xmlUpdSerAvs.isEmpty())
        {
            XPath xpath = XPathFactory.newInstance().newXPath();
            Document updVerXmlDoc =  DocumentBuilderFactory.newInstance().
                                     newDocumentBuilder().parse(new InputSource(new StringReader(xmlUpdSerAvs)));
            updServAvsVerHash = (String)xpath.compile("//total_ver_hash").evaluate(updVerXmlDoc,XPathConstants.STRING);
        }
        return updServAvsVerHash;
    }

    private boolean setScanVmRoleScanner(VM_INFO rootvm,VM_INFO scanvm)
    {
        boolean result = false;

        try
        {
            URL url = new URL("http://",rootvm.vmip,Config.checkManagerPort,"/SetRole/scanner/ip/"+scanvm.vmip);
            HttpURLConnection httpCon = (HttpURLConnection)url.openConnection();
            httpCon.setRequestMethod("POST");
            result = (httpCon.getResponseCode() == 200)?true:false;
        }
        catch(Exception e)
        {
            log.error(e);
        }

        return result;
    }

    public boolean stopScanOnThisVm(String servIp) throws Exception
    {
        for(CHECK_SERVER cs : Config.CheckServers)
        {
            if(!cs.ip.equalsIgnoreCase(servIp)) continue;
            cs.vm_status = SCANVM_STATE.NEED_STOP;
            while(true)
            {
                Thread.sleep(1000);
                if(cs.vm_status == SCANVM_STATE.MAY_STOP) return true;
            }
        }

        log.error("Scan server not found ip "+servIp);
        return false;
    }

    public boolean startScanOnThisVm(String servIp) throws Exception
    {
        for(CHECK_SERVER cs : Config.CheckServers)
        {
            if(!cs.ip.equalsIgnoreCase(servIp)) continue;
            cs.vm_status = SCANVM_STATE.RUNNING;
            return true;
        }

        log.error("Scan server not found ip "+servIp);
        return false;
    }

    public boolean isScanServerStarted(String ip,int port) throws Exception
    {
        Socket sock;
        try
        {
            sock = new Socket(ip,port);
        }
        catch(Exception e)
        {
            return false;
        }

        sock.close();
        return true;
    }
    
    public void run() 
    {
        long lastVmCheckTime = 0;
        VmController vmCtrl = new VmController("",22,"root","");
        
        while(!Config.stopFlag)
        {
            try
            {
                while(!Config.stopFlag)
                {
                    Thread.sleep(1000);
                    if(Utils.currentTimeStamp()-lastVmCheckTime >= 3*60*60*1000)
                    {
                        HashMap <String,VM_INFO> rootvms = vmCtrl.getRunningVms();
                        if(!rootvms.isEmpty())
                        {
                            Set<String> rootKeys = rootvms.keySet();
                            for(String Key : rootKeys)
                            {
                                VM_INFO rootvm = rootvms.get(Key);
                                if(rootvm.childvm != null) continue;
                                String updServAvsVerHash = getAvsVersionsHash(rootvm.vmip,Config.checkManagerPort);
                                if(updServAvsVerHash.isEmpty()) continue;
                                vmCtrl.powerOffVm(rootvm.vmid, VmController.POWEROFF_TYPE.SOFT);
                                for(VM_INFO scanvm : rootvm.childvm)
                                {
                                    String scanServAvsVerHash = getAvsVersionsHash(scanvm.vmip,Config.checkManagerPort);
                                    if(scanServAvsVerHash.isEmpty() || 
                                            scanServAvsVerHash.equalsIgnoreCase(updServAvsVerHash))
                                        continue;

                                    if(!stopScanOnThisVm(scanvm.vmip)) continue;
                                    vmCtrl.powerOffVm(scanvm.vmid, VmController.POWEROFF_TYPE.HARD);
                                    vmCtrl.copyVmDisks(rootvm.vmpath,scanvm.vmpath);
                                    vmCtrl.powerOnVm(scanvm.vmid);
                                    long countTimer = Utils.currentTimeStamp();
                                    while(true)
                                    {
                                        Thread.sleep(2*60*1000);
                                        if(setScanVmRoleScanner(rootvm,scanvm))
                                        {
                                            countTimer = Utils.currentTimeStamp();
                                            while(true)
                                            {
                                                Thread.sleep(5000);
                                                if(isScanServerStarted(scanvm.vmip,Config.checkManagerPort))
                                                {
                                                    startScanOnThisVm(scanvm.vmip);
                                                    break;
                                                }
                                                if(Utils.currentTimeStamp()-countTimer >= 20*60*1000)
                                                {
                                                    log.error("Unable to run scan vm "+scanvm.vmname);
                                                    break;
                                                }
                                            }

                                            break;
                                        }

                                        if(Utils.currentTimeStamp()-countTimer >= 20*60*1000)
                                        {
                                            log.error("Unable to run scan vm "+scanvm.vmname);
                                            break;
                                        }
                                    }
                                }

                                vmCtrl.powerOnVm(rootvm.vmid);
                               
                            }
                        }                   
                    }
                }
            }
            catch(Exception e)
            {
                    
                log.error(e);
            }
        }
    }
    
    
    
}
