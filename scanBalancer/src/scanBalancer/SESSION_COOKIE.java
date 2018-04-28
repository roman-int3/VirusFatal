package scanBalancer;



import java.util.ArrayList;



public class SESSION_COOKIE
{
    public String cacheFilesPath;
    private String rootDir;
    private ArrayList<String> cookieArr = new ArrayList<String>();
    
    SESSION_COOKIE(String rootDir)
    {
        this.rootDir = rootDir;
        cacheFilesPath = Config.ReportsCachePath+"/"+rootDir;
    }
    
    public void decode(String encodedStr)
    {
        String[] cookieStrArr = encodedStr.split("-");
        
        for(int i = 0; i < cookieStrArr.length; i++)
        {
            if(cookieStrArr[i].isEmpty()) continue;
            cookieArr.add(cookieStrArr[i]);
        }
    }
            
    public String encode()
    {
        String result = "";
        for(String cookie:cookieArr)
        {
            result += cookie+"-";
        }

        return result;
    }

    public boolean isExist(String searchCookie)
    {
        return cookieArr.indexOf(searchCookie) != -1;
    }

    public void add(String cookie)
    {
        cookieArr.add(cookie);
    }


    
}
