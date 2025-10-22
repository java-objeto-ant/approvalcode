/**
 * Code Approval Controller Class
 * 
 * @author Michael Cuison
 * @since 2019.07.02
 */

package org.rmj.approvalcode.agent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.DecoderException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.WebClient;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.approvalcode.base.UnitSystemCodeApproval;
import org.rmj.g3lr.android.core.CodeApproval;
import org.rmj.replication.utility.LogWrapper;

public class XMCodeApproval {
    public XMCodeApproval(Object foGRider){
        poGRider = (GRider) foGRider;
        psBranchCd = poGRider.getBranchCode();
        
        logwrapr = new LogWrapper("XMCodeApproval", "XMCodeApproval.log");
        
        pnEditMode = EditMode.UNKNOWN;
    }
    
    public XMCodeApproval(Object foGRider, String fsBranchCd){
        poGRider = (GRider) foGRider;
        psBranchCd = fsBranchCd;
        
        logwrapr = new LogWrapper("XMCodeApproval", "D:/GGC_Java_Systems/temp/XMCodeApproval.log");
        
        pnEditMode = EditMode.UNKNOWN;
    }
    
    public boolean NewTransaction(){
        poData = new UnitSystemCodeApproval();
        
        pnEditMode = EditMode.ADDNEW;
        
        return true;
    }
    
    public JSONObject SaveTransaction(){
        JSONObject loJSON = new JSONObject();
        
        if (poGRider == null){
            loJSON.put("result", "error");
            loJSON.put("message", "Application driver is not set...");
            return loJSON;
        }
        
        if (poData.getSystemCode().isEmpty()){
            loJSON.put("result", "error");
            loJSON.put("message", "System code is not set...");
            return loJSON;
        }
        
        if (poData.getRequestedBy().isEmpty()){
            loJSON.put("result", "error");
            loJSON.put("message", "Requesting branch is not set...");
            return loJSON;
        }
        
        if (poData.getDateRequested() == null){
            loJSON.put("result", "error");
            loJSON.put("message", "Requested date is not set...");
            return loJSON;
        }
        
        if (poData.getMiscInfo().isEmpty()){
            loJSON.put("result", "error");
            loJSON.put("message", "Miscellaneous info is not set...");
            return loJSON;
        }
        
        try {
            //convert from windows hex to utf8
            String lsUTF = CommonUtils.Win2UTF(poData.getMiscInfo());
            if (lsUTF.contains("ń")) lsUTF = lsUTF.replace("ń", "ñ");
            if (lsUTF.contains("Ń")) lsUTF = lsUTF.replace("Ń", "Ñ");
            //convert from utf8 to hex
            String lsHex = CommonUtils.UTF2Hex(lsUTF);
            
            poData.setRemarks1(lsHex);
            poData.setMiscInfo(lsUTF);
            
            //poData.setRemarks1(poData.getMiscInfo());
            //poData.setMiscInfo(CommonUtils.Win2UTF(poData.getMiscInfo()));
        } catch (DecoderException | UnsupportedEncodingException ex) {
            logwrapr.severe(ex.getMessage());
            loJSON.put("result", "error");
            loJSON.put("message", ex.getMessage());
            return loJSON;
        }
                
        poData.setEntryBy(poData.getEntryBy().isEmpty() ? 
                            poGRider.Decrypt(poGRider.getEmployeeNo()) : poData.getEntryBy());
        
        //save online only if it is main office.
        if ("M001»M0W1»GCO1»GCC1»GAP0»GK01»VTR1".contains(psBranchCd)){
            reqstdto = poData.getRequestedTo();
            reqstdby = poData.getRequestedBy();
            
            //is branch valid
            if (!isValidBranch(reqstdby)){
                createSMS(2);
                loJSON.put("result", "error");
                loJSON.put("message", "Invalid branch...");
                return loJSON;
            }
            
            if (reqstdto.substring(0, 2).equals("09")){
                if (isAuthorized()){                    
                    poData.setRequestedTo(reqstdto);
                    poData.setApprovedBy(reqstdto);
                    //poData.setReason("SMS approval request");
                    branchno = getBranchNo();
                    
                    //check first if request is existing.
                    loJSON = updateExisting();
                    if ("success".equals((String) loJSON.get("result"))) return loJSON;
                    
                    loJSON = saveOffline();
                    if ("success".equals((String) loJSON.get("result")) && !apprvlcd.equals("")){
                        String lsSQL = "UPDATE System_Code_Approval SET" + 
                                            "  cIssuedBy = '8'" + 
                                            ", sApprCode = " + SQLUtil.toSQL(apprvlcd) + 
                                            ", cTranStat = '1'" + 
                                        " WHERE sTransNox = " + SQLUtil.toSQL((String) loJSON.get("transno"));
                        
                        poGRider.executeUpdate(lsSQL);
                        createSMS(1);
                    }
                } else {
                    createSMS(0);
                    loJSON.put("result", "error");
                    loJSON.put("message", "Employee not registered.");
                    return loJSON;
                }
                return loJSON;
            }
            return saveOnline();
        }
        
        loJSON = saveOffline();
        
        String lsTransNox = "";
        if ("success".equals((String) loJSON.get("result")))
            lsTransNox = (String) loJSON.get("transno");
        
        loJSON = saveOnline();
        if ("success".equals((String) loJSON.get("result")) && !lsTransNox.equals("")){
            if (!((String) loJSON.get("transno")).substring(0, 4).equals("VTR1")){
                lsTransNox = "UPDATE System_Code_Approval SET" +
                                "  sTransNox = " + SQLUtil.toSQL((String) loJSON.get("transno")) +
                            " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
            
                if (poGRider.executeUpdate(lsTransNox) <= 0)
                    System.err.println(poGRider.getErrMsg() + "; " + poGRider.getMessage());
            }
        }
        
        return loJSON;        
    }
    
