package CheckManager.ReportsManager;

import CheckManager.Config;
import CheckManager.CommonObjects.EXEC_STRUCT;
import CheckManager.CommonObjects.INFECT_FLAG_TYPE;
import CheckManager.Utils.Common;
import com.almworks.sqlite4java.SQLiteBusyException;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;


public class ResultsParser implements Runnable
{
    private static final Logger log                          = Logger.getLogger(ResultsParser.class);
    private static final String szRepTemplateXml             = "<%s><result><![CDATA[%s]]></result><progVer>%s</progVer><dbVer>%s</dbVer></%s>";
    private static final Object reportsPathsCriticalSection  = new Object();
    private static final ArrayList <EXEC_STRUCT>reportsPaths = new ArrayList<EXEC_STRUCT>();
    private static final int        regExFlags                          = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
    private static final boolean    isDebugEnabled                      = log.isDebugEnabled();
    private static final XPath      xpath                               = XPathFactory.newInstance().newXPath();
    private static boolean isParserStart = false;

    public enum ResultsParserType
    {
        sqlite("sqlite"),
        regex("regex");

        private String type;
        ResultsParserType(String type)
        {
            this.type = type;
        }

        public String toString(){return this.type;}
    }


    
    
    
    
    public static ResultParserData init(EXEC_STRUCT exec) throws Exception
    {
        ResultParserData rpd = new ResultParserData();
        XPath xpath = XPathFactory.newInstance().newXPath();


        if(exec.avr.ResParserType == ResultsParserType.sqlite)
        {
            rpd.sqlQuery = Common.processTemplates(
                    (String)xpath.compile("resultparser/sqlite/sql_query").
                            evaluate(exec.avr.xmlRootNode,XPathConstants.STRING),exec);
            rpd.sqlDbFileName = Common.processTemplates(
                    (String)xpath.compile("resultparser/sqlite/filename").
                            evaluate(exec.avr.xmlRootNode,XPathConstants.STRING),exec);
        }
        else
        {
            rpd.szFoundStringRegEx     = Common.processTemplates(
                    (String)xpath.compile("resultparser/regex/foundStringRegEx/regEx").
                            evaluate(exec.avr.xmlRootNode,XPathConstants.STRING),exec);
            rpd.FoundStringRegExMatchLimit = -1;
            String smatch_limit = (String)xpath.compile("resultparser/regex/foundStringRegEx/regEx/@match_limit")
                        .evaluate(exec.avr.xmlRootNode,XPathConstants.STRING);
            if(!smatch_limit.isEmpty())
            {
                rpd.FoundStringRegExMatchLimit = Integer.parseInt(smatch_limit);
            }

            rpd.szDbVersionRegEx       = Common.processTemplates(
                    (String)xpath.compile("resultparser/regex/dbVersionRegEx").
                    evaluate(exec.avr.xmlRootNode,XPathConstants.STRING),exec);

            rpd.szFoundInfectFlagRegEx = Common.processTemplates(
                    (String)xpath.compile("resultparser/regex/foundInfectFlagRegEx").evaluate(
                        exec.avr.xmlRootNode,XPathConstants.STRING),exec);
            String szFoundInfectFlagType = (String)xpath.compile("resultparser/regex/foundInfectFlagRegEx/@type").evaluate(
                        exec.avr.xmlRootNode,XPathConstants.STRING);
            if(!szFoundInfectFlagType.isEmpty())
                rpd.InfectFlagType = INFECT_FLAG_TYPE.valueOf(szFoundInfectFlagType.toUpperCase());

            rpd.szProgVerRegEx         = Common.processTemplates(
                    (String)xpath.compile("resultparser/regex/progVerRegEx").evaluate(
                    exec.avr.xmlRootNode,XPathConstants.STRING),exec);

            rpd.szScanEndRegEx         = Common.processTemplates(
                    (String)xpath.compile("resultparser/regex/scanEndRegEx").evaluate(
                    exec.avr.xmlRootNode,XPathConstants.STRING),exec);


        }
        
        return rpd;
    }


    public static void addReportToParser(EXEC_STRUCT curExec)
    {
        log.debug("Added new report to parser queue : ");
        log.debug(curExec.szReportFileName);

        synchronized (reportsPathsCriticalSection)
        {
            reportsPaths.add(curExec);
        }
    }


    public static void start() throws Exception
    {
        new Thread(new ResultsParser()).start();
    }


