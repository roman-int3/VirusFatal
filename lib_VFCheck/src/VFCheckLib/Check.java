package VFCheckLib;

import SqlManager.SqlManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.io.AbstractBuffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Check
{
    public static final int SCAN_STATUS_STOP = 0;
    public static final int SCAN_STATUS_PROCESSING = 1;
    public static final int SCAN_STATUS_FINISH = 2;
    public static final int SCAN_STATUS_ERROR = 3;

    public static final String MANUAL_CHECK = "mc";
    public static final String AUTO_CHECK ="ac";
   

    private static final Logger log = Logger.getLogger(Check.class);
    public String lastError = null;
    public String fileinfoXML;
    public String ssjID;
    public String fsid = null;
    public String check_status = "will_be_checked";
    public String check_type;
    public int check_id = 0;
    public String file_name;
    public String user_id;
    private String scan_cookie = null;
    public int check_group_id;
    private ArrayList<Integer> sel_avs_ids;
    private ArrayList<String> sel_avs_names;
    public int ScanStatus = SCAN_STATUS_STOP;
    public ArrayList <HashMap> scanResultsArr;
    public int file_id;
    public String file_path;
    String uploadedFilePath;
    private static SqlManager sql;
    private String scan_balancer_url;
    private static final HttpClient httpClient = new HttpClient();

    static
    {
        try
        {
            httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
            httpClient.setThreadPool(new QueuedThreadPool(250));
            httpClient.start();
        }
        catch(Exception e)
        {
            log.error(e);
            System.exit(0);
        }
    };

    public Check(SqlManager sqlManager,
                    String file_name,
                    String uploadedFilePath,
                    String user_id,
                    String fsid,
                    String fileID,
                    String checkID,
                    ArrayList<Integer> sel_avs_ids,
                    ArrayList<String> sel_avs_names,
                    String check_type,
                    String scan_balancer_url) throws Exception
    {
        this.sql = sqlManager;
        if(fileID != null)
            this.file_id = Integer.parseInt(fileID);
        this.uploadedFilePath = uploadedFilePath;
        this.check_type = check_type;
        this.scanResultsArr = new ArrayList<HashMap>();
        this.user_id = user_id;
        this.file_name = file_name;
        this.sel_avs_names = sel_avs_names;
        this.scan_balancer_url = scan_balancer_url;
        if(checkID != null)
            this.check_id = Integer.parseInt(checkID);
        this.sel_avs_ids = sel_avs_ids;
        if(this.sel_avs_ids == null)
        {
            this.sel_avs_ids = getAvIdsByNames(sel_avs_names);
            this.sel_avs_names = getAvNamesByIds(this.sel_avs_ids);
        }

        if(sel_avs_names == null)
        {
            this.sel_avs_names = getAvNamesByIds(this.sel_avs_ids);
        }

        if(this.sel_avs_ids == null || this.sel_avs_names == null) throw new Exception();
        if(this.sel_avs_ids.size() != this.sel_avs_names.size()) throw new Exception();

        this.fsid = fsid;
    }

    private static String prepareAvsUri(ArrayList<String> antiviruses)
    {
        StringBuilder retString = new StringBuilder();
        int avCount = antiviruses.size();
        for(int i = 0; i < avCount; i++)
        {
           retString.append("/");
           retString.append(antiviruses.get(i));
        }

        return retString.toString();
    }

    private String fs_uploadFileInStore() throws Exception
    {
        String url = "http://"+scan_balancer_url+"/FileStore";
        File uploadFile = new File(uploadedFilePath);

        ContentExchange httpRequest = new ContentExchange();

        httpRequest.setURL(url);
        httpRequest.setMethod("PUT");
        httpRequest.setFileForUpload(uploadFile);
        httpRequest.setRequestHeader("Content-Length",Long.toString(uploadFile.length()) );
        httpRequest.setRequestHeader("FileName", uploadFile.getName());
        httpClient.send(httpRequest);
        int status = httpRequest.waitForDone();
        if(status != httpRequest.STATUS_COMPLETED && httpRequest.getResponseStatus() != 200)
        {
            throw new Exception();
        }

        String response = httpRequest.getResponseContent();
        Document xmlResp = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getBytes()));
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node node = (Node)xpath.compile("//fsid").evaluate(xmlResp.getFirstChild(), XPathConstants.NODE);
        if(node == null) throw new Exception();
        return node.getTextContent();

    }

    private void loadFileInfo()
    {

    }

    private void insertFileInfoToDb() throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;

        try
        {
            con = sql.dbconn.getConnection();

            if(this.check_type.equalsIgnoreCase(AUTO_CHECK))
            {
                if(this.fsid == null)
                {
                    this.fsid = fs_uploadFileInStore();
                }

                pstmt = con.prepareStatement("INSERT INTO files SET id=?, name=?, fsid=?, is_remote='N'");
                pstmt.setString(2, file_name);
                pstmt.setString(3, this.fsid);
            }
            else
            {
                pstmt = con.prepareStatement("INSERT INTO files SET id=?, name=?, is_remote='N'");
                pstmt.setString(2, file_name);
            }

            file_id = Math.abs(new Random().nextInt());
            pstmt.setInt(1, file_id);
            pstmt.execute();
            pstmt.close();
            con.close();
        }
        catch(SQLException e)
        {
            if(pstmt != null) pstmt.close();
            if(con != null) con.close();
            if(e.getErrorCode() == com.mysql.jdbc.MysqlErrorNumbers.ER_DUP_ENTRY)
            {
                insertFileInfoToDb();
            }
            else
            {
                throw e;
            }
        }

    }

    private void insertCheckInfoToDb() throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;

        try
        {
             con = sql.dbconn.getConnection();
             pstmt = con.prepareStatement("INSERT INTO checks SET id=?,user_id=?,"
                     + "file_id=?,status=?,check_type=?,start_date=NOW()");
             pstmt.setString(2, user_id);
             pstmt.setInt(3, file_id);
             pstmt.setString(4, this.check_status);
             pstmt.setString(5, this.check_type);
             check_id = Math.abs(new Random().nextInt());
             pstmt.setInt(1, check_id);
             pstmt.execute();
             pstmt.close();
             con.close();
        }
        catch(SQLException e)
        {
            if(pstmt != null) pstmt.close();
            if(con != null) con.close();
            if(e.getErrorCode() == com.mysql.jdbc.MysqlErrorNumbers.ER_DUP_ENTRY)
            {
                insertCheckInfoToDb();
            }
            else
            {
                throw e;
            }
        }

    }

    public void createCheck() throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;

        try
        {
            if(this.check_type.equalsIgnoreCase("mc"))
            {
                insertFileInfoToDb();
            }
            else
            {

            }


            insertCheckInfoToDb();

             con = sql.dbconn.getConnection();
             pstmt = con.prepareStatement("INSERT INTO check_details SET check_id=?, av_id=?");
             int l=sel_avs_ids.size();
             pstmt.setInt(1, check_id);
             for(int i=0; i < l; i++)
             {
                 int av_id = sel_avs_ids.get(i);
                 pstmt.setInt(2, av_id);
                 pstmt.addBatch();
             }
             pstmt.executeBatch();

             pstmt.close();pstmt=null;

        }
        finally
        {
           if(pstmt != null) pstmt.close();
           if(con != null) con.close();
        }
    }

    public boolean sb_StartScan()
    {
        PreparedStatement pstmt = null;
        Connection con = null;
        boolean retResult = true;

        try
        {
            this.check_status = ((this.check_type.equalsIgnoreCase(MANUAL_CHECK))
                                ?"progress":"will_be_checked");

            String sel_avs = prepareAvsUri(sel_avs_names);
            String url = "http://"+scan_balancer_url+"/StartScan"+sel_avs;

            File uploadFile = null;
            if(uploadedFilePath != null)
                uploadFile = new File(uploadedFilePath);

            ContentExchange httpRequest = new ContentExchange();

            httpRequest.setURL(url);
            httpRequest.setMethod("PUT");

            if(uploadFile != null)
            {
                httpRequest.setFileForUpload(uploadFile);
                httpRequest.setRequestHeader("Content-Length",Long.toString(uploadFile.length()) );
                httpRequest.setRequestHeader("FileName", uploadFile.getName());
                httpRequest.setRequestHeader("FileType", "data");
            }
            else
            {
                httpRequest.setRequestHeader("FileType", "fsid");
                AbstractBuffer content = new ByteArrayBuffer(this.fsid.getBytes("UTF-8"));
                httpRequest.setRequestContent(content);
                httpRequest.setRequestContentType("application/x-www-form-urlencoded;charset=utf-8");
            }

            httpClient.send(httpRequest);
            int status = httpRequest.waitForDone();
            if(status != httpRequest.STATUS_COMPLETED && httpRequest.getResponseStatus() != 200)
            {
                this.lastError = "ERROR";
                throw new Exception();
            }

            String response = httpRequest.getResponseContent();

            Document xmlResp = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getBytes()));
            XPath xpath = XPathFactory.newInstance().newXPath();
            Node node = (Node)xpath.compile("//status").evaluate(xmlResp.getFirstChild(), XPathConstants.NODE);
            if(node != null)
            {
                String statusCode = (String)xpath.compile("@code").evaluate(node, XPathConstants.STRING);
                if(statusCode.equalsIgnoreCase("err"))
                {
                    String errType = (String)xpath.compile("err_msg/@type").evaluate(node, XPathConstants.STRING);
                    if(errType.equalsIgnoreCase("user"))
                    {
                        this.lastError = (String)xpath.compile("err_msg").evaluate(node, XPathConstants.STRING);
                    }
                    else
                    {
                        this.lastError = "ERROR";
                    }

                    throw new Exception();
                }
            }

            ssjID = (String)xpath.compile("//ssjID").evaluate(xmlResp.getFirstChild(), XPathConstants.STRING);

            NodeList fileinfoNodes = (NodeList)xpath.compile("//fileInfo").evaluate(xmlResp.getFirstChild(), XPathConstants.NODESET);


            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT,"yes");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(fileinfoNodes.item(0));
            transformer.transform(source, result);
            fileinfoXML = result.getWriter().toString();
            fileinfoXML = fileinfoXML.substring(fileinfoXML.indexOf("?>")+2,fileinfoXML.length());

            con = sql.dbconn.getConnection();
            pstmt = con.prepareStatement("UPDATE files SET file_info=? WHERE id=?");
            pstmt.setString(1, fileinfoXML);
            pstmt.setInt(2, file_id);
            pstmt.executeUpdate();
        }
        catch(Exception e)
        {
            log.error(e);
            retResult = false;
        }

        try
        {
            if(pstmt != null) pstmt.close();
            if(con != null) con.close();
        }catch(Exception e2){}

        return retResult;
    }

    private int getRelAvCount() throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;
        ResultSet rs = null;
        int retResult = 0;

        try
        {
            con = sql.dbconn.getConnection();
            if(this.check_type.equalsIgnoreCase(MANUAL_CHECK))
	    {
                pstmt = con.prepareStatement("SELECT COUNT(id) AS idcount FROM check_details WHERE check_id=?");
                pstmt.setInt(1,this.check_id);
            }
            else
            {
                pstmt = con.prepareStatement("SELECT COUNT(file_id) AS idcount FROM ac_check_av WHERE file_id=?");
                pstmt.setInt(1,this.file_id);
            }
            
            rs = pstmt.executeQuery();
            if(!rs.next()) throw new Exception();
            retResult = rs.getInt("idcount");
        }
        finally
        {
            if(rs != null) rs.close();
            if(pstmt != null) pstmt.close();
            if(con != null) con.close();
        }

        return  retResult;
    }

    public static ArrayList<Integer> getAvIdsByNames(ArrayList<String> names) throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;
        ResultSet rs = null;
        ArrayList<Integer> retArr = null;

        try
        {
            con = sql.dbconn.getConnection();

            if(names.contains("all"))
            {
                pstmt = con.prepareStatement("SELECT id FROM antiviruses");
            }
            else
            {
                int namesCount = names.size();
                StringBuilder subQuery=new StringBuilder();
                for(int i=0; i < namesCount; i++)
                {
                    subQuery.append("?,");
                }
                subQuery.deleteCharAt(subQuery.length()-1);
                
                pstmt = con.prepareStatement("SELECT id FROM antiviruses WHERE LOWER(name) IN ("+subQuery.toString()+")");
                for(int i=1; i <= namesCount; i++)
                {
                    pstmt.setString(i, names.get(i-1));
                }
                
            }

            rs = pstmt.executeQuery();

            retArr = new ArrayList<Integer>();
            while(rs.next())
            {
                retArr.add(rs.getInt("id"));
            }
        }
        finally
        {
            if(rs != null) rs.close();
            if(pstmt != null) pstmt.close();
            if(con != null) con.close();
        }

        if(retArr == null) throw new Exception();
        return retArr;
    }

    public static ArrayList<String> getAvNamesByIds(ArrayList<Integer> ids) throws Exception //TODO: need check function
    {
        PreparedStatement pstmt = null;
        Connection con = null;
        ResultSet rs = null;
        ArrayList<String> retArr = null;

        try
        {
            con = sql.dbconn.getConnection();
            retArr = new ArrayList<String>();
            int namesCount = ids.size();
            StringBuilder subQuery=new StringBuilder();
            for(int i=0; i < namesCount; i++)
            {
                subQuery.append("?,");
            }
            subQuery.deleteCharAt(subQuery.length()-1);
            pstmt = con.prepareStatement("SELECT name FROM antiviruses WHERE id IN ("+subQuery.toString()+")");
            for(int i=1; i <= namesCount; i++)
            {
                pstmt.setInt(i, ids.get(i-1));
            }
            rs = pstmt.executeQuery();
           
            while(rs.next())
            {
                retArr.add(rs.getString("name"));
            }
     
        }
        finally
        {
            if(rs != null) rs.close();
            if(pstmt != null) pstmt.close();
            if(con != null) con.close();
        }

        return retArr;
    }

    public static int getAvIdByName(String name) throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;
        ResultSet rs = null;
        int avid = 0;

        try
        {
            con = sql.dbconn.getConnection();
            pstmt = con.prepareStatement("SELECT id FROM antiviruses WHERE LOWER(name)=?");
            pstmt.setString(1, name.toLowerCase());
            rs = pstmt.executeQuery();
            if(!rs.next()) throw new Exception();
            avid = rs.getInt("id");
        }
        finally
        {
            if(rs != null) rs.close();
            if(pstmt != null) pstmt.close();
            if(con != null) con.close();
        }

        return avid;
    }

    private HashMap updateScanResult(String result) throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;
        ResultSet rs = null;
        HashMap arr = new HashMap();

        try
        {
            Document xmlResp = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(result.getBytes()));
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList)xpath.compile("//avresults/*").evaluate(xmlResp.getFirstChild(), XPathConstants.NODESET);

            int nodesCount = nodes.getLength();
            con = sql.dbconn.getConnection();
            if(this.check_group_id == 0)
            {
                pstmt = con.prepareStatement("UPDATE check_details SET result=? WHERE check_id=? AND av_id=?");

            }
            else
            {
                pstmt = con.prepareStatement("INSERT INTO check_details SET result=?,check_id=?,av_id=?,group_id=?");
            }
            
            for(int i=0; i < nodesCount; i++)
            {
                Node node = nodes.item(i);
                String av_name = node.getNodeName();
                int av_id = getAvIdByName(av_name);
		String checkresult = node.getFirstChild().getTextContent();
		checkresult = (checkresult.equalsIgnoreCase("not found") == true)? "NOT FOUND" : checkresult;

                arr.put(av_name,checkresult);

		if(this.check_group_id == 0)
		{
                    pstmt.setString(1, checkresult);
                    pstmt.setInt(2, this.check_id);
                    pstmt.setInt(3, av_id);

		}
		else
		{
                    pstmt.setString(1, checkresult);
                    pstmt.setInt(2, this.check_id);
                    pstmt.setInt(3, av_id);
                    pstmt.setInt(4, this.check_group_id);

        	}

                pstmt.addBatch();
            }
            pstmt.executeBatch();
            pstmt.close();
            pstmt = null;

            int has_result_items_count = 0;
            if(this.check_group_id == 0)
            {
                pstmt = con.prepareStatement("SELECT COUNT(id) AS idcount FROM check_details WHERE check_id=? AND result IS NOT NULL");
                pstmt.setInt(1, this.check_id);
            }
            else
            {
                pstmt = con.prepareStatement("SELECT COUNT(id) AS idcount FROM check_details WHERE check_id=? AND group_id=? AND result IS NOT NULL");
                pstmt.setInt(1, this.check_id);
                pstmt.setInt(2, this.check_group_id);
            }

            rs = pstmt.executeQuery();
            if(rs.next())
            {
                has_result_items_count = rs.getInt("idcount");
                pstmt.close(); pstmt = null; rs.close();rs = null;
                if(this.getRelAvCount() == has_result_items_count)
                {
                    this.ScanStatus = SCAN_STATUS_FINISH;
                    pstmt = con.prepareStatement("UPDATE checks SET status=? WHERE id=?");
                    pstmt.setString(1, "finish");
                    pstmt.setInt(2, this.check_id);
                    pstmt.executeUpdate();
                }
            }
	}
        finally
        {
            if(pstmt != null) pstmt.close();
            if(rs != null) rs.close();
            if(con != null) con.close();
        }

        return arr;
    }

    private void updateChecksCount() throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;

        try
        {
            con = sql.dbconn.getConnection();
            pstmt = con.prepareStatement("UPDATE checks SET last_check=NOW(), made_checks=made_checks+1 WHERE id=?");
            pstmt.setInt(1, this.check_id);
            pstmt.executeUpdate();
        }
        finally
        {
            if(pstmt != null) pstmt.close();
            if(con != null) con.close();
        }
    }

    public HashMap sb_getScanResult() throws Exception
    {

        HashMap retResult = null;

            String url = "http://"+scan_balancer_url+"/ScanResult/"+ssjID;
            ContentExchange httpRequest = new ContentExchange();

            if(scan_cookie != null)
            {
                httpRequest.setRequestHeader("Set-Cookie", scan_cookie);
            }

            httpRequest.setURL(url);
            httpRequest.setMethod("GET");
            httpClient.send(httpRequest);
            int status = httpRequest.waitForDone();
            if(status == httpRequest.STATUS_COMPLETED && httpRequest.getResponseStatus() == 200)
            {
                String xml_result = httpRequest.getResponseContent();

                if(xml_result != null && !xml_result.isEmpty())
		{
                    HttpFields fields = httpRequest.getResponseFields();
                    if(fields != null)
                    {
                        String cookie = fields.getStringField("Set-Cookie");
                        if(!cookie.isEmpty())
                        {
                            scan_cookie = cookie;
                        }
                    }
                    
                    retResult = this.updateScanResult(xml_result);
                    this.scanResultsArr.add(retResult);
                }
            }

            if(ScanStatus == SCAN_STATUS_FINISH)
            {
                this.updateChecksCount();
            }


        return retResult;
    }

    public void assignGroupID() throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;

        try
        {
            con = sql.dbconn.getConnection();
            
            check_group_id = Math.abs(new Random().nextInt());
            pstmt = con.prepareStatement("INSERT INTO check_group_id SET id=?");
            pstmt.setInt(1, check_group_id);
            pstmt.execute();
            
            pstmt = con.prepareStatement("UPDATE checks SET group_id=? WHERE id=?");
            pstmt.setInt(1, check_group_id);
            pstmt.setInt(2, check_id);
            pstmt.executeUpdate();
        }
        catch(SQLException e)
        {
            if(pstmt != null) pstmt.close();
            if(con != null) con.close();
            if(e.getErrorCode() == com.mysql.jdbc.MysqlErrorNumbers.ER_DUP_ENTRY)
            {
                assignGroupID();
            }
            else
            {
                throw e;
            }
        }
    }
    
    public void freeGroupID(int group_id) throws Exception
    {
        PreparedStatement pstmt = null;
        Connection con = null;

        try
        {
            con = sql.dbconn.getConnection();
            pstmt = con.prepareStatement("DELETE FROM check_group_id WHERE id=?");
            pstmt.setInt(1, group_id);
            pstmt.execute();
        }
        catch(Exception e)
        {}

        if(pstmt != null) pstmt.close();
        if(con != null) con.close();
    }
    

}
