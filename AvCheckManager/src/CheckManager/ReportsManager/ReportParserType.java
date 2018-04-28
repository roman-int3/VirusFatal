

package CheckManager.ReportsManager;


public enum ReportParserType
{
    REPORTFROMFILE("reportFromFile"),
    AUTOMATION("automation"),
    LOGFILE("logFile");

    private String szType;
    private int Type;
    ReportParserType(String type)
    {
        szType = type;
        Type = type.hashCode();
    }

    public String toString(){return szType;}
}
