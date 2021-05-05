package org.rmj.approvalcode;

import org.rmj.appdriver.agent.GRiderX;
import org.rmj.appdriver.agentfx.FileUtil;
import org.rmj.approvalcode.agent.XMCodeApproval;
import org.rmj.replication.utility.LogWrapper;

public class ValidateApprovalNew {           
    public static void main(String[] args) {             
        LogWrapper logwrapr = new LogWrapper("ValidateApprovalNew", "D:/GGC_Java_Systems/temp/XMCodeApproval.log");
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
        
        GRiderX poGRider = new GRiderX("IntegSys");
        
        if(!poGRider.getErrMsg().isEmpty()){
            logwrapr.severe(poGRider.getErrMsg());
            logwrapr.severe("GRiderX has error...");
            System.exit(1);
        }
        
        poGRider.setOnline(false);
        
        XMCodeApproval instance = new XMCodeApproval(poGRider);
        
        if (instance.ValidateJSON(args[1], args[0]) == true){ //validate regular expresion
            //successfull
            FileUtil.fileWrite(RESULT_DIR, instance.getMessage());
            System.exit(0);
        } else if (instance.ValidateJSON_AllCaps(args[1], args[0]) == true){ //validate all caps 
            //successfull
            FileUtil.fileWrite(RESULT_DIR, instance.getMessage());
            System.exit(0);
        } else if (instance.ValidateJSON_AllSmall(args[1], args[0]) == true){ //validate all small caps
            //successfull
            FileUtil.fileWrite(RESULT_DIR, instance.getMessage());
            System.exit(0);
        } else{
            //failed
            FileUtil.fileWrite(RESULT_DIR, instance.getMessage());
            System.exit(1);
        }
    }
}