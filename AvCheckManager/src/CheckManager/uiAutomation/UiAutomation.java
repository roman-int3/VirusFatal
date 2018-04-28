

package CheckManager.uiAutomation;

import CheckManager.CommonObjects.EXEC_FLAG_TYPE;
import CheckManager.CommonObjects.EXEC_STRUCT;
import CheckManager.CommonObjects.SCAN_STATUS;
import CheckManager.CommonObjects.WIN_MSG;
import CheckManager.JNI;
import CheckManager.ReportsManager.FileEnum;
import CheckManager.Utils.Common;
import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class UiAutomation
{
    private enum BREAK_LEVEL
    {
        CANCEL,
        NEXT_NODE
    };

    private static class CMD_HASHS
    {
        static final int getText = "gettext".hashCode();
        static final int matchRegEx = "matchregex".hashCode();
        static final int clickElem = "clickelem".hashCode();
        static final int clickByString = "clickbystring".hashCode();
        static final int sleep = "sleep".hashCode();
        static final int sendMsg = "sendmsg".hashCode();
        static final int closeApp = "closeapp".hashCode();
        static final int terminateApp = "terminateapp".hashCode();
        static final int exit = "exit".hashCode();
        static final int fileRead = "fileread".hashCode();
        static final int fileGetLastModified = "filegetlastmodified".hashCode();
        static final int getClipboardText = "getclipboardtext".hashCode();
        static final int trayTrackPopupMenu = "traytrackpopupmenu".hashCode();
        static final int menuGetItemState = "menugetitemstate".hashCode();
        static final int regGetValue = "reggetvalue".hashCode();
        static final int menuClick = "menuclick".hashCode();
        static final int waitWin = "waitwin".hashCode();
        static final int getWinTitle = "getwintitle".hashCode();
        static final int trayGetTooltip = "traygettooltip".hashCode();
        static final int setAppContext = "setappcontext".hashCode();
        static final int exec = "exec".hashCode();
        static final int trayGetMenuText = "traygetmenutext".hashCode();
        static final int trayDblClk = "traydblclk".hashCode();

    };

    private static final            Logger log           = Logger.getLogger(UiAutomation.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();

    public boolean isAutomationFinished = false;


    public void automate(EXEC_STRUCT curExec, Node node, Node rootNode,
                                            String grabbedText) throws Exception
    {
       

A:      do
        {
B:          do
            {
                if(!curExec.pgistate.isThisNodeProcessed(node))
                {
                    String nodeName = node.getNodeName();
                    log.debug(nodeName);
                    final int nodeNameHash = nodeName.toLowerCase().hashCode();

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <gettext>
                    if(nodeNameHash == CMD_HASHS.getText)
                    {
                        if(getText(curExec,node,rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }
            
                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <matchRegEx>
                    else if(nodeNameHash == CMD_HASHS.matchRegEx)
                    {
                        if(matchRegEx(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <clickElem>
                    else if(nodeNameHash == CMD_HASHS.clickElem)
                    {
                        if(clickElem(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <clickByString>
                    else if(nodeNameHash == CMD_HASHS.clickByString)
                    {
                        if(clickByString(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <sleep>
                    else if(nodeNameHash == CMD_HASHS.sleep)
                    {
                        if(sleep(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <sendMsg>
                    else if(nodeNameHash == CMD_HASHS.sendMsg)
                    {
                        if(sendMsg(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <closeapp>
                    else if(nodeNameHash == CMD_HASHS.closeApp)
                    {
                        if(closeApp(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <exit>
                    else if(nodeNameHash == CMD_HASHS.exit)
                    {
                         if(exit(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <terminateapp>
                    else if(nodeNameHash == CMD_HASHS.terminateApp)
                    {
                        if(terminateApp(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <fileRead>
                    else if(nodeNameHash == CMD_HASHS.fileRead)
                    {
                        if(fileRead(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <fileGetLastModified>
                    else if(nodeNameHash == CMD_HASHS.fileGetLastModified)
                    {
                        if(fileGetLastModified(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <exec>
                    else if(nodeNameHash == CMD_HASHS.exec)
                    {
                        if(exec(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <getClipboardText>
                    else if(nodeNameHash == CMD_HASHS.getClipboardText)
                    {
                        if(getClipboardText(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <menuClick>
                    else if(nodeNameHash == CMD_HASHS.menuClick)
                    {
                        if(menuClick(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <menuGetItemState>
                    else if(nodeNameHash == CMD_HASHS.menuGetItemState)
                    {
                        if(menuGetItemState(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <regGetValue>
                    else if(nodeNameHash == CMD_HASHS.regGetValue)
                    {
                        if(regGetValue(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <setAppContext>
                    else if(nodeNameHash == CMD_HASHS.setAppContext)
                    {
                        if(setAppContext(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <trayDblClk>
                    else if(nodeNameHash == CMD_HASHS.trayDblClk)
                    {
                        if(trayDblClk(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <trayGetMenuText>
                    else if(nodeNameHash == CMD_HASHS.trayGetMenuText)
                    {
                        if(trayGetMenuText(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <trayGetTooltip>
                    else if(nodeNameHash == CMD_HASHS.trayGetTooltip)
                    {
                        if(trayGetTooltip(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <exec>
                    else if(nodeNameHash == CMD_HASHS.exec)
                    {
                        if(exec(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <waitWin>
                    else if(nodeNameHash == CMD_HASHS.waitWin)
                    {
                        if(waitWin(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <trayTrackPopupMenu>
                    else if(nodeNameHash == CMD_HASHS.trayTrackPopupMenu)
                    {
                        if(trayTrackPopupMenu(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Обработка тега <getWinTitle>
                    else if(nodeNameHash == CMD_HASHS.getWinTitle)
                    {
                        if(getWinTitle(curExec, node, rootNode) == BREAK_LEVEL.CANCEL)
                            break A;
                    }



                    else
                    {
                        log.error("UNKNOWN COMMANDS - "+nodeName);
                        throw new Exception("UNKNOWN COMMANDS - "+nodeName);
                    }






                }

                // Next Child node
                if(!node.hasChildNodes()) break;
            
                log.debug("Process child node "+node.getChildNodes().item(0).getNodeName());
                automate(curExec,node.getChildNodes().item(0),
                                        rootNode,grabbedText);

            }while(false);
         
             // Next Node
             node = node.getNextSibling();

        }while(node != null);
        

    }



    private BREAK_LEVEL clickElem(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(!curExec.pgistate.isThisNodeProcessed(node)) //TODO: вынести в цикл
        {
            if(attrMap != null)
            {
                Node attrNodeID = attrMap.getNamedItem("id");
                Node attrNodeName = attrMap.getNamedItem("name");

                if(attrNodeName != null || attrNodeID != null)
                {

                    String elemName="";
                    int elemID=0;
                    if(attrNodeName != null)
                    {
                        elemName=attrNodeName.getNodeValue();
                    }
                    else
                    {
                        elemID=Integer.parseInt(attrNodeID.getNodeValue());
                    }

                    if(isDebugEnabled)
                    {
                        log.debug("JNI.sendMessage::elemName="+elemName+" elemID="+elemID);
                    }

                    if(!JNI.sendMessage(elemName,
                                        "",
                                        elemID,
                                        WIN_MSG.BM_CLICK.code(),
                                        0,
                                        0,
                                        curExec.pInfo))
                    {
                        log.error("ERROR clickElemByName elemID="+elemID+"elemName="+elemName);
                        return BREAK_LEVEL.CANCEL;
                    }

                    curExec.pgistate.setThisNodeAsProcessed(node);  //TODO: вынести в цикл
                }
                else
                {
                    log.error("attrNodeName or attrNodeID is empty.");
                    return BREAK_LEVEL.CANCEL;  // critical error
                }
            }
            else
            {
                log.error("attrMap is empty.");
                return BREAK_LEVEL.CANCEL; // critical error
            }
        }
        else
        {
            log.debug("skip processing clickElem, already processed.");
        }
        
        return BREAK_LEVEL.NEXT_NODE;
    }

    private BREAK_LEVEL sendMsg(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {

        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            int msg = Integer.parseInt(attrMap.getNamedItem("msg").getNodeValue());
            int wParam = Integer.parseInt(attrMap.getNamedItem("wParam").getNodeValue());
            int lParam = Integer.parseInt(attrMap.getNamedItem("lParam").getNodeValue());
            String elemName="";
            String className="";
            int elemID=0;
            Node attrNode = attrMap.getNamedItem("name");
            if(attrNode != null)
            {
                elemName = attrNode.getNodeValue();
            }
            else
            {
                attrNode = attrMap.getNamedItem("class");
                if(attrNode != null)
                {
                    className = attrNode.getNodeValue();
                }
            }

            attrNode = attrMap.getNamedItem("id");
            if(attrNode != null)
            {
                String tmp = attrNode.getNodeValue();
                if(tmp!=null && !tmp.isEmpty())
                {
                    elemID = Integer.parseInt(tmp);
                }
            }

            if(!JNI.sendMessage(elemName,className, elemID, msg, wParam,
                                            lParam, curExec.pInfo))
            {
                log.error(String.format("JNI.sendMessage FAILED - %s,%s,%d,%d,%d,%d",
                                        elemName,className, elemID, msg, wParam,
                                            lParam));
                return BREAK_LEVEL.CANCEL;
            }

            curExec.pgistate.setThisNodeAsProcessed(node);
        }
        else
        {
            log.error("attrMap is empty.");
            return BREAK_LEVEL.CANCEL; // critical error
        }

        return BREAK_LEVEL.NEXT_NODE;
    }

    private BREAK_LEVEL sleep(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            long timeout = Long.parseLong(attrMap.getNamedItem("ms").getNodeValue());
            Thread.sleep(timeout);

        }
        else
        {
            log.error("attrMap is empty.");
            return BREAK_LEVEL.CANCEL; // critical error
        }

        return BREAK_LEVEL.NEXT_NODE;
    }

    private BREAK_LEVEL matchRegEx(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null && !curExec.pgistate.isThisNodeProcessed(node) ) 
        {
            if(curExec.grabbedText == null) return BREAK_LEVEL.CANCEL;

            Node attrNode = attrMap.getNamedItem("ifMatch");
            if(attrNode != null)
            {
                String matchRegEx = attrNode.getNodeValue();
                String matchType = "";
                attrNode = attrMap.getNamedItem("type");
                if(attrNode != null)
                {
                    matchType = attrNode.getNodeValue();
                }

                Matcher match = Pattern.compile(matchRegEx,
                                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(curExec.grabbedText);
                if(!match.find())
                {
                    if(isDebugEnabled)
                    {
                        log.debug("Match not found - "+matchRegEx);
                        log.debug(curExec.avr.exeFileName);
                    }
                    return BREAK_LEVEL.CANCEL;  // match not found exit from loop
                }
                else
                {
                    if(matchType.equalsIgnoreCase("bool"))
                    {
                        String foundRez = match.group(1);
                        if(!foundRez.isEmpty())
                        {
                            curExec.pgistate.setThisNodeAsProcessed(node);
                            if(Integer.parseInt(foundRez) == 0)
                            {

                                return BREAK_LEVEL.CANCEL;
                            }
                        }
                        else
                        {
                            log.error("foundRez Empty");
                            return BREAK_LEVEL.CANCEL;
                        }
                    }
                    else
                    {
                        curExec.pgistate.setThisNodeAsProcessed(node);
                    }
                }
            }
        }
        
        return BREAK_LEVEL.NEXT_NODE;
    }

    private BREAK_LEVEL clickByString(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String clickName = attrMap.getNamedItem("string").getNodeValue();
            if(!JNI.clickByString(clickName,curExec.pInfo))
            {
                log.error(String.format("JNI.clickByString FAILED - %s",clickName));
                return BREAK_LEVEL.CANCEL;
            }

            curExec.pgistate.setThisNodeAsProcessed(node);
       }
       else
       {
            log.error("attrMap is empty.");
            return BREAK_LEVEL.CANCEL;
       }

       return BREAK_LEVEL.NEXT_NODE;
    }

    private BREAK_LEVEL getText(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        String from = "";
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            from = attrMap.getNamedItem("from").getNodeValue();
        }
        
        if(curExec.avr.AvCmdType != EXEC_FLAG_TYPE.SHELL && JNI.isProcessFinished(curExec.pInfo))
        {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String report = (String)xpath.compile("scanner/ifAppExit/writeReport").evaluate(rootNode,XPathConstants.STRING);
            Common.saveReport(curExec, report);
            isAutomationFinished = true;
            return BREAK_LEVEL.CANCEL;
        }

        curExec.grabbedText = new StringBuilder();

        curExec.grabbedText.append(JNI.getInjectedGrabText(curExec.pInfo,from));
        return BREAK_LEVEL.NEXT_NODE;
    }

    private BREAK_LEVEL terminateApp(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        JNI.terminateProcess(curExec.pInfo);
        curExec.pgistate.setThisNodeAsProcessed(node);
        return BREAK_LEVEL.NEXT_NODE;
    }

    private BREAK_LEVEL exit(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        isAutomationFinished = true;
        return BREAK_LEVEL.CANCEL;
    }

    private BREAK_LEVEL closeApp(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null )
        {
            Node attr = attrMap.getNamedItem("reportFromFile");
            if(attr != null)
            {
                log.debug("reportFromFile attribute found");
                String reportFromFile = attr.getNodeValue();
                reportFromFile = Common.processTemplates(reportFromFile,curExec);
                attr = attrMap.getNamedItem("encoding");
                String encoding = null;
                encoding = attr.getNodeValue();
                log.debug("reportFromFile="+reportFromFile);
                log.debug("encoding="+encoding);
                attr = attrMap.getNamedItem("searchLoopLimit");
                int searchlimit = -1;
                if(attr != null)
                {
                    searchlimit = Integer.parseInt(attr.getNodeValue());
                }
                
                curExec.grabbedText.append(curExec.ReportParser.getReportFromFile(reportFromFile,encoding,searchlimit));
            }
        }
        if(curExec.grabbedText.length() == 0)
        {
            log.debug("closeapp - grabText Empty");
            return BREAK_LEVEL.CANCEL;
        }

        ////////////////////////////////////////////////////////
        // Фильтруем собранный текст.
        String grabText = Common.applyFilters(curExec, curExec.grabbedText.toString());
        
        log.debug("closeapp - szReportFileName - "+curExec.szReportFileName);
        Common.saveReport(curExec, grabText);
        curExec.pgistate.setThisNodeAsProcessed(node);
        isAutomationFinished = true;
        return BREAK_LEVEL.CANCEL;
    }

    private BREAK_LEVEL fileRead(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String filePath = attrMap.getNamedItem("path").getNodeValue();
            String isOurFileFlagRegEx = attrMap.getNamedItem("isOurFileFlagRegEx").getNodeValue();
            String startOff = attrMap.getNamedItem("startOff").getNodeValue();
            String length = attrMap.getNamedItem("length").getNodeValue();
            String encoding = attrMap.getNamedItem("encoding").getNodeValue();
            int readOff=0,readLength = 0;
            if(!startOff.isEmpty()) readOff = Integer.parseInt(startOff);
            if(!length.isEmpty()) readLength = Integer.parseInt(length);

            if(!filePath.isEmpty())
            {
                FileEnum fenum = new FileEnum(filePath,encoding,readOff,readLength);
                while(fenum.hasNext())
                {
                    String fData = fenum.nextFileData();
                    if(!isOurFileFlagRegEx.isEmpty())
                    {
                        Matcher match = Pattern.compile(isOurFileFlagRegEx,Pattern.MULTILINE|Pattern.CASE_INSENSITIVE).matcher(fData);
                        if(!match.find()) continue;
                    }

                    curExec.grabbedText.append(fData);
                    curExec.pgistate.setThisNodeAsProcessed(node);
                    return BREAK_LEVEL.NEXT_NODE;
                }
            }
         }
        
        return BREAK_LEVEL.CANCEL;
    }

    private BREAK_LEVEL fileGetLastModified(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String filePath = attrMap.getNamedItem("path").getNodeValue();
            String addText = attrMap.getNamedItem("addText").getNodeValue();
            if(!filePath.isEmpty())
            {
                File file = new File(filePath);
                long lastMod = file.lastModified();
                DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(lastMod);
                curExec.grabbedText.append(addText);
                curExec.grabbedText.append(dfm.format(date));
                curExec.pgistate.setThisNodeAsProcessed(node);
                return BREAK_LEVEL.NEXT_NODE;
            }
        }
        return BREAK_LEVEL.CANCEL;
    }
    
    private BREAK_LEVEL getClipboardText(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        String result = JNI.getClipboardText();
        if(!result.isEmpty())
        {
            curExec.grabbedText.append(result);

            return BREAK_LEVEL.NEXT_NODE;
        }
        return BREAK_LEVEL.CANCEL;
    }
    
    private BREAK_LEVEL trayTrackPopupMenu(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            String toolTipRegEx = attrMap.getNamedItem("tooltipRegEx").getNodeValue();
            if(JNI.tray_TrackPopupMenu(toolTipRegEx,fileName)) return BREAK_LEVEL.NEXT_NODE;
        }
        return BREAK_LEVEL.CANCEL;
    }

    private BREAK_LEVEL menuGetItemState(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            String itemPath = attrMap.getNamedItem("itemPath").getNodeValue();
            String winTitle = attrMap.getNamedItem("winTitle").getNodeValue();
            String itemState = JNI.menuGetItemState(fileName,itemPath,winTitle);
            if(!itemState.isEmpty())
            {
                curExec.grabbedText.append("menuItemState:");
                curExec.grabbedText.append(itemPath);
                curExec.grabbedText.append("=");
                curExec.grabbedText.append(itemState);
                curExec.grabbedText.append("\r\n");
                return BREAK_LEVEL.NEXT_NODE;
            }
        }
        return BREAK_LEVEL.CANCEL;
    }

    private BREAK_LEVEL regGetValue(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String addText = attrMap.getNamedItem("addText").getNodeValue();
            String key = attrMap.getNamedItem("key").getNodeValue();
            String path = attrMap.getNamedItem("path").getNodeValue();
            String regValue = JNI.regGetValue(key,path);
            if(!regValue.isEmpty())
            {
                curExec.grabbedText.append(addText);
                curExec.grabbedText.append(regValue);
                curExec.grabbedText.append("\r\n");
                return BREAK_LEVEL.NEXT_NODE;
            }
        }
        return BREAK_LEVEL.CANCEL;
    }

    private BREAK_LEVEL menuClick(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            String itemPath = attrMap.getNamedItem("itemPath").getNodeValue();
            String type = attrMap.getNamedItem("type").getNodeValue();
            String winTitle = attrMap.getNamedItem("winTitle").getNodeValue();
            if(JNI.menuClick(fileName,itemPath,type,winTitle))
            {
                return BREAK_LEVEL.NEXT_NODE;
            }
        }
        return BREAK_LEVEL.CANCEL;
    }

    private BREAK_LEVEL waitWin(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            String winTitle = attrMap.getNamedItem("title").getNodeValue();
            if(JNI.waitWin(fileName,winTitle))
            {
                return BREAK_LEVEL.NEXT_NODE;
            }
        }
        return BREAK_LEVEL.CANCEL;
    }
    
    private BREAK_LEVEL getWinTitle(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            String winTitle = attrMap.getNamedItem("titlePattern").getNodeValue();
            winTitle = JNI.getWinTitle(fileName,winTitle);
            if(!winTitle.isEmpty())
            {
                curExec.grabbedText.append(winTitle);
                curExec.grabbedText.append("\r\n");
                return BREAK_LEVEL.NEXT_NODE;
            }
        }
        return BREAK_LEVEL.CANCEL;
    }
    
    private BREAK_LEVEL trayGetTooltip(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            String tooltip = attrMap.getNamedItem("tooltipRegEx").getNodeValue();
            tooltip = JNI.tray_GetTooltipText(tooltip,fileName);
            if(!tooltip.isEmpty())
            {
                curExec.grabbedText.append(tooltip);
                curExec.grabbedText.append("\r\n");
                return BREAK_LEVEL.NEXT_NODE;
            }
        }
        return BREAK_LEVEL.CANCEL;
    }
    
    private BREAK_LEVEL setAppContext(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            String title = attrMap.getNamedItem("title").getNodeValue();
            if(!fileName.isEmpty())
            {
                curExec.avr.exeFileName = fileName;
            }
            if(!title.isEmpty())
            {
                curExec.avr.szWinCharTitle = title;
            }
            return BREAK_LEVEL.NEXT_NODE;
        }
        return BREAK_LEVEL.CANCEL;
    }
    
    private BREAK_LEVEL exec(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String clsid = attrMap.getNamedItem("clsid").getNodeValue();
            String cmd = attrMap.getNamedItem("cmd").getNodeValue();
            String getOutput = attrMap.getNamedItem("getOutput").getNodeValue();
            String path = attrMap.getNamedItem("path").getNodeValue();
            String type = attrMap.getNamedItem("type").getNodeValue();
            String lpVerb = attrMap.getNamedItem("lpVerb").getNodeValue();
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            
            if(type.equalsIgnoreCase("console"))
            {
                curExec.pInfo = JNI.createNewScanProcess(
                                Common.processTemplates(cmd,curExec),
                                null,
                                EXEC_FLAG_TYPE.CONSOLE.value());
                if(curExec.pInfo != 0)
                {
                    if(getOutput.equalsIgnoreCase("true"))
                    {
                        curExec.grabbedText.append(JNI.getProcessOutputData(curExec.pInfo));
                        curExec.grabbedText.append("\r\n");
                    }
                    return BREAK_LEVEL.NEXT_NODE;
                }
            }
            else if(type.equalsIgnoreCase("shell"))
            {
                curExec.pInfo = JNI.createNewShellScanProcess(
                                    clsid,
                                    Common.processTemplates(cmd,curExec) ,
                                    Integer.parseInt(lpVerb),
                                    fileName,
                                    null,
                                    null);
                if(curExec.pInfo != 0)
                {
                    return BREAK_LEVEL.NEXT_NODE;
                }
            }
            else if(type.equalsIgnoreCase("exec"))
            {
                curExec.pInfo = JNI.createNewScanProcess(
                                    Common.processTemplates(cmd,curExec),
                                    null,
                                    EXEC_FLAG_TYPE.EXEC.value());
                if(curExec.pInfo != 0)
                {
                    return BREAK_LEVEL.NEXT_NODE;
                }
            }
        }
        return BREAK_LEVEL.CANCEL;
    }
    
    private BREAK_LEVEL trayGetMenuText(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String clickType = attrMap.getNamedItem("clickType").getNodeValue();
            String count = attrMap.getNamedItem("count").getNodeValue();
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            String tooltipRegEx = attrMap.getNamedItem("tooltipRegEx").getNodeValue();
            String menuText = JNI.tray_GetMenuText(clickType,Integer.parseInt(count),fileName,tooltipRegEx);
            if(!menuText.isEmpty())
            {
                curExec.grabbedText.append(menuText);
                curExec.grabbedText.append("\r\n");
                return BREAK_LEVEL.NEXT_NODE;
            }
        }
        return BREAK_LEVEL.CANCEL;
    }
    
    private BREAK_LEVEL trayDblClk(EXEC_STRUCT curExec, Node node, Node rootNode) throws Exception
    {
        NamedNodeMap attrMap = node.getAttributes();
        if(attrMap != null)
        {
            String fileName = attrMap.getNamedItem("fileName").getNodeValue();
            String tooltipRegEx = attrMap.getNamedItem("tooltipRegEx").getNodeValue();
            if(JNI.tray_DblClick(tooltipRegEx, fileName))
            {
                return BREAK_LEVEL.NEXT_NODE;
            }
        }
        return BREAK_LEVEL.CANCEL;
    }
    
    
    
    
    
    
    
    
    
    
    
    
}

