

package CheckManager.ReportsManager;

import CheckManager.CommonObjects.INFECT_FLAG_TYPE;
import java.util.regex.Pattern;


public interface Parser
{

    public boolean isReportReady() throws Exception;
    public void close();
     public String getReportFromFile(String fileName,String encoding,int searchlimit) throws Exception;


}
