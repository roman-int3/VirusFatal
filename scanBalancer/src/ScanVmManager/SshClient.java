

package ScanVmManager;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import java.io.InputStream;


public class SshClient extends JSch implements UserInfo
{
    private Session session;

    SshClient(String host,int port,String login,String passwd) throws Exception
    {
        super();
        session = getSession(login,host,port);
        session.setPassword(passwd);
        session.setUserInfo(this);
        session.connect();
    }

    public void close()
    {
        session.disconnect();
    }

    public String execCmd(String cmd) throws Exception
    {
        InputStream in = null;
        StringBuilder retDataBuf = new StringBuilder();
        byte[] tmp=new byte[1024];
        Channel channel = null;

        try
        {
            channel=session.openChannel("exec");
            channel.setInputStream(null);
            ((ChannelExec)channel).setErrStream(System.err);
            ((ChannelExec)channel).setCommand(cmd);
            in = channel.getInputStream();
            channel.connect();

            while(true)
            {
                while(in.available()>0)
                {
                    int i=in.read(tmp, 0, 1024);
                    if(i<0)break;
                    retDataBuf.append(new String(tmp,0,i));
                }
                if(channel.isClosed())
                {
                    break;
                }
                Thread.sleep(100);
            }
        }
        finally
        {
            if(in != null) in.close();
            if(channel.isConnected()) channel.disconnect();
        }

        return retDataBuf.toString();
    }

    public String getPassphrase() {
            throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getPassword() {
            throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean promptPassword(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean promptPassphrase(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean promptYesNo(String string) {
            return true;
    }

    public void showMessage(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
    }



}
