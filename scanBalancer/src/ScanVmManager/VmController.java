

package ScanVmManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import scanBalancer.Config;
import scanBalancer.Utils;

public class VmController
{
    private static final            Logger log           = Logger.getLogger(VmController.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();
    private String servIP;
    private int sshPort;
    private String Login;
    private String Passwd;
    private static final Pattern getippattern = Pattern.compile("ipAddress\\s*=\\s*\"([^\"]*)\",",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
    private static final Pattern patrn = Pattern.compile("^(\\d*)\\s*([^\\s]*)\\s*[^\\s]*\\s*([^\\s]*)",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);

    public enum SCANVM_STATE
    {
        RUNNING,
        NEED_STOP,
        MAY_STOP,
        STOPPED,
        INITIALIZING
    }

    public enum POWEROFF_TYPE
    {
        SOFT,
        HARD
    }

    public class VM_INFO
    {
        String vmid;
        String vmname;
        String vmpath;
        String vmip;
        ArrayList <VM_INFO> childvm=null;
    }

    public VmController(String servIP,int sshPort,String Login,String Passwd)
    {
        this.Login = Login;
        this.Passwd = Passwd;
        this.servIP = servIP;
        this.sshPort = sshPort;
    }

    public void copyVmDisks(String SrcVmPath,String DstVmPath) throws Exception
    {
        SshClient sshClient = null;

        try
        {
            sshClient = new SshClient(servIP,sshPort,Login,Passwd);
            if(!DstVmPath.endsWith("/")) DstVmPath+="/";
            if(!SrcVmPath.endsWith("/")) SrcVmPath+="/";
            String[] SrcVmDisks = sshClient.execCmd("ls "+Config.esxiDatastorePath+SrcVmPath+" | grep .vmdk").split("\t\n");

            for(String srcVmDisk : SrcVmDisks)
            {
                if(srcVmDisk.contains("-flat.")) continue;
                String result = sshClient.execCmd("vmkfstools -U "+Config.esxiDatastorePath+DstVmPath+srcVmDisk);
                result = sshClient.execCmd("vmkfstools -i "+Config.esxiDatastorePath+SrcVmPath+srcVmDisk+" "+
                        Config.esxiDatastorePath+DstVmPath+srcVmDisk);
            }

        }
        finally
        {
            if(sshClient != null) sshClient.close();
        }
    }

    public void powerOnVm(String vmid) throws Exception
    {
        SshClient sshClient = null;
        try
        {
            sshClient = new SshClient(servIP,sshPort,Login,Passwd);
            sshClient.execCmd("vim-cmd vmsvc/power.on "+vmid);
        }
        finally
        {
            if(sshClient != null) sshClient.close();
        }
    }

    public String getVmOnOffStatus(String vmid) throws Exception
    {
        SshClient sshClient = null;
        String result = "";
        try
        {
            sshClient = new SshClient(servIP,sshPort,Login,Passwd);
            result = sshClient.execCmd("vim-cmd vmsvc/power.getstate "+vmid+" | grep -E \"on|off\" | cut -d \" \" -f 2");
        }
        finally
        {
            if(sshClient != null) sshClient.close();
        }

        return result;
    }



    public boolean powerOffVm(String vmid,POWEROFF_TYPE type) throws Exception
    {
        SshClient sshClient = null;
        try
        {
            sshClient = new SshClient(servIP,sshPort,Login,Passwd);
            if(type == POWEROFF_TYPE.SOFT)
                sshClient.execCmd("vim-cmd vmsvc/power.shutdown "+vmid);
            else
                sshClient.execCmd("vim-cmd vmsvc/power.off "+vmid);

            long shutdownTimeStamp = Utils.currentTimeStamp();
            while(true)
            {
                Thread.sleep(1000);
                String vmstate = sshClient.execCmd("vim-cmd vmsvc/power.getstate "+vmid+" | grep -E \"on|off\" | cut -d \" \" -f 2");
                if(vmstate.equalsIgnoreCase("off")) return true;
                if(Utils.currentTimeStamp()-shutdownTimeStamp >= 5*60*1000)
                {
                    sshClient.execCmd("vim-cmd vmsvc/power.off "+vmid);
                    shutdownTimeStamp = Utils.currentTimeStamp();
                    log.error("VM "+vmid+" not responded for shutdown command, power off");
                }
            }
        }
        finally
        {
            if(sshClient != null) sshClient.close();
        }
    }
    
    public HashMap <String,VM_INFO> getRunningVms() throws Exception
    {
        HashMap <String,VM_INFO> rootvms = new HashMap();
        SshClient sshClient = null;

        try
        {
            sshClient = new SshClient(servIP,sshPort,Login,Passwd);
            String result = sshClient.execCmd("vim-cmd vmsvc/getallvms | grep ScanVmGroup");
            System.out.println(result);

            Matcher match = patrn.matcher(result);
            if(match.matches())
            {
                while(match.find())
                {
                    VM_INFO vi = new VM_INFO();
                    vi.vmid = match.group(1);
                    vi.vmname = match.group(2);
                    vi.vmpath = match.group(3);
                    vi.vmpath = vi.vmpath.substring(0, vi.vmpath.lastIndexOf("/"));

                    String guest_result = sshClient.execCmd("vim-cmd vmsvc/get.guest "+vi.vmid);
                    Matcher gipmatch = getippattern.matcher(guest_result);
                    if(!gipmatch.find())
                    {
                        log.error("VM IP not found in ");
                        log.error(guest_result);
                        continue;
                    }
                    vi.vmip = gipmatch.group(1);

                    if(vi.vmname.matches("ScanVm_g\\d*_\\d*"))
                    {
                        String rootvmname = vi.vmname.substring(0, vi.vmname.lastIndexOf("_"));
                        if(!rootvms.containsKey(rootvmname))
                        {
                            VM_INFO dummyroot = new VM_INFO();
                            dummyroot.childvm = new ArrayList<VM_INFO>();
                            dummyroot.childvm.add(vi);
                            rootvms.put(rootvmname, dummyroot);
                        }
                        else
                        {
                            VM_INFO tmpinfo = rootvms.get(rootvmname);
                            tmpinfo.childvm.add(vi);
                        }
                    }
                    else
                    {
                        vi.childvm = new ArrayList<VM_INFO>();
                        rootvms.put(vi.vmname, vi);
                    }
                }
            }
        }
        finally
        {
            if(sshClient != null) sshClient.close();
        }

        return rootvms;
    }

}
