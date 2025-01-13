package org.rmj.approvalcode.sms;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.SQLUtil;
import org.rmj.replication.utility.LogWrapper;

public class SMSProcessor {
    public static void main (String [] args){
        LogWrapper logwrapr = new LogWrapper("SMS.Approval", "sms-token.log");       
        
        String lsProdctID = "gRider";
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
                        " WHERE LEFT(sMessagex, 8) = 'APP_RQST'" +
                            " AND (cTranStat IS NULL OR cTranStat = '0')";
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            iApproval instance;
            
            while (loRS.next()){
                logwrapr.info(loRS.getString("sTransNox") + "\t" + loRS.getString("sMobileNo") + "\t" + loRS.getString("sMessagex"));
                System.out.println(loRS.getString("sTransNox") + "\t" + loRS.getString("sMobileNo") + "\t" + loRS.getString("sMessagex"));
                
                poGRider.beginTrans();
                instance = SMSApprovalFactory.make(poGRider, loRS.getString("sMessagex").replace("", "_"), loRS.getString("sMobileNo"));
                
                if (instance.ProcessApproval()){
                    logwrapr.info(instance.getMessage());
                    
                    lsSQL = "UPDATE SMS_Incoming SET" +
                                "  cReadxxxx = '1'" +
                                ", dReadxxxx = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                                ", cTranStat = '1'" +
                            " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));
                } else {
                    logwrapr.warning(instance.getMessage());
                    
                    //if contains these error codes cancel the sms request
                    if ("1001»1002»1003»1004".contains(instance.getErrorCode())){
                        lsSQL = "UPDATE SMS_Incoming SET" +
                                "  cReadxxxx = '1'" +
                                ", dReadxxxx = " + SQLUtil.toSQL(poGRider.getServerDate()) +                                 
                                ", cTranStat = '3'" +
                            " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));
                    } else {
                        lsSQL = "UPDATE SMS_Incoming SET" +
                                "  cReadxxxx = '1'" +
                                ", dReadxxxx = " + SQLUtil.toSQL(poGRider.getServerDate()) +                                 
                                ", cTranStat = '0'" +
                            " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));
                    }
                }
                if (poGRider.executeQuery(lsSQL, "SMS_Incoming", poGRider.getBranchCode(), "") <= 0){
                    logwrapr.severe(poGRider.getErrMsg() + "; " + poGRider.getMessage());
                    poGRider.rollbackTrans();
                    System.exit(1);
                }
                
                poGRider.commitTrans();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            logwrapr.severe(ex.getMessage());
            System.exit(1);
        }
        
        System.exit(0);
    }
}
