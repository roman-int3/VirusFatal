package CheckManager;

import CheckManager.Utils.Rand;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.prefs.Preferences;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class Config
{
    public static enum SERVER_ROLE
    {
        scanner,
        updater
    }

    private static final            Logger log           = Logger.getLogger(Config.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();

    public static boolean isLogEnable;
    public static Preferences prefs = null;
    public static String rootPath;
    public static boolean stopFlag = false;
    public static String mysqlHost;
    public static String mysqlUser;
    public static String mysqlPwd;
    public static String listenPort;
    public static String acUploadDir;
    public static String mcUploadDir;
    public static String avRootPath;
    public static int scanTimeOutMill;
    public static int avUpdateTimeoutMill;
    public static Rand rand = new Rand();
    public static SERVER_ROLE server_role = SERVER_ROLE.updater;
    public static int totalRunThreads = 0;


    public static void init()
    {
        try
        {

            prefs = Preferences.userRoot().node("avcheckmanager");
            isLogEnable = Boolean.valueOf(prefs.get("islogenable","true"));
            String rootPathTmp = new AvCheckManager().getClass().getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
            rootPath = rootPathTmp.substring(0,rootPathTmp.lastIndexOf("/")+1);

            System.setProperty("vfapp.home",rootPath);
            DOMConfigurator.configure(rootPath+"log4j.xml");

            Document configDoc =  DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(Config.rootPath + "config.xml"));
            XPath xpath = XPathFactory.newInstance().newXPath();

            mysqlHost =     (String)xpath.compile("//mysql/host").evaluate(configDoc, XPathConstants.STRING);
            mysqlUser =     (String)xpath.compile("//mysql/user").evaluate(configDoc,XPathConstants.STRING);
            mysqlPwd =      (String)xpath.compile("//mysql/pwd").evaluate(configDoc,XPathConstants.STRING);
            listenPort =    (String)xpath.compile("//listenPort").evaluate(configDoc,XPathConstants.STRING);
            acUploadDir =   (String)xpath.compile("//acUploadDir").evaluate(configDoc,XPathConstants.STRING);
            mcUploadDir =   (String)xpath.compile("//mcUploadDir").evaluate(configDoc,XPathConstants.STRING);
            avRootPath =    (String)xpath.compile("//avsRootPath").evaluate(configDoc,XPathConstants.STRING);
            String tmp =    (String)xpath.compile("//server_role").evaluate(configDoc,XPathConstants.STRING);
            if(!tmp.isEmpty())
            {
                tmp = tmp.trim();
                if(tmp.equalsIgnoreCase("scanner")) server_role = SERVER_ROLE.scanner;
            }

            scanTimeOutMill = Integer.parseInt((String)xpath.compile("//ScanTimeOutSec").evaluate(configDoc,XPathConstants.STRING)) * 1000;
            avUpdateTimeoutMill = Integer.parseInt((String)xpath.compile("//AvUpdateTimeOutMinutes").evaluate(configDoc,XPathConstants.STRING)) * 60 * 1000;
        }
        catch (Throwable e)
        {
            log.error("Exception",e);
            System.exit(0);
        }
    }

}
