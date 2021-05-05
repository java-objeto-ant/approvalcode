package org.rmj.approvalcode.sms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.Tokenize;
import org.rmj.appdriver.agentfx.CommonUtils;

/**
 *
 * @author Michael Cuison
 *      2020.12.08  Started creating this object.
 */
public class APP_RQST implements iApproval{
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
        if (!isValidRequest(lasRequest[1])) return false;
        
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
        
        psMessage = "Token approval request was approved successfully.";
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
    
    //added methods
    private boolean createSMS(){
        return true;
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
    
    /*
    private boolean isValidApprovee(String fsEmployID, String fsMobileNo){
        //compare field value for employee id vs the employee id from auth token
        if (!fsEmployID.equals(psEmployID)){
            psErrCode = ApprvlErrorCode.AUTH_ERROR;
            psMessage = "Discrepancy on request's AUTHORIZED EMPLOYEE NO. and the SENDER EMPLOYEE NO. detected.";
            return false;
        }
        
        //compare if the sender number is same as the registered number on auth token
        if (!fsMobileNo.equals(psSender)){
            psErrCode = ApprvlErrorCode.AUTH_ERROR;
            psMessage = "Discrepancy on request's AUTHORIZED NO. and the SENDER NO. detected.";
            return false;
        }
        
        return true;
    }
    */
}