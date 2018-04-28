package CheckManager.Utils;


import CheckManager.CommonObjects.EXEC_FLAG_TYPE;
import CheckManager.CommonObjects.EXEC_STRUCT;
import CheckManager.Config;
import CheckManager.JNI;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;



public class Common
{
    private static final            Logger log           = Logger.getLogger(Common.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();


    public static void copyFile(String sFile, String dFile) throws Exception
        {
            File sourceFile = new File(sFile);
            File destFile = new File(dFile);

            if(!destFile.exists())
                destFile.createNewFile();

            FileChannel source = null;
            FileChannel destination = null;

            try
            {
                source = new FileInputStream(sourceFile).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
                destination.force(false);
            }
            catch(Exception e)
            {
                log.error("Exception",e);
                if(source != null) source.close();
                if(destination != null) destination.close();

                throw new Exception(e);
            }

            if(source != null) source.close();
            if(destination != null) destination.close();
        }

        public static boolean createDirs(String Path) throws IOException
        {
            File dirs = new File(Path);
            return dirs.mkdirs();
        }

    public static String getFileFullPath(String rootDir, String fileName)
    {
        File searchDir = new File(rootDir);
        File Files[] = searchDir.listFiles();
        if(Files != null && Files.length > 0)
        {
            for(File file : Files)
            {
                if(file.isDirectory())
                {
                    String retData = getFileFullPath(file.getAbsolutePath(), fileName);
                    if(!retData.isEmpty()) return retData;
                }
                else
                {
                    if(file.getName().equalsIgnoreCase(fileName)) return rootDir+"\\"+fileName;
                }
            }
        }
        return "";
    }

    public static String getHeaderValue(String headerName,Headers headers)
    {
        String retValue = "";
        for (Map.Entry<String, List<String>> e : headers.entrySet())
        {
            if(e.getKey().equalsIgnoreCase(headerName))
            {
                retValue = e.getValue().get(0);
                break;
            }
        }
        return retValue;
    }

    public static String logHttpRequest(HttpExchange httpExchange)
    {

        Headers headers = httpExchange.getRequestHeaders();
        String sHeaders = "ID: "+ httpExchange.hashCode() +"\r\n" + httpExchange.getRequestMethod() +
                " " + httpExchange.getRequestURI().getPath() + " " + httpExchange.getProtocol() +"\r\n";
        for (Map.Entry<String, List<String>> e : headers.entrySet())
        {
            sHeaders += e.getKey() + " : " + e.getValue().get(0) + "\r\n";

        }
        sHeaders += "\r\n";
        return sHeaders;
    }

    public static String logResponce(HttpExchange httpExchange,String Responce)
    {

        Headers headers = httpExchange.getResponseHeaders();
        String sHeaders = "ID: "+ httpExchange.hashCode() + "\r\n" + httpExchange.getResponseCode() +"\r\n";
        for (Map.Entry<String, List<String>> e : headers.entrySet())
        {
            sHeaders += e.getKey() + " : " + e.getValue().get(0) + "\r\n";

        }

        sHeaders += Responce + "\r\n";
        return sHeaders;

    }

    public static long currentTimeStamp()
    {
        return System.currentTimeMillis()/1000;
    }

    public static long currentTimeStampInMillis()
    {
        return System.currentTimeMillis();
    }

    public static void DeleteFilesAndFolders(File path,FileFilter filter)
    {
        if(!path.exists()) return;
        File names[] = path.listFiles(filter);
        if(names == null) return;
        for(File name : names)
        {
            if(name.isDirectory())
            {
                DeleteFilesAndFolders(name, filter);
            }
           
            name.delete();
        }
    }

    public static String processTemplates(String sourceStr,EXEC_STRUCT exec)
    {
        if(sourceStr == null || sourceStr.isEmpty()) return sourceStr;

        String resultStr=sourceStr;

        if(exec != null)
        {
            if(resultStr.contains("{%SCAN_FILE_PATH_AND_NAME%}"))
            {

                resultStr = resultStr.replace("{%SCAN_FILE_PATH_AND_NAME%}",exec.szScanFilePath);
            }

            if(resultStr.contains("{%SCAN_FILE_NAME%}"))
            {
                resultStr = resultStr.replace("{%SCAN_FILE_NAME%}",
                    exec.szScanFilePath.substring(exec.szScanFilePath.lastIndexOf("\\")+1));
            }

            if(resultStr.contains("{%SCAN_FILE_PATH%}"))
            {
                resultStr = resultStr.replace("{%SCAN_FILE_PATH%}",
                    exec.szScanFilePath.substring(0,exec.szScanFilePath.lastIndexOf("\\")));
            }

        }
        
        
            if(resultStr.contains("{%AV_ROOT_PATH%}"))
            {
                resultStr = resultStr.replace("{%AV_ROOT_PATH%}", Config.avRootPath);
            }
        

        return resultStr;

    }


    public static String applyFilters(EXEC_STRUCT curExec,String saveData) throws Exception
    {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList lists  = (NodeList)xpath.compile("reportParser/filters_regex/item").
                                evaluate(curExec.avr.xmlRootNode,XPathConstants.NODESET);
        log.debug("lists.getLength() == "+lists.getLength());
        for(int i=0; i < lists.getLength();i++)
        {
            String regex = lists.item(i).getTextContent();
            log.debug(regex);
            saveData = Pattern.compile(regex,Pattern.CASE_INSENSITIVE|Pattern.MULTILINE).
                                    matcher(saveData).replaceAll("");
            log.debug(saveData);
        }

        lists  = (NodeList)xpath.compile("reportParser/addToLog/item").
                                evaluate(curExec.avr.xmlRootNode,XPathConstants.NODESET);
        log.debug("lists.getLength() == "+lists.getLength());
        for(int i=0; i < lists.getLength();i++)
        {
            saveData += lists.item(i).getTextContent();
        }

        return saveData;
    }

    public static void saveReport(EXEC_STRUCT curExec,String saveData) throws Exception
    {
        FileOutputStream fout = null;
        log.debug(curExec.szReportFileName);
        try
        {
            File outRep = new File(curExec.szReportFileName);
            outRep.createNewFile();
            fout = new FileOutputStream(outRep);
            fout.getChannel().write(ByteBuffer.wrap(saveData.getBytes()));
            fout.close();
        }
        finally
        {
            if(fout != null)
            {
                fout.close();
            }
        }
    }

    public static void flushScanOutput(EXEC_STRUCT exec)
    {
        boolean retResult = true;//false;

        if(exec.avr.AvCmdType == EXEC_FLAG_TYPE.CONSOLE)
        {
            log.debug("Flush output to "+exec.szReportFileName);
            JNI.writeProcessOutputData(exec.pInfo,exec.szReportFileName);
            retResult = true;
        }
    }

    public static String MD5(byte[] byteArr)
    {
        try
        {
            MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(byteArr);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; ++i)
            {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {}

        return null;
    }

   
}
