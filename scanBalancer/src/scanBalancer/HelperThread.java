package scanBalancer;


import java.io.File;
import org.apache.log4j.Logger;

public class HelperThread implements Runnable
{
    private static final            Logger log          = Logger.getLogger(StartScan_Handler.class);
    private static final boolean    isDebugEnabled      = log.isDebugEnabled();

    public void run()
    {
        while(!Config.stopFlag)
        {
            try
            {
                while(!Config.stopFlag)
                {
                    Thread.sleep(1000);

                    clearCache(Config.ReportsCachePath,Config.scanReportsCacheLifeTimeInSec);
                    clearCache(Config.UploadFilesDir,Config.uploadFilesCacheLifeTimeInSec);
                   
                }
            }
            catch(Exception e)
            {
                log.error("[ERROR]",e);

            }
        }
    }
    
    private void clearCache(String path,long CacheLifeTimeInSec)
    {
        File cacheRootDir = new File(path);
        File cacheItems[] = cacheRootDir.listFiles();
        for(File cacheItem : cacheItems)
        {
            if(Utils.currentTimeStamp() - cacheItem.lastModified() > CacheLifeTimeInSec)
            {
                Utils.delAllFiles(cacheItem);
            }
        }
    }
    
    

}
