package CheckManager.ScanManager;


import CheckManager.AvUpdateManager.UpdateManager;
import CheckManager.CommonObjects.AV_EXEC_RECORD;
import CheckManager.Config;
import CheckManager.CommonObjects.EXEC_FLAG_TYPE;
import CheckManager.CommonObjects.EXEC_STRUCT;
import CheckManager.CommonObjects.SCAN_JOB;
import CheckManager.JNI;
import CheckManager.ReportsManager.ResultsParser;
import CheckManager.CommonObjects.SCAN_STATUS;
import CheckManager.CommonObjects.UPDATE_STATUS;
import CheckManager.Config.SERVER_ROLE;
import CheckManager.ReportsManager.FromFileReport;
import CheckManager.ReportsManager.GrabWinReport;
import CheckManager.ReportsManager.LogFileReport;
import CheckManager.ReportsManager.ReportParserType;

import CheckManager.Utils.Common;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import org.apache.log4j.Logger;


public class ScanManager implements Runnable
{
    private static final            Logger log           = Logger.getLogger(ScanManager.class);
    private static final boolean    isDebugEnabled       = log.isDebugEnabled();
    
    public int dTotalProcRun = 0;
    public int shellScansRun = 0;
    private final Object                           jobsCriticalSection;
    private ArrayList    <SCAN_JOB>          scanJobs;
    private ArrayList    <AV_EXEC_RECORD>     avsInfo;
    private ArrayList    <EXEC_STRUCT>       runScans;
    private ArrayList    <Integer>           preCreateProcess;

   
    public ScanManager() throws Exception
    {
        jobsCriticalSection = new Object();
        scanJobs            = new ArrayList<SCAN_JOB>();
        avsInfo             = new ArrayList<AV_EXEC_RECORD>();
        preCreateProcess    = new ArrayList<Integer>();
        runScans            = new ArrayList<EXEC_STRUCT>();

        loadOptions();
        
        if(Config.server_role == SERVER_ROLE.scanner)
        {
            ResultsParser.start();
            log.debug("ResultsParser started.");
            new Thread(this).start(); 
            log.debug("ScanManager started.");
        }
        else
        {
            new UpdateManager(avsInfo).start();
            log.debug("UpdateManager started.");
        }
    }
    
    public void close() throws InterruptedException
    {
        if(preCreateProcess != null)
        {
            while(preCreateProcess.size() > 0)
            {
                JNI.preCreateScanProcessClose(preCreateProcess.get(0));
                preCreateProcess.remove(0);
            }
            preCreateProcess = null;
        }
    }
        
    public SCAN_JOB addScanJob(String szScanFilePath,String[] selAVs) throws Exception
    {
        SCAN_JOB job    = new SCAN_JOB();
	job.FilePath    = szScanFilePath;
        job.selAVs      = selAVs;
        job.status      = SCAN_STATUS.SCAN_PENDING;

	synchronized (jobsCriticalSection)
        {
            scanJobs.add(job);
        }

        if(isDebugEnabled)
        {
            log.debug("Added new scan job "+job.FilePath);
            log.debug("scanJobs size "+scanJobs.size());
        }

        return job;
    }

    public String getScanResults(String szPath,String szCookie) throws Exception
    {
        ByteBuffer retData = null;
        String szRepDirPath = szPath + "\\reports\\";

        ArrayList<ByteBuffer> datas = new ArrayList();
        int datasSize = 0;
        FileInputStream rezOut = null;
	for(AV_EXEC_RECORD avr : avsInfo)
	{

                String pFileName = String.format("%s_Result.txt",avr.szAvName);
                File resultFile = new File(szRepDirPath+pFileName);
		if(resultFile.exists() && resultFile.canRead())
                {
                    try
                    {
                        rezOut = new FileInputStream(resultFile);
                        ByteBuffer tmpData = ByteBuffer.allocate((int)rezOut.getChannel().size());
                        rezOut.getChannel().read(tmpData);
                        datasSize += (int)rezOut.getChannel().size();
                        
                        datas.add(tmpData);

                    }
                    finally
                    {
                        if(rezOut != null) rezOut.close();
                    }
		}

	}


            ByteBuffer tmpBuf = ByteBuffer.allocate(datasSize);
            for(ByteBuffer buf : datas )
            {
                buf.position(0);
                tmpBuf.put(buf);
            }

            retData = tmpBuf;

	return new String((retData != null)?retData.array():"".getBytes());
    }