    public boolean SearchTransaction(String fsSystemCD, String fsFieldNme, String fsValue){
        if (fsSystemCD.isEmpty()){
            setMessage("UNSET System Code...");
            return false;
        }
        
        if (fsFieldNme.isEmpty()){
            setMessage("UNSET Field to search...");
            return false;
        }
        
        if (fsValue.isEmpty()){
            setMessage("No value to search...");
            return false;
        }
        
        String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sSystemCD = " + SQLUtil.toSQL(fsSystemCD));
        
        if (fsFieldNme.equals("sTransNox"))
            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));
        else if (fsFieldNme.equals("sMiscInfo"))
            lsSQL = MiscUtil.addCondition(lsSQL, "sMiscInfo LIKE " + SQLUtil.toSQL(fsValue + "%"));
        else {
            setMessage("Search field is not allowed to use for searching...");
            return false;
        }
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            if (MiscUtil.RecordCount(loRS) > 1){
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Trans No»Reference»Date»", 
                                                                "sTransNox»sMiscInfo»dTransact");
              
                if (loValue != null){
                    poData = (UnitSystemCodeApproval) LoadTransaction((String) loValue.get("sTransNox"));
                } else {
                    setMessage("No transaction to load...");
                    return false;
                }
            } else
                poData = (UnitSystemCodeApproval) LoadTransaction(loRS.getString("sTransNox"));
        } catch (SQLException e) {
            setMessage(e.getMessage());
            return false;
        }
        
        MiscUtil.close(loRS);
        
        return true;
    }
    
    public Object LoadTransaction(String fsTransNox){
        UnitSystemCodeApproval loOcc = new UnitSystemCodeApproval();
        Connection loCon = poGRider.getConnection();
      
        String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        Statement loStmt = null;
        ResultSet loRS = null;
        
        try {
            loStmt = loCon.createStatement();
            loRS = loStmt.executeQuery(lsSQL);
         
            if(!loRS.next())
                System.out.println("loadTransaction: No Record Found!" + lsSQL);            
            else{
                //load each column to the entity
                for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                    loOcc.setValue(lnCol, loRS.getObject(lnCol));
                }
            }
        } catch (SQLException ex) {
            logwrapr.severe(ex.getMessage());
            System.err.println(ex.getMessage());
        } finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }
        return loOcc;
    }
    
    private JSONObject saveOnline(){
        JSONObject loJSON = new JSONObject();
        
        setHeaders();
        
        headers.put("g-api-key", CommonUtils.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));        
        headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String)headers.get("g-api-imei") + (String)headers.get("g-api-key")));
        headers.put("g-api-user", poData.getEntryBy());    

        param.clear();
        param.put("trandate", SQLUtil.dateFormat(poGRider.getServerDate(),SQLUtil.FORMAT_SHORT_DATE));
        param.put("systemcd", poData.getSystemCode());
        param.put("reqstdby", poData.getRequestedBy());
        param.put("reqstdxx", SQLUtil.dateFormat(poData.getDateRequested(), SQLUtil.FORMAT_SHORT_DATE));
        param.put("miscinfo", poData.getMiscInfo());
        param.put("remarks1", poData.getRemarks1());
        param.put("remarks2", poData.getRemarks2());
        param.put("reqstdto", poData.getRequestedTo());
        param.put("entrybyx", poData.getEntryBy());

        try {
            response = WebClient.sendHTTP(CommonUtils.getConfiguration(poGRider, "WebSvr") +  REQSTAPI, param.toJSONString(), (HashMap<String, String>) headers);
            
            if(response != null){
                param.clear();
                param = (JSONObject) parser.parse(response);

                loJSON.put("result", "success");
                loJSON.put("transno", param.get("transnox").toString());
                loJSON.put("branchcd", param.get("branchcd").toString());
                loJSON.put("status", "online");
                loJSON.put("message", "Transaction saved successfully...");
                apprvlcd = getApproval().toUpperCase();
                
                return loJSON;
            } else apprvlcd = "";
            
            loJSON.put("result", "error");
            loJSON.put("message", "No response from the server...");
            return loJSON;
        } catch (IOException | ParseException ex) {
            loJSON.put("result", "error");
            loJSON.put("message", ex.getMessage());
            return loJSON;
        }
    }
    
    private JSONObject updateExisting(){
        JSONObject loJSON = new JSONObject();
        
        Connection loConn = poGRider.getConnection();
        if (loConn == null){
            loJSON.put("result", "error");
            loJSON.put("message", "Connection is not set...");
            return loJSON;
        }
        
        String lsValue = poData.getMiscInfo().replace("Ã±", "ñ").replace("Ã‘", "Ñ");
        lsValue = poData.getMiscInfo().replace("ñ", "_").replace("Ñ", "_");
        
        String lsSQL = " SELECT *" +
                        " FROM System_Code_Approval" + 
                        " WHERE sMiscInfo LIKE " + SQLUtil.toSQL(lsValue) +
                            " AND sSystemCD = " + SQLUtil.toSQL(poData.getSystemCode()) + 
                            " AND sReqstdBy = " + SQLUtil.toSQL(poData.getRequestedBy()) + 
                            " AND dReqstdxx = " + SQLUtil.toSQL(poData.getDateRequested()) + 
                            " AND cTranStat <> '3'" + 
                        " ORDER BY sTransNox" +
                        " LIMIT 1";
        
        ResultSet loRS = poGRider.executeQuery(MiscUtil.addCondition(lsSQL, "sRemarks2 LIKE " + SQLUtil.toSQL("System%")));
        
        try {
            if (!loRS.next()){
                loRS = poGRider.executeQuery(MiscUtil.addCondition(lsSQL, "sRemarks2 LIKE " + SQLUtil.toSQL("SMS%")));
                
                if (!loRS.next()){
                    loJSON.put("result", "error");
                    loJSON.put("message", "No existing request.");
                    return loJSON;
                }
            }           
            poData.setRemarks1(loRS.getString("sRemarks1"));
            
            //approved the existing request.
            apprvlcd = getApproval().toUpperCase();
            

            if (!loRS.getString("sApprCode").equalsIgnoreCase(apprvlcd)){
                lsSQL = "UPDATE System_Code_Approval SET" + 
                        "  cIssuedBy = '8'" + 
                        ", sApprCode = " + SQLUtil.toSQL(apprvlcd) + 
                        ", sApprvByx = " + SQLUtil.toSQL(poData.getApprovedBy()) + 
                        ", sReasonxx = " + SQLUtil.toSQL(poData.getReason()) + 
                        ", cTranStat = '1'" +
                        ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) + 
                        ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                    " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));
                        
                poGRider.executeUpdate(lsSQL);
            }
            createSMS(1);
            
            loJSON.put("result", "success");
            loJSON.put("transno", loRS.getString("sTransNox"));
            loJSON.put("branchcd", loRS.getString("sReqstdBy"));
            loJSON.put("status", "offline");
            loJSON.put("message", "Transaction saved successfully...");
            return loJSON;
        } catch (SQLException ex) {
            logwrapr.warning(ex.getMessage());
        }
        
        loJSON.put("result", "error");
        loJSON.put("message", "No existing request.");
        return loJSON;
    }
    
    private JSONObject saveOffline(){
        JSONObject loJSON = new JSONObject();
        
        Connection loConn = poGRider.getConnection();
        if (loConn == null){
            loJSON.put("result", "error");
            loJSON.put("message", "Connection is not set...");
            return loJSON;
        }
        
        String lsSQL = "";
        
        if (("M001»M0W1»GCO1»GCC1»GAP0»GK01»VTR1").contains(psBranchCd)){
            psBranchCd = "VTR1";
            poData.setSendStat("1");
        }
        
        poData.setModifiedBy(poGRider.getUserID());
        poData.setDateModified(poGRider.getServerDate());
        
        if (pnEditMode == EditMode.ADDNEW){
            poData.setTransactionNo(MiscUtil.getNextCode(poData.getTable(), "sTransNox", true, loConn, psBranchCd));
            poData.setDateTransact(poGRider.getServerDate());
            
            lsSQL = MiscUtil.makeSQL((GEntity) poData);
        } else {
            lsSQL = "";
        }
        
        if (!lsSQL.isEmpty()){  
            if (poGRider.executeUpdate(lsSQL) != 1){
                loJSON.put("result", "error");
                loJSON.put("message", poGRider.getMessage() + "\n" + poGRider.getErrMsg());
                return loJSON;
            }
            
            loJSON.put("result", "success");
            loJSON.put("transno", poData.getTransactionNo());
            loJSON.put("branchcd", poData.getRequestedBy());
            loJSON.put("status", "offline");
            loJSON.put("message", "Transaction saved successfully...");
            apprvlcd = getApproval().toUpperCase();
        } else {
            loJSON.put("result", "error");
            loJSON.put("message", "Record to update...");
            apprvlcd = "";
        }

        //poData = null;
        //pnEditMode = EditMode.UNKNOWN;
        
        return loJSON;
    }
    
    public boolean ValidateOnline(String fsTransNox, String fsApprvlCd, String fsUserID){
        JSONObject loJSON = new JSONObject();
        
        setHeaders();
        
        headers.put("g-api-key", CommonUtils.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));        
        headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String)headers.get("g-api-imei") + (String)headers.get("g-api-key")));
        headers.put("g-api-user", fsUserID);    

        param.clear();
        param.put("transnox", fsTransNox);
        param.put("apprvlcd", fsApprvlCd);

        try {
            response = WebClient.sendHTTP(CommonUtils.getConfiguration(poGRider, "WebSvr") + VLDTEAPI, param.toJSONString(), (HashMap<String, String>) headers);
            
            if(response != null){
                param.clear();
                param = (JSONObject) parser.parse(response);
                
                if (("success").equalsIgnoreCase((String) param.get("result"))){
                    setMessage("Validation successful.");
                    logwrapr.info("Validation successful.");
                    return true;
                } else {
                    response = param.get("error").toString();
                    param = (JSONObject) parser.parse(response);
                    setMessage((String) param.get("message"));
                    logwrapr.info((String) param.get("message"));
                    return false;
                }
            } else {
                setMessage("Server has no response.");
                logwrapr.warning("Server has no response.");            
            }
        } catch (IOException | ParseException ex) {
            setMessage(ex.getMessage());
            logwrapr.warning(ex.getMessage());
        }
        return false;
    }
    
    public String getApproval(){
        try {
            CodeApproval instance = new CodeApproval();
            
            String lsName = CommonUtils.Win2UTF((String) poData.getRemarks1());
            if (lsName.contains("ń")) lsName = lsName.replace("ń", "ñ");
            
            instance.Branch((String) poData.getRequestedBy());
            instance.DateRequested(SQLUtil.dateFormat(poData.getDateRequested(), SQLUtil.FORMAT_SHORT_DATE));
            instance.IssuedBy("8");
            instance.MiscInfo(lsName);
            instance.System((String) poData.getSystemCode());
            
            instance.Encode();
            return instance.Result();
        } catch (DecoderException | UnsupportedEncodingException ex) {
            logwrapr.warning(ex.getMessage());
        }
        return "";
    }
    
    public boolean ValidateJSON(String fsValue1, String fsValue2){
        if (fsValue1.isEmpty() || fsValue2.isEmpty()){            
            setMessage("Empty string cannot be converted to JSONObject.");
            return false;
        }        
        
        JSONParser loParser = new JSONParser();
        try {
            JSONObject loJSON1 =(JSONObject) loParser.parse(fsValue1);
            JSONObject loJSON2 =(JSONObject) loParser.parse(fsValue2);

            CodeApproval instance = new CodeApproval();
            
            String lsName = CommonUtils.Win2UTF((String) loJSON1.get("miscinfo"));
            if (lsName.contains("ń")) lsName = lsName.replace("ń", "ñ");
            if (lsName.contains("Ń")) lsName = lsName.replace("Ń", "Ñ");
            
            instance.Branch((String) loJSON1.get("reqstdby"));
            instance.DateRequested((String) loJSON1.get("reqstdxx"));
            instance.IssuedBy(String.valueOf(((String) loJSON1.get("apprvlcd")).charAt(3)));
            instance.MiscInfo(lsName);
            instance.System((String) loJSON1.get("systemcd"));
            
            instance.Encode();
            String lsResult = instance.Result();
                        
            System.out.println("Given code: " + (String) loJSON1.get("apprvlcd"));
            System.out.println("Result: " + lsResult);

            logwrapr.info("Given code: " + (String) loJSON1.get("apprvlcd"));
            logwrapr.info("Result: " + lsResult);
            
            int lnResponse = 0;
            if (("").equals((String) loJSON1.get("reqstdby")))
                lnResponse = instance.isEqualx(lsResult, (String) loJSON1.get("apprvlcd"));
            else
                lnResponse = instance.isEqual(lsResult, (String) loJSON1.get("apprvlcd"));

            if (lnResponse == 0){
                setMessage("Validation successful.");
                logwrapr.info("Validation successful.");
                return true;
            } else{
                setMessage("Invalid approval code detected.");
                logwrapr.warning("Invalid approval code detected.");
                return false;
            }
        } catch (ParseException | DecoderException | UnsupportedEncodingException ex) {
            logwrapr.severe(ex.getMessage());
            setMessage(ex.getMessage());
            return false;
        }
    }
    
    public boolean ValidateJSON_AllCaps(String fsValue1, String fsValue2){
        if (fsValue1.isEmpty() || fsValue2.isEmpty()){            
            setMessage("Empty string cannot be converted to JSONObject.");
            return false;
        }        
        
        JSONParser loParser = new JSONParser();
        try {
            JSONObject loJSON1 =(JSONObject) loParser.parse(fsValue1);
            JSONObject loJSON2 =(JSONObject) loParser.parse(fsValue2);

            CodeApproval instance = new CodeApproval();
            
            String lsName = CommonUtils.Win2UTF((String) loJSON1.get("miscinfo"));
            if (lsName.contains("ń")) lsName = lsName.replace("ń", "ñ");
            if (lsName.contains("Ń")) lsName = lsName.replace("Ń", "Ñ");
            
            //convert all characters to lower case
            lsName = lsName.toUpperCase();
            
            instance.Branch((String) loJSON1.get("reqstdby"));
            instance.DateRequested((String) loJSON1.get("reqstdxx"));
            instance.IssuedBy(String.valueOf(((String) loJSON1.get("apprvlcd")).charAt(3)));
            instance.MiscInfo(lsName);
            instance.System((String) loJSON1.get("systemcd"));
            
            instance.Encode();
            String lsResult = instance.Result();
                        
            System.out.println("Given code: " + (String) loJSON1.get("apprvlcd"));
            System.out.println("Result: " + lsResult);

            logwrapr.info("Given code: " + (String) loJSON1.get("apprvlcd"));
            logwrapr.info("Result: " + lsResult);
            
            int lnResponse = 0;
            if (("").equals((String) loJSON1.get("reqstdby")))
                lnResponse = instance.isEqualx(lsResult, (String) loJSON1.get("apprvlcd"));
            else
                lnResponse = instance.isEqual(lsResult, (String) loJSON1.get("apprvlcd"));

            if (lnResponse == 0){
                setMessage("Validation successful.");
                logwrapr.info("Validation successful.");
                return true;
            } else{
                setMessage("Invalid approval code detected.");
                logwrapr.warning("Invalid approval code detected.");
                return false;
            }
        } catch (ParseException | DecoderException | UnsupportedEncodingException ex) {
            logwrapr.severe(ex.getMessage());
            setMessage(ex.getMessage());
            return false;
        }
    }
    
    public boolean ValidateJSON_AllSmall(String fsValue1, String fsValue2){
        if (fsValue1.isEmpty() || fsValue2.isEmpty()){            
            setMessage("Empty string cannot be converted to JSONObject.");
            return false;
        }        
        
        JSONParser loParser = new JSONParser();
        try {
            JSONObject loJSON1 =(JSONObject) loParser.parse(fsValue1);
            JSONObject loJSON2 =(JSONObject) loParser.parse(fsValue2);

            CodeApproval instance = new CodeApproval();
            
            String lsName = CommonUtils.Win2UTF((String) loJSON1.get("miscinfo"));
            if (lsName.contains("ń")) lsName = lsName.replace("ń", "ñ");
            if (lsName.contains("Ń")) lsName = lsName.replace("Ń", "Ñ");
            
            //convert all characters to lower case
            lsName = lsName.toLowerCase();
            
            instance.Branch((String) loJSON1.get("reqstdby"));
            instance.DateRequested((String) loJSON1.get("reqstdxx"));
            instance.IssuedBy(String.valueOf(((String) loJSON1.get("apprvlcd")).charAt(3)));
            instance.MiscInfo(lsName);
            instance.System((String) loJSON1.get("systemcd"));
            
            instance.Encode();
            String lsResult = instance.Result();
                        
            System.out.println("Given code: " + (String) loJSON1.get("apprvlcd"));
            System.out.println("Result: " + lsResult);

            logwrapr.info("Given code: " + (String) loJSON1.get("apprvlcd"));
            logwrapr.info("Result: " + lsResult);
            
            int lnResponse = 0;
            if (("").equals((String) loJSON1.get("reqstdby")))
                lnResponse = instance.isEqualx(lsResult, (String) loJSON1.get("apprvlcd"));
            else
                lnResponse = instance.isEqual(lsResult, (String) loJSON1.get("apprvlcd"));

            if (lnResponse == 0){
                setMessage("Validation successful.");
                logwrapr.info("Validation successful.");
                return true;
            } else{
                setMessage("Invalid approval code detected.");
                logwrapr.warning("Invalid approval code detected.");
                return false;
            }
        } catch (ParseException | DecoderException | UnsupportedEncodingException ex) {
            logwrapr.severe(ex.getMessage());
            setMessage(ex.getMessage());
            return false;
        }
    }
    
    public boolean ValidateOffline(String fsTransNox, String fsApprvlCd){        
        if (fsTransNox.length() != 12){
            logwrapr.severe("TRANSACTION NO: " + fsTransNox);
            logwrapr.severe("Invalid approval request transaction no...");
            setMessage("Invalid approval request transaction no...");
            return false;
        }
        
        UnitSystemCodeApproval lpoGRider = (UnitSystemCodeApproval) LoadTransaction(fsTransNox);
        
        if (lpoGRider == null) {
            logwrapr.severe("TRANSACTION NO: " + fsTransNox);
            logwrapr.severe("No record found for approval request transaction no...");
            setMessage("No record found for approval request transaction no...");
            return false;
        }
        
        CodeApproval instance = new CodeApproval();
        
        try {
            String lsName = CommonUtils.Win2UTF(lpoGRider.getRemarks1());
            if (lsName.contains("ñ") || lsName.contains("Ñ") || lsName.contains("ń")){
                return ValidateOnline(fsTransNox, fsApprvlCd, lpoGRider.getApprovedBy());
            }
            
            instance.Branch(lpoGRider.getRequestedBy());
            instance.DateRequested(SQLUtil.dateFormat(lpoGRider.getDateRequested(), SQLUtil.FORMAT_SHORT_DATE));
            instance.IssuedBy(String.valueOf(fsApprvlCd.charAt(3)));
            instance.MiscInfo(lsName);
            instance.System(lpoGRider.getSystemCode());
        } catch (DecoderException | UnsupportedEncodingException ex) {
            logwrapr.severe(ex.getMessage());
            setMessage(ex.getMessage());
            return false;
        }
        
        instance.Encode();
        String lsResult = instance.Result();
        int lnResponse;
        
        System.out.println("Given code: " + fsApprvlCd);
        System.out.println("Result: " + lsResult);
        
        logwrapr.info("Given code: " + fsApprvlCd);
        logwrapr.info("Result: " + lsResult);
        
        if (lpoGRider.getRequestedBy().equals(""))
            lnResponse = instance.isEqualx(lsResult, fsApprvlCd);
        else
            lnResponse = instance.isEqual(lsResult, fsApprvlCd);
        
        if (lnResponse == 0){
            setMessage("Validation successful.");
            logwrapr.info("Validation successful.");
            return true;
        } else{
            setMessage("Invalid approval code detected.");
            logwrapr.warning("Invalid approval code detected.");
            return false;
        }
    }
    
    private String getSQ_Master(){
        return MiscUtil.makeSelect(new UnitSystemCodeApproval());
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
    
    public void setSystemCode(String fsValue){systemcd = fsValue;}
    public void setRequestedBy(String fsValue){reqstdby = fsValue;}
    public void setRequestedDt(String fsValue){reqstdxx = fsValue;}
    public void setMiscInfo(String fsValue){miscinfo = fsValue;}
    public void setApprovedBy(String fsValue){approved = fsValue;}
    public void setReason(String fsValue){reasonxx = fsValue;}
    public void setRequestedTo(String fsValue){reqstdto = fsValue;}
    public void setEntryBy(String fsValue){entrybyx = fsValue;}
    
    public void setRemarks1(String fsValue){remarks1 = fsValue;}
    public void setRemarks2(String fsValue){remarks2 = fsValue;}
    
    public String getMessage(){return psMessage;}
    private void setMessage(String fsValue){psMessage = fsValue;}
    
    public UnitSystemCodeApproval getMaster(){ 
        return poData;
    }
    
    public void setMaster(String sFieldNm, Object fsValue){
        if (poData == null)
            return;
        
        poData.setValue(sFieldNm, fsValue);
    }
    
    public static String convertToUTF8(String input) {
        String converted;
        try {
            converted = new String(input.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            converted = input;
        }
        return converted;
    }
    
    private boolean isValidBranch(String fsValue){
        try {
            String lsSQL = "SELECT sBranchCd, sBranchNm FROM Branch WHERE sBranchCd = " + SQLUtil.toSQL(fsValue);
            
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            
            return loRS.next();
        } catch (SQLException ex) {
            logwrapr.warning(ex.getMessage());
        }
        return false;
    }
    
    private boolean isAuthorized(){
        String lsSQL = "SELECT" +
                            "  a.sAuthCode" +
                            ", a.sMobileNo" +
                            ", a.sEmployID" +
                            ", IFNULL(c.sCompnyNm, '') sEmployNm" +
                        " FROM System_Code_Mobile a" +
                            ", Employee_Master001 b" +
                                " LEFT JOIN Client_Master c" +
                                    " ON b.sEmployID = c.sClientID" +
                        " WHERE a.sMobileNo = " + SQLUtil.toSQL(reqstdto) +
                            " AND a.sEmployID = b.sEmployID";
                        
        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        mobileno = reqstdto;
        try {
            while (loRS.next()) {
                lsSQL = loRS.getString("sAuthCode");
                
                if (!lsSQL.contains("CA")){
                    setMessage("Employee " + reqstdto + " is not authorized to issue approval code.");
                    logwrapr.warning("Employee " + reqstdto + " is not authorized to issue approval code.");
                    return false;
                }
                entrybyx = loRS.getString("sEmployID");
                reqstdto = loRS.getString("sEmployID");
                employnm = loRS.getString("sEmployNm");
                branchno = getBranchNo();
                
                logwrapr.info("Employee " + reqstdto + " authorized mobile is " + mobileno);
                return true;
            }
        } catch (SQLException ex) {
            logwrapr.warning(ex.getMessage());
        }
        
        logwrapr.warning(mobileno + " is not authorized to approve.");
        return false;
    }
    
    private String getBranchNo(){
        String lsSQL = "SELECT sMobileNo FROM Branch_Mobile" +
                        " WHERE sBranchCd = " + SQLUtil.toSQL(reqstdby) +
                        " ORDER BY nEntryNox LIMIT 1";
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            while (loRS.next()){
                return loRS.getString("sMobileNo");
            }
        } catch (SQLException ex) {
            logwrapr.warning(ex.getMessage());
        }
        return "";
    }
    
    /**
     * Create SMS
     * 
     * @param fnResult 0 = unauthorized; 1 = success; 2 = invalid requesting branch; 
     *      3 = invalid date; 4 = invalid format
     * @return true/false
     */
    private boolean createSMS(int fnResult){  
        if (poGRider == null) return true;
        
        String lsMessage;
        String lsSQL;
        
        switch (fnResult){
            case 1:
                lsMessage = apprvlcd + " is the CI approval code for Mr/Ms " + poData.getMiscInfo() + 
                            " issued by " + employnm + ".\nGuanzon Group";
                
//                lsMessage = "GUANZON GROUP: CI approval code for Mr/Ms. " + poData.getMiscInfo() + 
//                            " with application date of " + reqstdxx + " is " + apprvlcd + " issued by " + employnm;
                
                if (!branchno.equals("")){
                    lsSQL = "INSERT INTO HotLine_Outgoing SET" +
                            "  sTransNox = " + SQLUtil.toSQL(MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, poGRider.getConnection(), poGRider.getBranchCode())) +
                            ", dTransact = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                            ", sDivision = " + SQLUtil.toSQL("CSS") +
                            ", sMobileNo = " + SQLUtil.toSQL(branchno) +
                            ", sMessagex = " + SQLUtil.toSQL(lsMessage) +
                            ", cSubscrbr = " + SQLUtil.toSQL("0") +
                            ", dDueUntil = " + SQLUtil.toSQL(CommonUtils.dateAdd(poGRider.getServerDate(), 5)) +
                            ", cSendStat = '0'" +
                            ", nNoRetryx = 0" +
                            ", sUDHeader = ''" +
                            ", sReferNox = " + SQLUtil.toSQL(apprvlcd) +
                            ", sSourceCd = " + SQLUtil.toSQL("CODE") +
                            ", cTranStat = '0'" +
                            ", nPriority = 0" +
                            ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());

                    System.out.println(lsSQL);
                    if (poGRider.executeUpdate(lsSQL) < 0)
                        logwrapr.warning("Unable to create sms branch.");
                    }
                
                logwrapr.info("Approval: SMS Created for branch...");
                break;
            case 2:
                lsMessage = "GUANZON GROUP: Please make sure that requesting branch value on your request is correct. Thank you.";
                break;
            case 3:
                lsMessage = "GUANZON GROUP: Please make sure that requesting date value on your request is correct. Thank you.";
                break;
            case 4:
                lsMessage = "GUANZON GROUP: Please make sure to use the preferred format for issuing approval. Thank you.";
                break;
            case 5:
                /*lsMessage = "GUANZON GROUP: Approval code request created for Mr/Ms. " + miscinfo + 
                            " with application date of " + reqstdxx + ". TRANSACTION NO: " + transnox +
                            " BRANCH CODE: " + branchcd + ".";
                
                if (!branchno.equals("")){
                    lsSQL = "INSERT INTO HotLine_Outgoing SET" +
                            "  sTransNox = " + SQLUtil.toSQL(MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, poGRider.getConnection(), poGRider.getBranchCode())) +
                            ", dTransact = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                            ", sDivision = " + SQLUtil.toSQL("CSS") +
                            ", sMobileNo = " + SQLUtil.toSQL(branchno) +
                            ", sMessagex = " + SQLUtil.toSQL(lsMessage) +
                            ", cSubscrbr = " + SQLUtil.toSQL("0") +
                            ", dDueUntil = " + SQLUtil.toSQL(CommonUtils.dateAdd(poGRider.getServerDate(), 5)) +
                            ", cSendStat = '0'" +
                            ", nNoRetryx = 0" +
                            ", sUDHeader = ''" +
                            ", sReferNox = " + SQLUtil.toSQL("") +
                            ", sSourceCd = " + SQLUtil.toSQL("CODE") +
                            ", cTranStat = '0'" +
                            ", nPriority = 0" +
                            ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());

                    System.out.println(lsSQL);
                    if (poGRider.executeUpdate(lsSQL) < 0)
                        logwrapr.warning("Unable to create sms branch.");
                }
                logwrapr.info("Request: SMS Created for branch...");
                return true;*/
                lsMessage = "";
                break;
            default:
                //lsMessage = "GUANZON GROUP: Please make sure that this mobile number is registered for approval code issuance. " +
                //            "Kindly inform your branch manager regarding this matter.";
                
                lsMessage = mobileno + " is not registered for approval code issuance. Contact CSS Department to register. -Guanzon";
            }
        
            if (!lsMessage.equals("")){
                lsSQL = "INSERT INTO HotLine_Outgoing SET" +
                        "  sTransNox = " + SQLUtil.toSQL(MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, poGRider.getConnection(), poGRider.getBranchCode())) +
                        ", dTransact = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                        ", sDivision = " + SQLUtil.toSQL("CSS") +
                        ", sMobileNo = " + SQLUtil.toSQL(mobileno) +
                        ", sMessagex = " + SQLUtil.toSQL(lsMessage) +
                        ", cSubscrbr = " + SQLUtil.toSQL("0") +
                        ", dDueUntil = " + SQLUtil.toSQL(CommonUtils.dateAdd(poGRider.getServerDate(), 5)) +
                        ", cSendStat = '0'" +
                        ", nNoRetryx = 0" +
                        ", sUDHeader = ''" +
                        ", sReferNox = " + SQLUtil.toSQL(apprvlcd == null ? "" : apprvlcd) +
                        ", sSourceCd = " + SQLUtil.toSQL("CODE") +
                        ", cTranStat = '0'" +
                        ", nPriority = 0" +
                        ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) +
                        ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());

                if (poGRider.executeUpdate(lsSQL) < 0)
                    logwrapr.warning("Unable to create sms for field specialist.");
                else
                    logwrapr.info("SMS Created for field specialists...");
            }
            
                
        return true;
    }
    
    String systemcd = "";
    String reqstdby = "";
    String reqstdxx = "";
    String miscinfo = "";
    String approved = "";
    String reasonxx = "";
    String reqstdto = "";
    String entrybyx = "";
    
    String remarks1 = "";
    String remarks2 = "";
    
    String mobileno = "";
    String employnm = "";
    String branchno = "";
    String apprvlcd = "";
    
    GRider poGRider;
    String psBranchCd;
    String psMessage;
    int pnEditMode;
    
    UnitSystemCodeApproval poData;
    
    final String MODULE_NAME = this.getClass().getSimpleName();
    LogWrapper logwrapr;    
    
    //json process variables
    Map<String, String> headers = new HashMap<>();
    Calendar calendar = Calendar.getInstance();
    JSONObject param = new JSONObject();
    JSONParser parser = new JSONParser();
    String response;
    
    final String REQSTAPI = "integsys/codeapproval/code_request.php";
    final String DECDEAPI = "integsys/codeapproval/code_decide.php";
    final String VLDTEAPI = "integsys/codeapproval/valid_approval_java.php";
}