

package CheckManager.ReportsManager;

import CheckManager.uiAutomation.UiAutomation;
import CheckManager.CommonObjects.EXEC_STRUCT;
import CheckManager.CommonObjects.SCAN_STATUS;
import CheckManager.JNI;
import org.apache.log4j.Logger;


public class GrabWinReport extends ReportsParser implements Parser
{

    private static final            Logger log           = Logger.getLogger(FromFileReport.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();
    private final UiAutomation ui = new UiAutomation();
    
    public GrabWinReport(EXEC_STRUCT curExec) throws Exception
    {
        super(curExec);

    }
    
   
    public boolean isReportReady() throws Exception
    {
        ui.automate(curExec, curExec.avr.ui_automation.item(0),
                                        curExec.avr.ui_automation.item(0).getParentNode(),null);
        if(ui.isAutomationFinished)
        {
            curExec.scan_status = SCAN_STATUS.SCAN_FINISHED_OK;
            return true;
        }

        return false;
    }

    public void close()
    {
        JNI.dropInjectedGrabText(curExec.pInfo);
    }

    

}
