package org.rmj.approvalcode.sms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;

public class HEALTHCK implements iApproval{
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
        
        psSMS = psSMS.substring(5);
        
        String [] lasMessage = psSMS.split(";");
        
        if (lasMessage.length == 13){
            try {
                String lsFullName = lasMessage[0].trim();
                String lsBranchCd = lasMessage[1].trim().toUpperCase();
                double lnTemprtre = Double.valueOf(lasMessage[2].trim());
                String lcWithHdch = lasMessage[3].trim().substring(0, 1).toUpperCase();
                String lcWithCghx = lasMessage[4].trim().substring(0, 1).toUpperCase();
                String lcWithCold = lasMessage[5].trim().substring(0, 1).toUpperCase();
                String lcWithSore = lasMessage[6].trim().substring(0, 1).toUpperCase();
                String lcWithPain = lasMessage[7].trim().substring(0, 1).toUpperCase();
                String lcDiarrhea = lasMessage[8].trim().substring(0, 1).toUpperCase();
                String lcStayedxx = lasMessage[9].trim().substring(0, 1).toUpperCase();
                String lcContactx = lasMessage[10].trim().substring(0, 1).toUpperCase();
                String lcTravelld = lasMessage[11].trim().substring(0, 1).toUpperCase();
                String lcTravlNCR = lasMessage[12].trim().substring(0, 1).toUpperCase();            

                String lsSQL = "SELECT" +
                                    "  b.sLastName" +
                                    ", b.sFrstName" +
                                    ", b.sMiddName" +
                                    ", IFNULL(b.sSuffixNm, '') sSuffixNm" +
                                    ", b.cGenderCd" +
                                    ", FLOOR(DATEDIFF(NOW(), b.dBirthDte) / 360) nCltAgexx" +
                                    ", b.sMobileNo" +
                                    ", TRIM(CONCAT(b.sHouseNox, ' ', b.sAddressx)) sAddressx" +
                                    ", b.sTownIDxx" +
                                " FROM Employee_Master001 a" +
                                    ", Client_Master b" +
                                " WHERE a.sEmployID = b.sClientID" +
                                    " AND a.cRecdStat = '1'" +
                                    " AND a.dFiredxxx IS NULL" +
                                    " AND b.sCompnyNm = " + SQLUtil.toSQL(lsFullName);

                ResultSet loRS = poGRider.executeQuery(lsSQL);

                if (loRS.next()){
                    String lsLastName = loRS.getString("sLastName");
                    String lsFrstName = loRS.getString("sFrstName");
                    String lsMiddName = loRS.getString("sMiddName");
                    String lsSuffixNm = loRS.getString("sSuffixNm");
                    String lcGenderCd = loRS.getString("cGenderCd");
                    int lnCltAgexx = loRS.getInt("nCltAgexx");
                    String lsMobileNo = loRS.getString("sMobileNo");
                    String lsAddressx = loRS.getString("sAddressx");
                    String lsTownIDxx = loRS.getString("sTownIDxx");

                    lsSQL = MiscUtil.getNextCode("Health_Checklist", "sTransNox", true, poGRider.getConnection(), poGRider.getBranchCode());

                    lsSQL = "INSERT INTO Health_Checklist SET" +
                            "  sTransNox = " + SQLUtil.toSQL(lsSQL) +
                            ", sBranchCd = " + SQLUtil.toSQL(lsBranchCd) +
                            ", sLastName = " + SQLUtil.toSQL(lsLastName) +        
                            ", sFrstName = " + SQLUtil.toSQL(lsFrstName) +
                            ", sMiddName = " + SQLUtil.toSQL(lsMiddName) +
                            ", sSuffixNm = " + SQLUtil.toSQL(lsSuffixNm) +
                            ", cGenderxx = " + SQLUtil.toSQL(lcGenderCd) +
                            ", nCltAgexx = " + lnCltAgexx +
                            ", sMobileNo = " + SQLUtil.toSQL(lsMobileNo) +
                            ", sAddressx = " + SQLUtil.toSQL(lsAddressx) +
                            ", sTownIDxx = " + SQLUtil.toSQL(lsTownIDxx) +
                            ", nTemprtre = " + lnTemprtre +
                            ", cWithSore = " + SQLUtil.toSQL(lcWithSore.equals("N") ? "0" : "1") +
                            ", cWithPain = " + SQLUtil.toSQL(lcWithPain.equals("N") ? "0" : "1") +
                            ", cWithCghx = " + SQLUtil.toSQL(lcWithCghx.equals("N") ? "0" : "1") +
                            ", cWithCold = " + SQLUtil.toSQL(lcWithCold.equals("N") ? "0" : "1") +
                            ", cWithHdch = " + SQLUtil.toSQL(lcWithHdch.equals("N") ? "0" : "1") +
                            ", cDiarrhea = " + SQLUtil.toSQL(lcDiarrhea.equals("N") ? "0" : "1") +
                            ", cStayedxx = " + SQLUtil.toSQL(lcStayedxx.equals("N") ? "0" : "1") +
                            ", cContactx = " + SQLUtil.toSQL(lcContactx.equals("N") ? "0" : "1") +
                            ", cTravelld = " + SQLUtil.toSQL(lcTravelld.equals("N") ? "0" : "1") +
                            ", cTravlNCR = " + SQLUtil.toSQL(lcTravlNCR.equals("N") ? "0" : "1") +
                            ", dTransact = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                            ", dSubmittd = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                            ", cRecdStat = '1'";

                    if (poGRider.executeQuery(lsSQL, "Health_Checklist", poGRider.getBranchCode(), "") <= 0){
                        psErrCode = ApprvlErrorCode.INVALID_REQUEST;
                        psMessage = "ERROR - " + poGRider.getErrMsg() + ";" + poGRider.getMessage();
                        return false;
                    }

                    psMessage = "Health Declaration of " + lsFullName + " saved successfully.";
                    return true;
                }

                psErrCode = ApprvlErrorCode.NO_RECORD_FOUND;
                psMessage = "No record found. (" + lsFullName + ")";
                return false;
            } catch (SQLException | NumberFormatException ex) {
                ex.printStackTrace();
                psErrCode = "";
                psMessage = "Exception detected.";
                return false;
            }
        }
        
        psErrCode = ApprvlErrorCode.INVALID_REQUEST;
        psMessage = "Invalid message format detected.";
        
        return false;
    }

    @Override
    public String getErrorCode() {
        return psErrCode;
    }

    @Override
    public String getMessage() {
        return psMessage;
    }
}
