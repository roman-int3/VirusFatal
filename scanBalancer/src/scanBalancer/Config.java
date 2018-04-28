package scanBalancer;


import ScanVmManager.VmController.SCANVM_STATE;
import SqlManager.SqlManager;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import scanBalancer.ScanQueue.CHECK_SERVER;
import scanBalancer.ScanQueue.SERVER_STATUS;


public class Config
{
    private static final            Logger log           = Logger.getLogger(Config.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();

    public static String rootPath;
    public static boolean stopFlag = false;
    public static String mysqlHost;
    public static String mysqlUser;
    public static String mysqlPwd;
    public static String mysqlPort;
    public static int acCheckTimeOut;

    public static String acJabberID;
    public static String acJabberPwd;
    public static String acJabberHost;
    public static String acJabberPort;
    public static String scan_balancer_url;
    public static String jabber_reports_dir;

    public static String listenPort;
    public static String ServersListPath;
    public static String ReportsCachePath;
    public static int scanTimeOut;
    public static int connectionTimeOutMs;
    public static int responceWaitTimeoutMs;
    public static long uploadFilesCacheLifeTimeInSec;
    public static long scanReportsCacheLifeTimeInSec;
    public static String UploadFilesDir;
    public static String filestorepath;
    private static long last_refresh;
    public static ArrayList <String> allowFileTypes = new ArrayList<String>();
    public static ArrayList <String> denyFileTypes = new ArrayList<String>();
    public static List <CHECK_SERVER> CheckServers = Collections.synchronizedList(new ArrayList<CHECK_SERVER>());
    
    public static int checkManagerPort;
    public static String esxiDatastorePath;
    public static String esxiHost;
    public static int esxiSshPort;
    public static String esxiLogin;
    public static String esxiPasswd;
    
    public static void init()
    {
        try
        {
            CheckServers.clear();
            allowFileTypes.clear();
            denyFileTypes.clear();
            last_refresh = 0;

            String rootPathTmp = new scanBalancer().getClass().getProtectionDomain().getCodeSource().getLocation().toString().substring(5);
            rootPath = rootPathTmp.substring(0,rootPathTmp.lastIndexOf("/")+1);
            
            System.setProperty("vfapp.home",rootPath);
            String configFile=null;
            if(new File(rootPath+"..\\..\\log4j.xml").exists())
            {
                DOMConfigurator.configure(rootPath+"..\\..\\log4j.xml");
                configFile = Config.rootPath + "../../config.xml";
            }
            else
            {
                configFile = Config.rootPath + "config.xml";
                DOMConfigurator.configure(rootPath+"log4j.xml");
            }
            
            Document configDoc =  DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(configFile));
            XPath xpath = XPathFactory.newInstance().newXPath();

            /////////////////////////////////////////////////////////////////////
            // Mysql server options
            mysqlHost = (String)xpath.compile("//mysql/host").evaluate(configDoc, XPathConstants.STRING);
            mysqlUser = (String)xpath.compile("//mysql/user").evaluate(configDoc,XPathConstants.STRING);
            
            mysqlPwd  = (String)xpath.compile("//mysql/pwd").evaluate(configDoc,XPathConstants.STRING);
            mysqlPort = (String)xpath.compile("//mysql/port").evaluate(configDoc,XPathConstants.STRING);

            /////////////////////////////////////////////////////////////////////
            // ScanVMManager options
            checkManagerPort = Integer.parseInt((String)xpath.compile("//esxi/av_check_manager/port").evaluate(configDoc, XPathConstants.STRING));
            esxiDatastorePath = (String)xpath.compile("//esxi/vm_datastore_path").evaluate(configDoc, XPathConstants.STRING);
            esxiHost = (String)xpath.compile("//esxi/host").evaluate(configDoc, XPathConstants.STRING);
            esxiSshPort = Integer.parseInt((String)xpath.compile("//esxi/ssh_port").evaluate(configDoc, XPathConstants.STRING));
            esxiLogin = (String)xpath.compile("//esxi/login").evaluate(configDoc, XPathConstants.STRING);
            esxiPasswd = (String)xpath.compile("//esxi/passwd").evaluate(configDoc, XPathConstants.STRING);


            refresh_options();
            

            ///////////////////////////////////////////////////////////////////////
            // AutoChecker Options
            acCheckTimeOut      =   Integer.parseInt((String)xpath.compile("//autochecker/timeout_in_min").evaluate(configDoc,XPathConstants.STRING))*60*1000;
            acJabberID          =   (String)xpath.compile("//autochecker/jabber/id").evaluate(configDoc,XPathConstants.STRING);
            acJabberPwd         =   (String)xpath.compile("//autochecker/jabber/pwd").evaluate(configDoc,XPathConstants.STRING);
            acJabberHost        =   (String)xpath.compile("//autochecker/jabber/host").evaluate(configDoc,XPathConstants.STRING);
            acJabberPort        =   (String)xpath.compile("//autochecker/jabber/port").evaluate(configDoc,XPathConstants.STRING);
            scan_balancer_url   =   (String)xpath.compile("//autochecker/scan_balancer_url").evaluate(configDoc,XPathConstants.STRING);
            jabber_reports_dir  =   (String)xpath.compile("//autochecker/jabber/reports_dir").evaluate(configDoc,XPathConstants.STRING);
            new File(jabber_reports_dir).mkdirs();
            
            /////////////////////////////////////////////////////////////////////
            // ScanBalancer Options
            connectionTimeOutMs     = Integer.parseInt((String)xpath.compile("//scanbalancer/connectionTimeOutMs").evaluate(configDoc,XPathConstants.STRING));
            responceWaitTimeoutMs   = Integer.parseInt((String)xpath.compile("//scanbalancer/responceWaitTimeoutMs").evaluate(configDoc,XPathConstants.STRING));
            listenPort              = (String)xpath.compile("//scanbalancer/listenPort").evaluate(configDoc,XPathConstants.STRING);
            ReportsCachePath        = (String)xpath.compile("//scanbalancer/scanReportsCachePath").evaluate(configDoc,XPathConstants.STRING);
            ServersListPath         = (String)xpath.compile("//scanbalancer/checkServersListPath").evaluate(configDoc,XPathConstants.STRING);
            UploadFilesDir          = (String)xpath.compile("//scanbalancer/uploadFilesPath").evaluate(configDoc,XPathConstants.STRING);
            filestorepath           = (String)xpath.compile("//scanbalancer/fileStorePath").evaluate(configDoc,XPathConstants.STRING);

            new File(UploadFilesDir).mkdirs();
            new File(ReportsCachePath).mkdirs();
            new File(filestorepath).mkdirs();

            scanTimeOut                   = Integer.parseInt((String)xpath.compile("//scanbalancer/scanTimeOutMinutes").evaluate(configDoc,XPathConstants.STRING)) * 60 * 1000;
            uploadFilesCacheLifeTimeInSec = Integer.parseInt((String)xpath.compile("//scanbalancer/uploadFilesCacheLifeTimeMinutes").evaluate(configDoc,XPathConstants.STRING)) * 60 * 1000;
            scanReportsCacheLifeTimeInSec = Integer.parseInt((String)xpath.compile("//scanbalancer/scanReportsCacheLifeTimeMinutes").evaluate(configDoc,XPathConstants.STRING)) * 60 * 1000;

            NodeList nodes = (NodeList)xpath.compile("//scanbalancer/allowFileTypes/type").evaluate(configDoc,XPathConstants.NODESET);
            int count = nodes.getLength();
            for(int i=0; i < count; i++)
            {
                allowFileTypes.add(nodes.item(i).getTextContent().toLowerCase());
            }

            nodes = (NodeList)xpath.compile("//scanbalancer/denyFileTypes/type").evaluate(configDoc,XPathConstants.NODESET);
            count = nodes.getLength();
            for(int i=0; i < count; i++)
            {
                denyFileTypes.add(nodes.item(i).getTextContent().toLowerCase());
            }
            
        }
        catch (Throwable e)
        {
            log.error("Exception",e);
            log.error("FATAL EXIT",e);
            System.exit(0);
        }
    }
        
