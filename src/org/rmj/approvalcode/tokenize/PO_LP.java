package org.rmj.approvalcode.tokenize;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
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

public class PO_LP implements iNotification{
    private final String DATABASE = "CASys_DBF_LP";
    private final String MASKNAME = "LOSPEDRITOS";
    private final String SOURCECD = "PO";
    private final String RQSTTYPE = "LP";

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
                    //get details
                    lsDetail = generateDetailSMS(_sourceno);
                    
                    if (!lsDetail.isEmpty()) {
                        //send sms first
                        if (sendSMS(loRS.getString("sMobileNo"), lsSQL, loRS.getString("sSourceNo"))) {
                            if (sendSMS(loRS.getString("sMobileNo"), lsDetail, loRS.getString("sSourceNo"))) {
                                System.out.println("Order detailed notification sent successfully.");
                            } else {
                                System.err.println("Unable to send order detailed notification sent successfully.");
                            }
                            
                            System.out.println("SMS notification sent successfully.");
                            lnStat += 1;                     

                            lsSQL = "UPDATE GGC_ISysDBF.Tokenized_Approval_Request SET" +
                                        "  cSendxxxx = " + SQLUtil.toSQL(lnStat) +
                                        ", dSendDate = " + SQLUtil.toSQL(_instance.getServerDate()) +
                                    " WHERE sTransNox = " + SQLUtil.toSQL(_transnox);
                            _instance.executeQuery(lsSQL, "Tokenized_Approval_Request", _instance.getBranchCode(), "");
                        }
                    } else {
                        System.err.println("A transaction does not have details. Waiting data replication.");
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