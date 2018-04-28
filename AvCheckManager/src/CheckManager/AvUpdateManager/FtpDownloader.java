package CheckManager.AvUpdateManager;


import CheckManager.CommonObjects.AV_EXEC_RECORD;
import CheckManager.Utils.Common;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPFile;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;

public class FtpDownloader extends FTPClient
{
    private int downloadJobCount;
   
    public boolean start(String avName,URL url,Node xmlNode,String updateDir) throws Exception
    {
        XPath xpath = XPathFactory.newInstance().newXPath();
        boolean result = false;
        Preferences prefs;
        Preferences root = Preferences.userRoot();
        prefs = root.node("updatemanager/"+avName.toLowerCase()+"/ftpupdate");
        downloadJobCount = 0;
        String isArchive = (String)xpath.compile("@isArchive").evaluate(xmlNode,
                                                                        XPathConstants.STRING);
        String ftpHost = url.getHost();
        String dir = url.getPath();
        dir = dir.substring(0,dir.lastIndexOf("/"));
        String file = url.getFile();
        file = file.substring(file.lastIndexOf("/")+1);
        String[] userPass = url.getUserInfo().split(":");
        int port = url.getPort();
        if(port == -1) port = 21;
                
        connect(ftpHost,port);
        login(userPass[0],userPass[1]);
        changeDirectory(dir);
        FTPFile[] files = list(file);
        this.disconnect(false);
        for(int j=0; j < files.length; j++)
        {
            String lastUpdate = prefs.get(files[j].getName(), "");
            Date fileDate = files[j].getModifiedDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(fileDate);
            long ftpFileTime = cal.getTimeInMillis();
                    
            if(!lastUpdate.isEmpty())
            {
                long lastUpdateDate = Long.parseLong(lastUpdate);
                if(lastUpdateDate == ftpFileTime) continue;
            }

            while(this.downloadJobCount >= 20) Thread.sleep(1000);
            new Thread(new downloadJob(ftpHost,port,userPass[0],userPass[1],dir,
                                files[j].getName(),prefs,ftpFileTime,updateDir,
                                isArchive.equalsIgnoreCase("yes"))).start();
        }
                   
        while(downloadJobCount != 0)
        {
            Thread.sleep(1000);
        }

        return result;
    }

    private class downloadJob implements Runnable
    {
        private String Host;
        private int port;
        private String login;
        private String passw;
        private String dir;
        private String ftpFile;
        private FTPClient client;
        private Preferences prefs;
        private long modifiedDate;
        private String UpdateDir;
        private boolean isArchive;
        
        downloadJob(String Host,int port,String login,String passw,String dir,
                String ftpFile,Preferences prefs,long modifiedDate,String UpdateDir,boolean isArchive)
        {
            this.Host = Host;
            this.port = port;
            this.dir = dir;
            this.ftpFile = ftpFile;
            this.login = login;
            this.passw = passw;
            this.prefs = prefs;
            this.modifiedDate = modifiedDate;
            this.UpdateDir = UpdateDir;
            this.isArchive = isArchive;
            client = new FTPClient();
        }
        
        public void run()
        {
            downloadJobCount++;
            int BUFFER_SIZE = 1024;
            byte data[] = new byte[BUFFER_SIZE];
            FileInputStream fis = null;
            ZipInputStream zis = null;
            FileOutputStream fos = null;
            BufferedOutputStream dest = null;
            int index=0;
            
            String tmpSavePath = System.getProperty("java.io.tmpdir");
            String fileName = ftpFile;
            tmpSavePath += "\\"+Common.currentTimeStampInMillis()+"_"+fileName;
            File tmpFile = new File(tmpSavePath);
            
            while(true)
            {
                try
                {
                    tmpFile.delete();
                    client.connect(Host,port);
                    client.login(login,passw);
                    client.changeDirectory(dir);
                    client.setType(FTPClient.TYPE_BINARY);
                    client.download(ftpFile,tmpFile);
                    long ftpFileSize = client.fileSize(ftpFile);
                    //client.logout();
                    client.disconnect(false);
                    if(ftpFileSize != tmpFile.length()) continue;
                    
                    if(!isArchive)
                    {
                        tmpFile.renameTo(new File(UpdateDir+"\\"+fileName));
                        tmpFile.delete();
                        break;
                    }
                    else
                    {
                        try
                        {
                            fis = new FileInputStream(tmpSavePath);
                            zis = new ZipInputStream(fis);
                            ZipEntry entry;
                                        
                            while((entry = zis.getNextEntry()) != null)
                            {
                                fos = new FileOutputStream(UpdateDir+"\\"+entry.getName());
                                dest = new BufferedOutputStream(fos, BUFFER_SIZE);
                                int count;
                                while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) 
                                {
                                    dest.write(data, 0, count);
                                }
                                            
                                dest.flush();
                                dest.close();
                                dest = null;
                                fos.close();
                                fos = null;
                            }
                            
                            zis.close();
                            zis = null;
                            fis.close();
                            fis = null;
                            tmpFile.delete();
                            break;
                        }
                        catch(Exception e)
                        {
                           // result = false;
                            if(dest != null) dest.close();
                            dest = null;
                            if(fos != null) fos.close();
                            fos = null;
                            if(zis != null) zis.close();
                            zis = null;
                            if(fis != null) fis.close();
                            fis = null;
                        }
                    }
                }
                catch(Exception e)
                {
                    if(client.isConnected())
                    {
                        try
                        {
                            client.disconnect(false);
                        }
                        catch(Exception ee){}
                    }

                    System.out.println(e.getMessage());
                }
            }
            
            prefs.put(ftpFile, Long.toString(modifiedDate));
            downloadJobCount--;
        }
    }
}
