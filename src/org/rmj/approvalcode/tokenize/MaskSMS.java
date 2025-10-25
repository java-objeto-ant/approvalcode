package org.rmj.approvalcode.tokenize;

import java.io.IOException;
import java.util.ArrayList;
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
import org.rmj.lib.net.LogWrapper;

public class MaskSMS {
    GRider _instance;
    String _message;
    boolean _save = true;
    LogWrapper _logwrapper;
    
    String maskname = null;
    String message = null;
    String sourceno = null;
    ArrayList<String> recipient = null;
    
    public MaskSMS(GRider foGRider, LogWrapper foLogger){
        _instance = foGRider;
        _logwrapper = foLogger;
    }
    
    public String getMessage(){
        return _message;
    }
    
    public void setMaskName(String fsMaskName){
        maskname = fsMaskName;
    }
    
    public void setRecipient(ArrayList fsMobileNo){
        recipient = fsMobileNo;
    }
    
    public void setSMS(String fsMessage){
        message = fsMessage;
    }
    
    public void setSourceNo(String fsSourceNo){
        sourceno = fsSourceNo;
    }
    
    public void saveMessage(boolean fbSave){
        _save = fbSave;
    }
    
    public boolean sendMessage(){
        if (_instance == null){
            _message = "Application driver is not set.";
            return false;
        }
        
//        if (_logwrapper == null){
//            _message = "System logger is not set.";
//            return false;
//        }
        
        if (maskname == null){
            _message = "Mask name is not set.";
            return false;
        }
        
        if (!"GUANZON|LOSPEDRITOS|CARTRADE|THE MONARCH".contains(maskname)){
            _message = "Mask name is not registered.";
            return false;
        }
        
        if (recipient == null){
            _message = "Recipients are not set.";
            return false;
        }
        
        if (message == null){
            _message = "Message to send is not set.";
            return false;
        }
        
        String lsSQL;
        boolean sent;
        int lnRow = 0;
        
        for (int lnCtr = 0; lnCtr <= recipient.size()-1; lnCtr++){
            sent = sendMessage(recipient.get(lnCtr));
            
            if (sent) lnRow += 1;
            
            if (_save){
                lsSQL = MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, _instance.getConnection(), _instance.getBranchCode());
                
                lsSQL = "INSERT INTO HotLine_Outgoing SET" +
                        "  sTransNox = " + SQLUtil.toSQL(lsSQL) +
                        ", dTransact = " + SQLUtil.toSQL(_instance.getServerDate()) +
                        ", sDivision = 'MIS'" +
                        ", sMobileNo = " + SQLUtil.toSQL(recipient.get(lnCtr)) +
                        ", sMessagex = " + SQLUtil.toSQL(message) +
                        ", cSubscrbr = " + SQLUtil.toSQL(CommonUtils.classifyNetwork(recipient.get(lnCtr))) +
                        ", dDueUntil = " + SQLUtil.toSQL(_instance.getServerDate()) +
                        ", cSendStat = '2'" +
                        ", nNoRetryx = '1'" +
                        ", sUDHeader = ''" +
                        ", sReferNox = " + SQLUtil.toSQL(sourceno.isEmpty() ? maskname : sourceno) +
                        ", sSourceCd = " + SQLUtil.toSQL("MASK") +
                        ", cTranStat = " + SQLUtil.toSQL(sent ? "1" : "0") +
                        ", nPriority = 1" +
                        ", sModified = " + SQLUtil.toSQL(_instance.getUserID()) +
                        ", dModified = " + SQLUtil.toSQL(_instance.getServerDate());
                
                if (_instance.executeUpdate(lsSQL) <= 0){
                    System.err.println(recipient.get(lnCtr) + "\t" + sent + "\t" + _instance.getErrMsg() + "; " + _instance.getMessage());
                    return false;
                }
            }
        }
        
        System.out.println(lnRow + "sent out of " + recipient.size());
        
        return true;
    }
    
    private boolean sendMessage(String fsMobileNo){        
        String fsURL = "https://restgk.guanzongroup.com.ph/system/masking/sendSMS.php";
        
        String clientid = _instance.getClientID(); //this must be replaced based on the client id using it
        String productid = _instance.getProductID(); //this must be replaced based on the product id using it
        String imei = MiscUtil.getPCName(); //this must be replaced based on the computer name using it
        String userid = _instance.getUserID(); //this must be replaced based on the user id using it
        
        Calendar calendar = Calendar.getInstance();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("g-api-id", productid);
        headers.put("g-api-imei", imei);
        
        headers.put("g-api-key", SQLUtil.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));        
        headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String)headers.get("g-api-imei") + (String)headers.get("g-api-key")));
        headers.put("g-api-client", clientid);    
        headers.put("g-api-user", userid);    
        headers.put("g-api-log", "");    
        headers.put("g-api-token", "");    
        headers.put("g-api-mobile", "");    
        
        JSONObject param = new JSONObject();
        param.put("message", message);
        param.put("mobileno", fsMobileNo);
        param.put("maskname", maskname);
        
        String response;
        try {
            response = WebClient.sendHTTP(fsURL, param.toJSONString(), (HashMap<String, String>) headers);
            if(response == null){
                System.err.println("No Response");
                return false;
            } 

            JSONParser loParser = new JSONParser();
            JSONObject loJSON = (JSONObject) loParser.parse(response);
            
            if (loJSON.get("result").equals("success")){
                System.out.println((String) loJSON.get("message") + "(" + (String) loJSON.get("maskname") + " - " + (String) loJSON.get("id") + ")");
                return true;
            } else {
                loJSON = (JSONObject) loJSON.get("error");
                System.err.println(String.valueOf(loJSON.get("code")) + " - " + (String) loJSON.get("message"));
                return false;
            }
            
        } catch (IOException | ParseException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
