package org.rmj.approvalcode;

import org.rmj.appdriver.agent.GRiderX;
import org.rmj.appdriver.agentfx.FileUtil;
import org.rmj.approvalcode.agent.XMCodeApproval;
import org.rmj.replication.utility.LogWrapper;

public class ValidateApproval {           
    public static void main(String[] args) {       
        LogWrapper logwrapr = new LogWrapper("UploadRequests", "D:/GGC_Java_Systems/temp/XMCodeApproval.log");
        String RESULT_DIR = "D:/GGC_Java_Systems/temp/res.TMP";
        
        if (args.length != 2){
            logwrapr.severe("Invalid parameters detected...");
            FileUtil.fileWrite(RESULT_DIR, "Invalid parameters detected...");
            System.exit(1);
        }
        
        GRiderX poGRider = new GRiderX("IntegSys");
        
        if(!poGRider.getErrMsg().isEmpty()){
            logwrapr.severe(poGRider.getErrMsg());
            logwrapr.severe("GRiderX has error...");
            System.exit(1);
        }
        
        poGRider.setOnline(false);
        
        XMCodeApproval instance = new XMCodeApproval(poGRider);

        //args[0], args[1]
        if (instance.ValidateOffline(args[0], args[1]) == true){
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