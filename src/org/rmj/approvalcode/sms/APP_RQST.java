package org.rmj.approvalcode.sms;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.engr.purchasing.pojo.UnitPOMaster;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.Tokenize;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.WebClient;
import org.rmj.appdriver.agentfx.service.PO_Master;
import org.rmj.engr.purchasing.base.PurchaseOrder;

/**
 *
 * @author Michael Cuison
 *      2020.12.08  Started creating this object.
 */
public class APP_RQST implements iApproval{
    private final String TOPMGMNT = "M00119002175"; //1 - bos jo; 2 - bos onel; 3 - bos guan; ;M00119002653;M00120001290
    
    GRider poGRider;
    String psSender;
    String psSMS;

    String psEmployID;
    String psMessage;
    String psErrCode;
    
    @Override
    public void setGRider(GRider foApp) {
        poGRider = foApp;
    }

    @Override
    public void setSender(String fsValue) {
        psSender = fsValue;
    }

    @Override
    public void setSMS(String fsValue) {
        psSMS = fsValue;
    }

    @Override
    public boolean ProcessApproval() {
        if (poGRider == null){
            psMessage = "Application driver is not set.";
            return false;
        }
        
        if (psSender.isEmpty()){
            psMessage = "Sender number must not be empty.";
            return false;
        }
        
        if (psSMS.isEmpty()){
            psMessage = "SMS request must not be empty.";
            return false;
        }
               
        psEmployID = "";
                
        /**
         * Sample:
         *      APP_RQST EP/M0W2/M0W212000001;2020-11-21;25000;For Jolo Residence Parking Area/By: Jun Calaustro        
         * 
         * 0 - APP_REQUEST <Request Code>
         * 1 - Branch code
         * 2 - Other info
         * 3 - Requesting associate
         */
        
        //dissect the SMS request
        String [] lasSMS = psSMS.split("/");
        
        //get the request code
        String [] lasRequest = lasSMS[0].split(" ");
        String lsRequest = lasRequest[1];
        if (!isValidRequest(lsRequest)) return false;
        
        //get and validate auth token vs the records passed
        String lsAuthTokn = getAuthToken(lasRequest[1]);
        if (lsAuthTokn.isEmpty()) return false;
        if (!isValidAuthToken(lsAuthTokn)) return false;
        
        //dissect the other info to the the transaction number
        String [] lasOthrInfo = lasSMS[2].split(";");        
        JSONObject loJSON = getTokenRequest(lasOthrInfo[0], lasRequest[1]);
        if (loJSON == null) return false;
        
        if (!"success".equals((String) loJSON.get("result"))){
            loJSON = (JSONObject) loJSON.get("error");
            psMessage = (String) loJSON.get("message");
            return false;
        }
        
        //generate approval code
        String lsAppvlCde = Tokenize.EncryptApprovalToken((String) loJSON.get("sTransNox"), "1", (String) loJSON.get("sRqstType"), (String) loJSON.get("sReqstdTo"));
        
        //all entries are valid, you can now approve the request.
        String lsSQL = "UPDATE Tokenized_Approval_Request SET" +
                            "  cApprType = '1'" + 
                            ", sAuthTokn = " + SQLUtil.toSQL(lsAuthTokn) + 
                            ", sApprCode = " + SQLUtil.toSQL(lsAppvlCde) + 
                            ", dApproved = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                            ", cTranStat = '1'" +
                        " WHERE sTransNox = " + SQLUtil.toSQL((String) loJSON.get("sTransNox"));
        
        if (poGRider.executeQuery(lsSQL, "Tokenized_Approval_Request", poGRider.getBranchCode(), "") <= 0){
            psMessage = poGRider.getErrMsg() + "; " + poGRider.getMessage();
            return false;
        }
        
        //update casys_dbf
        lsSQL = "UPDATE CASys_DBF.Tokenized_Approval_Request SET" +
                    "  cApprType = '1'" + 
                    ", sAuthTokn = " + SQLUtil.toSQL(lsAuthTokn) + 
                    ", sApprCode = " + SQLUtil.toSQL(lsAppvlCde) + 
                    ", dApproved = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                    ", cTranStat = '1'" +
                " WHERE sTransNox = " + SQLUtil.toSQL((String) loJSON.get("sTransNox"));
        if (poGRider.executeUpdate(lsSQL) <= 0){
            psMessage = poGRider.getErrMsg() + "; " + poGRider.getMessage();
            return false;
        }

        if (TOPMGMNT.contains((String) loJSON.get("sReqstdTo"))){
            String lsMessage = "Thank you for approving Engineering Purchase Order with transaction number " + (String) loJSON.get("sSourceNo") + ".";
            
            sendSMS((String) loJSON.get("sSourceNo"), lsMessage, (String) loJSON.get("sMobileNo"));
        }
        
        psMessage = "Token approval request was approved successfully.";
        
        if (lsRequest.equalsIgnoreCase("ep"))
            if (autoApproveEP((String) loJSON.get("sSourceNo"), lsAuthTokn, (String) loJSON.get("sReqstdTo"))) psMessage += " Transaction was totally approved.";
        
        return true;
    }
    
