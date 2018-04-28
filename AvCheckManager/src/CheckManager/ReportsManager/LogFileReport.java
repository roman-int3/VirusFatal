

package CheckManager.ReportsManager;

import CheckManager.CommonObjects.EXEC_STRUCT;
import CheckManager.CommonObjects.SCAN_STATUS;
import CheckManager.JNI;
import CheckManager.Utils.Common;
import org.apache.log4j.Logger;

public class LogFileReport extends ReportsParser implements Parser
{

    private static final            Logger log           = Logger.getLogger(FromFileReport.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();

    public LogFileReport(EXEC_STRUCT curExec) throws Exception
    {
        super(curExec);
    }

    public boolean isReportReady() throws Exception
    {
        if(JNI.isProcessFinished(curExec.pInfo))
        {
            log.debug("Scan process finished, flushing scan output.");
            Common.flushScanOutput(curExec);
            curExec.scan_status = SCAN_STATUS.SCAN_FINISHED_OK;
            return true;
        }
        
        return false;
    }

    public void close() {

    }
}


