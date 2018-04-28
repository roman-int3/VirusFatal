package scanBalancer;

import AutoChecker.AutoChecker;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;


public class scanBalancer
{
    static private HttpServer       httpServ;
    static private final Logger     log             = Logger.getLogger(scanBalancer.class);
    static private final boolean    isDebugEnabled  = log.isDebugEnabled();
    
    public static void main(String[] args) throws Exception
    {
        File        lockFile;
        FileChannel channel=null;
        FileLock    lock=null;
         
        
        try
        {

            lockFile = new File("scanBalancer.lk");
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
                System.out.println("Another instance of scanBalancer running.");
                throw e;
            }

            try
            {
                startServer();

                while(true)
                {
                    Thread.sleep(10000);//TODO::
                }


            }
            catch (Exception e)
            {
                log.error("Exception",e);
            }
        }
        catch (Exception e)
        {
            log.error("Exception",e);
        }
        finally
        {
            if(lock != null) lock.release();
            if(channel != null) channel.close();
            System.exit(0);
        }
    }

    static private void printBanner()
    {
        System.out.println(
                  "scanBalancer v1\r\n"+
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
         );
    }

    static private void stopServer() throws Exception
    {
        Config.stopFlag = true;
        httpServ.stop(0);
        Thread.sleep(10000);
    }

    static private void startServer() throws Exception
    {
        Config.init();
        Config.stopFlag = false;

        new Thread(new HelperThread()).start();
        new Thread(new ScanQueue()).start();

        httpServ = HttpServer.create(new InetSocketAddress(Integer.parseInt(Config.listenPort)),0);
        httpServ.createContext("/StartScan",            new StartScan_Handler());
        httpServ.createContext("/ScanResult",           new ScanResult_Handler());
        httpServ.createContext("/FileStore",  new FileStore_Handler());
        httpServ.setExecutor(Executors.newCachedThreadPool());
        httpServ.start();

        new Thread(new AutoChecker()).start();

    }

    








}
