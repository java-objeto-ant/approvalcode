package org.rmj.approvalcode.agent;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.WebClient;
import org.rmj.replication.utility.LogWrapper;

public class XMUpload {
    public XMUpload(GRider foGRider){
        poGRider = foGRider;
        
        logwrapr = new LogWrapper("XMCodeApproval", "XMCodeApproval.log");
    }
    
    public boolean UploadRequests(){
        String lsSQL = "SELECT" + 
                            "  sTransNox" +
                            ", dTransact" +
                            ", sSystemCD" +
                            ", sReqstdBy" +
                            ", dReqstdxx" +
                            ", cIssuedBy" +
                            ", sMiscInfo" +
                            ", sRemarks1" +
                            ", sRemarks2" +
                            ", sApprCode" +
                            ", sEntryByx" +
                            ", sApprvByx" +
                            ", sReasonxx" +
                            ", sReqstdTo" +
                            ", cSendxxxx" +
                            ", cTranStat" +
                            ", sModified" +
                            ", dModified" +
                        " FROM " + TABLE_NAME + 
                        " WHERE cSendxxxx = '0'" +
                            " AND sTransNox LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%") +
                            " AND sSystemCd IN ('CA', 'HM')" + 
                        " ORDER BY sTransNox";
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);       
        
        boolean bHasRec = false;
        
        try {
            setHeaders(); //set header values
            
            while (loRS.next()){
                bHasRec = true;
                headers.put("g-api-key", CommonUtils.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));        
                headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String)headers.get("g-api-imei") + (String)headers.get("g-api-key")));
                headers.put("g-api-user", loRS.getString("sEntryByx"));    
                
                param.clear();
                param.put("trandate", loRS.getString("dTransact"));
                param.put("systemcd", loRS.getString("sSystemCD"));
                param.put("reqstdby", loRS.getString("sReqstdBy"));
                param.put("reqstdxx", loRS.getString("dReqstdxx"));
                param.put("miscinfo", loRS.getString("sMiscInfo"));
                param.put("remarks1", loRS.getString("sRemarks1"));
                param.put("remarks2", loRS.getString("sRemarks2"));
                param.put("reqstdto", loRS.getString("sReqstdTo"));
                param.put("entrybyx", loRS.getString("sEntryByx"));
                
                response = WebClient.sendHTTP(CommonUtils.getConfiguration(poGRider, "WebSvr") + REQSTAPI, param.toJSONString(), (HashMap<String, String>) headers);
                
                if(response != null){
                    param.clear();
                    param = (JSONObject) parser.parse(response);
                    
                    if (param.get("result").toString().equals("success")){
                        response = param.get("transnox").toString();
                    
                        if (!response.isEmpty()){
                            lsSQL = "UPDATE " + TABLE_NAME + " SET" + 
                                        "  sTransNox = " + SQLUtil.toSQL(response) +
                                        ", cSendxxxx = '1'" + 
                                        ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                                    " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));

                            if (poGRider.executeQuery(lsSQL, TABLE_NAME, poGRider.getBranchCode(), poGRider.getBranchCode()) == 0)
                                logwrapr.warning("No record updated for ->>" + lsSQL);

                            
                            switch (loRS.getString("sSystemCD").toLowerCase()){
                                case "ca":
                                    lsSQL = "UPDATE MC_Credit_Application SET" +
                                            "  sCIAppReq = " + SQLUtil.toSQL(response) +
                                            ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) +
                                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                                        " WHERE sCIAppReq = " + SQLUtil.toSQL(loRS.getString("sTransNox")) + 
                                            " AND LEFT(sTransNox, 4) = " + SQLUtil.toSQL(poGRider.getBranchCode()) +
                                            " AND LEFT(sQMatchNo, 2) = 'CI'";

                                    if (poGRider.executeQuery(lsSQL, "MC_Credit_Application", poGRider.getBranchCode(), poGRider.getBranchCode()) == 0)
                                        logwrapr.warning("No record updated for ->>" + lsSQL);
                                    
                                    break;
                                case "hm":
                                    lsSQL = "UPDATE MC_Hot_Item_Application SET" +
                                                "  sApproved = " + SQLUtil.toSQL(response) +
                                                ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) +
                                                ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                                            " WHERE sApproved = " + SQLUtil.toSQL(loRS.getString("sTransNox")) + 
                                                " AND LEFT(sTransNox, 4) = " + SQLUtil.toSQL(poGRider.getBranchCode());
                                    
                                    if (poGRider.executeQuery(lsSQL, "MC_Hot_Item_Application", poGRider.getBranchCode(), poGRider.getBranchCode()) == 0)
                                        logwrapr.warning("No record updated for ->>" + lsSQL);
                            }
                        }
                    } else {
                        param = (JSONObject) parser.parse(param.get("error").toString());
                        logwrapr.severe((String) param.get("message"));
                        setMessage((String) param.get("message"));
                        return false;
                    }   
                } else {
                    logwrapr.severe("Server has no response...");
                    setMessage("Server has no response...");
                    return false;
                }
            }
        } catch (SQLException | IOException | ParseException ex) {
            logwrapr.severe(ex.getMessage());
            setMessage(ex.getMessage());
            return false;
        }       
        
        if (bHasRec)
            setMessage("Request uploaded successfully...");
        else
            setMessage("No request to upload...");
        
        return true;
    }
    
    private void setHeaders(){
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("g-api-id", poGRider.getProductID());
        headers.put("g-api-imei", MiscUtil.getPCName());
        headers.put("g-api-client", poGRider.getClientID());
        headers.put("g-api-log", "");
        headers.put("g-api-token", "");
    }
    
    public String getMessage(){return psMessage;}
    private void setMessage(String fsValue){psMessage = fsValue;}
    
    GRider poGRider;
    String psMessage;
    
    final String TABLE_NAME = "System_Code_Approval";
    final String MODULE_NAME = this.getClass().getSimpleName();
    LogWrapper logwrapr;    
    
    //json process variables
    Map<String, String> headers = new HashMap<>();
    Calendar calendar = Calendar.getInstance();
    JSONObject param = new JSONObject();
    JSONParser parser = new JSONParser();
    String response;
    
    final String REQSTAPI = "integsys/codeapproval/code_request.php";
}