    public static void refresh_options() throws Exception
    {
        if(System.currentTimeMillis() - last_refresh <= 5*60*1000) return;

        SqlManager options_sql = null;
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
                
        try
        {
            options_sql = new SqlManager(mysqlHost, mysqlPort, mysqlUser,
                    mysqlPwd,"vf_options",1,1);
            con = options_sql.dbconn.getConnection();
            stmt = con.createStatement();
            
            /////////////////////////////////////////////////////////////////////
            // ScanBalancer Options
            rs = stmt.executeQuery("SELECT name,value FROM scanbalancer");
            
            while(rs.next())
            {
                String name = rs.getString("name");
                String value = rs.getString("value");
                
                if(name.equalsIgnoreCase("connectionTimeOutMs")) connectionTimeOutMs = Integer.parseInt(value);
                else if(name.equalsIgnoreCase("responceWaitTimeoutMs")) responceWaitTimeoutMs = Integer.parseInt(value);
                else if(name.equalsIgnoreCase("listenPort")) listenPort = value;
                else if(name.equalsIgnoreCase("scanReportsCachePath")) ReportsCachePath = value;
                else if(name.equalsIgnoreCase("uploadFilesPath")) UploadFilesDir = value;
                else if(name.equalsIgnoreCase("fileStorePath")) filestorepath = value;
                else if(name.equalsIgnoreCase("scanTimeOutMinutes")) scanTimeOut = Integer.parseInt(value) * 60 * 1000;
                else if(name.equalsIgnoreCase("uploadFilesCacheLifeTimeMinutes")) uploadFilesCacheLifeTimeInSec = Integer.parseInt(value) * 60 * 1000;
                else if(name.equalsIgnoreCase("scanReportsCacheLifeTimeMinutes")) scanReportsCacheLifeTimeInSec = Integer.parseInt(value) * 60 * 1000;
                else if(name.equalsIgnoreCase("allowFileTypes"))
                {
                    String[] typeArr = value.split("\r\n");
                    allowFileTypes.clear();
                    for(String type : typeArr)
                    {
                        allowFileTypes.add(type.trim());
                    }
                }
                else if(name.equalsIgnoreCase("denyFileTypes"))
                {
                    String[] typeArr = value.split("\r\n");
                    denyFileTypes.clear();
                    for(String type : typeArr)
                    {
                        denyFileTypes.add(type.trim());
                    }
                }
            }
            new File(UploadFilesDir).mkdirs();
            new File(ReportsCachePath).mkdirs();
            new File(filestorepath).mkdirs();
            rs.close();
            
            ///////////////////////////////////////////////////////////////////////
            // AutoChecker Options
            rs = stmt.executeQuery("SELECT name,value FROM autochecker");
            
            while(rs.next())
            {
                String name = rs.getString("name");
                String value = rs.getString("value");
                
                if(name.equalsIgnoreCase("check_timeout")) acCheckTimeOut = Integer.parseInt(value)*60*1000;
                else if(name.equalsIgnoreCase("jabber_id")) acJabberID = value;
                else if(name.equalsIgnoreCase("jabber_pwd")) acJabberPwd = value;
                else if(name.equalsIgnoreCase("jabber_host")) acJabberHost = value;
                else if(name.equalsIgnoreCase("jabber_port")) acJabberPort = value;
                else if(name.equalsIgnoreCase("jabber_reports_dir")) jabber_reports_dir = value;
            }
            new File(jabber_reports_dir).mkdirs();
            rs.close();stmt.close();con.close();options_sql.dbconn.close();

            options_sql = new SqlManager(mysqlHost, mysqlPort, mysqlUser,
                    mysqlPwd,"virusfatal",1,1);
            con = options_sql.dbconn.getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery("SELECT value FROM sys_settings WHERE name='SCAN_BALANCER_URL'");
            rs.next();
            scan_balancer_url = rs.getString("value");
            rs.close();

            rs = stmt.executeQuery("SELECT name,ip FROM antiviruses");
            HashMap <String,ArrayList>avsmap = new HashMap<String,ArrayList>();
            
            while(rs.next())
            {
                String avname = rs.getString("name");
                String avips = rs.getString("ip");
                String[] ipsArr = avips.split("\r\n");
                
                for(String ip:ipsArr)
                {
                    ArrayList<String> avnames = avsmap.get(ip);
                    if(avnames == null)
                    {
                        avnames = new ArrayList<String>();
                        avsmap.put(ip, avnames);
                    }
                    avnames.add(avname);
                }
            }
            rs.close();
            
            Set <String>keys = avsmap.keySet();
            for(String key:keys)
            {
                ArrayList avnames = avsmap.get(key);
                CHECK_SERVER cs = new CHECK_SERVER();
                cs.ip = key;

                Socket sock;
                try
                {
                    String[] addrArr = cs.ip.split(":");
                    sock = new Socket(addrArr[0],Integer.parseInt(addrArr[1]));
                }
                catch(Exception e)
                {
                    log.error(e);
                    log.error(cs.ip);
                    continue;
                }

                sock.close();

                cs.avNamesHashs = new int[avnames.size()+1];
                for(int j=0; j < avnames.size(); j++)
                {
                    String avname = (String)avnames.get(j);
                    cs.avNamesHashs[j] = avname.trim().toLowerCase().hashCode();

                    if(isDebugEnabled)
                        log.debug("Added new antivirus "+avname+" to ip "+cs.ip+" avhash "+cs.avNamesHashs[j]);
                }

                CheckServers.add(cs);
            }


        }
        finally
        {
            if(rs != null) rs.close();
            if(stmt != null) stmt.close();
            if(con != null) con.close();
            if(options_sql != null && options_sql.dbconn != null) options_sql.dbconn.close();
        }
    }

}
