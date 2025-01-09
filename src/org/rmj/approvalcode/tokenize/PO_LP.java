package org.rmj.approvalcode.tokenize;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.WebClient;
import org.rmj.appdriver.agentfx.service.PO_Master;
import org.rmj.lib.net.MiscReplUtil;
import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class PO_LP implements iNotification{
    private final String DATABASE = "CASys_DBF_LP";
    private final String MASKNAME = "LOSPEDRITOS";
    private final String SOURCECD = "PO";
    private final String RQSTTYPE = "LP";
    
    private final String LIMIT = "engr.po.large";
    private final String TOPMGMNT = "M00117002119;"; 
    private final long WAITTIME = 30; //30 minutes waiting time, if not yet approved send a request to the next top managment approving officer
    
    GRider _instance;
    String _sourcecd;
    String _rqsttype;
    String _transnox;
    String _sourceno;
    String _messagex;
    
    boolean _singletx = false;

    @Override
    public void setGRider(GRider foApp) {
        _instance = foApp;
        _transnox = "";
    }

    @Override
    public void setSourceCode(String fsSourceCd) {
        _sourcecd = fsSourceCd.toUpperCase();
    }

    @Override
    public void setRequestType(String fsRqstType) {
        _rqsttype = fsRqstType.toUpperCase();
    }
    
    @Override
    public void setTransNox(String fsTransNox) {
        _transnox = fsTransNox;
        
        if (!_transnox.equals("")) _singletx = true;
    }

    @Override
    public boolean SendNotification() {
        _messagex = "";

        if (!_sourcecd.equals(SOURCECD)) {
            _messagex = "Source transaction is not for this object.";
            return false;
        }

        if (!_rqsttype.equals(RQSTTYPE)) {
            _messagex = "Request type is not for this object.";
            return false;
        }

        if (_instance == null) {
            _messagex = "Application driver is not set.";
            return false;
        }

        processTopManagementApproval();

        String lsSQL;
        String lsDetail;

        lsSQL = "SELECT" +
                "  a.sTransNox" +
                ", a.dTransact" +
                ", a.sSourceNo" +
                ", a.sSourceCd" +
                ", a.sRqstType" +
                ", a.sReqstInf" +
                ", a.sReqstdTo" +
                ", a.sMobileNo" +
                ", a.cSendxxxx" +
                ", a.sReqstdBy" +
            " FROM GGC_ISysDBF.Tokenized_Approval_Request a" +
            " WHERE a.sSourceCd = " + SQLUtil.toSQL(SOURCECD) +
                " AND a.sRqstType = " + SQLUtil.toSQL(RQSTTYPE) +
                " AND a.cApprType = '1'" + //requested approval type is tokenized
                " AND a.cTranStat = '0'" + //not approved request
                " AND a.cSendxxxx < '2'"; //not yet sent notification

        //user is sending an specific PO request
        if (!_transnox.equals("")) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox = " + SQLUtil.toSQL(_transnox));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.dRcvdDate <= DATE_ADD(CURRENT_TIMESTAMP(), INTERVAL -1 MINUTE)");
        }

        ResultSet loRS = _instance.executeQuery(lsSQL);

        try {
            int lnStat = 0;

            while (loRS.next()) {
                _transnox = loRS.getString("sTransNox");
                _sourceno = loRS.getString("sSourceNo");
                lnStat = 0;

                lsSQL = "APP_RQST " + RQSTTYPE + "/" + loRS.getString("sReqstInf").substring(0, 4) + "/" + loRS.getString("sReqstInf") + "/By:" + getUserName(loRS.getString("sReqstdBy"), false);

                //the request already sent an sms but email is not yet sent
                if (loRS.getString("cSendxxxx").equals("1")) {
                } else { //no notification has been sent
                    if (sendSMS(loRS.getString("sMobileNo"), lsSQL, loRS.getString("sSourceNo"))) { //send sms first

                        if (TOPMGMNT.contains(loRS.getString("sReqstdTo"))) {
                            sendSMS("09176387208", lsSQL, loRS.getString("sSourceNo"));
                            sendSMS("09176340516", lsSQL, loRS.getString("sSourceNo"));

                        }
                        //send details
                        lsDetail = generateDetailSMS(_sourceno);
                        if (!lsDetail.isEmpty()) {
                            if (sendSMS(loRS.getString("sMobileNo"), lsDetail, loRS.getString("sSourceNo"))) {
                                System.out.println("Order detailed notification sent successfully.");
                            } else {
                                System.err.println("Unable to send order detailed notification sent successfully.");
                            }
                        }

                        System.out.println("SMS notification sent successfully.");
                        lnStat += 1;                     
                        
                        lsSQL = "UPDATE GGC_ISysDBF.Tokenized_Approval_Request SET" +
                                    "  cSendxxxx = " + SQLUtil.toSQL(lnStat) +
                                    ", dSendDate = " + SQLUtil.toSQL(_instance.getServerDate()) +
                                " WHERE sTransNox = " + SQLUtil.toSQL(_transnox);
                        _instance.executeQuery(lsSQL, "Tokenized_Approval_Request", _instance.getBranchCode(), "");
                    }
                }
            }

            if (_singletx) {
                _messagex = "Notification sent successfully(" + lnStat + ")";
            } else {
                _messagex = "Notifications sent successfully via utility.";
            }
        } catch (SQLException ex) {
            _messagex = ex.getMessage();
            return false;
        }

        return true;
    }

    @Override
    public String getMessage() {
        return _messagex;
    }
    
    private boolean sendSMS(String fsMobileNo, String fsMessagex, String fsSourceNo){
        System.out.println("Sending SMS to " + fsMobileNo);
        
        String fsURL = "https://restgk.guanzongroup.com.ph/system/masking/sendSMS.php";
        
        String clientid = _instance.getClientID(); //this must be replaced based on the client id using it
        String productid = _instance.getProductID(); //this must be replaced based on the product id using it
        String imei = "GMC_SEG09"; //this must be replaced based on the computer name using it
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
        param.put("message", fsMessagex);
        param.put("mobileno", fsMobileNo);
        param.put("maskname", MASKNAME);
        
        String response;
        boolean sent;
        try {
            response = WebClient.sendHTTP(fsURL, param.toJSONString(), (HashMap<String, String>) headers);
            if(response == null){
                System.out.println("No Response");
                sent = false;
            } 

            JSONParser loParser = new JSONParser();
            JSONObject loJSON = (JSONObject) loParser.parse(response);
            
            if (loJSON.get("result").equals("success")){
                System.out.println((String) loJSON.get("message") + "(" + (String) loJSON.get("maskname") + " - " + (String) loJSON.get("id") + ")");
                sent = true;
            } else {
                loJSON = (JSONObject) loJSON.get("error");
                System.err.println(String.valueOf(loJSON.get("code")) + " - " + (String) loJSON.get("message"));
                sent = false;
            }
        } catch (IOException | ParseException ex) {
            ex.printStackTrace();
            sent = false;
        }
                
        String lsSQL = MiscUtil.getNextCode("HotLine_Outgoing", "sTransNox", true, _instance.getConnection(), "MX01");

        lsSQL = "INSERT INTO HotLine_Outgoing SET" +
                "  sTransNox = " + SQLUtil.toSQL(lsSQL) +
                ", dTransact = " + SQLUtil.toSQL(_instance.getServerDate()) +
                ", sDivision = 'MIS'" +
                ", sMobileNo = " + SQLUtil.toSQL(fsMobileNo) +
                ", sMessagex = " + SQLUtil.toSQL(fsMessagex) +
                ", cSubscrbr = " + SQLUtil.toSQL(CommonUtils.classifyNetwork(fsMobileNo)) +
                ", dDueUntil = " + SQLUtil.toSQL(_instance.getServerDate()) +
                ", cSendStat = '2'" +
                ", nNoRetryx = '1'" +
                ", sUDHeader = ''" +
                ", sReferNox = " + SQLUtil.toSQL(fsSourceNo) +
                ", sSourceCd = " + SQLUtil.toSQL("APTK") +
                ", cTranStat = " + SQLUtil.toSQL(sent ? "1" : "0") +
                ", nPriority = 1" +
                ", sModified = " + SQLUtil.toSQL(_instance.getUserID()) +
                ", dModified = " + SQLUtil.toSQL(_instance.getServerDate());

        _instance.executeUpdate(lsSQL);
        
        return sent;
    }
    
    private boolean sendgmail(String fsRcvr, String fsCopy, String fsSubj, String fsBody, String fsFile){
        final String username = "mis.guanzon1945@gmail.com";
        final String password = "ywbgxbcgccrwsojy";
        //ywbgxbcgccrwsojy
        //48269661945

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); //TLS
        
        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "Guanzon"));
            message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(fsRcvr));
            message.setRecipients(Message.RecipientType.BCC,InternetAddress.parse(fsCopy));
            message.setSubject(fsSubj);
            
            if (!fsFile.isEmpty()){
                BodyPart messageBodyPart = new MimeBodyPart(); 
                messageBodyPart.setText(fsBody);
            
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(new File(fsFile));
                
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                multipart.addBodyPart(attachmentPart);
                
                message.setContent(multipart);
            } else 
                message.setText(fsBody);
            
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return true;
    }
    
    private boolean sendGMail(){
        String lsSQL;
        ResultSet loRS;
        String emailad = "";
        String bcc = "";
        String subj = "";
        String body = "";
        String file = "";
        String reqstinf = "";
        
        try {
            lsSQL = "SELECT sSourceNo, sReqstInf, sReqstdBy, sReqstdTo FROM Tokenized_Approval_Request WHERE sTransNox = " + SQLUtil.toSQL(_transnox);
            loRS = _instance.executeQuery(lsSQL);
            
            if (!loRS.next()){
                _messagex = "No token was set for this approvee.";
                return false;
            }
            
            reqstinf = "APP_RQST " + RQSTTYPE + "/" + loRS.getString("sReqstInf").substring(0, 4) + "/" + loRS.getString("sReqstInf") + "/By:" + getUserName(loRS.getString("sReqstdBy"), false);
            
            String lsReqstdTo = loRS.getString("sReqstdTo");
            
            emailad = getEmailAd(lsReqstdTo);
            
            if (emailad.equals("")){
                 _messagex = "E-mail address of recepient is not set.";
                return false;
            }
            
            lsSQL = "SELECT sTransNox, sRemarksx FROM CASys_DBF.PO_Master WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sSourceNo"));
            loRS = _instance.executeQuery(lsSQL);
            
            if (loRS.next()){
                //add bcc to MIS
                if (TOPMGMNT.contains(lsReqstdTo))bcc = "sirabanal@guanzongroup.com.ph, mtcuison@guanzongroup.com.ph";
                
                subj = "For approval: " + loRS.getString("sRemarksx");
                body = "Good day sir.\n\n" +
                        "Please see attached file for the details of the Purchase Order requested for your approval.\n\n" +
                        "A SMS approval request was sent to your mobile number with this format:\n\n" +
                        reqstinf + "\n\n" +
                        "To approve the transaction kindly forward the SMS approval request to 09479906531.\n\n" +
                        "Thank you.";
                
                file = System.getProperty("sys.default.path.temp") + "/" + loRS.getString("sTransNox") + ".pdf";
                
                return sendgmail(emailad, bcc, subj, body, file);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return false;
    }
    
    private boolean sendMail(){
        String lsSQL;
        ResultSet loRS;
        String emailad = "";
        String reqstinf = "";
        
        try {
            lsSQL = "SELECT sSourceNo, sReqstInf, sReqstdBy, sReqstdTo FROM Tokenized_Approval_Request WHERE sTransNox = " + SQLUtil.toSQL(_transnox);
            loRS = _instance.executeQuery(lsSQL);
            
            if (!loRS.next()){
                _messagex = "No token was set for this approvee.";
                return false;
            }
            
            reqstinf = "APP_RQST " + RQSTTYPE + "/" + loRS.getString("sReqstInf").substring(0, 4) + "/" + loRS.getString("sReqstInf") + "/By:" + getUserName(loRS.getString("sReqstdBy"), false);
            
            String lsReqstdTo = loRS.getString("sReqstdTo");
            
            emailad = getEmailAd(lsReqstdTo);
            
            if (emailad.equals("")){
                 _messagex = "E-mail address of recepient is not set.";
                return false;
            }
            
            lsSQL = "SELECT sTransNox, sRemarksx FROM CASys_DBF.PO_Master WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sSourceNo"));
            loRS = _instance.executeQuery(lsSQL);
            
            if (loRS.next()){
                //path for e-mail information to send
                String mailinfo;
                
                if (_singletx) //called by the PHP API
                    mailinfo = System.getProperty("sys.default.path.temp") + "/mailinfo-" + _transnox + ".json";
                else //utility running on a given interval
                    mailinfo = System.getProperty("sys.default.path.temp") + "/mailinfo.json";
                
                
                JSONObject loJSON = new JSONObject();
                //recipient
                loJSON.put("to", emailad);
                
                //add bcc to MIS
                if (TOPMGMNT.contains(lsReqstdTo)){
                    loJSON.put("bcc1", "sirabanal@guanzongroup.com.ph");
                    loJSON.put("bcc2", "mtcuison@guanzongroup.com.ph");
                }
                
                //sender name
                loJSON.put("from", "Guanzon - Engineering");
                //email title
                loJSON.put("subject", "For approval: " + loRS.getString("sRemarksx"));
                
                loJSON.put("body", "Good day sir.\n\n" +
                                    "Please see attached file for the details of the Purchase Order requested for your approval.\n\n" +
                                    "A SMS approval request was sent to your mobile number with this format:\n\n" +
                                    reqstinf + "\n\n" +
                                    "To approve the transaction kindly forward the SMS approval request to 09479906531.\n\n" +
                                    "Thank you.");
                //attachment
                loJSON.put("filename1", System.getProperty("sys.default.path.temp") + "/" + loRS.getString("sTransNox") + ".pdf");
                
                //delete the old file if exists
                if (MiscReplUtil.fileDelete(mailinfo))
                    System.out.println("Mail info file was deleted successfully.");
                else
                    System.err.println("Unable to delete mail info.");


                //create file
                MiscReplUtil.fileWrite(mailinfo, loJSON.toJSONString());

                //check file if created successfully.
                if (MiscReplUtil.fileExists(mailinfo)){
                    System.out.println("Mail info file was created successfully.");
                                     
                    try {
                        Process process;
                        
                        if (_singletx){ //called by the PHP API
                            if(System.getProperty("os.name").toLowerCase().contains("win"))
                                process = Runtime.getRuntime().exec("cmd /c sendmail.bat access mailinfo-" + _transnox, null, new File(System.getProperty("sys.default.path.config")));
                            else
                                process = Runtime.getRuntime().exec(System.getProperty("sys.default.path.config") + "/sendmail.sh access mailinfo-" + _transnox);
                        }else{ //utility running on a given interval
                            if(System.getProperty("os.name").toLowerCase().contains("win"))
                                process = Runtime.getRuntime().exec("cmd /c sendmail.bat access mailinfo", null, new File(System.getProperty("sys.default.path.config")));
                            else
                                process = Runtime.getRuntime().exec(System.getProperty("sys.default.path.config") + "/sendmail.sh access mailinfo");
                        }
                        
                        StringBuilder output = new StringBuilder();

                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));

                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line + "\n");
                        }

                        int exitVal = process.waitFor();
                        
                        //delete the email configuration file
                        MiscReplUtil.fileDelete(mailinfo);
                        //delete the attachment file created
                        MiscReplUtil.fileDelete(System.getProperty("sys.default.path.temp") + "/" + loRS.getString("sTransNox") + ".pdf");
                        
                        if (exitVal == 0) return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Unable to create mail info.");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return false;
    }
    
    private String getEmailAd(String fsEmployID){
        try {
            String lsSQL = "SELECT sEmailAdd" +
                            " FROM System_Code_Mobile" +
                            " WHERE sEmployID = " + SQLUtil.toSQL(fsEmployID) +
                                " AND sAuthCode = " + SQLUtil.toSQL(RQSTTYPE);
        
            ResultSet loRS = _instance.executeQuery(lsSQL);
            
            if (loRS.next()) return loRS.getString("sEmailAdd");
            
            lsSQL = "SELECT IFNULL(sEmailAdd, 'mtcuison@guanzongroup.com.ph') sEmailAdd FROM Client_Master WHERE sClientID = " + SQLUtil.toSQL(fsEmployID);
            
            if (loRS.next()) return loRS.getString("sEmailAdd"); 
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return "mtcuison@guanzongroup.com.ph";
    }
    
    private boolean export2PDF(String fsTransNox){
        try {
            //load master
            ResultSet loMaster = loadMaster(fsTransNox);
            if (!loMaster.next()){
                _messagex = "Unable to load master record. " + fsTransNox;
                return false;
            }
            
            //load detail
            ResultSet loDetail = loadDetail(fsTransNox);
            if (loDetail == null){
                _messagex = "Unable to load detail record. " + fsTransNox;
                return false;
            }

            //Create the parameter
            Map<String, Object> params = new HashMap<>();
            params.put("sCompnyNm", "Guanzon Group");
            params.put("sBranchNm", _instance.getBranchName());
            params.put("sAddressx", _instance.getAddress() + ", " + _instance.getTownName() + " " + _instance.getProvince());
            params.put("sTransNox", loMaster.getString("sTransNox"));
            params.put("sReferNox", loMaster.getString("sReferNox"));
            params.put("dTransact", SQLUtil.dateFormat(loMaster.getDate("dTransact"), SQLUtil.FORMAT_LONG_DATE));
            params.put("sPrintdBy", getUserName(loMaster.getString("sPrepared"), true));

            //mac 2021.01.12
            //  insert name of approvee and reference no
            params.put("sApprval1", "-");
            params.put("sApprval2", "-");
            params.put("sApprval3", "-");

            String lsSQL = "SELECT" +
                                "  a.sTransNox" + 
                                ", b.sClientNm" +
                            " FROM " + DATABASE + ".Tokenized_Approval_Request a" +
                                " LEFT JOIN " + DATABASE + ".Client_Master b" +
                                    " ON a.sReqstdTo = b.sClientID" +
                            " WHERE a.cTranStat = '1'" +
                                " AND a.sSourceCd = 'PO'" +
                                " AND a.sRqstType = 'EP'" +
                                " AND a.sSourceNo = " + SQLUtil.toSQL(fsTransNox) +
                            " ORDER BY a.dApproved" +
                            " LIMIT 3";

            ResultSet loRS = _instance.executeQuery(lsSQL);
        
        
            int lnCtr = 0;
            while (loRS.next()){
                switch (lnCtr) {
                    case 0:
                        params.put("sApprval1", loRS.getString("sTransNox") + " - " + loRS.getString("sClientNm"));
                        break;
                    case 1:
                        params.put("sApprval2", loRS.getString("sTransNox") + " - " + loRS.getString("sClientNm"));
                        break;
                    default:
                        params.put("sApprval3", loRS.getString("sTransNox") + " - " + loRS.getString("sClientNm"));
                        break;
                }
                lnCtr++;
            }
            
            params.put("xBranchNm", loMaster.getString("xBranchNm"));
            params.put("xDestinat", "n/a");
            params.put("xSupplier", loMaster.getString("xSupplier"));
            params.put("xRemarksx", loMaster.getString("sRemarksx"));
            
            JSONObject loJSON;
            JSONArray loArray = new JSONArray();
        
            while (loDetail.next()){
                loJSON = new JSONObject();
                loJSON.put("sField01", loDetail.getString("sBarCodex"));
                loJSON.put("sField02", loDetail.getString("sDescript"));
                loJSON.put("nField01", loDetail.getInt("nQuantity"));
                loJSON.put("lField01", loDetail.getDouble("nUnitPrce"));
                loArray.add(loJSON);
            }                   
            InputStream stream = new ByteArrayInputStream(loArray.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson;
            
            jrjson = new JsonDataSource(stream);
            
            String printFileName = JasperFillManager.fillReportToFile(System.getProperty("sys.default.path.config") + "/reports/" + 
                                                                "POApproval.jasper", params, jrjson);

            if (printFileName != null){
                JasperExportManager.exportReportToPdfFile(printFileName, System.getProperty("sys.default.path.temp") + "/" + loMaster.getString("sTransNox") + ".pdf");
                return true;
            } else {
                System.err.println("Report data is null. Unable to export report.");
                return false;
            }
        } catch (SQLException | UnsupportedEncodingException | JRException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    private ResultSet loadMaster(String fsTransNox){
        String lsSQL = "SELECT" +
                            "  a.sTransNox" +
                            ", a.dTransact" +
                            ", a.sBranchCd" +
                            ", a.sDestinat" +
                            ", a.sSupplier" +
                            ", a.sReferNox" +
                            ", a.sTermCode" +
                            ", a.nTranTotl" +
                            ", a.sRemarksx" +
                            ", a.sInvTypCd" +
                            ", a.nEntryNox" +
                            ", c.sClientNm xSupplier" +
                            ", d.sDescript xTermName" +
                            ", a.sPrepared" +
                        " FROM "  + DATABASE + ".PO_Master a" +
                            " LEFT JOIN " + DATABASE + ".Client_Master c ON a.sSupplier = c.sClientID" +
                            " LEFT JOIN " + DATABASE + ".Term d ON a.sTermCode = d.sTermCode" +
                        " WHERE a.sTransNox = " + SQLUtil.toSQL(fsTransNox);
        
        return _instance.executeQuery(lsSQL);
    }
    
    private ResultSet loadDetail(String fsTransNox){
        String lsSQL = "SELECT" +
                            "  a.sTransNox" +
                            ", a.nEntryNox" +
                            ", a.sStockIDx" +
                            ", a.nQuantity" +
                            ", a.nUnitPrce" +
                            ", b.sBarCodex" +
                            ", b.sDescript" +
                        " FROM "  + DATABASE + ".PO_Detail a" +
                            " LEFT JOIN " + DATABASE + ".Inventory b ON a.sStockIDx = b.sStockIDx" +
                        " WHERE a.sTransNox = " + SQLUtil.toSQL(fsTransNox) +
                        " ORDER BY a.nEntryNox";
        
        return _instance.executeQuery(lsSQL);
    }
    
    private String getUserName(String fsValue, boolean fbUserIDxx){
        try {
            String lsSQL;
            ResultSet loRS;
                    
            if (!fbUserIDxx)
                lsSQL = "SELECT CONCAT(sFrstName, ' ', sLastName) xFullname FROM " + DATABASE + ".Client_Master WHERE sClientID = " + SQLUtil.toSQL(fsValue);
            else
                lsSQL = "SELECT" +
                        "  CONCAT(b.sFrstName, ' ', b.sLastName) xFullname" +
                    " FROM " + DATABASE + ".xxxSysUser a" +
                        ", " + DATABASE + ".Client_Master b" +
                    " WHERE a.sEmployNo = b.sClientID" +
                        " AND a.sUserIDxx = " + SQLUtil.toSQL(fsValue);
        
            loRS = _instance.executeQuery(lsSQL);
        
            if (loRS.next()) return loRS.getString("xFullname");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return "-";
    }   
    
    private boolean processTopManagementApproval(){
        System.out.println("Processing Top Management approval request.");
        
        //connect to CASys_DBF
        String lsProdctID = "General";
        String lsUserIDxx = "M001111122";

        GRider loGRider = new GRider(lsProdctID);

        if (!loGRider.loadUser(lsProdctID, lsUserIDxx)) return false;
        
        String lsSQL;
        ResultSet loRS;
        ResultSet loRS1;
        ResultSet loRS2;
        
        double lnLimit = getLimitAmount();
        String lsMgmnt [] =  TOPMGMNT.split(";");
        String lsManagement = "";
        
        int lnCtr;
        
        for (lnCtr = 0; lnCtr <= lsMgmnt.length -1; lnCtr++){
            lsManagement = lsManagement + SQLUtil.toSQL(lsMgmnt[lnCtr]) + ",";
        }
        
        lsManagement = "(" + lsManagement.substring(0, lsManagement.length() - 1) + ")";
        
        //get the Purchases that execeeds the engineering only approval
        lsSQL = "SELECT *" +
                " FROM " + DATABASE + ".PO_Master" +
                " WHERE nTranTotl > " + lnLimit +
                        " AND dTransact > '2021-01-01'" +
                        " AND cTranStat = '0'" +
                        " AND dPrepared <= DATE_ADD(CURRENT_TIMESTAMP(), INTERVAL -5 MINUTE)";
        loRS = _instance.executeQuery(lsSQL);
        
        try {
            PO_Master loPO = new PO_Master();
            loPO.setGRider(loGRider);
            
            while(loRS.next()){
                //check token approval request
                lsSQL = "SELECT *" +
                        " FROM " + DATABASE + ".Tokenized_Approval_Request" +
                        " WHERE sRqstType = 'EP'" +
                            " AND sSourceNo = " + SQLUtil.toSQL(loRS.getString("sTransNox")) +
                        " ORDER BY dSendDate";
                
                loRS1 = _instance.executeQuery(lsSQL);
                
                loPO.setTransNmbr(loRS.getString("sTransNox"));
                
                if (MiscUtil.RecordCount(loRS1) == 0){ //no request has been made
                    loPO.createCodeRequest(); //request approvals
                } else { //process by interval request on officers
                    for (lnCtr = 0; lnCtr <= lsMgmnt.length -1; lnCtr++){
                        lsSQL = "SELECT *" +
                                " FROM " + DATABASE + ".Tokenized_Approval_Request" +
                                " WHERE sRqstType = 'EP'" +
                                    " AND sSourceNo = " + SQLUtil.toSQL(loRS.getString("sTransNox")) +
                                    " AND sReqstdTo = " + SQLUtil.toSQL(lsMgmnt[lnCtr]);
                    
                        loRS2 = _instance.executeQuery(lsSQL);
                        
                        if (loRS2.next()){
                            //request not yet sent to server
                            if (loRS2.getString("cSendxxxx").equals("0") && 
                                loRS2.getDate("dSendDate") == null){
                                loPO.uploadCodeRequest(loRS2.getString("sTransNox"), "1");
                            } else if (loRS2.getString("cSendxxxx").equals("2")){ //all notifications has been sent
                                //is the notification was date sent is less than the alloted time for resending to other officer, exit for
                                if (CommonUtils.dateDiff(loGRider.getServerDate(), loRS2.getDate("dSendDate")) < WAITTIME) break;
                            } else if (loRS2.getString("cSendxxxx").equals("1")){ 
                                //only the sms notification was sent
                                //just wait for the utility to send the notification
                            } 
                        }
                    }
                }
            }
            
            System.out.println("End - Processing Top Management approval request.");
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
     
        return false;
    }
    
    private double getLimitAmount(){
        String lsSQL = "SELECT * FROM " + DATABASE + ".Purchase_Level WHERE sPurLvlID = " + SQLUtil.toSQL(LIMIT);
        
        ResultSet loRS = _instance.executeQuery(lsSQL);
        
        try {
            if (loRS.next()) return loRS.getDouble("nAmntThru");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return 0.00;
    }
    
    private String generateDetailSMS(String fsSource) throws SQLException {
        String lsDetail;
        ResultSet loMaster = loadMaster(fsSource);
        if (!loMaster.next()) {
            _messagex = "Unable to load master record. " + _sourceno;
            return "";
        }

        //load detail
        ResultSet loDetail = loadDetail(fsSource);
        if (loDetail == null) {
            _messagex = "Unable to load detail record. " + _sourceno;
            return "";
        }

        String lsSupplier = loMaster.getString("xSupplier").trim();
        String lsTotalAmt = CommonUtils.NumberFormat(loMaster.getDouble("nTranTotl"), "#,##0.00") ;

        lsDetail = "Good day.\n"
                    + "Here are the order details of transaction " + _sourceno + "/" + lsSupplier + "/" + lsTotalAmt + " requesting for your approval.\n\n";
        
        loDetail.beforeFirst();
        while (loDetail.next()) {
            lsDetail += loDetail.getString("nEntryNox") + ". "
                        + loDetail.getString("sDescript") + "/"
                        + "Q:" + loDetail.getString("nQuantity") + "/"
                        + "C:" + loDetail.getString("nUnitPrce") + "\n";

        }
        
        lsDetail += "\nA separate approval message was sent to your mobile number."
                    + " To approve the transaction kindly forward the approval message to 09479906531."
                    + " Thank you.";

        return lsDetail;
    }
}