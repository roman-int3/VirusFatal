package scanBalancer;


import java.io.File;
import java.io.InputStream;
import java.util.Random;


public class FileStore
{
    private static final Object syncObj = new Object();
    private static final String Password = "";
        
    public static String addFile(InputStream InStream,String fileName,long fSize) throws Exception
    {
        int folderID;
        String upFileStorePath;
        
        synchronized(syncObj)
        {
            do
            {
                upFileStorePath = "/"+Long.toString(Utils.currentTimeStamp());
                new File(Config.filestorepath+upFileStorePath).mkdir();
                folderID = Math.abs(new Random().nextInt());
                upFileStorePath += "/"+Integer.toString(folderID);
            }while(!(new File(Config.filestorepath+upFileStorePath).mkdir()));
        }

        upFileStorePath += "/"+fileName;
        Utils.saveToFile(InStream, Config.filestorepath+upFileStorePath, fSize);
        
        
        DesEncrypter crypt = new DesEncrypter(Password);
        return crypt.encrypt(upFileStorePath);
    }

    public static void delFile(String fsid)
    {
        DesEncrypter crypt = new DesEncrypter(Password);
        new File(Config.filestorepath+crypt.decrypt(fsid)).delete();
    }

    public static String getPath(String fsid)
    {
        DesEncrypter crypt = new DesEncrypter(Password);
        return Config.filestorepath+crypt.decrypt(fsid);

    }
}