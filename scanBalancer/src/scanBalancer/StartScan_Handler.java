package scanBalancer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Random;
import org.apache.log4j.Logger;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// void StartScan(selAVs,filedata)
public class StartScan_Handler implements HttpHandler
{
    private static final            Logger log          = Logger.getLogger(StartScan_Handler.class);
    private static final boolean    isDebugEnabled      = log.isDebugEnabled();
    private static final Random     rnd = new Random(Utils.currentTimeStamp());
    private static final String     ResponceXml         = "<responce><queue>%d</queue><ssjID>%x</ssjID><fileInfo>"+
"<TrID><![CDATA[%s]]></TrID>"+
"<SigCheck><![CDATA[%s]]></SigCheck>"+
"<Entropy>%s</Entropy>"+
"<md5>%s</md5>"+
"<sha1>%s</sha1>"+
"<filesize>%s</filesize>"+
"</fileInfo>"+
"</responce>";
    
    private static final String     ResponceExtNotAllowXml         = "<responce><status code=\"err\">"+
                                                                        "<err_msg type=\"user\">Scan cancelled. This file type not allowed.</err_msg>"+
                                                                        "</status>"+
                                                                        "</responce>";

    private static final String     GeneralErrorXml         = "<responce><status code=\"err\"/></responce>";

    public void handle(HttpExchange httpExchange) throws IOException
    {
        try
        {
            if(isDebugEnabled)
            {
                log.debug(Utils.logHttpRequest(httpExchange));
            }


            String uriPath = httpExchange.getRequestURI().getPath().toLowerCase();
            if(!httpExchange.getRequestMethod().equalsIgnoreCase("PUT"))
                throw new Exception("Invalid Request Method: " + uriPath);
            String[] Request = uriPath.split("/");

            if(Request.length < 3) throw new Exception("Invalid path: " + uriPath);
            if(!Request[1].equalsIgnoreCase("StartScan"))
                throw new Exception("Invalid function call: " + Request[1]);
            
            Headers RequestHeaders = httpExchange.getRequestHeaders();
            String bodyLength = Utils.getHeaderValue("Content-Length",RequestHeaders);
            if(bodyLength.isEmpty()) throw new Exception("bodyLength empty.");
            int totalSize = Integer.parseInt(bodyLength);
            
            String FileType = Utils.getHeaderValue("FileType",RequestHeaders);
            String uploadedFilePath = "";
            InputStream body = httpExchange.getRequestBody();

            if(FileType.equalsIgnoreCase("data"))
            {
                String FileName = Utils.getHeaderValue("FileName",RequestHeaders);
                uploadedFilePath = File.createTempFile(FileName, 
                                                Integer.toString(rnd.nextInt()), 
                                                new File(Config.UploadFilesDir)).getAbsolutePath();

                Utils.saveToFile(body,uploadedFilePath, totalSize);
            }
            else if(FileType.equalsIgnoreCase("fsid"))
            {
                byte[] dataBuf = new byte[totalSize];
                int readSize = 0;
                while(true)
                {
                    int rSize = body.read(dataBuf, readSize, totalSize-readSize);
                    if(rSize == -1) break;
                    if(rSize == 0) continue;
                    readSize += rSize;
                }

                if(readSize != totalSize) throw new Exception("readSize != totalSize");
                
                uploadedFilePath = FileStore.getPath(new String(dataBuf));
            }
            else
            {
                throw new Exception("Unknown FileType header : "+FileType);
            }

            String responce = "";
            DataOutputStream out = new DataOutputStream(httpExchange.getResponseBody());
            boolean IsExtAllow = true;
            String TrIdInfo = FileInfo.getTrIdInfo(uploadedFilePath);
            log.debug(TrIdInfo);
            if(!Config.allowFileTypes.isEmpty())
            {
                IsExtAllow = false;
                String TrFileInfo = TrIdInfo.toLowerCase();
                for(String ext : Config.allowFileTypes)
                {
                    if(TrFileInfo.contains(ext))
                    {
                        IsExtAllow = true;
                        break;
                    }
                }
            }

            if(!IsExtAllow)
            {
                responce = ResponceExtNotAllowXml;
                new File(uploadedFilePath).delete();
            }
            else
            {
                ScanQueue.SCAN_SESSION_JOB ssj = new ScanQueue.SCAN_SESSION_JOB();
            
                ssj.filePath        = uploadedFilePath;
                ssj.status          = ssj.status.PENDING;
                ssj.avCount         = Request.length-2;
                ssj.avNames         = new String[ssj.avCount];
                ssj.avNamesHashs    = new int[ssj.avCount+1];
                for(int i=2,j=0; j < ssj.avCount; i++,j++)
                {
                    ssj.avNames[j] = Request[i].toLowerCase();
                    ssj.avNamesHashs[j] = ssj.avNames[j].hashCode();

                    if(isDebugEnabled)
                    {
                        log.debug("Processing new avname "+ssj.avNames[j]+" avhash "+ssj.avNamesHashs[j]);
                    }
                }

                ssj.SSJID       = Math.abs(rnd.nextLong());
                ssj.startTime   = Utils.currentTimeStamp();
                    
                if(!ScanQueue.add(ssj))
                {
                    responce = GeneralErrorXml;
                }
                else
                {
                    String SigCheck = FileInfo.getSigCheckInfo(uploadedFilePath);
                    String md5 = FileInfo.getMD5(uploadedFilePath).toUpperCase();
                    String sha1 = FileInfo.getSHA1(uploadedFilePath).toUpperCase();
                    double entropy = FileInfo.getEntropy(uploadedFilePath);
                    String Entropy = String.format("%.2f", entropy);
                    Entropy += " ("+FileInfo.getPackedFlagByEntropy(entropy)+")";

                    responce = String.format(ResponceXml,
                                            ssj.QueueNum,
                                            ssj.SSJID,
                                            TrIdInfo,
                                            SigCheck,
                                            Entropy,
                                            md5,
                                            sha1,
                                            new File(uploadedFilePath).length()+" bytes");
                }
            }

            httpExchange.sendResponseHeaders(200,0);
            out.write(Charset.forName("UTF-8").encode(responce).array());

            if(isDebugEnabled)
            {
                log.debug(Utils.logResponce(httpExchange,""));
            }

            out.flush();
            out.close();
            out = null;
            
        }
        catch (Throwable e)
        {
            httpExchange.sendResponseHeaders(404,0);
            log.error("Exception",e);
        }
        
        httpExchange.getResponseBody().close();
    }
    
    
}

