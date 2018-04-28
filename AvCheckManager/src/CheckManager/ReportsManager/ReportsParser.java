
package CheckManager.ReportsManager;

import CheckManager.CommonObjects.EXEC_STRUCT;
import CheckManager.CommonObjects.INFECT_FLAG_TYPE;
import CheckManager.Utils.Common;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;


public class ReportsParser 
{
    private static final            Logger log           = Logger.getLogger(ReportsParser.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();

    public Pattern RegEx_infectedflag;
    public INFECT_FLAG_TYPE infectedflagtype = INFECT_FLAG_TYPE.STRING;
    public Pattern RegEx_scanend;
    public Pattern RegEx_infected_results;
    public String notinfected_results;
    public Pattern RegEx_isourfileflag;
    public String szAddToLog;
    public EXEC_STRUCT curExec;
    private int searchloopcount = 1;

    public ReportsParser(EXEC_STRUCT CurExec) throws Exception
    {
        curExec = CurExec;
        XPath xpath = XPathFactory.newInstance().newXPath();
        RegEx_infectedflag = Pattern.compile(
                                    Common.processTemplates(
                                        (String)xpath.compile("reportParser/RegEx/infectedflag").evaluate(
                                                    CurExec.avr.xmlRootNode,XPathConstants.STRING),CurExec),Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
        String type = (String)xpath.compile("reportParser/RegEx/infectedflag/@type").evaluate(
                                                    CurExec.avr.xmlRootNode,XPathConstants.STRING);
        if(!type.isEmpty())
            infectedflagtype = INFECT_FLAG_TYPE.valueOf(type.toUpperCase());

        RegEx_scanend = Pattern.compile(
                                    Common.processTemplates(
                                        (String)xpath.compile("reportParser/RegEx/scanend").evaluate(
                                                    CurExec.avr.xmlRootNode,XPathConstants.STRING),CurExec),Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);

        String tmp = (String)xpath.compile("reportParser/RegEx/isourfileflag").evaluate(
                                                    CurExec.avr.xmlRootNode,XPathConstants.STRING);
        if(!tmp.isEmpty())
        {
            RegEx_isourfileflag = Pattern.compile(Common.processTemplates(tmp,
                                        CurExec),Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
        }

        RegEx_infected_results = Pattern.compile(
                                    Common.processTemplates(
                                        (String)xpath.compile("reportParser/RegEx/infected/results").evaluate(
                                                    CurExec.avr.xmlRootNode,XPathConstants.STRING),CurExec),Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);

        notinfected_results = (String)xpath.compile("reportParser/RegEx/notinfected/results").evaluate(
                                                    CurExec.avr.xmlRootNode,XPathConstants.STRING);
        szAddToLog = (String)xpath.compile("reportParser/addToLog").evaluate(
                                                    CurExec.avr.xmlRootNode,XPathConstants.STRING);

    }

    public String parseReport(String reportData) throws Exception
    {
        String retData = null;
        boolean isInfected = false;
        log.debug(RegEx_scanend.pattern());

        if(RegEx_scanend == null || RegEx_scanend.matcher(reportData).reset().find())
        {
            if(RegEx_isourfileflag == null || RegEx_isourfileflag != null && RegEx_isourfileflag.matcher(reportData).reset().find())
            {
                if(RegEx_infectedflag == null  )
                {
                    retData = reportData;

                }
                else
                {
                    log.debug(RegEx_infectedflag.pattern());
                    Matcher match = RegEx_infectedflag.matcher(reportData).reset();
                    if(match.find())
                    {
                        isInfected = true;
                        if(infectedflagtype == INFECT_FLAG_TYPE.BOOL)
                        {
                            retData = match.group(1);
                            if(Integer.parseInt(retData) == 0) isInfected = false;
                        }
                    }

                    if(isInfected)
                    {
                        match = RegEx_infected_results.matcher(reportData).reset();
                        if(!match.find()) throw new Exception("RegEx_infected_results not found.");
                        retData = match.group(1);
                    }
                    else
                    {

                        retData = notinfected_results;
                    }

                    retData += "\r\n";
                    retData += szAddToLog;
                }
            }
        }

        return retData;
    }


    public String getReportFromFile(String FileName,String encoding,int searchlooplimit) throws Exception
    {
        String reportData = null;
        String retData = null;



        FileEnum rf = new FileEnum(FileName,encoding,0,0);



         while(rf.hasNext())
         {
            reportData = rf.nextFileData();
            log.debug(reportData);
            if(reportData == null) {log.debug("reportData == null");break;}
            retData = parseReport(reportData);
            if(retData != null)
            {

                break;
             }

        }

        searchloopcount++;
        if(retData == null && searchloopcount > searchlooplimit) return notinfected_results;

        return retData;
    }




}
