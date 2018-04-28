package scanBalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//responceXML getScanResult(sCheckFileID,sSessionCookie)


public class ScanResult_Handler implements HttpHandler
{
    private static final            Logger log      = Logger.getLogger(ScanResult_Handler.class);
    private static final boolean    isDebugEnabled  = log.isDebugEnabled();
    private static final String     ResponceXml     = "<responce><status>%s</status><queue>%d</queue><avresults>%s</avresults></responce>";


    public void handle(HttpExchange httpExchange) throws IOException
    {
        try
        {
            if(isDebugEnabled)
            {
                log.debug(Utils.logHttpRequest(httpExchange));
            }

            String uriPath = httpExchange.getRequestURI().getPath().toLowerCase();
            if(!httpExchange.getRequestMethod().equalsIgnoreCase("GET"))
                throw new Exception("Invalid Request Method: " + uriPath);

            String[] Request = uriPath.split("/");

            if(Request.length != 3) throw new Exception("Invalid path: " + uriPath);
            if(!Request[1].equalsIgnoreCase("ScanResult"))
                throw new Exception("Invalid function call: " + Request[1]);

            SESSION_COOKIE sCookie = new SESSION_COOKIE(Request[2]);
            String cookie = Utils.getHeaderValue("Cookie",httpExchange.getRequestHeaders());
            if(!cookie.isEmpty())
            {
                String[] tmp = cookie.split("SC=");
                if(tmp != null && tmp.length > 0)
                {
                    sCookie.decode(tmp[1]);
                }
            }

           
            ScanQueue.SCAN_SESSION_JOB ssj = ScanQueue.getSessionJob(Request[2]);
            if(ssj == null)
            {
                ssj = new ScanQueue.SCAN_SESSION_JOB();
                ssj.status = ssj.status.PROCESSED;
            }
            
            String finalResultStr = "";
            File reportDir = new File(sCookie.cacheFilesPath);
            File repFiles[] = reportDir.listFiles();
            if(repFiles != null && repFiles.length > 0)
            {
                String outResult = "";
            
                for(File repFile : repFiles)
                {
                    if(repFile.length() <= 0 || repFile.canRead() != true || sCookie.isExist(repFile.getName()) == true) continue;

                    FileInputStream in = new FileInputStream(repFile);
                    ByteBuffer bb = ByteBuffer.allocate((int)repFile.length());
                    in.getChannel().read(bb);
                    String rezStr = new String(bb.array());
                    sCookie.add(repFile.getName());
                    outResult = outResult.concat(rezStr);
                }
            
                finalResultStr = String.format(ResponceXml,ssj.status.name(),ssj.QueueNum,outResult);
                cookie = sCookie.encode();
                if(!cookie.isEmpty())
                {
                    httpExchange.getResponseHeaders().add("Set-Cookie","SC="+cookie);
                }

                if(isDebugEnabled)
                {
                    log.debug(Utils.logResponce(httpExchange,finalResultStr));
                }
            }
            
            DataOutputStream out = new DataOutputStream(httpExchange.getResponseBody());
            httpExchange.sendResponseHeaders(200,0);
            
            
            
            out.writeBytes(finalResultStr);
            out.flush();
            out.close();
        }
        catch (Throwable e)
        {
            httpExchange.sendResponseHeaders(404,0);
            log.error("Exception",e);
        }
        finally
        {
            httpExchange.getResponseBody().close();
        }
    }
}