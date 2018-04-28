package scanBalancer;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Formatter;

public class FileInfo
{
    private static final double LN2 = 0.693147180559945309417232121458177;
    private static final Runtime runTime = Runtime.getRuntime();
    
    public static String calcHash(MessageDigest algorithm,String fileName) throws Exception
    {
        FileInputStream     fis = new FileInputStream(fileName);
        BufferedInputStream bis = new BufferedInputStream(fis);
        DigestInputStream   dis = new DigestInputStream(bis, algorithm);

        while (dis.read() != -1);

        byte[] hash = algorithm.digest();
        
        dis.close();
        bis.close();
        fis.close();

        return byteArray2Hex(hash);
    }

    private static String byteArray2Hex(byte[] hash) 
    {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static String getSHA1(String fileName)  throws Exception
    {
        return calcHash(MessageDigest.getInstance("SHA1"), fileName);
    }

    public static String getMD5(String fileName)  throws Exception
    {
        return calcHash(MessageDigest.getInstance("MD5"), fileName);
    }
    
    public static double getEntropy(String filePath) throws Exception
    {
        File    file           = new File(filePath);
        FileInputStream in  = null;
        double  entropy      = 0;
        byte[]  numr         = new byte[256];
        int     fSize           = (int)file.length();
        double  dS           = 0;	
	long    NumBytes=0;
	long	Num256Read = 0;
	double	dbyte;
	int bufIndex = 0;

        try
        {
            in = new FileInputStream(file);
            byte[] buff = new byte[fSize];
            in.read(buff, 0, fSize);
            ByteBuffer bb = ByteBuffer.wrap(buff);
                       
            
            while(fSize != 0)
            {
                ;
                if(fSize > 256)
                {
                    NumBytes = 256;
                    fSize -= 256;
                }
                else
                {
                    NumBytes = fSize;
                    fSize = 0;
                }
                
                dS = 0;
                
                
                for(int i = 0; i < NumBytes; i++)
                {
                    ++numr[bb.get(bufIndex+i) & 0xFF];
                }
                
                for (int i = 0; i < 256; ++i)
		{
                    if (numr[i] != 0)
                    {	
                        dbyte = ((double)(numr[i]& 0xFF)) / 256;
			dS -= dbyte * Math.log(dbyte) / LN2;
			numr[i] = 0;
                    }
		}

                ++Num256Read;

		entropy += dS;
		bufIndex += NumBytes;
            }
            
      
        }
        finally
        {
            if(in != null) in.close();
        }

        return entropy / Num256Read;
    }
    
    public static String getPackedFlagByEntropy(double entropy)
    {
             if(entropy <= 5.5) return "not packed";
        else if(entropy <= 6.0) return "hardly packed";
        else if(entropy <= 7.0) return "partial packed";
        else if(entropy <= 7.2) return "maybe packed";
        else                    return "packed";
    }

    public static String getTrIdInfo(String filePath) throws Exception
    {
        System.out.println(Config.rootPath+"/AdditionalSoft/TrID/trid_w32/trid.exe "+filePath);
        Process ps;

        if(System.getProperty("os.name").toLowerCase().contains("windows" ))
        {
            ps = runTime.exec("E:\\DEV\\Development\\PROJECTS\\VirusFatal\\AdditionalSoft\\TrID\\trid_w32\\trid.exe "+filePath);
        }
        else
        {
            ps = runTime.exec(new String[]{"wine", Config.rootPath+"AdditionalSoft/TrID/trid_w32/trid.exe",filePath});
        }

        InputStream is = ps.getInputStream();
        InputStream erris = ps.getErrorStream();
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        String results;
        Reader reader = new BufferedReader(new InputStreamReader(is));
        Reader err_reader = new BufferedReader(new InputStreamReader(erris));

        try
        {
            int n;
            while ((n = reader.read(buffer,0,1024)) != -1) {
                    writer.write(buffer, 0, n);
            }

            results = writer.toString();
            if(results.isEmpty())
            {
                while ((n = err_reader.read(buffer,0,1024)) != -1) {
                    writer.write(buffer, 0, n);
                }
                results = writer.toString();

                throw new Exception("getTrIdInfo::Empty results: "+results);
            }
        }
        finally
        {
            is.close();
            erris.close();
            reader.close();
            err_reader.close();
            writer.close();
        }
            
        System.out.println("resuylt = "+results);
            
        results = results.substring(results.indexOf(filePath)+filePath.length());
        results = results.trim();
        results = results.replaceAll("\n", "<br>");
        return results;
    }
    
    public static String getSigCheckInfo(String filePath) throws Exception
    {
        System.out.println(Config.rootPath+"/AdditionalSoft/Sigcheck/trid_w32/sigcheck.exe "+filePath);
        Process ps;
        String fName;

        if(System.getProperty("os.name").toLowerCase().contains("windows" ))
        {
            ps = runTime.exec("E:\\DEV\\Development\\PROJECTS\\VirusFatal\\AdditionalSoft\\Sigcheck\\sigcheck.exe "+filePath);
            fName = filePath.substring(filePath.lastIndexOf("\\")+1);
        }
        else
        {
            ps = runTime.exec(new String[]{"wine", Config.rootPath+"AdditionalSoft/Sigcheck/sigcheck.exe",filePath});
            fName = filePath.substring(filePath.lastIndexOf("/")+1);
        }

        InputStream is = ps.getInputStream();
        InputStream erris = ps.getErrorStream();
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        String results;
        Reader reader = new BufferedReader(new InputStreamReader(is));
        Reader err_reader = new BufferedReader(new InputStreamReader(erris));

        try
        {
            int n;
            while ((n = reader.read(buffer,0,1024)) != -1) {
                    writer.write(buffer, 0, n);
            }

            results = writer.toString();
            if(results.isEmpty())
            {
                while ((n = err_reader.read(buffer,0,1024)) != -1) {
                    writer.write(buffer, 0, n);
                }
                results = writer.toString();

                throw new Exception("getSigCheckInfo::Empty results: "+results);
            }
        }
        finally
        {
            is.close();
            erris.close();
            reader.close();
            err_reader.close();
            writer.close();
        }

        System.out.println("result = "+results);

        results = results.substring(results.indexOf(fName)+fName.length()+3);
        results = results.trim();
        results = results.replaceAll("\n", "<br>");
        return results;
    }

}
