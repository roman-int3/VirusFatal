
package AutoChecker;

import SqlManager.SqlManager;
import VFCheckLib.*;
import VFUserLib.User;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import scanBalancer.Config;
import scanBalancer.Utils;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class AutoChecker implements Runnable
{
    private static Logger log = Logger.getLogger(AutoChecker.class);
    public static long lastCheckTime = 0;
    private static SqlManager sql;
    private static final String selectAcChecksQuery = "SELECT ch.id, ch.user_id, "
                                    + "f.uid,f.name,f.fsid,f.id AS file_id FROM "
                                    + "checks ch LEFT JOIN files f ON "
                                    + "ch.file_id=f.id WHERE ch.check_type='ac'";
    private List <Check>sentReportsQueue;
    private static final String reportXml = "<root><jabber_id>%s</jabber_id><message><![CDATA[%s]]></message></root>";
    private static final String report_body = "AutoCheck has been successfully completed!\r\n"+
                                                  "File name: %s\r\n"+
                                                  "Check time: %s\r\n"+
                                                  "File info: \r\n%s\r\n\r\n"+
                                                  "Check info: \r\n%s\r\n"+
                                                  "\r\nBest regards";
    private static final String smtpRelayHost = "localhost";
    private static final String smtpSubject = "Autocheck report.";
    private static final String smtpFrom = "localhost";

    public void run()
    {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        PreparedStatement pstmt2 = null;
        ResultSet rs2 = null;

        sentReportsQueue = Collections.synchronizedList(new ArrayList());
        
        try
        {
            ArrayList <Check>checks = new ArrayList<Check>();
            sql = new SqlManager(Config.mysqlHost,Config.mysqlPort,
                                    Config.mysqlUser,Config.mysqlPwd,
                                    "virusfatal",50,10);
            
            new Thread(new CheckReportSender(),"CheckReportSender").start();
            new Thread(new JabberReportsSender(),"JabberReportsSender").start();

            while(!Config.stopFlag)
            {
                Thread.sleep(10000);
            
                try
                {
                    while(!Config.stopFlag)
                    {
                        Thread.sleep(10000);
                //TODO:
                        if(Utils.currentTimeStamp() - lastCheckTime > Config.acCheckTimeOut)
                        {
                            // Start autocheck
                            checks.clear();
                            con = sql.dbconn.getConnection();
                            pstmt = con.prepareStatement(selectAcChecksQuery);
                            rs = pstmt.executeQuery();
                            if(rs.next())
                            {
                                do
                                {
                                    String userID = rs.getString("user_id");
                                    User user = new User(sql,userID,null);
                                    if(user.checkBalance(Check.AUTO_CHECK))
                                    {
                                        pstmt2 = con.prepareStatement("SELECT av_id FROM ac_check_av WHERE file_id=?");
                                        String file_id = rs.getString("file_id");
                                        pstmt2.setString(1, file_id);
                                        rs2 = pstmt2.executeQuery();
                                        if(rs2.next())
                                        {
                                            ArrayList <Integer>avs_ids = new ArrayList<Integer>();
                                            
                                            do
                                            {
                                                avs_ids.add(rs2.getInt("av_id"));
                                            }while(rs2.next());
                                            
                                            String fsid = rs.getString("fsid");
                                            Check check = new Check(sql,
                                                    rs.getString("name"),
                                                    null,
                                                    userID,
                                                    fsid,
                                                    file_id,
                                                    rs.getString("id"),
                                                    avs_ids,
                                                    null,
                                                    Check.AUTO_CHECK,
                                                    Config.scan_balancer_url);
                                            // check.create();
                                            if(check.sb_StartScan())
                                            {
                                                check.assignGroupID();
                                                user.reduceBalance();
                                                checks.add(check);
                                            }
                                        }
                                    }
                                }while(rs.next());
                                                                
                                while(!checks.isEmpty())
                                {
                                    try
                                    {
                                        for(int i=0; i < checks.size(); i++)
                                        {
                                            Check check = checks.get(i);
                                            if(check.ScanStatus == Check.SCAN_STATUS_FINISH)
                                            {
                                                checks.remove(i);
                                                sentReportsQueue.add(check);
                                            }
                                            else
                                            {
                                                check.sb_getScanResult();
                                            }

                                            Thread.sleep(1000);
                                        }
                                        Thread.sleep(5000);
                                    }
                                    catch(Exception e)
                                    {
                                        log.error(e);
                                    }
                                }                                                               
                            }
                                    
                            lastCheckTime =  Utils.currentTimeStamp();
                        }
                    }
                }
                catch(Exception e)
                {
                    log.error(e);
                }
            }
        }
        catch(Exception e)
        {
            log.error(e);
        }
    }

    private class CheckReportSender implements Runnable
    {

        public void run()
        {
            Connection          con = null;
            PreparedStatement   pstmt = null;
            ResultSet           rs = null;
            PreparedStatement   pstmt2 = null;
            ResultSet           rs2 = null;
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            try
            {
                while(!Config.stopFlag)
                {
                    Thread.sleep(10000);
                    con = null; pstmt = null; rs = null;
                    try
                    {
                        while(!Config.stopFlag)
                        {
                            if(!sentReportsQueue.isEmpty())
                            {
                                for(int i=0; i < sentReportsQueue.size(); i++)
                                {
                                    Check check = sentReportsQueue.get(i);
                                    pstmt = con.prepareStatement("SELECT u.login, "
                                            + "u.report_email, u.is_report_sent, "
                                            + "u.sent_method, u.jabber FROM users u "
                                            + "LEFT JOIN checks c ON u.id=c.user_id WHERE c.id=?");
                                    pstmt.setInt(1, check.check_id);
                                    rs = pstmt.executeQuery();
                                    if(rs.next())
                                    {
                                        String isRepSent = rs.getString("is_report_sent");
                                        String sentMethode = rs.getString("sent_method");
                                        if(isRepSent.equalsIgnoreCase("Y"))
                                        {
                                            
                                            pstmt2 = con.prepareStatement("SELECT f.name,f.file_info, c.report_url "
                                                    + "FROM files AS f LEFT JOIN checks c ON c.file_id=f.id "
                                                    + "WHERE c.id=?");
                                            pstmt2.setInt(1, check.check_id);
                                            rs2 = pstmt2.executeQuery();
                                            if(!rs2.next())
                                            {
                                                sentReportsQueue.remove(i);
                                                rs2.close();pstmt2.close();rs2 = null;pstmt2 =null;
                                                continue;
                                            }
                                            
                                            String FileName = rs2.getString("name");
                                            String file_info = rs2.getString("file_info");
                                            String report_url = rs2.getString("report_url");
                                            rs2.close();pstmt2.close();rs2 = null;pstmt2 =null;

                                            pstmt2 = con.prepareStatement("SELECT c.result, c.last_check, a.name FROM check_details c "
                                                    + "LEFT JOIN antiviruses a ON a.id=c.av_id WHERE c.check_id=? AND c.group_id=?");
                                            pstmt2.setInt(1,check.check_id);
                                            pstmt2.setInt(2,check.check_group_id);
                                            rs2 = pstmt2.executeQuery();
                                            if(!rs2.next())
                                            {
                                                sentReportsQueue.remove(i);
                                                rs2.close();pstmt2.close();rs2 = null;pstmt2 =null;
                                                continue;
                                            }
                                            
                                            String last_check = rs2.getString("last_check");
                                            
                                            String FileInfo = "";
                                            String CheckInfo = "";
                                            Document xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(file_info.getBytes()));
                                            NodeList nodes = xmlDoc.getChildNodes();
                                            
                                            for(int j=0; j < nodes.getLength(); j++)
                                            {
                                               Node node = nodes.item(j);
                                               FileInfo += "\t"+node.getNodeName()+": "+node.getNodeValue()+"\r\n";
                                            }
                                            
                                            do
                                            {
                                                CheckInfo += "\t"+rs2.getString("name")+"\t-\t"+rs2.getString("result")+"\r\n";
                                            }while(rs2.next());
                                            rs2.close();pstmt2.close();rs2=null;pstmt2=null;
                                            
                                            String reportBody = String.format(report_body,
                                                    FileName,
                                                    last_check,
                                                    FileInfo,
                                                    CheckInfo);
                                            
                                            if(sentMethode.equalsIgnoreCase("jabber"))
                                            {
                                                File reportFile;

                                                String reportData = String.format(reportXml,
                                                                        rs.getString("jabber"),
                                                                        reportBody);

                                                do
                                                {
                                                    reportFile = new File(Config.jabber_reports_dir+"/"+Math.abs(new Random().nextInt()));
                                                }while(!reportFile.createNewFile());

                                                reportFile.setReadable(false);
                                                FileOutputStream os = new FileOutputStream(reportFile);
                                                os.write(reportData.getBytes());
                                                os.close();
                                                reportFile.setReadable(true);


                                            }
                                            else if(sentMethode.equalsIgnoreCase("email"))
                                            {
                                                Properties properties = System.getProperties();
                                                properties.setProperty("mail.smtp.host", smtpRelayHost);
                                                Session session = Session.getDefaultInstance(properties);

                                                try{
                                                    MimeMessage message = new MimeMessage(session);
                                                    message.setFrom(new InternetAddress(smtpFrom));
                                                    message.addRecipient(Message.RecipientType.TO,
                                                                new InternetAddress(rs.getString("report_email")));
                                                    message.setSubject(smtpSubject);
                                                    message.setText(reportBody);
                                                    Transport.send(message);
                                                }
                                                catch (MessagingException mex)
                                                {
                                                    log.error(mex);
                                                }


                                            }

                                            sentReportsQueue.remove(i);
                                        }
                                    }
                                }
                            }

                            Thread.sleep(10000);
                        }
                    }
                    catch(Exception e)
                    {
                        if(rs != null) rs.close();
                        if(pstmt != null) pstmt.close();
                        if(con != null) con.close();
                    }
                }
            }
            catch(Exception e2){}
        }

    }
    
    
}