    public void run()
    {
        Config.totalRunThreads++;
        EXEC_STRUCT curExec = null;

        while(!Config.stopFlag)
        {
            try
            {
                while(!Config.stopFlag)
                {
                    Thread.sleep(100);
                    SCAN_JOB job = null;
                    synchronized (jobsCriticalSection)
                    {
                        if(scanJobs.size() > 0) job = scanJobs.get(0);
                    }

                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    // Create new scan
                    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    if(job != null)
                    {
                        if(job.selAVs.length == 0)  // RUN ALL ANTIVIRUSES
			{
                            for(AV_EXEC_RECORD avs : avsInfo)
			    {
                                EXEC_STRUCT exec = prepareScan(avs,job);
				if(exec != null) runScans.add(exec);
                            }
			}
			else		// RUN SELECTED ANTIVIRUSES
			{
                            for(String selAV : job.selAVs)
                            {

                                EXEC_STRUCT exec = null;
                                int selAvHash = selAV.toLowerCase().hashCode();
				for(AV_EXEC_RECORD avs : avsInfo)
				{
                                    if(selAvHash == avs.AvNameHash)
				    {
                                        exec = prepareScan(avs,job);
					if(exec != null)
                                        {
                                            if(isDebugEnabled) log.debug("Scan prepared for : " + exec.szCmd);
                                            exec.scan_status = SCAN_STATUS.SCAN_PENDING;
                                            runScans.add(exec);
                                        }
                                        else // ERROR
                                        {
                                            job.status = SCAN_STATUS.SCAN_FINISHED_ERROR;
                                            log.error("Failed to prepare scan : ");
                                        }

                                        break;
                                    }
				}

                                if(exec == null)
                                {
                                    job.status = SCAN_STATUS.SCAN_FINISHED_ERROR;
                                    log.debug("job.status = SCAN_STATUS.SCAN_FINISHED_ERROR");
                                }
                            }
			}

			synchronized (jobsCriticalSection)
                        {
                            scanJobs.remove(0);
                        }
                    }


                    for(int i = 0; i < runScans.size(); i++)
                    {
                        curExec = runScans.get(i);

                        //::::::::::::::::::::::::::::::::::::::::::::::::::::::
                        // Run new scan process
                        //::::::::::::::::::::::::::::::::::::::::::::::::::::::
                        if(curExec.scan_status == SCAN_STATUS.SCAN_PENDING)
			{
                            int cpuUsage = JNI.getCpuUsage();
                            if(cpuUsage >= 95 || dTotalProcRun >= 4 &&
                                    curExec.avr.update_status != UPDATE_STATUS.FINISHED)
                            {
                                log.debug("Skip new scan job");
                                log.debug("CpuUsage = "+cpuUsage);
                                log.debug("dTotalProcRun ="+dTotalProcRun);
                                log.debug("curExec.avr.update_status ="+curExec.avr.update_status.toString());

                                Thread.sleep(10);
                                continue;
                            }


                            if(curExec.avr.runScanJobs > 0)
                            {
                                log.debug("AV_STATUS.RUNNING at "+curExec.avr.szAvName);
                               continue;
                            }

                            if(curExec.avr.AvCmdType == EXEC_FLAG_TYPE.EXEC || curExec.avr.AvCmdType == EXEC_FLAG_TYPE.CONSOLE)
                            {
                                log.debug("JNI.createNewScanProcess: "+curExec.szCmd+" : "+curExec.szStartDir+" : "+curExec.avr.AvCmdType.value());
                                curExec.pInfo = JNI.createNewScanProcess(curExec.szCmd,curExec.szStartDir,curExec.avr.AvCmdType.value());
                                log.debug("JNI.createNewScanProcess end");
                            }
                            else if(curExec.avr.AvCmdType == EXEC_FLAG_TYPE.SHELL)
                            {
                                if(isDebugEnabled)
                                {
                                    log.debug("JNI.createNewShellScanProcess");
                                    log.debug("curExec.avr.szAvCmd="+curExec.avr.szAvCmd);
                                    log.debug("curExec.szScanFilePath="+curExec.szScanFilePath);
                                    log.debug("curExec.avr.iShell_lpVerb="+curExec.avr.iShell_lpVerb);
                                    log.debug("curExec.avr.exeFileName="+curExec.avr.exeFileName);
                                    log.debug("curExec.avr.szWinCharTitle = "+curExec.avr.szWinCharTitle);
                                    log.debug("curExec.avr.szWinCharClass = "+curExec.avr.szWinCharClass);
                                }

                                curExec.pInfo = JNI.createNewShellScanProcess(curExec.avr.szAvCmd,
                                                                            curExec.szScanFilePath ,
                                                                            curExec.avr.iShell_lpVerb,
                                                                            curExec.avr.exeFileName,
                                                                            curExec.avr.szWinCharTitle,
                                                                            curExec.avr.szWinCharClass);

                            }
                            else
                            {
                                i--;
                                curExec.scan_status = SCAN_STATUS.SCAN_FINISHED_ERROR;
                                runScans.remove(curExec);
                                log.error("Unknown curExec.avr.AvCmdType - "+curExec.avr.AvCmdType.value());
                                continue;
                            }

                            if(curExec.pInfo != 0)
                            {
                                curExec.avr.runScanJobs++;
                                curExec.scan_status = SCAN_STATUS.SCAN_STARTED;
				dTotalProcRun++;

                                curExec.startTime = Common.currentTimeStampInMillis();
                                JNI.dropInjectedGrabText(curExec.pInfo);
                            }
                            else
                            {
                               log.debug("curExec pInfo = 0");
                               curExec.scan_status = SCAN_STATUS.SCAN_FINISHED_ERROR;
                            }
                        }


                        if(curExec.scan_status == SCAN_STATUS.SCAN_STARTED)
			{
                            if(curExec.ReportParser.isReportReady())
                            {
                                curExec.scan_status = SCAN_STATUS.SCAN_FINISHED_OK;
                            }
                            else if((Common.currentTimeStampInMillis() - curExec.startTime) > Config.scanTimeOutMill)
                            {
                                curExec.scan_status = SCAN_STATUS.SCAN_FINISHED_ERROR;
                            }
                        }


			if(curExec.scan_status == SCAN_STATUS.SCAN_FINISHED_ERROR)
			{
                            scanEndWithError(curExec);
                            i--;
                            continue;
			}


			if(curExec.scan_status == SCAN_STATUS.SCAN_FINISHED_OK)
			{
                            i--;
                            ResultsParser.addReportToParser(curExec);
                            cleanAfterScan(curExec);
		        }

                    } // END FOR
                    curExec = null;
	        }
            }
            catch (Exception e)
            {
                if(curExec != null)
                {
                    scanEndWithError(curExec);
                    curExec = null;
                }

                log.error(e);
            }
        }  // END WHILE

        Config.totalRunThreads--;

    }