    public void run()
    {
        log.debug("RESULTS Parser STARTED");

        EXEC_STRUCT exec    = null;
        SQLiteStatement st = null;
        int index           = 0;
        Config.totalRunThreads++;

        while(!Config.stopFlag)
        {
            try
            {
                while(!Config.stopFlag)
                {
                    exec    = null;
                    Thread.sleep(100);

                    for(index = 0; index < reportsPaths.size(); index++)
                    {
                        String szProgVer = "";
                        String szDbVer = "";
                        String szCheckResult = "Not Found";
                        
                        exec = reportsPaths.get(index);
                        if(exec == null)
                        {
                            log.error("reportsPaths.get == 0; index="+index);
                            continue;
                        }
                    
                        if(isDebugEnabled)
                        {
                            log.debug("Processing report job : "+exec.szReportFileName);
                            log.debug("index = "+index);
                        }

                        if(exec.reportWaitStartTime == 0)
                        {
                            exec.reportWaitStartTime = Common.currentTimeStampInMillis();
                        }


                        if(Common.currentTimeStampInMillis() - exec.reportWaitStartTime >= Config.scanTimeOutMill)
                        {
                            saveReport(exec,"ERROR2","","");
                        }
                        else
                        {
                            
                            if(exec.avr.ResParserType == ResultsParserType.sqlite)
                            {
                                try
                                {
                                    SQLiteConnection db = new SQLiteConnection(new File(exec.ResParserData.sqlDbFileName));
                                    db.openReadonly();
                                    st = db.prepare(exec.ResParserData.sqlQuery);
                                    log.debug(exec.ResParserData.sqlQuery);

                                    if(st.step())
                                        szCheckResult = st.columnString(0);
                               
                                    st.dispose();
                                    saveReport(exec,szCheckResult,szProgVer,szDbVer);
                                }
                                catch(SQLiteBusyException busyexept)
                                {
                                    log.debug("SQLiteBusyException catched");
                                    log.debug(exec.ResParserData.sqlDbFileName);
                                    continue;
                                }
     
                            }
                            else
                            {
                                                       
                            
                            File reportFile = new File(exec.szReportFileName);
                            if(reportFile.length() <= 0)
                            {
                                if(isDebugEnabled)
                                {
                                    log.debug("File Size = 0; File="+exec.szReportFileName);
                                }
                        
                                continue;
                            }
                    
                            FileInputStream reportStream = new FileInputStream(reportFile);
                            ByteBuffer reportData = ByteBuffer.allocate((int)reportStream.getChannel().size());
                            reportStream.getChannel().read(reportData);
                            String repData = new String(reportData.array());
                            reportStream.close();
                            reportStream = null;


                            log.debug(repData);
                            String tmpStr = repData.substring(0,5);
                            log.debug(tmpStr);
                            if(tmpStr.equalsIgnoreCase("ERROR"))
                            {
                                saveReport(exec,"ERROR","","");
                            }
                            else
                            {

                                Matcher match = Pattern.compile(exec.ResParserData.szScanEndRegEx,regExFlags).matcher(repData);
                                if(!match.find())
                                {
                                    if(isDebugEnabled)
                                    {
                                        log.debug("szScanEndRegEx : "+exec.ResParserData.szScanEndRegEx+ " not found in : "+repData);
                                    }
                                    continue;
                                }
                    

                                log.debug(exec.ResParserData.szFoundInfectFlagRegEx);
                                if(match.usePattern(Pattern.compile(exec.ResParserData.szFoundInfectFlagRegEx,regExFlags)).reset().find())
                                {
                                    String foundRez = match.group(1);
                                    if(!foundRez.isEmpty())
                                    {
                                        if(exec.ResParserData.InfectFlagType == INFECT_FLAG_TYPE.BOOL)
                                        {
                                            if(Integer.parseInt(foundRez) != 0)
                                            {
                                                log.debug(exec.ResParserData.szFoundStringRegEx);
                                                szCheckResult = processFoundStringRegEx(
                                                        match.usePattern(Pattern.compile(exec.ResParserData.szFoundStringRegEx,regExFlags)).reset(),exec);
                                            }
                                        }
                                        else
                                        {
                                            log.debug(exec.ResParserData.szFoundStringRegEx);
                                            szCheckResult = processFoundStringRegEx(
                                                    match.usePattern(Pattern.compile(exec.ResParserData.szFoundStringRegEx,regExFlags)).reset(),exec);
                                        }
                                    }
                                    else
                                    {
                                        log.error("match.group(1) not found result : "+exec.ResParserData.szFoundInfectFlagRegEx+ " not found");
                                    }
                                }
                                else
                                {
                                    log.error("FoundInfectFlagRegEx : "+exec.ResParserData.szFoundInfectFlagRegEx+ " not found");
                                }

                                ////////////////////////////////////////////////////////////
                                // Antivirus version
                                if(!exec.ResParserData.szProgVerRegEx.isEmpty())
                                {
                                    if(match.usePattern(Pattern.compile(exec.ResParserData.szProgVerRegEx,regExFlags)).reset().find())
                                    {
                                        szProgVer = match.group(1);
                                    }
                                    else
                                    {
                                        log.error("ProgVerRegEx : "+exec.ResParserData.szProgVerRegEx+ " not found");
                                    }
                                }
                    
                                ////////////////////////////////////////////////////////////
                                // Antivirus database version
                                if(!exec.ResParserData.szDbVersionRegEx.isEmpty())
                                {
                                    if(match.usePattern(Pattern.compile(exec.ResParserData.szDbVersionRegEx,regExFlags)).reset().find())
                                    {
                                        szDbVer = match.group(1);
                                    }
                                    else
                                    {
                                        log.error("DbVersionRegEx : "+exec.ResParserData.szDbVersionRegEx+ " not found");
                                    }
                                }

                                ////////////////////////////////////////////////////////////
                                // Save report to file
                                saveReport(exec,szCheckResult,szProgVer,szDbVer);
                            }
                        }
                        }

                        synchronized (reportsPathsCriticalSection)
                        {
                            reportsPaths.remove(index);
                        }

                        index--;
                    
                        if(isDebugEnabled)
                        {
                            log.debug("Report job procecced and removed from queue : "+exec.szReportFileName);
                            log.debug("index = "+index);
                        }
                    }
                }
            }
            catch (Throwable e)
            {
                log.error("UNHANDLED EXCEPTION IN MAIN LOOP",e);
                
                if(exec != null)
                    saveReport(exec,"ERROR","","");
                
                if(st != null) st.dispose();

                synchronized (reportsPathsCriticalSection)
                {
                    if(reportsPaths != null && reportsPaths.size() > 0)
                    {
                        reportsPaths.remove(index);
                        if(isDebugEnabled)
                        {
                            log.debug("Removed exeption job from queue : "+exec.szReportFileName);
                            log.debug("index = "+index);
                        }
                    }
                }
            }
   
        }

        Config.totalRunThreads--;
    }


