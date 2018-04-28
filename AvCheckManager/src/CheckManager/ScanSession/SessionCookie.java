package CheckManager.ScanSession;



import java.math.BigInteger;
import java.util.ArrayList;

public class SessionCookie
{
    private ArrayList <Integer> iCookie;
    private String delim = ";";

    public SessionCookie(String cookieDelim )
    {
        this.delim = cookieDelim;
        this.iCookie = new ArrayList<Integer>();
    }

    public int unserialize(String szCookie)
    {
        this.iCookie.clear();
	if(!szCookie.isEmpty())
	{
            String[] parts = szCookie.split(delim);
            for(String item : parts)
            {
                BigInteger bi = new BigInteger(item,16);
                this.iCookie.add(bi.intValue());
            }
        }

        return this.iCookie.size();
    }

    public String serialize()
    {
        String retData = "";

        for(int item : this.iCookie)//"0x"+
        {
            retData += Integer.toHexString(item).toUpperCase() + delim;
        }
        return retData;
    }

    public void add(String szStr)
    {
 	this.iCookie.add(szStr.hashCode());
    }

    public boolean isCookie(String szStr)
    {
        return this.iCookie.contains(szStr.hashCode());
    }

}
