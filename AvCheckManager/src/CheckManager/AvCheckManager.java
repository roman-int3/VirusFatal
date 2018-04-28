package CheckManager;

import CheckManager.HttpHandlers.StartScan_Handler;
import CheckManager.HttpHandlers.SetRole_Handler;
import CheckManager.HttpHandlers.ScanResult_Handler;
import CheckManager.HttpHandlers.AvsVersions_Handler;
import CheckManager.AvUpdateManager.UpdateManager;
import CheckManager.Config.SERVER_ROLE;
import CheckManager.ScanManager.ScanManager;
import CheckManager.SheduledJobsManager.SheduledJobsManager;
import CheckManager.Utils.Rand;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;


public class AvCheckManager
{
    static private HttpServer hServ;
    static private Logger log = Logger.getLogger(AvCheckManager.class);
    public static ScanManager scanManager;
    
    
    
    static public void main(String args[]) throws Exception
    {

        File lockFile;
        FileChannel channel=null;
        FileLock lock=null;
       
        try
        {
            lockFile = new File("AvCheckManager.lk");
            channel = new RandomAccessFile(lockFile, "rw").getChannel();

            try
            {
                lock = channel.tryLock();
                if(lock == null)
                {
                    System.out.println("Failed get lock.");
                    throw new OverlappingFileLockException();
                }
            }
            catch (Exception e)
            {
                System.out.println("Another instance of AvCheckManager running.");
                throw e;
            }

            try
            {
                startServer();
                Scanner con = new Scanner(System.in);
                while(true)
                {
                    printBanner();
                    String str = con.next();
                    if(str.equalsIgnoreCase("-help"))
                    {
                        usage();
                        //
                    }
                    else if(str.equalsIgnoreCase("-quit"))
                    {
                        System.out.println("Bye!");
                        break;
                    }
                    else if(str.equalsIgnoreCase("-logenable"))
                    {
                        Config.isLogEnable = true;
                        Config.prefs.put("islogenable","true");
                        //LogWriter.enableLogs();
                        System.out.println("Detailed log enabled.");
                    }
                    else if(str.equalsIgnoreCase("-logdisable"))
                    {
                        Config.isLogEnable = false;
                        Config.prefs.put("islogenable","false");
                        //LogWriter.disableLogs();
                        System.out.println("Detailed log disabled.");
                    }
                    else if(str.equalsIgnoreCase("-status"))
                    {

                        double totalMem = Runtime.getRuntime().totalMemory();
                        totalMem /= 1024*1024;
                        System.out.printf("Total JVM memory (MB): %.2f\n",totalMem );
                        totalMem = Runtime.getRuntime().freeMemory();
                        totalMem /= 1024*1024;
                        System.out.printf("Free JVM memory (MB): %.2f\n",totalMem);

                        long maxMemory = Runtime.getRuntime().maxMemory();
                        totalMem = maxMemory;
                        totalMem /= 1024*1024;
                        if(maxMemory == Long.MAX_VALUE)
                            System.out.println("Maximum JVM memory (MB): no limit");
                        else
                            System.out.printf("Maximum JVM memory (MB): %.2f\n",totalMem);
                    }
                    else if(str.equalsIgnoreCase("-stop"))
                    {
                        if(!Config.stopFlag)
                        {
                            stopServer();
                        }

                    }
                    else if(str.equalsIgnoreCase("-start"))
                    {
                        if(Config.stopFlag)
                        {
                            startServer();
                        }

                    }
                    else if(str.equalsIgnoreCase("-restart"))
                    {
                        stopServer();
                        startServer();
);
                    }
                }
            }
            catch (Exception e)
            {
                log.error("Exception",e);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.error("Exception",e);
        }
        finally
        {
            scanManager.close();

            if(lock != null) lock.release();
            if(channel != null) channel.close();
            System.exit(0);
        }

    }

    static private void printBanner()
    {
        System.out.println(
                  "AvChecker v1\r\n"+
                          "Server Status : " + (Config.stopFlag?"Stopped":"Started")
                );
    }


    static private void usage()
    {
        System.out.println(
                "Options:\n"
                +"      -help                       This help.\n"
                +"      -quit                       End programm.\n"
                +"      -stop                       Stop server.\n"
                +"      -start                      Start server.\n"
                +"      -restart                    Restart server.\n"
                +"      -status                     Server statistics.\n"
                +"      -logenable                  Enable detailed log.\n"
                +"      -logdisable                 Disable detailed log.\n"
        );
    }

    static private void stopServer() throws Exception
    {
        Config.stopFlag = true;
        hServ.stop(0);
        scanManager.close();
        while(Config.totalRunThreads != 0) Thread.sleep(1000);
    }

    static public void restart() throws Exception
    {
        stopServer();
        startServer();
    }

    static private void startServer() throws Exception
    {
        Config.init();
        Config.stopFlag = false;

        JNI.init();
        JNI.getCpuUsage();

        scanManager = new ScanManager();

        new Thread(new SheduledJobsManager()).start();
        log.debug("Sheduler started");

        hServ = HttpServer.create(new InetSocketAddress(Integer.parseInt(Config.listenPort)),0);

        if(Config.server_role == SERVER_ROLE.scanner)
        {
            hServ.createContext("/StartScan",       new StartScan_Handler());
            hServ.createContext("/ScanResult",      new ScanResult_Handler());
        }

        hServ.createContext("/AvsVersions",      new AvsVersions_Handler());
        hServ.createContext("/SetRole",      new SetRole_Handler());
        hServ.setExecutor(Executors.newCachedThreadPool());
        hServ.start();

    }
}
