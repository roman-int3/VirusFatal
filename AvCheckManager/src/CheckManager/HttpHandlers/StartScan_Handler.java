package CheckManager.HttpHandlers;

import CheckManager.AvCheckManager;
import CheckManager.CommonObjects.SCAN_JOB;
import CheckManager.CommonObjects.SCAN_STATUS;
import CheckManager.Config;
import CheckManager.ScanManager.ScanManager;
import CheckManager.Utils.Common;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;


public class StartScan_Handler implements HttpHandler
{
    private static final            Logger log           = Logger.getLogger(StartScan_Handler.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();
    public void handle(HttpExchange httpExchange) throws IOException
    {
        try
        {
            if(isDebugEnabled)
            {
                log.debug(Common.logHttpRequest(httpExchange));
            }

            String uriPath = httpExchange.getRequestURI().getPath().toLowerCase();
            if(!httpExchange.getRequestMethod().equalsIgnoreCase("POST")) throw new Exception("Invalid Request Method: " + uriPath);
            String[] Request = uriPath.split("/");

            if(Request.length < 3) throw new Exception("Invalid path: " + uriPath);
            if(!Request[1].equalsIgnoreCase("StartScan"))
                throw new Exception("Invalid function call: " + Request[1]);

            String fileName = Common.getHeaderValue("FileName",httpExchange.getRequestHeaders());
            if(fileName.isEmpty()) throw new Exception("FileName header missing");

            long timestamp = Common.currentTimeStampInMillis();
            String path;
            int rndID;
            do
            {
               rndID = Config.rand.nextInt();
               path = String.format("%s\\%08X\\%08X",
                                        Config.mcUploadDir,
                                        timestamp,
                                        rndID);

            }while(!new File(path).mkdirs());

            String upFile = path+"\\"+fileName;
            File uploadedFile = new File(upFile);
            if(!uploadedFile.createNewFile()) throw new Exception("Failed to create uploaded file");
            FileOutputStream outFile = new FileOutputStream(uploadedFile);

            byte[] dataBuf = new byte[4096];
            InputStream body = httpExchange.getRequestBody();
            int readSize = 0;

            while(true)
            {
                int rSize = body.read(dataBuf);
                if(rSize == -1) break;
                if(rSize == 0) continue;
                outFile.write(dataBuf,0,rSize);
                readSize += rSize;
            }

            outFile.flush();
            outFile.close();
            outFile = null;


            String[] selAVs = new String[1];

            selAVs[0] = Request[2];
            SCAN_JOB job = AvCheckManager.scanManager.addScanJob(upFile,selAVs);

            String responce;
            while(true)
            {
                Thread.sleep(100);
                responce = AvCheckManager.scanManager.getScanResults(path, "");
                if(!responce.isEmpty() || job.status == SCAN_STATUS.SCAN_FINISHED_ERROR) break;
                if(job.exec != null && job.exec.scan_status == SCAN_STATUS.SCAN_FINISHED_ERROR) break;
            }

            DataOutputStream out = new DataOutputStream(httpExchange.getResponseBody());
            httpExchange.sendResponseHeaders(200,0);
            out.writeBytes(responce);

            if(isDebugEnabled)
            {
              log.debug(Common.logResponce(httpExchange,""));
            }

            out.flush();
            out.close();
            out = null;
        }
        catch (Throwable e)
        {
            httpExchange.sendResponseHeaders(404,0);
            log.error(e);
        }
        finally
        {
            httpExchange.getResponseBody().close();
        }
    }

    
}

