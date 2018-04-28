package scanBalancer;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Map;

public class Utils
{
     public static String logHttpRequest(HttpExchange httpExchange)
    {

        Headers headers = httpExchange.getRequestHeaders();
        String sHeaders = "ID: "+ httpExchange.hashCode() +"\r\n" +
                httpExchange.getRequestMethod() + " " +
                httpExchange.getRequestURI().getPath() + " " +
                httpExchange.getProtocol() +"\r\n";
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
        String sHeaders = "ID: "+ httpExchange.hashCode() + "\r\n" +
                httpExchange.getResponseCode() +"\r\n";
        for (Map.Entry<String, List<String>> e : headers.entrySet())
        {
            sHeaders += e.getKey() + " : " + e.getValue().get(0) + "\r\n";

        }

        sHeaders += Responce + "\r\n";
        return sHeaders;

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

    public static long currentTimeStamp()
    {
        return System.currentTimeMillis()/1000;
    }

    public static boolean delAllFiles(File path)
    {
        if(!path.isDirectory())
        {
            return path.delete();
        }

        boolean result = false;
        File files[] = path.listFiles();
        for(File file:files)
        {
            result = delAllFiles(file);
        }

        return result;
    }

    public static void saveToFile(InputStream InStream,String fileName,long fSize) throws Exception 
    {
        FileOutputStream outFileStream = null;
        ReadableByteChannel inChannel = null;

        try
        {
            File outFile = new File(fileName);
            outFileStream = new FileOutputStream(outFile);
            inChannel = Channels.newChannel(InStream);
            outFileStream.getChannel().transferFrom(inChannel, 0, fSize);

        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        finally
        {
            if(outFileStream != null) outFileStream.close();
            if(inChannel != null) inChannel.close();
        }
    }

    public static void XorCrypt(ByteBuffer data,int dataSize,byte XorKey)
    {
        for(int i=0; i < dataSize; i++)
        {
            byte xorData = data.get(i);
            xorData ^= XorKey;
            data.put(i,xorData);
        }

    }
}
