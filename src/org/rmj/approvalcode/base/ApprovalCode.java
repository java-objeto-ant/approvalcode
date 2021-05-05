package org.rmj.approvalcode.base;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.DecoderException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GProperty;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.StringHelperMisc;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.FileUtil;
import org.rmj.appdriver.agentfx.WebClient;
import org.rmj.g3lr.android.core.CodeApproval;

public class ApprovalCode {
    final String PRODCTID = "IntegSys";
    final String JAVAPATH = "D:/GGC_Java_Systems/";
    final String TEMPPATH = "D:/GGC_Java_Systems/temp/";
    final String UPLDPATH = "D:/GGC_Java_Systems/temp/upload/";
    final String INVLDPTH = "D:/GGC_Java_Systems/temp/upload/invalid/";
    final String SUCSSPTH = "D:/GGC_Java_Systems/temp/upload/success/";
    
    final static String REQUEST = "request";
    final static String SYSTEM = "system";
    
    final String REQSTAPI = "/integsys/codeapproval/code_request.php";
    final String DECDEAPI = "/integsys/codeapproval/code_decide.php";
    final String VLDTEAPI = "/integsys/codeapproval/valid_approval.php";
    
    Map<String, String> headers;
    JSONObject param;
    
    private GRider oApp;
    private GProperty oProp = new GProperty("GhostRiderXP");
     
    private String psFile1;
    private String psFile2;
    private boolean pbApproval;
    
    private String transnox;
    private String mobileno;
    private String branchcd;
    private String branchno;
    private String employnm;
    
    private JSONParser parser = new JSONParser();
    private JSONObject json;
        
    private String apprvlcd;
    private String email;
    
    private String systemcd;
    private String reqstdby;
    private String issuedby;
    private String reqstdxx;
    private String miscinfo;
    
    private String trandate;
    private String remarks1;
    private String remarks2;
    private String reqstdto;
    private String entrybyx;
    
    private String reasonxx;
    private String approved;
    
    private String sClientID;
    private String sProdctID;
    private String sCompName;
    private String sUserIDxx;
    
    private String sResponse;    
    
    /**
     * Approval Code
     * 
     * @param fsFile1 client info JSON formatted file
     * @param fsFile2 request in JSON formated file
     * @param fsUserID empty if using IntegSys; user id if using a utility
     */
    public ApprovalCode(String fsFile1, String fsFile2, String fsUserID){
        psFile1 = fsFile1;
        psFile2 = fsFile2;
        
        if (!fsUserID.equals("")){
            oApp = new GRider(PRODCTID);
            if (!oApp.loadEnv(PRODCTID)){
                createLog(oApp.getErrMsg());
                System.exit(0);
            } 
            if (!oApp.logUser(PRODCTID, fsUserID)){
                createLog(oApp.getErrMsg());
                System.exit(0);
            }
        }
    }
    
    public ApprovalCode(GRider foApp, String fsFile1, String fsFile2){
        oApp = foApp;
        psFile1 = fsFile1;
        psFile2 = fsFile2;
    }
    
    /**
     * Send Request
     * 
     * Sends approval code request to main office or \n
     * validates approval code issued to branch.
     * 
     * @param fbNewRecord true - create request; false - validate approval; \n
     *  if user id on the object instantiation is not empty, \n 
     *  this parameter will be disregarded
     * @return true/false
     */
    public boolean SendRequest(boolean fbNewRecord){
        psFile1 = JAVAPATH + psFile1;
        psFile2 = JAVAPATH + psFile2;
        
        if (!fbNewRecord){
            if (!getParamClient()) return false;
            if (!getParamRequest(false)) return false;
            if (!setParameters(false)) return false;
            
            return validateOffline();
        } else{
            if (oApp == null){
                if (!getParamClient()) return false;
                if (!getParamRequest(fbNewRecord)) return false;
                if (!setParameters(fbNewRecord)) return false;

                if (fbNewRecord) 
                    return codeRequest(true);
                else
                    return validateOffline();
            } else {
                if (!getParamClient()) return false;
                if (!getParamDecide()) return false;
                if (!isAuthorized()){
                    //uploadFailed(true);
                    moveInvalid();
                    createSMS(0);
                    return false;
                }
                
                if (checkRequest() == false){
                    trandate = CommonUtils.dateFormat(oApp.getServerDate(), "yyyy-MM-dd");
                    remarks1 = "";
                    remarks2 = "Utility generated request.";

                    if (!setParameters(true)) return false;
                    if (!codeRequest(true)) return false;
                }

                if (!setParamDecide()) return false;
                return issueApproval();
            }
        }
    }    
    
