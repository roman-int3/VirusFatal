package CheckManager.CommonObjects;

import CheckManager.ReportsManager.FromFileReport;
import CheckManager.ReportsManager.Parser;
import CheckManager.ReportsManager.ReportParserType;
import CheckManager.ReportsManager.ResultsParser.ResultsParserType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AV_EXEC_RECORD
{
    public Node ReportParserNode;
    public ReportParserType parserType;
    public ResultsParserType ResParserType;

    public int		        AvNameHash;
    public int                 iShell_lpVerb;
    
    public String              exeFileName;
    public String              szShell_lpVerb;

    public String	        szAvName;
    public String	        szAvCmd;
    public String              szScanFileCmd;
    public String	        szReportCmd;
    public String	        szReportFileName;
    public String	        szScanOptions;

    public String	        szUpdateCmd;
    public String	        szUpdateDir;
    public long              UpdateRefreshTimeOut;

    public String              szWinCharTitle;
    public String              szWinCharClass;


    public String               szReportFromFileFileName;
    public String                szReportFromFileFileNameEncoding;
        

    public EXEC_FLAG_TYPE	AvCmdType;
    public Node                xmlRootNode;

    public NodeList            ui_automation;
    public UPDATE_STATUS       update_status;
    public int                 runScanJobs;

}