    private static void saveReport(EXEC_STRUCT exec,String szCheckResult,String szProgVer,String szDbVer)
    {
        String resultFileName = exec.szReportFileName.substring(0,
                                                              exec.szReportFileName.lastIndexOf("\\")+1) +
                                                              String.format("%s_Result.txt",exec.avr.szAvName);

        File out = new File(resultFileName);
        FileOutputStream outStream = null;

        try
        {
            outStream = new FileOutputStream(out);
            outStream.write(String.format(szRepTemplateXml,
						    exec.avr.szAvName,
						    szCheckResult,
						    szProgVer,
						    szDbVer,
						    exec.avr.szAvName
						    ).getBytes());
        }
        catch (Throwable e)
        {
            log.error("Failed to open FileOutputStream on file "+resultFileName,e);
        }
        finally
        {
            try
            {
                if(outStream != null) outStream.close();
            }catch(Exception e){}
            
            outStream = null;
            resultFileName = null;
        }
    }


    private static String processFoundStringRegEx(Matcher match,EXEC_STRUCT exec) throws Exception
    {
        StringBuilder szResult = new StringBuilder();
        int match_limit = exec.ResParserData.FoundStringRegExMatchLimit;
        log.debug("exec.avr.FoundStringRegExMatchLimit="+exec.ResParserData.FoundStringRegExMatchLimit);
        
        for(;match.find() == true && match_limit != 0; match_limit--)
        {
            log.debug(match.pattern().pattern());
            log.debug("match.group(0) : "+match.group(0));
            
            ArrayList rezArr = new ArrayList();
            for(int i = 1; i <= match.groupCount(); i++)
            {
                String tmpStr = match.group(i);
                log.debug("tmpStr : "+tmpStr);
                if(tmpStr ==null) continue;
                rezArr.add(tmpStr);
            }
            
            String regex = String.format("%s%s%s",
                                            "foundStringRegEx/ifgroupCount",
                                            rezArr.size(),
                                            "/format_string");
            String format_string = (String)xpath.compile(regex)
                        .evaluate(exec.avr.xmlRootNode,XPathConstants.STRING);
                
            log.debug("foundStringRegEx/ifgroupCount"+match.groupCount()+"/format_string");

            if(format_string.isEmpty())
            {
                szResult.append(rezArr.get(0));
            }
            else
            {
                szResult.append(String.format(format_string,rezArr.toArray()));
                szResult.append("<br>");
            }
        }

        if(szResult.length() == 0) return "Not Found";
        return szResult.toString();
    }
}
