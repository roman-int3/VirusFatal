package CheckManager;

import org.apache.log4j.Logger;

public class JNI
{
    private static final            Logger log           = Logger.getLogger(JNI.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();

    native static public byte[] loadAvConfig() throws Exception;
    native static public int getCpuUsage();
    
    native static public void preCreateScanProcessClose(int pi);
    native static public int preCreateScanProcess(String fillePath);
    native static public int createNewScanProcess(String avCmd,String avStartDir,int execType);
    native static public boolean isProcessFinished(int pi);
    native static public boolean writeProcessOutputData(int pi,String outFileName);
    native static public void freePROCESSStruct(int pi);
    native static public void terminateProcess(int pi);
    native static public int createNewShellScanProcess(String avCLSID,
                                                       String checkFilePath,
                                                       int menuID,
                                                       String avExeName,
                                                       String MainWinTitle,
                                                       String MainWinClass);
    native static public boolean isShellAvExist(String avCLSID);

    native static public boolean sendMessage(String elemName,
                                                String className,
                                                int elemID,
						int msg,
						int wParam,
						int lParam,
						int pi);
    
    native static public boolean clickByString(String elemName,int pi);
    
    
    native static private double getFileEntropy(String sFileName);

    native static public void InitEngine();
    native static public void InitHookEngine(String HookXmlConfig);
    native static public void StopHookEngine();

    native static public String getInjectedGrabText(int pi,String from);
    native static public void dropInjectedGrabText(int pi);

    native static public boolean tray_DblClick(String ttPattern,String fileName);
    native static public String tray_GetTooltipText(String ttPattern,String fileName);
    native static public int createDummySpi(String avExeName,String MainWinTitle,String MainWinClass);
    native static public boolean tray_TrackPopupMenu(String ttPattern,String fileName);

    native static public String getClipboardText();
    native static public String tray_GetMenuText(String clickType,int count,String fileName,String tooltipRegEx);
    native static public String getWinTitle(String fileName,String winTitle);
    native static public boolean waitWin(String fileName,String winTitle);
    native static public boolean menuClick(String fileName,String itemPath,String type,String winTitle);
    native static public String regGetValue(String key,String path);
    native static public String menuGetItemState(String fileName,String itemPath,String winTitle);
    native static public String getProcessOutputData(int pi);
 
    
    

    public static void init()
    {
        try
        {
            if(isDebugEnabled)
            {
                log.debug("Loading JNI dll "+Config.rootPath + "avManager.dll");
            }
            System.load(Config.rootPath + "avManager.dll");
            log.debug("InitEngine() BEGIN");
            InitEngine();
            log.debug("InitEngine() END");
        }
        catch (Throwable e)
        {
            log.error(e);
            System.exit(0);
        }
    }

    static public String GetFileEntropy(String sFileName)
    {
        return String.format("<Entropy>%.2f</Entropy>",getFileEntropy(sFileName));
    }
   
}
