package CheckManager.HttpHandlers;

import CheckManager.AvCheckManager;
import CheckManager.Config;
import CheckManager.ScanManager.ScanManager;
import CheckManager.ScanSession.SessionID;
import CheckManager.Utils.Common;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.DataOutputStream;
import java.io.IOException;
import org.apache.log4j.Logger;

public class ScanResult_Handler implements HttpHandler
{
    private static final            Logger log           = Logger.getLogger(ScanResult_Handler.class);
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
            if(!httpExchange.getRequestMethod().equalsIgnoreCase("GET"))
                throw new Exception("Invalid Request Method: " + uriPath);

            String[] Request = uriPath.split("/");

            if(Request.length < 3) throw new Exception("Invalid path: " + uriPath);
            if(!Request[1].equalsIgnoreCase("ScanResult"))
                throw new Exception("Invalid function call: " + Request[1]);

            SessionID sid = new SessionID(Config.acUploadDir,Config.mcUploadDir);
            String checkPath = sid.buildPatchFromSID(Request[2]);
            if(checkPath.isEmpty()) throw new Exception("Invalid SessionID: " + Request[2]);

            String result = AvCheckManager.scanManager.getScanResults(checkPath,Request.length > 3?Request[3]:"");

            DataOutputStream out = new DataOutputStream(httpExchange.getResponseBody());

            httpExchange.sendResponseHeaders(200,0);
            result = "<responce>" + result + "</responce>";

            if(isDebugEnabled)
            {
                log.debug(Common.logResponce(httpExchange,result));
            }

            out.writeBytes(result);
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