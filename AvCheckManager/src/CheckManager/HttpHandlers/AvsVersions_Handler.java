
package CheckManager.HttpHandlers;

import CheckManager.Utils.Common;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 *
 * GET /AvsVersions
 * GET /AvsVersions/md5
 */
public class AvsVersions_Handler implements HttpHandler
{

    private static final            Logger log           = Logger.getLogger(AvsVersions_Handler.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();

    public void handle(HttpExchange httpExchange) throws IOException
    {
        DataOutputStream out = null;
        FileInputStream in = null;
        try
        {
            if(isDebugEnabled)
            {
                log.debug(Common.logHttpRequest(httpExchange));
            }

            String uriPath = httpExchange.getRequestURI().getPath().toLowerCase();
            if(!httpExchange.getRequestMethod().equalsIgnoreCase("GET")) throw new Exception("Invalid Request Method: " + uriPath);
            
            File verFile = new File("avsversions.xml");
            if(!verFile.exists()) throw new Exception("avsversions.xml not exist.");
            in = new FileInputStream(verFile);
            out = new DataOutputStream(httpExchange.getResponseBody());
            byte[] responce = new byte[(int)verFile.length()];
            in.read(responce);
            httpExchange.sendResponseHeaders(200,0);

            String[] Request = uriPath.split("/");
            if(Request.length > 1)
            {
                String md5 = Common.MD5(responce);
                out.write(md5.getBytes());
            }
            else
            {
                out.write(responce, 0, (int)verFile.length());
            }

            if(isDebugEnabled)
            {
              log.debug(Common.logResponce(httpExchange,""));
            }

            out.flush();
            out.close();
            in.close();
        }
        catch(Exception e)
        {
            if(out != null) out.close();
            if(in != null) in.close();
            log.error(e);
            httpExchange.sendResponseHeaders(404,0);
        }
        finally
        {
            httpExchange.getResponseBody().close();
        }
    }

}
