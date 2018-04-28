

package CheckManager.HttpHandlers;

import CheckManager.AvCheckManager;
import CheckManager.Config;
import CheckManager.Utils.Common;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * POST /SetRole/scanner/ip/192.168.3.100
 */
public class SetRole_Handler implements HttpHandler
{
    private static final            Logger log           = Logger.getLogger(SetRole_Handler.class);
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

            if(Request.length < 4) throw new Exception("Invalid path: " + uriPath);
            if(!Request[1].equalsIgnoreCase("SetRole"))
                throw new Exception("Invalid function call: " + Request[1]);

            Document configDoc =  DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(Config.rootPath + "config.xml"));
            XPath xpath = XPathFactory.newInstance().newXPath();
            Node node = (Node)xpath.compile("//server_role").evaluate(configDoc, XPathConstants.NODE);
            node.setNodeValue("scanner");
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            Result result = new StreamResult(new File(Config.rootPath + "config.xml"));
            Source source = new DOMSource(configDoc);
            transformer.transform(source, result);

            httpExchange.sendResponseHeaders(200,0);
            httpExchange.getResponseBody().close();

            String[] cmd = {"cmd.exe", "/c",
                "netsh interface ip set address name=\"Local Area Connection\" static "+Request[3]+" 255.255.0.0 192.168.1.1 1"};
            Process ps = Runtime.getRuntime().exec(cmd);
            ps.waitFor();
            AvCheckManager.restart();


        }
        catch(Exception e)
        {
            log.error(e);
            httpExchange.sendResponseHeaders(404,0);
            httpExchange.getResponseBody().close();
        }
        

    }

}