    /**
     * Upload Unsent Request
     * 
     * Resends unsuccessful request to main office.
     * 
     * @return true/false
     */
    public boolean UploadUnsent(){
        boolean lbSuccess;
        
        File f1, f2;
        File dir = new File(UPLDPATH);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                //get files with creditapp prefix
                if (child.getName().substring(0, 7).equalsIgnoreCase(REQUEST)){
                    //create instance of the file
                    createLog("File 1: " + UPLDPATH + child.getName());
                    f1 = new File(UPLDPATH + child.getName());
                    
                    //check if the system file exists
                    System.out.println("File 2: " + UPLDPATH + SYSTEM + child.getName().substring(7));
                    f2 = new File(UPLDPATH + SYSTEM + child.getName().substring(7));
                    createLog("File 2 Exists: " + f2.exists());
                    
                    //re upload
                    psFile1 = f1.getAbsolutePath();
                    psFile2 = f2.getAbsolutePath();
                    createLog("File 1 Location: " + psFile1);
                    createLog("File 2 Location: " + psFile2);
                    
                    if (oApp == null){
                        if (!getParamClient()) return false;
                        if (!getParamRequest(true)) return false;
                        if (!setParameters(true)) return false;
                        lbSuccess = codeRequest(false);
                    } else {
                        if (!getParamClient()) return false;
                        if (!getParamDecide()) return false;
                        if (!isAuthorized()){
                            //uploadFailed(false);
                            moveInvalid();
                            createSMS(0);
                            return false;
                        }

                        if (checkRequest() == false){
                            trandate = CommonUtils.dateFormat(oApp.getServerDate(), "yyyy-MM-dd");
                            remarks1 = "";
                            remarks2 = "Utility generated request.";

                            if (!setParameters(true)) return false;
                            lbSuccess =  codeRequest(false);
                        }
                        
                        if (!setParamDecide()) return false;
                        lbSuccess = issueApproval();
                    }
                    
                    System.out.println("Files Uploaded: " + lbSuccess);
                    
                    //delete file if uploaded
                    if (lbSuccess){
                        System.out.println("Delete File 1: " + f1.delete());
                        System.out.println("Delete File 2: " + f2.delete());
                    }
                }
            }
        }
        
        return true;
    }
    
    private boolean issueApproval(){        
        apprvlcd = approveRequest();
        
        if (!apprvlcd.equals("")){
            if (updateRequest())
                createLog("Requests have been updated.");
            else createLog("No request have been updated.");
            
            if (!apprvlcd.equals("3")){
                if (createSMS(1))
                    createLog("Text SMS created...");
                else
                    createLog("Unable to create text sms...");
            }
            
            return true;
        } else return false;
    }
    
    private String approveRequest(){
        createLog("Requesting to approve...");
        
        if (reasonxx.equals("")){
            showSuccess("Unable to process empty reason.");
            createLog("Unable to process empty reason.");
            return "";
        }

        if (!setParamDecide()){
            showSuccess("Unable to set parameters...");
            createLog("Unable to set parameters...");
            return "";
        }

        try {
            sResponse = WebClient.sendHTTP(CommonUtils.getConfiguration(oApp, "WebSvr") + DECDEAPI, param.toString(), (HashMap<String, String>) headers);
        } catch (IOException e) {
            showSuccess(e.getMessage());
            createLog(e.getMessage());
            return "";
        }

        if(sResponse == null){
            showSuccess("Internet connection not stable...");
            createLog("Internet connection not stable...");
            return "";
        }else{
            try {
                json = (JSONObject) parser.parse(sResponse);
                
                if (json.get("result").toString().equalsIgnoreCase("success")){
                    if (approved.equalsIgnoreCase("yes")){
                        showSuccess("success");
                        createLog(json.get("apprcode").toString());
                        return json.get("apprcode").toString();
                    } else {
                        showSuccess("Application disapproved successfully.");
                        createLog("Application disapproved successfully.");
                        return "3";
                    }
                } else {
                    sResponse = json.get("error").toString();
                    json = (JSONObject) parser.parse(sResponse);
                    createLog((String) json.get("message"));
                    
                    if (approved.equalsIgnoreCase("yes")){
                        showSuccess("Unable to approve transaction.");
                        createLog("Unable to approve transaction.");
                        return "";
                    } else {
                        showSuccess("Unable to disapprove transaction.");
                        createLog("Unable to disapprove transaction.");
                        return "";
                    }
                }
            } catch (ParseException ex) {
                showSuccess(ex.getMessage());
                createLog(ex.getMessage());
                return "";
            }
        }
    }
    
    private boolean updateRequest(){
        String lsSQL = "SELECT * FROM System_Code_Approval" +
                        " WHERE sSystemCD = " + SQLUtil.toSQL(systemcd) +
                            " AND sReqstdBy = " + SQLUtil.toSQL(reqstdby) +
                            " AND dReqstdxx = " + SQLUtil.toSQL(reqstdxx) +
                            " AND sMiscInfo = " + SQLUtil.toSQL(miscinfo) +
                            " AND cTranStat = '0'" +
                        " LIMIT 1";
        
        ResultSet loRS = oApp.executeQuery(lsSQL);
        try {
            while(loRS.next()){
                lsSQL = "UPDATE System_Code_Approval SET" +
                            "  sReasonxx = " + SQLUtil.toSQL("Duplicate Entry; Forced system update.") +
                            ", cTranStat = " + SQLUtil.toSQL(approved.equalsIgnoreCase("yes") ? "1" : "3") +
                        " WHERE sSystemCD = " + SQLUtil.toSQL(systemcd) +
                            " AND sReqstdBy = " + SQLUtil.toSQL(reqstdby) +
                            " AND dReqstdxx = " + SQLUtil.toSQL(reqstdxx) +
                            " AND sMiscInfo = " + SQLUtil.toSQL(miscinfo) +
                            " AND cTranStat = '0'";

                System.out.println(lsSQL);
                return oApp.executeQuery(lsSQL, "System_Code_Approval", "", "") > 0;
            }
        } catch (SQLException ex) {
            createLog(ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    private boolean checkRequest(){
        String lsSQL = "SELECT" +
                            "  sTransNox" +
                            ", sReqstdBy" +
                        " FROM System_Code_Approval" +
                        " WHERE sSystemCD = " + SQLUtil.toSQL(systemcd) +
                            " AND sReqstdBy = " + SQLUtil.toSQL(reqstdby) +
                            " AND dReqstdxx = " + SQLUtil.toSQL(reqstdxx) +
                            " AND sMiscInfo = " + SQLUtil.toSQL(miscinfo) +
                            " AND cTranStat = '0'" +
                        " ORDER BY sTransNox" +
                        " LIMIT 1";
        
        System.out.println(lsSQL);
        ResultSet loRS = oApp.executeQuery(lsSQL);
        
        transnox = "";
        try {
            while(loRS.next()){
               transnox = loRS.getString("sTransNox");
            }
        } catch (SQLException ex) {
            createLog(ex.getMessage());
        }
        MiscUtil.close(loRS);
        
        return !transnox.equals("");
    }
    
    private boolean validateOffline(){        
        createLog("Offline validation...");
        
        CodeApproval instance = new CodeApproval();
        
        instance.Branch(reqstdby);
        instance.DateRequested(reqstdxx);
        instance.IssuedBy(String.valueOf(apprvlcd.charAt(3)));
        instance.MiscInfo(miscinfo);
        instance.System(systemcd);
        instance.Encode();
        
        String lsResult = instance.Result();
        int lnResponse;
        
        createLog("Given code: " + apprvlcd);
        createLog("Result: " + lsResult);
        
        if (reqstdby.equals(""))
            lnResponse = instance.isEqualx(lsResult, apprvlcd);
        else
            lnResponse = instance.isEqual(lsResult, apprvlcd);
        
        if (lnResponse == 0){
            showSuccess("success");
            createLog("Validation successful.");
        } else{
            showSuccess("Invalid approval code detected.");
            createLog("Invalid approval code detected.");
            System.exit(0);
        }
        
        return true;
    }
    
    private boolean codeRequest(boolean fbNewEntry){
        try {
            sResponse = WebClient.sendHTTP(CommonUtils.getConfiguration(oApp, "WebSvr") + REQSTAPI, param.toJSONString(), (HashMap<String, String>) headers);
            if(sResponse == null){
                uploadFailed(fbNewEntry);
                
                showSuccess("No response from the server.");
                createLog("No response from the server.");
                System.exit(0);
            } 
            
            try {
                json = (JSONObject) parser.parse(sResponse);
                
                if (json.get("result").toString().equalsIgnoreCase("success")){
                    transnox = json.get("transnox").toString();
                    branchcd = json.get("branchcd").toString();   
                    
                    showSuccess("TRANSACTION NO.:" + transnox + " BRANCH:" + branchcd);
                    createLog("TRANSACTION NO.:" + transnox + " BRANCH:" + branchcd);
                    createSMS(5);
                    
                    //delete the parameter files
                    FileUtil.fileDelete(psFile1);
                    FileUtil.fileDelete(psFile2);
                    //moveSuccess();
                } else {
                    json = (JSONObject) parser.parse(json.get("error").toString());
                    
                    uploadFailed(fbNewEntry);
                    
                    showSuccess(json.get("message").toString());
                    createLog(json.get("message").toString());
                    System.exit(0);
                }
            } catch (ParseException ex) {
                uploadFailed(fbNewEntry);
                
                showSuccess("PARSE Exception: " + sResponse);
                createLog("PARSE Exception: " + sResponse);
                System.exit(0);
            }
        } catch (IOException ex) {
            uploadFailed(fbNewEntry);

            showSuccess("PARSE Exception: " + sResponse);
            createLog("PARSE Exception: " + sResponse);
            System.exit(0);
        }
        
        return true;
    }
    
    private boolean setParameters(boolean fbNewRecord){
        createLog("Setting paramaters...");
        
        Calendar calendar = Calendar.getInstance();
        
        headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("g-api-id", sProdctID);
        headers.put("g-api-imei", sCompName);
        
        headers.put("g-api-key", CommonUtils.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));        
        headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String)headers.get("g-api-imei") + (String)headers.get("g-api-key")));
        headers.put("g-api-client", sClientID);    
        headers.put("g-api-user", sUserIDxx);    
        headers.put("g-api-log", "");
        headers.put("g-api-token", "");  

        param = new JSONObject();
        
        if (fbNewRecord){
            if (!systemcd.equalsIgnoreCase("ca")){
                moveInvalid();
                createLog("Invalid system code detected: " + systemcd);
                createSMS(4);
                return false;
            }
            
            //check requesting branch
            if (!reqstdby.equals("")){
                if (!isBranchExist(reqstdby)){
                    moveInvalid();
                    createLog("Invalid branch detected: " + reqstdby);
                    createSMS(2);
                    return false;
                } 
            }
            
            //check if date is valid
            if (!reqstdxx.equals("")){
                if (!StringHelperMisc.isDate(reqstdxx)){
                    moveInvalid();
                    createLog("Invalid date detected: " + reqstdxx);
                    createSMS(3);
                    return false;
                }
            }
            
        //trim spaces on misc info
        if (!miscinfo.equals("")) miscinfo = miscinfo.trim();
            
            param.put("trandate", trandate);
            param.put("systemcd", systemcd);
            param.put("reqstdby", reqstdby);
            param.put("reqstdxx", reqstdxx);
            param.put("miscinfo", CommonUtils.UTF2Hex(miscinfo));
            param.put("remarks1", remarks1);
            param.put("remarks2", remarks2);
            param.put("reqstdto", reqstdto);
            param.put("entrybyx", entrybyx);
        } else {
            param.put("apprvlcd", apprvlcd);
            param.put("systemcd", systemcd);
            param.put("reqstdby", reqstdby);
            param.put("issuedby", issuedby);
            param.put("reqstdxx", reqstdxx);
            param.put("miscinfo", CommonUtils.UTF2Hex(miscinfo));
        }
        
        createLog("Setting paramaters finished...");
        return true;
    }
    
    private boolean setParamDecide(){
        createLog("Setting paramaters...");
        
        Calendar calendar = Calendar.getInstance();
        
        headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("g-api-id", sProdctID);
        headers.put("g-api-imei", sCompName);
        
        headers.put("g-api-key", CommonUtils.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));        
        headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String)headers.get("g-api-imei") + (String)headers.get("g-api-key")));
        headers.put("g-api-client", sClientID);    
        headers.put("g-api-user", sUserIDxx);    
        headers.put("g-api-log", "");
        headers.put("g-api-token", ""); 

        param = new JSONObject();
        param.put("transnox", transnox);
        param.put("reasonxx", reasonxx);
        param.put("approved", approved);
        param.put("email", "xurpas7@gmail.com");        
        
        createLog("Setting decide parameters finished...");
        return true;
    }
    
    private boolean getParamClient(){
        createLog("Reading client parameter file...");        
        if (!psFile1.equals("")){
            sResponse = FileUtil.fileRead(psFile1);
            sResponse = sResponse.replace("'", "\"");
            createLog("Client variables: " + sResponse);
            
            try {
                json = (JSONObject) parser.parse(sResponse);
                sClientID = json.get("clientid").toString();
                sProdctID = json.get("prodctid").toString();
                sCompName = json.get("pcnamexx").toString();
                sUserIDxx = json.get("useridxx").toString();
            } catch (ParseException ex) {
                createLog("PARSE Exception: " + sResponse);
                showSuccess("PARSE Exception: " + sResponse);
                System.exit(0);
            }
        } else {
            createLog("File not found.");
            showSuccess("File not found.");
        }
        createLog("Reading client parameter file successful...");
        return true;
    }
    private boolean getParamRequest(boolean fbNewRecord) {
        createLog("Reading request parameter file...");  
        if (!psFile2.equals("")){           
            sResponse = FileUtil.fileRead(psFile2);
            sResponse = sResponse.replace("'", "\"");
            createLog("Request variables: " + sResponse);
            
            try {
                json = (JSONObject) parser.parse(sResponse);
                
                systemcd = json.get("systemcd").toString();
                reqstdby = json.get("reqstdby").toString();
                reqstdxx = json.get("reqstdxx").toString();
                miscinfo = CommonUtils.Win2UTF(json.get("miscinfo").toString());
                
                pbApproval = false;
                if (fbNewRecord){
                    //android request
                    trandate = json.get("trandate").toString();
                    remarks1 = json.get("remarks1").toString();
                    remarks2 = json.get("remarks2").toString();
                    entrybyx = json.get("entrybyx").toString();
                    reqstdto = json.get("reqstdto").toString();
                } else {
                    apprvlcd = json.get("apprvlcd").toString();
                    issuedby = json.get("issuedby").toString();
                }
            } catch (ParseException | DecoderException | UnsupportedEncodingException ex) {
                createLog("PARSE Exception: " + sResponse);
                showSuccess("PARSE Exception: " + sResponse);
                System.exit(0);
            }
        } else {
            createLog("File not found.");
            showSuccess("File not found.");
        }
        createLog("Reading request parameter file successful...");
        return true;
    }    
    
    private boolean getParamDecide(){
        createLog("Reading deciding parameter file...");  
        if (!psFile2.equals("")){           
            sResponse = FileUtil.fileRead(psFile2);
            sResponse = sResponse.replace("'", "\"");
            createLog("Request variables: " + sResponse);
            
            try {
                json = (JSONObject) parser.parse(sResponse);
                
                systemcd = json.get("systemcd").toString();               
                reqstdby = json.get("reqstdby").toString();
                reqstdxx = json.get("reqstdxx").toString();
                miscinfo = CommonUtils.Win2UTF(json.get("miscinfo").toString());
                approved = json.get("approved").toString();
                reasonxx = json.get("reasonxx").toString();
                reqstdto = json.get("reqstdto").toString();
                entrybyx = json.get("reqstdto").toString();
            } catch (ParseException | DecoderException | UnsupportedEncodingException ex) {
                createLog("PARSE Exception: " + sResponse);
                showSuccess("PARSE Exception: " + sResponse);
                System.exit(0);
            }
        } else {
            createLog("File not found.");
            showSuccess("File not found.");
        }
        createLog("Reading request parameter file successful...");
        return true;
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
        ResultSet loRS = oApp.executeQuery(lsSQL);
        
        mobileno = reqstdto;
        try {
            while (loRS.next()) {
                lsSQL = loRS.getString("sAuthCode");
                
                if (!lsSQL.contains("CA")){
                    createLog("Employee " + reqstdto + " is not authorized to issue approval code.");
                    return false;
                }
                entrybyx = loRS.getString("sEmployID");
                reqstdto = loRS.getString("sEmployID");
                employnm = loRS.getString("sEmployNm");
                branchno = getBranchNo();
                
                createLog("Employee " + reqstdto + " authorized mobile is " + mobileno);
                return true;
            }
        } catch (SQLException ex) {
            createLog(ex.getMessage());
        }
        
        createLog(mobileno + " is not authorized to approve.");
        return false;
    }
    
    private String getBranchNo(){
        String lsSQL = "SELECT sMobileNo FROM Branch_Mobile" +
                        " WHERE sBranchCd = " + SQLUtil.toSQL(reqstdby) +
                        " ORDER BY nEntryNox LIMIT 1";
        
        ResultSet loRS = oApp.executeQuery(lsSQL);
        
        try {
            while (loRS.next()){
                return loRS.getString("sMobileNo");
            }
        } catch (SQLException ex) {
            createLog(ex.getMessage());
        }
        return "";
    }
    
    
    private void moveInvalid(){
        createLog("Failed request: Moving to files to for upload.");
        if (FileUtil.moveFile(psFile1, INVLDPTH + REQUEST + "»" + systemcd + "»" + reqstdxx + "»" + miscinfo + ".TMP"))
            createLog(psFile1 + " ---> " + INVLDPTH + REQUEST + "»" + systemcd + "»" + reqstdxx + "»" + miscinfo + ".TMP");

        if (FileUtil.moveFile(psFile2, INVLDPTH + SYSTEM + "»" + systemcd + "»" + reqstdxx + "»" + miscinfo + ".TMP"))
            createLog(psFile2 + " ---> " + INVLDPTH + SYSTEM + "»" + systemcd + "»" + reqstdxx + "»" + miscinfo + ".TMP");
    }
    
    private void moveSuccess(){
        createLog("Success request: Moving to files to success path.");
        if (FileUtil.moveFile(psFile1, SUCSSPTH + REQUEST + "»" + transnox))
            createLog(psFile1 + " ---> " + SUCSSPTH + REQUEST + "»" + transnox);

        if (FileUtil.moveFile(psFile2, SUCSSPTH + SYSTEM + "»" + transnox))
            createLog(psFile2 + " ---> " + SUCSSPTH + SYSTEM + "»" + transnox);
    }
    
    private void uploadFailed(boolean fbNewEntry){
        if (fbNewEntry){
            createLog("Failed request: Moving to files to for upload.");
            if (FileUtil.moveFile(psFile1, UPLDPATH + REQUEST + "»" + systemcd + "»" + reqstdxx + "»" + miscinfo + ".TMP"))
                createLog(psFile1 + " ---> " + UPLDPATH + REQUEST + "»" + systemcd + "»" + reqstdxx + "»" + miscinfo + ".TMP");
                
            if (FileUtil.moveFile(psFile2, UPLDPATH + SYSTEM + "»" + systemcd + "»" + reqstdxx + "»" + miscinfo + ".TMP"))
                createLog(psFile2 + " ---> " + UPLDPATH + SYSTEM + "»" + systemcd + "»" + reqstdxx + "»" + miscinfo + ".TMP");
        } else
            createLog("Failed to re-upload " + miscinfo + " data.");
    }
    
    private void showSuccess(String fsMessage){
        FileUtil.fileWrite(JAVAPATH + "res.TMP", fsMessage);
    }
    private void createLog(String fsMessage){
        CommonUtils.createLog(TEMPPATH + "approvalcode.log", fsMessage);
    }
    
    private boolean isBranchExist(String fsBranchCD){
        if (oApp == null) return true;
        
        String lsSQL = "SELECT sBranchNm FROM Branch WHERE sBranchCd  = " + SQLUtil.toSQL(fsBranchCD);
        ResultSet loRS = oApp.executeQuery(lsSQL);
        return MiscUtil.RecordCount(loRS) > 0;
    }
    
    /**
     * Create SMS
     * 
     * @param fnResult 0 = unauthorized; 1 = success; 2 = invalid requesting branch; 
     *      3 = invalid date; 4 = invalid format
     * @return true/false
     */
    private boolean createSMS(int fnResult){  
        if (oApp == null) return true;
        
        String lsMessage;
        String lsSQL;
        
        switch (fnResult){
            case 1:
                lsMessage = "GUANZON GROUP: CI approval code for Mr/Ms. " + miscinfo + 
                            " with application date of " + reqstdxx + " is " + apprvlcd + " issued by " + employnm;
                
                if (!branchno.equals("")){
                    lsSQL = "INSERT INTO HotLine_Outgoing SET" +
                            "  sTransNox = " + SQLUtil.toSQL(MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, oApp.getConnection(), oApp.getBranchCode())) +
                            ", dTransact = " + SQLUtil.toSQL(oApp.getServerDate()) +
                            ", sDivision = " + SQLUtil.toSQL("CSS") +
                            ", sMobileNo = " + SQLUtil.toSQL(branchno) +
                            ", sMessagex = " + SQLUtil.toSQL(lsMessage) +
                            ", cSubscrbr = " + SQLUtil.toSQL("0") +
                            ", dDueUntil = " + SQLUtil.toSQL(CommonUtils.dateAdd(oApp.getServerDate(), 5)) +
                            ", cSendStat = '0'" +
                            ", nNoRetryx = 0" +
                            ", sUDHeader = ''" +
                            ", sReferNox = " + SQLUtil.toSQL(apprvlcd) +
                            ", sSourceCd = " + SQLUtil.toSQL("CODE") +
                            ", cTranStat = '0'" +
                            ", nPriority = 0" +
                            ", sModified = " + SQLUtil.toSQL(oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(oApp.getServerDate());

                    System.out.println(lsSQL);
                    if (oApp.executeQuery(lsSQL, "HotLine_Outgoing", "", "") < 0)
                        createLog("Unable to create sms branch.");
                }
                
                createLog("Approval: SMS Created for branch...");
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
                lsMessage = "GUANZON GROUP: Approval code request created for Mr/Ms. " + miscinfo + 
                            " with application date of " + reqstdxx + ". TRANSACTION NO: " + transnox +
                            " BRANCH CODE: " + branchcd + ".";
                
                if (!branchno.equals("")){
                    lsSQL = "INSERT INTO HotLine_Outgoing SET" +
                            "  sTransNox = " + SQLUtil.toSQL(MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, oApp.getConnection(), oApp.getBranchCode())) +
                            ", dTransact = " + SQLUtil.toSQL(oApp.getServerDate()) +
                            ", sDivision = " + SQLUtil.toSQL("CSS") +
                            ", sMobileNo = " + SQLUtil.toSQL(branchno) +
                            ", sMessagex = " + SQLUtil.toSQL(lsMessage) +
                            ", cSubscrbr = " + SQLUtil.toSQL("0") +
                            ", dDueUntil = " + SQLUtil.toSQL(CommonUtils.dateAdd(oApp.getServerDate(), 5)) +
                            ", cSendStat = '0'" +
                            ", nNoRetryx = 0" +
                            ", sUDHeader = ''" +
                            ", sReferNox = " + SQLUtil.toSQL("") +
                            ", sSourceCd = " + SQLUtil.toSQL("CODE") +
                            ", cTranStat = '0'" +
                            ", nPriority = 0" +
                            ", sModified = " + SQLUtil.toSQL(oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(oApp.getServerDate());

                    System.out.println(lsSQL);
                    if (oApp.executeQuery(lsSQL, "HotLine_Outgoing", "", "") < 0)
                        createLog("Unable to create sms branch.");
                }
                createLog("Request: SMS Created for branch...");
                return true;
            default:
                lsMessage = "GUANZON GROUP: Please make sure that this mobile number is registered for approval code issuance. " +
                            "Kindly inform your branch manager regarding this matter.";
            }
        
            lsSQL = "INSERT INTO HotLine_Outgoing SET" +
                    "  sTransNox = " + SQLUtil.toSQL(MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, oApp.getConnection(), oApp.getBranchCode())) +
                    ", dTransact = " + SQLUtil.toSQL(oApp.getServerDate()) +
                    ", sDivision = " + SQLUtil.toSQL("CSS") +
                    ", sMobileNo = " + SQLUtil.toSQL(mobileno) +
                    ", sMessagex = " + SQLUtil.toSQL(lsMessage) +
                    ", cSubscrbr = " + SQLUtil.toSQL("0") +
                    ", dDueUntil = " + SQLUtil.toSQL(CommonUtils.dateAdd(oApp.getServerDate(), 5)) +
                    ", cSendStat = '0'" +
                    ", nNoRetryx = 0" +
                    ", sUDHeader = ''" +
                    ", sReferNox = " + SQLUtil.toSQL(apprvlcd == null ? "" : apprvlcd) +
                    ", sSourceCd = " + SQLUtil.toSQL("CODE") +
                    ", cTranStat = '0'" +
                    ", nPriority = 0" +
                    ", sModified = " + SQLUtil.toSQL(oApp.getUserID()) +
                    ", dModified = " + SQLUtil.toSQL(oApp.getServerDate());

            if (oApp.executeQuery(lsSQL, "HotLine_Outgoing", "", "") < 0)
                createLog("Unable to create sms for field specialist.");
            else
                createLog("SMS Created for field specialists...");
                
        return true;
    }
    
    private boolean createRequest(){
        String lsSQL = "INSERT INTO System_Code_Approval SET" +
                        "  sTransNox = " + SQLUtil.toSQL(MiscUtil.getNextCode("System_Code_Approval", "sTransNox", true, oApp.getConnection(), oApp.getBranchCode())) +
                        ", dTransact = " + SQLUtil.toSQL(trandate) +
                        ", sSystemCD = " + SQLUtil.toSQL(systemcd) +
                        ", sReqstdBy = " + SQLUtil.toSQL(reqstdby) +
                        ", sReqstdxx = " + SQLUtil.toSQL(reqstdxx) +
                        ", cIssuedBy = ''" +
                        ", sMiscInfo = " + SQLUtil.toSQL(miscinfo) +
                        ", sRemarks1 = " + SQLUtil.toSQL(remarks1) +
                        ", sRemarks2 = " + SQLUtil.toSQL("System generated request.") +
                        ", sApprCode = ''" +
                        ", sEntryByx = " + SQLUtil.toSQL(entrybyx) +
                        ", sApprvByx = ''" +
                        ", sReasonxx = ''" +
                        ", sReqstdTo = " + SQLUtil.toSQL(reqstdto) +
                        ", cSendStat = '0'" +
                        ", cTranStat = '0'" +
                        ", sModified = " + SQLUtil.toSQL(oApp.getUserID()) +
                        ", dModified = " + SQLUtil.toSQL(oApp.getServerDate());       
        
        System.out.println(lsSQL);
        return oApp.executeQuery(lsSQL, "System_Code_Approval", oApp.getBranchCode(), "") > 0;
    }
    
    private boolean sendRequest(){
        String lsSQL = "";
        
        return true;
    }
}