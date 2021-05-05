package org.rmj.approvalcode;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GProperty;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.FileUtil;
import org.rmj.approvalcode.agent.XMCodeApproval;
import org.rmj.replication.utility.LogWrapper;

/**
 * Save Approval Code Request
 * 
 * Files needed:
 *      D:/GGC_Java_Systems/temp/creditapp.TMP
 *      D:/GGC_Java_Systems/temp/system.TMP
 * Returns \n\t
 *      0 = success \n\t
 *      1 = error \n
 * 
 * @author Michael Torres Cuison
 * @since 2019.07.03
 */

public class NewRequest {      
    public static void main(String [] args){        
        LogWrapper logwrapr = new LogWrapper("SaveRequest", "D:/GGC_Java_Systems/temp/XMCodeApproval.log");
        String FILE1_PATH = "D:/GGC_Java_Systems/temp/creditapp.TMP";
        String FILE2_PATH = "D:/GGC_Java_Systems/temp/system.TMP";
        String RESULT_DIR = "D:/GGC_Java_Systems/temp/res.TMP";
        
        String lsClientXX = FileUtil.fileRead(FILE1_PATH);
        String lsSystemXX = FileUtil.fileRead(FILE2_PATH);
        
        if (lsClientXX.equals("") || lsSystemXX.equals("")){
            logwrapr.severe("Required parameters not set...");
            System.exit(1);
        }
        
        args = new String[2];
        args[0] = lsSystemXX.replace("'", "\"");
        args[1] = lsClientXX.replace("'", "\"");
        
        String lsProdctID = "";
        String lsUserIDxx = "";
        
        JSONObject loJSON;
        JSONParser loParser = new JSONParser();
        
        try {           
            loJSON = (JSONObject) loParser.parse(args[0]);
            
            lsProdctID = (String) loJSON.get("prodctid");
            lsUserIDxx = (String) loJSON.get("useridxx");
        } catch (ParseException ex) {
            logwrapr.severe(ex.getMessage());
            System.exit(1);
        }

        GRider poGRider = new GRider(lsProdctID);
        GProperty loProp = new GProperty("GhostRiderXP");

        if (!poGRider.loadEnv(lsProdctID)) {
            logwrapr.severe(poGRider.getErrMsg() + "; " + poGRider.getMessage());
            System.exit(1);
        }
        if (!poGRider.logUser(lsProdctID, lsUserIDxx)) {
            logwrapr.severe(poGRider.getErrMsg() + "; " + poGRider.getMessage());
            System.exit(1);
        }
        
        XMCodeApproval instance = new XMCodeApproval(poGRider);
        
        try {            
            loJSON = (JSONObject) loParser.parse(args[1]);
            
            instance.NewTransaction();
            instance.setMaster("dTransact", SQLUtil.toDate((String)loJSON.get("trandate"), SQLUtil.FORMAT_SHORT_DATE));
            instance.setMaster("sSystemCD", (String) loJSON.get("systemcd"));
            instance.setMaster("dReqstdxx", SQLUtil.toDate((String)loJSON.get("reqstdxx"), SQLUtil.FORMAT_SHORT_DATE));
            instance.setMaster("sReqstdBy", (String) loJSON.get("reqstdby"));
            instance.setMaster("sMiscInfo", (String) loJSON.get("miscinfo"));
            instance.setMaster("sReqstdTo", (String) loJSON.get("reqstdto"));
            instance.setMaster("sRemarks2", (String) loJSON.get("remarks2"));
            instance.setMaster("sEntryByx", (String) loJSON.get("entrybyx"));
        } catch (ParseException ex) {
            logwrapr.severe(ex.getMessage());
            System.exit(1);
        }
        
        loJSON = instance.SaveTransaction();

        //delete the parameter files.
        FileUtil.fileDelete(FILE1_PATH);
        FileUtil.fileDelete(FILE2_PATH);
        
        if (loJSON == null){
            FileUtil.fileWrite(RESULT_DIR, "No response from server...");
            System.out.println("1");
            System.exit(1); //error
        }
                
        //return the result
        if (loJSON.get("result").toString().equalsIgnoreCase("success")){
            FileUtil.fileWrite(RESULT_DIR, (String) loJSON.get("transno"));
            System.out.println("0");
            System.exit(0); //success
        }
        else{
            FileUtil.fileWrite(RESULT_DIR, (String) loJSON.get("message"));
            System.out.println("1");
            System.exit(1); //error
        }
            
    }
}
