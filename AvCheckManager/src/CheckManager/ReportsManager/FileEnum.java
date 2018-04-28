package CheckManager.ReportsManager;


import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.apache.log4j.Logger;


public class FileEnum implements FileFilter
{
    private static final            Logger log           = Logger.getLogger(FileEnum.class);
    private File rootFDir;
    private File[] fileArr;
    private String fExt;
    private int index=0;
    private String encoding;
    private String fileNamePattern;
    private int read_startOff,read_length;


    public FileEnum(String filePath,String encoding,int startOff,int length)
    {
        this.encoding = encoding;
        this.read_length = length;
        this.read_startOff = startOff;

        if(filePath.contains("*."))
        {
            String rootDir = filePath.substring(0,filePath.lastIndexOf("\\")+1);
            fExt = filePath.substring(filePath.lastIndexOf("*.")+1);
            rootFDir = new File(rootDir);
            fileNamePattern = null;
            fileNamePattern = filePath.substring(filePath.lastIndexOf("\\")+1,
                    filePath.lastIndexOf("*."));
            log.debug("fileNamePattern = "+ fileNamePattern );

            fileArr = rootFDir.listFiles(this);
        }
        else
        {
            fileArr = new File[1];
            fileArr[0] = new File(filePath);
        }
      
    }

    public boolean accept(File file)
    {
        log.debug(file.getAbsolutePath());
        if(fileNamePattern != null)
        {
            if(!file.getName().contains(fileNamePattern)) return false;
        }
        log.debug(fExt);
        if(fExt.contentEquals(".*")) return true;
        return file.getName().contains(fExt);
    }

    public boolean hasNext()
    {
        return (index < fileArr.length);
    }

    public String nextFileData()
    {
        FileInputStream is = null;
        try
        {
            File curFile = fileArr[index];
            index++;

            log.debug(curFile.getAbsolutePath());

            is = new FileInputStream(curFile);
            ByteBuffer[] bb = new ByteBuffer[1];
            bb[0] = ByteBuffer.allocate((this.read_length==0)?(int)curFile.length():this.read_length);
            if(this.read_length == 0)
                is.getChannel().read(bb,0,(int)curFile.length());
            else
                is.getChannel().read(bb,this.read_startOff,this.read_length);
            is.close();

            bb[0].position(0);
            if(encoding != null)
            {
                CharBuffer outBuf = Charset.forName(encoding).decode(bb[0]);
                return new String(outBuf.array());

            }

            return new String(bb[0].array());
        }
        catch(Exception e)
        {
            log.error(e);
            if(is != null) try{is.close();} catch(Exception e2){};
        }

        return null;
    }

    public String getCurrentFilePath()
    {
        int i = index-1;
        if(i >= fileArr.length) return null;
        return fileArr[i].getAbsolutePath();
    }



}