    void scanEndWithError(EXEC_STRUCT curExec)
    {
        try
        {
            log.debug(curExec.szReportFileName);
            File reportFile = new File(curExec.szReportFileName);
            FileOutputStream out = new FileOutputStream(reportFile);
            out.write("ERROR".getBytes());
            out.close();
            ResultsParser.addReportToParser(curExec);
        }catch(Exception e){log.error(e);}

        cleanAfterScan(curExec);
    }


    void cleanAfterScan(EXEC_STRUCT curExec)
    {
        curExec.ReportParser.close();
        
        JNI.freePROCESSStruct(curExec.pInfo);
        runScans.remove(curExec);
        if(dTotalProcRun > 0) dTotalProcRun--;
        if(curExec.avr.runScanJobs > 0) curExec.avr.runScanJobs--;

    }



   EXEC_STRUCT prepareScan(AV_EXEC_RECORD avs, SCAN_JOB job) throws Exception
    {
        EXEC_STRUCT exec = new EXEC_STRUCT(avs);


            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            // <avCmd>
            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            String szCmd="";
            String szAvStartDir="";
            if(avs.AvCmdType != avs.AvCmdType.SHELL)
            {
                szCmd = avs.szAvCmd.replace("{%AV_ROOT_PATH%}", Config.avRootPath);
                szAvStartDir = szCmd.substring(0,szCmd.lastIndexOf("\\"));
                exec.szStartDir = szAvStartDir;
            }

            String szReportDir = job.FilePath.substring(0,job.FilePath.lastIndexOf("\\")) + "\\reports";//\\
            String fileName = job.FilePath.substring(job.FilePath.lastIndexOf("\\")+1);
            String checkFileName = String.format("\\%X_%s",Config.rand.nextInt(),fileName);
            exec.szScanFilePath = job.FilePath.substring(0,job.FilePath.lastIndexOf("\\")) + "\\checks";
            Common.createDirs(exec.szScanFilePath);
            exec.szScanFilePath += checkFileName;
            Common.copyFile(job.FilePath,exec.szScanFilePath);
            log.debug("copying file to scan "+exec.szScanFilePath);
            log.debug(avs.szAvName);


            Common.createDirs(szReportDir);

            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            //<scanFileCmd>
            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            String szScanFileCmd = String.format(avs.szScanFileCmd,exec.szScanFilePath);

            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            //<reportCmd>
            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            String szReportCmd = "";
            if(!avs.szReportCmd.isEmpty())
            {
                szReportCmd = String.format(avs.szReportCmd,szReportDir);
	    }

            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            //<reportFileName>new String()
            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            exec.szReportFileName = szReportDir + "\\" + avs.szReportFileName;
log.debug("prepareScan: exec.szReportFileName = "+exec.szReportFileName);

            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            //<scanOptions>
            //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
            String szScanOptions="";
            if(avs.AvCmdType != avs.AvCmdType.SHELL)
            {
                szScanOptions = avs.szScanOptions.replace("{%AV_CURRENT_DIR%}",szAvStartDir);
            }



            if(avs.AvCmdType != avs.AvCmdType.SHELL)
            {
                exec.szCmd = String.format("%s %s %s %s",szCmd,szScanOptions,szScanFileCmd,(!szReportCmd.isEmpty()) ? szReportCmd : " ");
            }


            if(avs.parserType == ReportParserType.AUTOMATION)
                exec.ReportParser = new GrabWinReport(exec);
            else if(avs.parserType == ReportParserType.REPORTFROMFILE)
                exec.ReportParser = new FromFileReport(exec);
            else
                exec.ReportParser = new LogFileReport(exec);

            exec.ResParserData = ResultsParser.init(exec);

         return exec;
    }


