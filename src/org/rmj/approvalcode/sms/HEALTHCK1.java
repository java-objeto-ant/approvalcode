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

public class HEALTHCK1 implements iApproval{
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
        
        Properties loProp = new Properties();
        InputStream loStream = new ByteArrayInputStream(psMessage.getBytes());
        
        try {
            loProp.load((InputStream) loStream);
            
            String lsFullName = loProp.getProperty("FullName").trim().substring(0, 1).toUpperCase();
            double lnTemprtre = Double.valueOf(loProp.getProperty("Temprtre").trim());
            String lcWithHdch = loProp.getProperty("HeadAche").trim().substring(0, 1).toUpperCase();
            String lcWithCghx = loProp.getProperty("Coughxxx").trim().substring(0, 1).toUpperCase();
            String lcWithCold = loProp.getProperty("Coldsxxx").trim().substring(0, 1).toUpperCase();
            String lcWithSore = loProp.getProperty("SrThroat").trim().substring(0, 1).toUpperCase();
            String lcWithPain = loProp.getProperty("BodyAche").trim().substring(0, 1).toUpperCase();
            String lcDiarrhea = loProp.getProperty("Diarrhea").trim().substring(0, 1).toUpperCase();
            String lcStayedxx = loProp.getProperty("Stayedxx").trim().substring(0, 1).toUpperCase();
            String lcContactx = loProp.getProperty("Contactx").trim().substring(0, 1).toUpperCase();
            String lcTravelld = loProp.getProperty("Travelld").trim().substring(0, 1).toUpperCase();
            String lcTravlNCR = loProp.getProperty("TravlNCR").trim().substring(0, 1).toUpperCase();
            String lsBranchCd = loProp.getProperty("BranchCd").trim().substring(0, 1).toUpperCase();
            
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
                        ", cWithSore = " + SQLUtil.toSQL(lcWithSore) +
                        ", cWithPain = " + SQLUtil.toSQL(lcWithPain) +
                        ", cWithCghx = " + SQLUtil.toSQL(lcWithCghx) +
                        ", cWithCold = " + SQLUtil.toSQL(lcWithCold) +
                        ", cWithHdch = " + SQLUtil.toSQL(lcWithHdch) +
                        ", cDiarrhea = " + SQLUtil.toSQL(lcDiarrhea) +
                        ", cStayedxx = " + SQLUtil.toSQL(lcStayedxx) +
                        ", cContactx = " + SQLUtil.toSQL(lcContactx) +
                        ", cTravelld = " + SQLUtil.toSQL(lcTravelld) +
                        ", cTravlNCR = " + SQLUtil.toSQL(lcTravlNCR) +
                        ", dTransact = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                        ", dSubmittd = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                        ", cRecdStat = '1'";
                
                if (poGRider.executeQuery(lsSQL, "Health_Checklist", poGRider.getBranchCode(), "") <= 0){
                    psErrCode = ApprvlErrorCode.INVALID_REQUEST;
                    psMessage = poGRider.getErrMsg() + ";" + poGRider.getMessage();
                    return false;
                }
                
                return true;
            }
            
            psErrCode = ApprvlErrorCode.NO_RECORD_FOUND;
            psMessage = "No record found. (" + lsFullName + ")";
        } catch (SQLException | IOException | NumberFormatException ex) {
            Logger.getLogger(HEALTHCK1.class.getName()).log(Level.SEVERE, null, ex);
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
