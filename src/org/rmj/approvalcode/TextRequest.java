package org.rmj.approvalcode;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.approvalcode.agent.XMCodeApproval;
import org.rmj.replication.utility.LogWrapper;

public class TextRequest {
    public static void main(String [] args){           
        LogWrapper logwrapr = new LogWrapper("SMS.Approval", "sms.log");       
        
        String lsProdctID = "IntegSys";
        String lsUserIDxx = "M001111122";

        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/mac/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.config", path);
        
        GRider poGRider = new GRider(lsProdctID);

        if (!poGRider.loadUser(lsProdctID, lsUserIDxx)){
            System.out.println(poGRider.getMessage() + poGRider.getErrMsg());
            logwrapr.severe(poGRider.getMessage() + poGRider.getErrMsg());
            System.exit(1);
        }
        
        String lsSQL = "SELECT  " +
                            "  sTransNox" +
                            ", sMessagex" +
                            ", sMobileNo" +
                        " FROM SMS_Incoming" +
                        " WHERE UCASE(sMessagex) LIKE 'CODEAPPR CA%'" +
                            " AND (cTranStat IS NULL OR cTranStat = '0')";
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        JSONObject loJSON1 = new JSONObject();
        loJSON1.put("prodctid", poGRider.getProductID());
        loJSON1.put("pcnamexx", MiscUtil.getPCName());
        loJSON1.put("clientid", poGRider.getClientID());
        loJSON1.put("useridxx", poGRider.getUserID());
        
        JSONObject loJSON2;
        String [] lasMessage;
        String lsValue;
        
        XMCodeApproval instance = new XMCodeApproval(poGRider);
        
        try {
            while (loRS.next()){
                lasMessage = loRS.getString("sMessagex").split("/");
                
                loJSON2 = new JSONObject();
                
                //check format if valid
                if (lasMessage.length != 6){
                    createSMS(poGRider, 2, loRS.getString("sMobileNo"));
                    
                    lsSQL = "UPDATE SMS_Incoming SET cTranStat = '3'" + 
                            " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));
                    
                    poGRider.executeUpdate(lsSQL);
                }               
                
                for (int lnCtr = 0; lnCtr <= lasMessage.length-1; lnCtr++){
                    switch(lnCtr){
                        case 0:
                            lsValue = lasMessage[lnCtr].trim();
                            
                            if (lsValue.equalsIgnoreCase("CODEAPPR CA"))
                                lsValue = "CA";
                            else
                                lsValue = "";
                            
                            loJSON2.put("systemcd", lsValue);
                            break;
                        case 1:
                            lsValue = lasMessage[lnCtr].trim();
                            loJSON2.put("reqstdby", lsValue);
                            break;
                        case 2:
                            lsValue = lasMessage[lnCtr].trim().replace("Ã±", "ñ").replace("Ã‘", "Ñ");
                            lsValue = CommonUtils.UTF2Hex(lsValue);
                            loJSON2.put("miscinfo", lsValue.replace("c3b1", "f1").replace("c391", "f1"));
                            break;
                        case 3:
                            lsValue = lasMessage[lnCtr].trim();
                            loJSON2.put("reqstdxx", lsValue);
                            break;
                        case 4:
                            lsValue = lasMessage[lnCtr].trim();
                            loJSON2.put("approved", lsValue);
                            break;
                        case 5:
                            lsValue = lasMessage[lnCtr].trim();
                            loJSON2.put("reasonxx", lsValue);
                    }
                }

                loJSON2.put("trandate", SQLUtil.dateFormat(poGRider.getServerDate(), SQLUtil.FORMAT_SHORT_DATE));
                loJSON2.put("reqstdto", loRS.getString("sMobileNo"));
                loJSON2.put("entrybyx", poGRider.getUserID());
                loJSON2.put("remarks2", "SMS approval request");
                       
                instance.NewTransaction();
                instance.setMaster("dTransact", SQLUtil.toDate((String)loJSON2.get("trandate"), SQLUtil.FORMAT_SHORT_DATE));
                instance.setMaster("sSystemCD", (String) loJSON2.get("systemcd"));
                
                //validate date
                if (!CommonUtils.isDate((String)loJSON2.get("reqstdxx"), SQLUtil.FORMAT_SHORT_DATE)){
                    createSMS(poGRider, 3, loRS.getString("sMobileNo"));
                    
                    lsSQL = "UPDATE SMS_Incoming SET cTranStat = '3'" + 
                            " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));
                    
                    poGRider.executeUpdate(lsSQL);
                }
                
                instance.setMaster("dReqstdxx", SQLUtil.toDate((String)loJSON2.get("reqstdxx"), SQLUtil.FORMAT_SHORT_DATE));
                instance.setMaster("sReqstdBy", (String) loJSON2.get("reqstdby"));
                instance.setMaster("sMiscInfo", (String) loJSON2.get("miscinfo"));
                instance.setMaster("sReqstdTo", (String) loJSON2.get("reqstdto"));
                instance.setMaster("sRemarks2", (String) loJSON2.get("remarks2"));
                instance.setMaster("sEntryByx", (String) loJSON2.get("entrybyx"));
                instance.setMaster("sReasonxx", (String) loJSON2.get("reasonxx"));
        
                loJSON2 = instance.SaveTransaction();
                
                if ("success".equals((String) loJSON2.get("result"))){
                    lsSQL = "UPDATE SMS_Incoming SET cTranStat = '1'" + 
                            " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));
                    
                    poGRider.executeUpdate(lsSQL);
                } else {
                    lsSQL = "UPDATE SMS_Incoming SET cTranStat = '3'" + 
                            " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));
                    
                    poGRider.executeUpdate(lsSQL);
                }
                    
            }
        } catch (SQLException ex) {
            logwrapr.severe(ex.getMessage());
            System.exit(1);
        }
        
        System.exit(0);
    }
    
    private static void createSMS(GRider foGRider, int fnValue, String fsMobileNo){
        String lsMessage;
        
        switch(fnValue){
            case 2:
                lsMessage = "GUANZON GROUP: Please make sure to use the preferred format for issuing approval. Thank you.";
                break;
            case 3:
                lsMessage = "GUANZON GROUP: Please make sure that requesting date value on your request is correct. Thank you.";
                break;
            default:
                lsMessage = "";
        }
        
        if (!lsMessage.equals("")){
                lsMessage = "INSERT INTO HotLine_Outgoing SET" +
                        "  sTransNox = " + SQLUtil.toSQL(MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, foGRider.getConnection(), foGRider.getBranchCode())) +
                        ", dTransact = " + SQLUtil.toSQL(foGRider.getServerDate()) +
                        ", sDivision = " + SQLUtil.toSQL("CSS") +
                        ", sMobileNo = " + SQLUtil.toSQL(fsMobileNo) +
                        ", sMessagex = " + SQLUtil.toSQL(lsMessage) +
                        ", cSubscrbr = " + SQLUtil.toSQL("0") +
                        ", dDueUntil = " + SQLUtil.toSQL(CommonUtils.dateAdd(foGRider.getServerDate(), 5)) +
                        ", cSendStat = '0'" +
                        ", nNoRetryx = 0" +
                        ", sUDHeader = ''" +
                        ", sReferNox = ''" + 
                        ", sSourceCd = " + SQLUtil.toSQL("CODE") +
                        ", cTranStat = '0'" +
                        ", nPriority = 0" +
                        ", sModified = " + SQLUtil.toSQL(foGRider.getUserID()) +
                        ", dModified = " + SQLUtil.toSQL(foGRider.getServerDate());

                foGRider.executeUpdate(lsMessage);
            }
    }
}
