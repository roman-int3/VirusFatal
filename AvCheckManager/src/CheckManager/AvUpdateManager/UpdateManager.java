

package CheckManager.AvUpdateManager;


import CheckManager.CommonObjects.AV_EXEC_RECORD;
import CheckManager.CommonObjects.EXEC_FLAG_TYPE;
import CheckManager.CommonObjects.EXEC_STRUCT;
import CheckManager.CommonObjects.UPDATE_STATUS;
import CheckManager.Config;
import CheckManager.Config.SERVER_ROLE;
import CheckManager.JNI;
import CheckManager.uiAutomation.UiAutomation;
import CheckManager.Utils.Common;
import com.almworks.sqlite4java.SQLiteBusyException;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import com.sun.org.apache.xpath.internal.NodeSet;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UpdateManager implements Runnable
{

    private static final            Logger log           = Logger.getLogger(UpdateManager.class);

    private ArrayList    <AV_EXEC_RECORD>     avsInfo;
    Preferences prefs;
    Preferences root = Preferences.userRoot();

    XPath xpath = XPathFactory.newInstance().newXPath();

    public static void testFunc()
    {
        System.out.println("testFunc");
    }


    public static void main(String args[]) throws Exception
    {
        String fName = "testFunc";


        DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        long timestamp = Common.currentTimeStampInMillis();
        Date date = new Date(timestamp);
        System.out.println(dfm.format(date));
        new UpdateManager(null).parseVersion(null,null);
    }

    public UpdateManager(ArrayList    <AV_EXEC_RECORD>     avsInfo)
    {
        this.avsInfo = avsInfo;
        prefs = root.node("updatemanager");
    }

    public void start()
    {
        new Thread(this).start();
    }

    public void run()
    {
        Config.totalRunThreads++;
        while(!Config.stopFlag)
        {
            try
            {
                Thread.sleep(10000);

                // Check for update av db
                for(AV_EXEC_RECORD av : avsInfo)
                {
                    if(update(av))
                    {
                        String verInfoStr = getVersion(av);
                        parseVersion(av,verInfoStr);
                    }
                }

               }
                catch (Exception e)
                {
                    log.error(e);

                }
            }
        Config.totalRunThreads--;
    }

    String getVersion(AV_EXEC_RECORD av) throws Exception
    {
        UiAutomation automation = new UiAutomation();
        Node rootNode = (Node)xpath.compile("updater/versionInfo/getVerInfo/automation").evaluate(av.xmlRootNode,XPathConstants.NODE);
        EXEC_STRUCT curExec = new EXEC_STRUCT(av);
        long timer = Common.currentTimeStampInMillis();
        while(Common.currentTimeStampInMillis() - timer < 5*60*1000)
        {
            automation.automate(curExec, rootNode,rootNode, null);
            if(automation.isAutomationFinished) break;
            Thread.sleep(1000);
        }

        return curExec.grabbedText.toString();
    }

    void parseVersion(AV_EXEC_RECORD av,String versionData) throws Exception
    {
        File avVerXmlFile = new File( "avsversions.xml");
        
        Document avVerXmlDoc = null;
        if(avVerXmlFile.exists())
        {
            avVerXmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(avVerXmlFile);
        }
        else
        {
            avVerXmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        }
        
        Element avNode = (Element)xpath.compile("//av[@name='"+"av.szAvName']").evaluate(avVerXmlDoc,XPathConstants.NODE);
        if(avNode == null)
        {
            avNode = avVerXmlDoc.createElement("av");
            avVerXmlDoc.insertBefore(avNode, null);
        }

        String dbVersion = "";
	String softVersion = "";
	String lastUpdate = "";
	String engineVersion = "";
	String softName = "";

        Node verInfoRegExNode = (Node)xpath.compile("updater/versionInfo/verInfoParser/verInfoRegEx").evaluate(av.xmlRootNode,XPathConstants.NODE);
        if(verInfoRegExNode != null)
        {
            dbVersion = Common.processTemplates(
                    (String)xpath.compile("dbVersion").evaluate(verInfoRegExNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            softVersion = Common.processTemplates(
                    (String)xpath.compile("softVersion").evaluate(verInfoRegExNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            lastUpdate = Common.processTemplates(
                    (String)xpath.compile("lastUpdate").evaluate(verInfoRegExNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            engineVersion = Common.processTemplates(
                    (String)xpath.compile("engineVersion").evaluate(verInfoRegExNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            softName = Common.processTemplates(
                    (String)xpath.compile("softName").evaluate(verInfoRegExNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            
            dbVersion = regexGetResults(dbVersion,versionData);
            softVersion = regexGetResults(softVersion,versionData);
            //dateType="timestamp" queryType="xpath" fromFile
            if(!lastUpdate.isEmpty())
            {
                String queryType = (String)xpath.compile("lastUpdate/@queryType").evaluate(verInfoRegExNode,XPathConstants.STRING);
                String dateType = (String)xpath.compile("lastUpdate/@dateType").evaluate(verInfoRegExNode,XPathConstants.STRING);
                if(queryType.equalsIgnoreCase("xpath"))
                {
                    String fromFile = (String)xpath.compile("lastUpdate/@fromFile").evaluate(verInfoRegExNode,XPathConstants.STRING);
                    if(!fromFile.isEmpty())
                    {
                        Document XmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(fromFile));
                        lastUpdate = (String)xpath.compile(lastUpdate).evaluate(XmlDoc,XPathConstants.STRING);
                    }
                }
                else
                {
                    lastUpdate = regexGetResults(lastUpdate,versionData);
                }

                if(dateType.equalsIgnoreCase("timestamp"))
                {
                    DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(Integer.parseInt(lastUpdate));
                    lastUpdate = dfm.format(date);
                }
            }
            
            engineVersion = regexGetResults(engineVersion,versionData);
            softName = regexGetResults(softName,versionData);
        }

        Node verInfoSqliteNode = (Node)xpath.compile("updater/versionInfo/verInfoParser/verInfoSqlite").evaluate(av.xmlRootNode,XPathConstants.NODE);
        if(verInfoSqliteNode != null)
        {
            String dbFile = (String)xpath.compile("@dbFile").evaluate(verInfoSqliteNode,XPathConstants.STRING);
            dbVersion = Common.processTemplates(
                    (String)xpath.compile("dbVersion").evaluate(verInfoSqliteNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            softVersion = Common.processTemplates(
                    (String)xpath.compile("softVersion").evaluate(verInfoSqliteNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            lastUpdate = Common.processTemplates(
                    (String)xpath.compile("lastUpdate").evaluate(verInfoSqliteNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            engineVersion = Common.processTemplates(
                    (String)xpath.compile("engineVersion").evaluate(verInfoSqliteNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            softName = Common.processTemplates(
                    (String)xpath.compile("softName").evaluate(verInfoSqliteNode,XPathConstants.STRING),
                    new EXEC_STRUCT(av));
            
            dbVersion = sqliteGetResults(dbFile,dbVersion);
            softVersion = sqliteGetResults(dbFile,softVersion);
            lastUpdate = sqliteGetResults(dbFile,lastUpdate);
            engineVersion = sqliteGetResults(dbFile,engineVersion);
            softName = sqliteGetResults(dbFile,softName);
        }

        avNode.setAttribute("name", softName);
        avNode.setAttribute("softVer",softVersion);
        avNode.setAttribute("engineVer", engineVersion);
        avNode.setAttribute("dbVer",dbVersion);
        avNode.setAttribute("lastUpdate",lastUpdate);
               
        XMLSerializer serializer = new XMLSerializer();
        serializer.setOutputCharStream(new java.io.FileWriter("avsversions.xml"));
        serializer.serialize(avVerXmlDoc);
     
    }

    private String regexGetResults(String regEx,String Data)
    {
        StringBuilder result = new StringBuilder();
        Matcher match = Pattern.compile(regEx).matcher(Data);
        int count = match.groupCount();
        for(int i = 1; i <= count; i++)
        {
            result.append(match.group(i));
            if(i < count) result.append("\r\n");
        }
        
        return result.toString();
    }
    
    private String sqliteGetResults(String dbFile,String sqlQuery) throws Exception
    {
        SQLiteStatement st = null;
        String result = "";
        
        for(int i=0;i < 5000;i++)
        {
            try
            {
                SQLiteConnection db = new SQLiteConnection(new File(dbFile));
                db.openReadonly();
                st = db.prepare(sqlQuery);

                if(st.step())
                    result = st.columnString(0);
                               
                st.dispose();
                break;
            }
            catch(SQLiteBusyException busyexept)
            {
                log.debug("SQLiteBusyException catched");
                log.debug(dbFile);
                Thread.sleep(1000);
            }
        }
        return result;
    }
    
    boolean update(AV_EXEC_RECORD av) throws Exception
    {
        String lastUpdate = prefs.get(av.szAvName+"lastupdate","0");
        if(Common.currentTimeStampInMillis() - Long.decode(lastUpdate) > Config.avUpdateTimeoutMill)
        {
            String updateCmd = (String)xpath.compile("updater/updateCmd").evaluate(av.xmlRootNode,XPathConstants.STRING);
            if(!updateCmd.isEmpty())
            {
                av.update_status = UPDATE_STATUS.STARTED;
                ProcessBuilder pb = new ProcessBuilder(updateCmd);
                Process proc = pb.start();
                proc.waitFor();
                prefs.put(av.szAvName+"lastupdate",Long.toString(Common.currentTimeStampInMillis()));
                return true;
            }
            
            NodeList Nodes = (NodeSet)xpath.compile("updater/download/url").evaluate(av.xmlRootNode,XPathConstants.NODESET);
            if(Nodes.getLength() > 0)
            {
                String updateDir = (String)xpath.compile("updater/updateDir").evaluate(av.xmlRootNode,XPathConstants.STRING);
                DownloadUpdate(av.szAvName, Nodes, updateDir);
                prefs.put(av.szAvName+"lastupdate",Long.toString(Common.currentTimeStampInMillis()));
                return true;
            }

            return true;
        }

        return false;
    
    }
    
    
    
   
  
    void DownloadUpdate (String avName,NodeList urls,String updateDir) throws Exception
    {
        int BUFFER_SIZE = 1024;
        byte data[] = new byte[BUFFER_SIZE];
        FileInputStream fis = null;
        ZipInputStream zis = null;
        FileOutputStream fos = null;
        BufferedOutputStream dest = null;
        FileOutputStream out = null;
        InputStream in = null;
        FtpDownloader ftp = new FtpDownloader();
                
        URLConnection connection = null;

        if(urls == null || urls.getLength() == 0) return;
        try
        {
            int index=0;

                int nodesCount = urls.getLength();
                for(int i=0;i < nodesCount;i++)
                {
                    String szUrl = urls.item(i).getTextContent();
                    URL url = new URL(szUrl);

                    if(url.getProtocol().equalsIgnoreCase("ftp"))
                    {
                        ftp.start(avName,url,urls.item(i),updateDir);
                    }
                    else
                    {
                        String tmpSavePath = System.getProperty("java.io.tmpdir");
                        String fileName = url.getFile().substring(url.getFile().lastIndexOf("/")+1);
                        tmpSavePath += "\\"+Common.currentTimeStampInMillis()+"_"+fileName;

                        prefs = root.node("updatemanager/"+avName.toLowerCase()+"/httpupdate");
                        connection = url.openConnection();
                        String httpLastUpdate = prefs.get(avName+"httplastupdate","");
                        connection.addRequestProperty(
                                                "If-Modified-Since", httpLastUpdate);

                        in = connection.getInputStream();
                        File outFile = new File(tmpSavePath);
                        outFile.createNewFile();
                        out = new FileOutputStream(outFile);
                        int totalRead=0;
                        byte[] recvBuf = new byte[4096];
                        while(true)
                        {
                            int rb = in.read(recvBuf,0,4096);
                            if(rb == -1) break;
                            out.write(recvBuf,0,rb);
                            totalRead += rb;
                        }

                        in.close();
                        out.flush();
                        out.close();

                        if(totalRead != connection.getContentLength()) throw new Exception();

                        fis = new FileInputStream(tmpSavePath);
                        zis = new ZipInputStream(fis);
                        ZipEntry entry;

                        while((entry = zis.getNextEntry()) != null)
                        {
                            fos = new FileOutputStream(updateDir+"\\"+entry.getName());
                            dest = new BufferedOutputStream(fos, BUFFER_SIZE);
                            int count;
                            while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1)
                            {
                                dest.write(data, 0, count);
                            }

                            dest.flush();
                            dest.close();
                            dest = null;
                            fos.close();
                            fos = null;
                        }

                        zis.close();
                        zis = null;
                        fis.close();
                        fis = null;

                        new File(tmpSavePath).delete();

                        prefs.put(avName+"httplastupdate",
                                        connection.getHeaderField(
                                        "Last-Modified"));
                        break;
                    }
                }

        }
        catch(Exception e)
        {
            log.error(e);
            try
            {
                if(in != null) in.close();
                if(out != null) out.close();

                if(dest != null) dest.close();
                dest = null;
                if(fos != null) fos.close();
                fos = null;
                if(zis != null) zis.close();
                zis = null;
                if(fis != null) fis.close();
                fis = null;
            }
            catch(Exception ee){}
        }
     }





    }
