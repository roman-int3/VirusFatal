package CheckManager.ScanSession;


public class SessionID
{
	class CSID
	{
		String szCheckType;
		String szTimeStamp;
		String szUploadDir;
	}

	private final static int CSID_SIZE = 2+8+8;
	private String autoCheckDir;
	private String manualCheckDir;
	private CSID id;

	public SessionID(String aCheckDir,String mCheckDir)
	{
            autoCheckDir = aCheckDir;
            manualCheckDir = mCheckDir;
            id = new CSID();
	}

	public boolean decode(byte[] sID)//
	{
            if(sID.length < 0 || sID[0] < 'a' && sID[0] > 'z'
                    || sID[1] < 'a' && sID[1] > 'z' || sID.length != CSID_SIZE) return false;
            id.szCheckType = new String(sID,0,2);
            id.szTimeStamp = new String(sID,2,8);
            id.szUploadDir = new String(sID,10,8);
            return true;
	}

	public String encode(String checkType,long timestamp,int rndID)
	{
            return checkType + String.format("%08X%08X",timestamp,rndID);
	}

	public String buildPatchFromSID(String sID)
	{
            String retData = "";

            if(decode(sID.getBytes()))
            {
                if(id.szCheckType.equalsIgnoreCase("ac"))
		{
                    retData = String.format("%s\\%s\\%s",autoCheckDir,id.szTimeStamp,id.szUploadDir);
		}
		else if(id.szCheckType.equalsIgnoreCase("mc"))
		{
                    retData = String.format("%s\\%s\\%s",manualCheckDir,id.szTimeStamp,id.szUploadDir);
		}
            }

            return retData;
	}
}