    void loadOptions() throws Exception
    {
        byte[] avXmlCfg = JNI.loadAvConfig();
        String HookXmlConfig = "";
        Document avXmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(avXmlCfg));
        XPath xpath = XPathFactory.newInstance().newXPath();

        Element rootElem = avXmlDoc.getDocumentElement();
        NodeList nodes = rootElem.getElementsByTagName("av");
        for(int i = 0; i < nodes.getLength(); i++)
        {
            if(nodes.item(i).getNodeType() != Node.TEXT_NODE)
            {
                AV_EXEC_RECORD avRec = new AV_EXEC_RECORD();

                avRec.update_status = UPDATE_STATUS.FINISHED;
                avRec.xmlRootNode = nodes.item(i);
                avRec.szAvName = nodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                //AvStatus.put(avRec.szAvName,AV_STATUS.STOPPED);

                avRec.AvNameHash = avRec.szAvName.toLowerCase().hashCode();

                // SCANNER
                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                // <exeFileName>avgscanx.exe</exeFileName>
                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                avRec.exeFileName = (String)xpath.compile("scanner/exeFileName").evaluate(nodes.item(i),XPathConstants.STRING);

                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                /*<cmdLine>
                *    <avCmd exec_type="exec"><![CDATA[{%AV_ROOT_PATH%}\avg\avgscanx.exe]]></avCmd>
                *    <logFileCmd><![CDATA[/SCAN="%s"]]></logFileCmd>
                *    <scanOptions><![CDATA[/HEUR]]></scanOptions>
                *    <reportCmd><![CDATA[/REPORT="%s\avg_Report.txt"]]></reportCmd>
                *    <shell_lpVerb type="string|number"></shell_lpVerb>
                *</cmdLine>*/
                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                avRec.szAvCmd = (String)xpath.compile("scanner/cmdLine/avCmd").evaluate(nodes.item(i),XPathConstants.STRING);
                String szPaths[] = avRec.szAvCmd.split("\\{\\%FIND_FILE\\%\\}");
                if(szPaths.length > 1)
                {
                     avRec.szAvCmd = Common.getFileFullPath(szPaths[0],szPaths[1]);
                }

                String cmdType = (String)xpath.compile("scanner/cmdLine/avCmd/@execType").evaluate(nodes.item(i),XPathConstants.STRING);
                if(!cmdType.isEmpty())
                    avRec.AvCmdType = EXEC_FLAG_TYPE.valueOf(cmdType.toUpperCase());
                
                avRec.szScanFileCmd = (String)xpath.compile("scanner/cmdLine/scanFileCmd").evaluate(nodes.item(i),XPathConstants.STRING);
                avRec.szScanOptions = (String)xpath.compile("scanner/cmdLine/scanOptions").evaluate(nodes.item(i),XPathConstants.STRING);
                avRec.szReportCmd = (String)xpath.compile("scanner/cmdLine/reportCmd").evaluate(nodes.item(i),XPathConstants.STRING);

                String shellType = (String)xpath.compile("scanner/cmdLine/shellLpVerb/@type").evaluate(nodes.item(i),XPathConstants.STRING);
                if(shellType != null && !shellType.isEmpty())
                {
                    String lpVerb = (String)xpath.compile("scanner/cmdLine/shellLpVerb").evaluate(nodes.item(i),XPathConstants.STRING);
                    if(lpVerb != null && !lpVerb.isEmpty())
                    {
                        if(shellType.equalsIgnoreCase("string"))
                        {
                            avRec.szShell_lpVerb = lpVerb;
                        }
                        else
                        {
                            avRec.iShell_lpVerb = Integer.parseInt(lpVerb);
                        }
                    }
                }
                
                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                /*  <reportParser type="logfile">
                        <logfile>
                            <logFileName>asquared_Report.txt</logFileName>
                        </logfile>

                        <dbVersionRegEx><![CDATA[Last[\s]*update:[\s]*([\d\.\s:]*)]]></dbVersionRegEx>
                        <progVerRegEx><![CDATA[Command[\s]*Line[\s]*Scanner[\s]*[\W\s]*Version[\s]*([\d.]*)]]></progVerRegEx>
                        <foundInfectFlagRegEx type="bool"><![CDATA[Found[\s]*Files:[\s]*([\d]*)]]></foundInfectFlagRegEx>
                        <foundStringRegEx>
                            <regEx><![CDATA[detected:[\s]([^\r\n]*)]]></regEx>
                        </foundStringRegEx>
                        <scanEndRegEx><![CDATA[Scan[\s]*time[\s:\d]*]]></scanEndRegEx>
                    </reportParser>
                */
                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                String rtype = (String)xpath.compile("scanner/reportParser/@type").evaluate(nodes.item(i),XPathConstants.STRING);
                avRec.parserType = ReportParserType.valueOf(rtype.toUpperCase());

                avRec.szReportFileName = (String)xpath.compile("scanner/reportParser/logFileName").evaluate(
                        nodes.item(i),XPathConstants.STRING);
                log.debug("avRec.szReportFileName="+avRec.szReportFileName);
                log.debug("avRec.szAvName="+avRec.szAvName);

                // AUTOMATION
                if(avRec.parserType == ReportParserType.AUTOMATION)
                {
                    avRec.ui_automation = ((Node)xpath.compile("scanner/reportParser/automation").evaluate(
                            nodes.item(i),XPathConstants.NODE)).getChildNodes();
                }

                // REPORTFROMFILE
                else if(avRec.parserType == ReportParserType.REPORTFROMFILE)
                {
                    avRec.ui_automation = ((Node)xpath.compile("scanner/reportParser/reportFromFile/automation").evaluate(
                            nodes.item(i),XPathConstants.NODE)).getChildNodes();
                    avRec.szReportFromFileFileName = Common.processTemplates((String)xpath.compile("scanner/reportParser/reportFromFile/@name").evaluate(
                            nodes.item(i),XPathConstants.STRING),null);
                    avRec.szReportFromFileFileNameEncoding = (String)xpath.compile("scanner/reportParser/reportFromFile/@encoding").evaluate(
                            nodes.item(i),XPathConstants.STRING);

                    avRec.ReportParserNode = nodes.item(i);
                }

                /////////////////////////////////////////////////////////////////
                // INJECT


                NodeList hookNodes = (NodeList)xpath.compile("inject").evaluate(
                            nodes.item(i),XPathConstants.NODESET);

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT,"yes");
                StreamResult result = new StreamResult(new StringWriter());
                DOMSource source = new DOMSource(hookNodes.item(0));
                transformer.transform(source, result);
                String xmlString = result.getWriter().toString();
                xmlString = xmlString.substring(xmlString.indexOf("?>")+2,xmlString.length());
                HookXmlConfig += xmlString;//+"</item>";
                


                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                // <winCharacteristics>
                //    <title><![CDATA[On-Demand Malware Scanner]]></title>
                //    <class></class>
                //</winCharacteristics>
                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                avRec.szWinCharClass = (String)xpath.compile("scanner/reportParser/winCharacteristics/class").evaluate(
                        nodes.item(i),XPathConstants.STRING);
                avRec.szWinCharTitle = (String)xpath.compile("scanner/reportParser/winCharacteristics/title").evaluate(
                        nodes.item(i),XPathConstants.STRING);


                
                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                // avCmd/@preload
                //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

                avsInfo.add(avRec);

                String avCmd = avRec.szAvCmd.replace("{%AV_ROOT_PATH%}",Config.avRootPath);
                File avFile = new File(avCmd);
                if(avFile.exists())
                {
                    avsInfo.add(avRec);

                }
                else if(avRec.AvCmdType == EXEC_FLAG_TYPE.SHELL && JNI.isShellAvExist(avCmd) == true)
                {
                    avsInfo.add(avRec);
                }
                else
                {
                    log.error("Antivirus "+avRec.szAvName+" not found. AvCmd = "+avCmd);
                }
            }
        }


        if(avsInfo.isEmpty()) throw new Exception("Critical ERROR, avsInfo empty.");

        if(!HookXmlConfig.isEmpty())
        {
            log.debug(HookXmlConfig);
            JNI.InitHookEngine("<root>"+HookXmlConfig+"</root>");
        }
    }
   

   

    

}
