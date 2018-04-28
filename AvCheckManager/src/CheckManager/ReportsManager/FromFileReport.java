
package CheckManager.ReportsManager;

import CheckManager.uiAutomation.UiAutomation;
import CheckManager.CommonObjects.EXEC_STRUCT;
import CheckManager.CommonObjects.INFECT_FLAG_TYPE;
import CheckManager.JNI;
import CheckManager.Utils.Common;
import java.io.File;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;


public class FromFileReport extends ReportsParser implements Parser
{

    private static final            Logger log           = Logger.getLogger(FromFileReport.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();
    private final UiAutomation ui = new UiAutomation();

        public FromFileReport(EXEC_STRUCT CurExec) throws Exception
        {
            super(CurExec);
            log.debug("FromFileReport constructor");

        }


    public boolean isReportReady() throws Exception
    {

        log.debug("isReportReady called.");
        String saveData = null;

        saveData = getReportFromFile(curExec.avr.szReportFromFileFileName,
                curExec.avr.szReportFromFileFileNameEncoding,-1);

        if(saveData != null && !saveData.isEmpty())
        {log.debug("saveData: "+saveData);
            Common.saveReport(curExec,Common.applyFilters(curExec,saveData));
            if(curExec.avr.ui_automation != null)
            {
                long timer = Common.currentTimeStampInMillis();
                while(Common.currentTimeStampInMillis() - timer < 5*60*1000)
                {
                    ui.automate(curExec,curExec.avr.ui_automation.item(0),
                                        curExec.avr.ui_automation.item(0).getParentNode(),null);
                    if(ui.isAutomationFinished) break;
                    Thread.sleep(100);
                }
            }

            return true;
        }

        return false;
    }

    

    public void close() 
    {
        if(curExec.avr.ui_automation != null)
        {
            JNI.dropInjectedGrabText(curExec.pInfo);
        }
        
    }








    }