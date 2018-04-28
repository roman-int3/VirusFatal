package scanBalancer;


import ScanVmManager.VmController.SCANVM_STATE;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;


public class ScanQueue implements Runnable
{
    public static enum SCAN_JOB_STATUS
    {
        PROCESSING,
        FINISHED_OK,
        FINISHED_ERROR
    }

    public static class CHECK_SERVER
    {
	public String          ip;
	public SERVER_STATUS   status = SERVER_STATUS.FREE;
        public SCANVM_STATE    vm_status = SCANVM_STATE.RUNNING;
        public int             runningJobs;
	public int[]           avNamesHashs;
    }

    public enum SERVER_STATUS
    {
	FREE,
	BUSY
    }

    public static enum SESSION_JOB_STATUS
    {
	PENDING,
	PROCESSING,
	PROCESSED
    }

    public static enum PRIORITY
    {
        NORMAL,
        HIGHT
    }

    public static class SCAN_SESSION_JOB
    {
	int[]               avNamesHashs;
        String[]            avNames;
        int                 avCount;
	long                SSJID;
	long                startTime;
	SESSION_JOB_STATUS  status = SESSION_JOB_STATUS.PENDING;
        String              filePath;
        int                 QueueNum;
        PRIORITY            priority = PRIORITY.NORMAL;
    }

    

    static private       Logger             log = Logger.getLogger(ScanQueue.class);
    static private final boolean            isDebugEnabled = log.isDebugEnabled();
    static private List <SCAN_SESSION_JOB>  scanJobsQueue = Collections.synchronizedList(new ArrayList<SCAN_SESSION_JOB>());
    static private HttpClient               httpClient = null;
    private static final Random             rnd = new Random();
    
    public void run()
    {
        while(true)
        {
            try
            {
                httpClient = new HttpClient();
                httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
                httpClient.setConnectTimeout(Config.connectionTimeOutMs);
                httpClient.setMaxConnectionsPerAddress(200);
                httpClient.setThreadPool(new QueuedThreadPool(250));
                httpClient.setTimeout(Config.responceWaitTimeoutMs);
                httpClient.start();

                while(!Config.stopFlag)
                {
                    Thread.sleep(100);

                    for(CHECK_SERVER cs : Config.CheckServers)
                    {
                        if(cs.vm_status == SCANVM_STATE.NEED_STOP)
                        {
                            if(cs.runningJobs == 0) cs.vm_status = SCANVM_STATE.MAY_STOP;
                        }
                    }

                    if(scanJobsQueue.isEmpty()) continue;
                    
                    SCAN_SESSION_JOB ssj = scanJobsQueue.get(0);
                    if(ssj != null)
                    {
                        if(ssj.avCount == 0)
                        {
                            log.debug("removing scan job "+ssj.SSJID);
                            scanJobsQueue.remove(0);
                            continue;
                        }
                        
                        log.debug("Total scan jobs = "+scanJobsQueue.size());
                        log.debug("processing job "+ssj.SSJID);
                        for(CHECK_SERVER cs : Config.CheckServers)
                        {
                            if(cs.vm_status == SCANVM_STATE.NEED_STOP)
                            {
                                if(cs.runningJobs == 0) cs.vm_status = SCANVM_STATE.MAY_STOP;
                                continue;
                            }

                            if(cs.runningJobs == 4 || cs.status != SERVER_STATUS.FREE || cs.vm_status != SCANVM_STATE.RUNNING)
                            {
                                if(isDebugEnabled) log.debug("Server "+cs.ip+" busy, continue search.");
                                continue;
                            }
                            
                            for(int i=0; ssj.avNamesHashs[i] != 0; i++)
                            {
                                if(ssj.avNamesHashs[i] == -1) continue;

                                for(int j=0; cs.avNamesHashs[j] != 0; j++)
                                {
                                    if(cs.avNamesHashs[j] == ssj.avNamesHashs[i])
                                    {
                                        if(isDebugEnabled)
                                        {
                                            log.debug("avhash is identical, create StartScan request.");
                                            log.debug(ssj.SSJID);
                                        }
                                        
                                        SCAN_JOB sj = new SCAN_JOB(ssj.filePath,
                                                                cs.ip,
                                                                ssj.avNames[i],
                                                                Utils.currentTimeStamp(),
                                                                0,
                                                                ssj.SSJID,
                                                                cs);

                                        ssj.avNamesHashs[i] = -1;
                                        ssj.status = SESSION_JOB_STATUS.PROCESSING;
                                        ssj.avCount--;

                                        sj.setMethod("POST");
                                        sj.setURL("http://"+sj.ip+"/StartScan/"+sj.avName);
                                        File uploadFile = new File(sj.filePath);
                                        sj.setFileForUpload(uploadFile);
                                        sj.setRequestHeader("FileName", uploadFile.getName());
                                        httpClient.send(sj);
                                        cs.status = cs.status.BUSY;
                                        cs.runningJobs++;

                                        break;
                                    }
                                }

                                if(cs.runningJobs == 4)
                                {
                                    if(isDebugEnabled)
                                    {
                                        log.debug("Server "+cs.ip+" is busy, runningJobs="+cs.runningJobs);
                                    }
                                    break;
                                }
                            }
                        }

                        if(Config.CheckServers.isEmpty())
                        {
                            log.error("ERROR: Check servers is empty.");
                        }
                    }
                }

                break;
            }
            catch(Exception e)
            {
                log.error("EXCEPTION",e);
                try
                {
                    if(httpClient != null) httpClient.stop();
                }
                catch(Exception e2){}
            }
        }
    }

