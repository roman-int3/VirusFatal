package CheckManager.CommonObjects;


import CheckManager.ReportsManager.Parser;
import CheckManager.ReportsManager.ReportsParser;
import CheckManager.ReportsManager.ResultParserData;
import CheckManager.ReportsManager.ResultsParser;
import java.util.ArrayList;
import org.w3c.dom.Node;


 public class EXEC_STRUCT
 {
     public class PGI_STATE
     {
         private ArrayList nodeHashs;

         PGI_STATE()
         {
            nodeHashs = new ArrayList();
         }

         public boolean isThisNodeProcessed(Node node)
         {
            return (this.nodeHashs.indexOf(node.hashCode()) != -1);
         }
         
         public void setThisNodeAsProcessed(Node node)
         {
            this.nodeHashs.add(node.hashCode());
         }
    }

    public Parser ReportParser;
    public ResultParserData ResParserData;
    
    public StringBuilder   grabbedText = new StringBuilder();
    public String          szCmd;
    public String          szStartDir;


        
    public String          szReportFileName;
    public String          szScanFilePath;

    public PGI_STATE       pgistate;
    public AV_EXEC_RECORD   avr;
    public SCAN_STATUS     scan_status;
    

    public long	    startTime;
    public long	    reportWaitStartTime;
    public int             pInfo;
    public int             lastGrabTextHash;

    public EXEC_STRUCT(AV_EXEC_RECORD avr)
    {
        this.avr = avr;
        pgistate = new PGI_STATE();
    }
    
 }
