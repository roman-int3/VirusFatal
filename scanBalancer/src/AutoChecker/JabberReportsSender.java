

package AutoChecker;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.w3c.dom.Document;
import scanBalancer.Config;

public class JabberReportsSender implements Runnable
{
    private static Logger log = Logger.getLogger(JabberReportsSender.class);
    private ChatManager chatmanager;
    
    public void run()
    {
        ConnectionConfiguration jabconfig = new ConnectionConfiguration(Config.acJabberHost,
                                                Integer.parseInt(Config.acJabberPort));
        jabconfig.setCompressionEnabled(true);
        XMPPConnection jabber = null;
        MessageListener dummyListener = new MessageListener()
                                                    {public void processMessage(Chat chat, Message message) {}};
        while(!Config.stopFlag)
        {
            
            try
            {
                Thread.sleep(10000);
                if(jabber != null && jabber.isConnected())
                {
                    try
                    {
                        File repotsDir = new File(Config.jabber_reports_dir);
                        File[] files = repotsDir.listFiles();
                        for(File file:files)
                        {
                            if(file.canRead() && file.length() > 0 && file.isFile())
                            {
                                Document xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
                                XPath xpath = XPathFactory.newInstance().newXPath();
                                String jabberID = (String)xpath.compile("//jabber_id").evaluate(xmlDoc, XPathConstants.STRING);
                                String message = (String)xpath.compile("//message").evaluate(xmlDoc, XPathConstants.STRING);
                                Chat chat = chatmanager.createChat(jabberID,dummyListener);
                                
                                chat.sendMessage(message);
                                chat.removeMessageListener(dummyListener);
                                
                                file.delete();
                            }
                        }
                    }
                    catch(Exception e)
                    {
                        log.error(e);
                    }
                
                }
                else
                {
                    jabber = new XMPPConnection(jabconfig);
                    jabber.connect();
                    jabber.login(Config.acJabberID, Config.acJabberPwd);
                    chatmanager = jabber.getChatManager();
                           
                    Presence presence = new Presence(Presence.Type.available);
                    presence.setStatus("Ready");
                    jabber.sendPacket(presence);
                }
            }
            catch(Exception e)
            {
                log.error(e);
                if(jabber != null) jabber.disconnect();
                jabber = null;
            }
        }
    }
}