    public static boolean add(SCAN_SESSION_JOB ssj)
    {
        boolean rez = scanJobsQueue.add(ssj);
        ssj.QueueNum = scanJobsQueue.indexOf(ssj);
        return rez;
    }
    
    public static SCAN_SESSION_JOB getSessionJob(String ssjID)
    {
        SCAN_SESSION_JOB ssj = null;
        long searchID = Long.parseLong(ssjID,16);

        ListIterator iter = scanJobsQueue.listIterator();
        while(iter.hasNext())
        {
            ssj = (SCAN_SESSION_JOB)iter.next();
            if(ssj.SSJID == searchID) return ssj;
        }
        
        return null;
    }



    public class SCAN_JOB extends ContentExchange
    {
        private static final String ErrorCheckRespons = "<%s><result><![CDATA[ERROR]]></result></%s>";
	SCAN_JOB_STATUS jobStatus = SCAN_JOB_STATUS.PROCESSING;
        String ip;
	String avName;
        String filePath;
	long startTime;

	long SSJID;
        ScanQueue.CHECK_SERVER cs;
        private String respData;
        private final Object criticalSection = new Object();

        SCAN_JOB(String filePath,String ip,String avName,long startTime,long SID,long SSJID,ScanQueue.CHECK_SERVER cs)
        {
            super();

            this.filePath = filePath;
            this.ip = ip;
            this.avName = avName;
            this.startTime = startTime;
            this.SSJID = SSJID;
            this.cs = cs;
            this.respData = String.format(ErrorCheckRespons,avName,avName);
        }

        public String getCheckResult()
        {
            String tmp = "";
            synchronized(criticalSection)
            {
                if(respData != null)
                {
                    tmp = respData;
                    respData = null;
                }
            }

            return tmp;
        }

        private void jobComplete()
        {
            saveCheckResultToCache(respData);
            cs.runningJobs--;
        }

        @Override
        protected void onResponseComplete() throws IOException
        {
            super.onResponseComplete();
            switch(getResponseStatus())
            {
                case 200:
                {
                    respData = getResponseContent();
                    jobStatus = jobStatus.FINISHED_OK;
                    break;
                }
                default:
                {
                    log.error("ERROR - get unknown responce status : "+getResponseStatus());
                    jobStatus = jobStatus.FINISHED_ERROR;
                }
            }

            jobComplete();
        }

        @Override
        protected void onConnectionFailed(Throwable x)
        {
            jobComplete();
            super.onConnectionFailed(x);
            log.error("onConnectionFailed",x);
            jobStatus = jobStatus.FINISHED_ERROR;
        }

        @Override
        protected void onException(Throwable x)
        {
            jobComplete();
            super.onException(x);
            log.error("onException",x);
            jobStatus = jobStatus.FINISHED_ERROR;
        }

        @Override
        protected void onExpire()
        {
            jobComplete();
            super.onExpire();
            log.error("onExpire");
            jobStatus = jobStatus.FINISHED_ERROR;
        }

        private void saveCheckResultToCache(String data)
        {
            FileOutputStream out = null;
            
            try
            {
                String reportDirName = Config.ReportsCachePath+"/"+Long.toHexString(SSJID);
                new File(reportDirName).mkdir();

                File reportFile = new File(reportDirName+"/"+Integer.toHexString(Math.abs(rnd.nextInt())));
                reportFile.createNewFile();
                reportFile.setReadable(false);
                out = new FileOutputStream(reportFile);
                if(data != null)
                {
                    out.write(data.getBytes());
                }
                else
                {
                    out.write(String.format("<%s><result><![CDATA[ERROR]]></result></%s>",
                            this.avName,this.avName).getBytes());
                }
                out.flush();
                out.close();
                reportFile.setReadable(true);
            }
            catch(Exception e)
            {
                log.error("EXCEPTION",e);
                try
                {
                    if(out != null) out.close();
                }
                catch(Exception e2){}

            }
        }

    }

   

}
