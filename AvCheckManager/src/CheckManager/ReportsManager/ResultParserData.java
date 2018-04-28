
package CheckManager.ReportsManager;

import CheckManager.CommonObjects.INFECT_FLAG_TYPE;
import CheckManager.ReportsManager.ResultsParser.ResultsParserType;


public class ResultParserData
{
    public ResultsParserType type;
    public String szFoundStringRegEx;
    public String szDbVersionRegEx;
    public String szFoundInfectFlagRegEx;
    public String szProgVerRegEx;
    public String szScanEndRegEx;
    public String sqlDbFileName;
    public String sqlQuery;
    public int FoundStringRegExMatchLimit;
    public INFECT_FLAG_TYPE	InfectFlagType;
}