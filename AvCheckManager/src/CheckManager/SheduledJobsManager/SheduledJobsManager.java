
package CheckManager.SheduledJobsManager;

import CheckManager.Config;
import CheckManager.Utils.Common;
import java.io.File;
import java.io.FileFilter;
import org.apache.log4j.Logger;


public class SheduledJobsManager  implements Runnable
{
    private static final            Logger log           = Logger.getLogger(SheduledJobsManager.class);
    private static final FileFilter tmpsFileFilter = new FileFilter() {
                        public boolean accept(File name) {
                            return (System.currentTimeMillis() - name.lastModified() >= 24*60*60*1000);
                        }};
    private static final FileFilter tmpsUploadFileFilter = new FileFilter() {
                        public boolean accept(File name) {
                            return (System.currentTimeMillis() - name.lastModified() >= 60*60*1000);
                        }
                    };

        public void run()
        {
            Config.totalRunThreads++;

            while(!Config.stopFlag)
            {
                try
                {
                    while(!Config.stopFlag)
                    {
                        Thread.sleep(10000);

                        //#########################################################
                        // Clear scan upload folder
                        //#########################################################
                        Common.DeleteFilesAndFolders(new File(Config.mcUploadDir),tmpsUploadFileFilter);

                        //#########################################################
                        // Clear tmps folders
                        //#########################################################
                        String stmpDir = System.getProperty("java.io.tmpdir");
                        Common.DeleteFilesAndFolders(new File(stmpDir),tmpsFileFilter);
                        Common.DeleteFilesAndFolders(new File("C:\\tmp"),tmpsFileFilter); // TODO:
                        Common.DeleteFilesAndFolders(new File("C:\\windows\tmp"),tmpsFileFilter);// TODO:
                    }
                }
                catch(Exception e)
                {
                    log.error(e);
                }
            }

            Config.totalRunThreads--;
        }

    }


