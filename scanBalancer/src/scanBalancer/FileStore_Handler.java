package scanBalancer;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import org.apache.log4j.Logger;

/**
 * FileStore_Handler
 *
 * POST /FileStore
 * FileName: filename
 * HTTP-BODY = filedata
 * return filestore_path_id
 *
 * DELETE /FileStore/filestore_path_id
 * 
 */
public class FileStore_Handler  implements HttpHandler
{
    private static final            Logger log          = Logger.getLogger(StartScan_Handler.class);
    private static final String addFileResponceXML = "<responce><fsid>%s</fsid></responce>";


    public void handle(HttpExchange httpExchange) throws IOException
    {
        try
        {
            Headers RequestHeaders = httpExchange.getRequestHeaders();
            String FileName = Utils.getHeaderValue("FileName",RequestHeaders);
            String uriPath = httpExchange.getRequestURI().getPath().toLowerCase();
            if(httpExchange.getRequestMethod().equalsIgnoreCase("DELETE"))
            {
                String[] uriArr = uriPath.split("/");
                if(uriArr.length != 3) throw new Exception("Invalid Request Method: " + uriPath);
                FileStore.delFile(uriArr[2]);
            }
            else if( ! httpExchange.getRequestMethod().equalsIgnoreCase("PUT"))
            {
                throw new Exception("Invalid Request Method: " + uriPath);
            }
            else
            {
                int bodyLength = Integer.parseInt(Utils.getHeaderValue("Content-Length",RequestHeaders));
                if(bodyLength == 0) throw new Exception("bodyLength empty.");
                InputStream body = httpExchange.getRequestBody();
                String fsid = FileStore.addFile(body, FileName, bodyLength);
                String responce = String.format(addFileResponceXML,fsid);

                DataOutputStream out = new DataOutputStream(httpExchange.getResponseBody());
                httpExchange.sendResponseHeaders(200,0);
                out.writeBytes(responce);
                out.flush();
                out.close();
                out = null;
            }
        }
        catch (Throwable e)
        {
            httpExchange.sendResponseHeaders(404,0);
            log.error("Exception",e);
        }

        httpExchange.getResponseBody().close();
    }

    
}