    @Override
    public String getErrorCode() {
        return psErrCode;
    }

    @Override
    public String getMessage() {
        return psMessage;
    }
        
    private boolean isValidRequest(String fsRqstCode){        
        String lsSQL = "SELECT * FROM xxxSCA_Request WHERE cRecdStat = '1' AND sSCACodex = " + SQLUtil.toSQL(fsRqstCode);
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            if (loRS.next()) return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            psMessage = ex.getMessage();
            return false;
        }
        
        psErrCode = ApprvlErrorCode.INVALID_REQUEST;
        psMessage = "Request type is not registered to the system.";
        return false;
    }
        
    private JSONObject getTokenRequest(String fsSourceNo, String fsRqstType){
        String lsSQL = "SELECT * FROM Tokenized_Approval_Request" +
                        " WHERE sSourceNo = " + SQLUtil.toSQL(fsSourceNo) + 
                            " AND sRqstType = " + SQLUtil.toSQL(fsRqstType) +
                            " AND sReqstdTo = " + SQLUtil.toSQL(psEmployID) +
                            " AND sMobileNo = " + SQLUtil.toSQL(psSender);
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
                
        try {
            if (loRS.next()){
                if (!loRS.getString("cTranStat").equals("0")){
                    JSONObject err_detl = new JSONObject();
                    psErrCode = ApprvlErrorCode.ALREADY_PROCESSED;
                    err_detl.put("message", "Token request is already APPROVED/POSTED/CANCELLED/VOID.");

                    JSONObject err_mstr = new JSONObject();
                    err_mstr.put("result", "error");
                    err_mstr.put("error", err_detl);
                    return err_mstr;
                }
                
                JSONObject loJSON = CommonUtils.loadJSON(loRS);
                loJSON.put("result", "success");
                
                return loJSON;
            } else {
                JSONObject err_detl = new JSONObject();
                psErrCode = ApprvlErrorCode.NO_RECORD_FOUND;
                err_detl.put("message", "No token request found on the given criteria.");

                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "error");
                err_mstr.put("error", err_detl);
                return err_mstr;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", ex.getMessage());

            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "error");
            err_mstr.put("error", err_detl);
            return err_mstr;
        }
    }
    
    private String getAuthToken(String fsRqstType){
        String lsSQL = "SELECT" +
                            "  sEmployID" +
                            ", sAuthTokn" +
                        " FROM System_Code_Mobile" +
                        " WHERE sMobileNo = " + SQLUtil.toSQL(psSender) + 
                            " AND sAuthCode LIKE " + SQLUtil.toSQL("%" + fsRqstType + "%");
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            if (loRS.next()) {
                psEmployID = loRS.getString("sEmployID");
                return loRS.getString("sAuthTokn");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            psMessage = ex.getMessage();
            return "";
        }
        
        psErrCode = ApprvlErrorCode.AUTH_ERROR;
        psMessage = "Mobile number is not authorized for approving requests.";
        return "";
    }
    
    private boolean isValidAuthToken(String fsAuthTokn){
        fsAuthTokn = Tokenize.DecryptToken(fsAuthTokn, psEmployID);
        String [] lasAuthTokn = fsAuthTokn.split(":");
        
        String lsEmployID = lasAuthTokn[0];
        String lsMobileNo = lasAuthTokn[1];
        
        //compare field value for employee id vs the employee id from auth token
        if (!lsEmployID.equals(psEmployID)){
            psErrCode = ApprvlErrorCode.AUTH_ERROR;
            psMessage = "Discrepancy on record's AUTHORIZED EMPLOYEE NO. and the SENDER EMPLOYEE NO. detected.";
            return false;
        }
        
        //compare if the sender number is same as the registered number on auth token
        if (!lsMobileNo.equals(psSender)){
            psErrCode = ApprvlErrorCode.AUTH_ERROR;
            psMessage = "Discrepancy on record'sAUTHORIZED NO. and the SENDER NO. detected.";
            return false;
        }
        
        return true;
    }

    private boolean autoApproveEP(String fsTransNox, String fsAuthTokn, String fsEmployID){
        String lsProdctID = "General";
        String lsUserIDxx = "M001111122";

        GRider loGRider = new GRider(lsProdctID);

        if (!loGRider.loadUser(lsProdctID, lsUserIDxx)) return false;
        
        String lsEngrNmbr = CommonUtils.getConfiguration(poGRider, "EngrNmbr");
        
        PO_Master instance = new PO_Master();
        instance.setGRider(loGRider);
        instance.setTransNmbr(fsTransNox);
        
        //get required approval weight to approve the transaction
        int lnReqWeight = instance.getWeight2Apprv();
        
        if (lnReqWeight < 0) return false;
        
        //get the approved requests
        JSONArray arr = instance.loadCodeRequest();
        
        JSONObject loJSON;
        
        int lnCount = 0;
        String lsAuthTokn;
        String lasAuthTokn [];
        
        //get the weight of the current approval
        lsAuthTokn = Tokenize.DecryptToken(fsAuthTokn, fsEmployID);
        lasAuthTokn = lsAuthTokn.split(":");
        lnCount += Integer.parseInt(lasAuthTokn[3]);
        
        for (Object obj : arr){
            loJSON = (JSONObject) obj;
            
            if ("1".equals((String) loJSON.get("cTranStat"))){
                lsAuthTokn = Tokenize.DecryptToken((String) loJSON.get("sAuthTokn"), (String) loJSON.get("sEmployID"));
                lasAuthTokn = lsAuthTokn.split(":");
                lnCount += Integer.parseInt(lasAuthTokn[3]);
            }
        }
        
        if (lnCount >= lnReqWeight){
            PurchaseOrder loPO = new PurchaseOrder();
            loPO.setGRider(loGRider);
            loPO.setWithParent(true);
            
            if (loPO.closeTransaction(fsTransNox, lsUserIDxx, "TOKENAPPROVL")) {
                UnitPOMaster loUnit = loPO.loadTransaction(fsTransNox);
                
                if (!loUnit.getReferNo().isEmpty())
                    psSMS = "Your PO with refer # " + loUnit.getReferNo() + " was successfully approved. You can now print the transaction.";
                else
                    psSMS = "Your PO with trans # " + fsTransNox + " was successfully approved. You can now print the transaction.";
                
                sendSMS(fsTransNox, lsEngrNmbr, psSMS);
            }
        }
        
        return false;
    }
    
    private boolean sendSMS(String fsTransNox, String fsMessage, String fsMobileNo){
        boolean sent = sendSMS(fsMobileNo, fsMessage);
        
        String lsSQL = MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, poGRider.getConnection(), "MX01");
                
        lsSQL = "INSERT INTO HotLine_Outgoing SET" +
                "  sTransNox = " + SQLUtil.toSQL(lsSQL) +
                ", dTransact = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                ", sDivision = 'MIS'" +
                ", sMobileNo = " + SQLUtil.toSQL(fsMobileNo) +
                ", sMessagex = " + SQLUtil.toSQL(psSMS) +
                ", cSubscrbr = " + SQLUtil.toSQL(CommonUtils.classifyNetwork(fsMobileNo)) +
                ", dDueUntil = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                ", cSendStat = '2'" +
                ", nNoRetryx = '1'" +
                ", sUDHeader = ''" +
                ", sReferNox = " + SQLUtil.toSQL(fsTransNox) +
                ", sSourceCd = " + SQLUtil.toSQL("APTK") +
                ", cTranStat = " + SQLUtil.toSQL(sent ? "1" : "0") +
                ", nPriority = 1" +
                ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) +
                ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());

        poGRider.executeUpdate(lsSQL);
        
        return true;
    }
    
    private boolean sendSMS(String fsMobileNo, String fsMessagex){
        System.out.println("Sending SMS to " + fsMobileNo);
        
        String fsURL = "https://restgk.guanzongroup.com.ph/system/masking/sendSMS.php";
        
        String clientid = poGRider.getClientID(); //this must be replaced based on the client id using it
        String productid = poGRider.getProductID(); //this must be replaced based on the product id using it
        String imei = "GMC_SEG09"; //this must be replaced based on the computer name using it
        String userid = poGRider.getUserID(); //this must be replaced based on the user id using it
        
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
        param.put("message", fsMessagex);
        param.put("mobileno", fsMobileNo);
        param.put("maskname", "GUANZON");
        
        String response;
        try {
            response = WebClient.sendHTTP(fsURL, param.toJSONString(), (HashMap<String, String>) headers);
            if(response == null){
                System.out.println("No Response");
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