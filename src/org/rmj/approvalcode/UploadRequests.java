package org.rmj.approvalcode;

import org.rmj.appdriver.GRider;
import org.rmj.approvalcode.agent.XMUpload;
import org.rmj.replication.utility.LogWrapper;

public class UploadRequests {
    public static void main(String [] args){
        LogWrapper logwrapr = new LogWrapper("UploadRequests", "D:/GGC_Java_Systems/temp/XMCodeApproval.log");       
        
        String lsProdctID = "IntegSys";
        String lsUserIDxx = "M001111122";

        GRider poGRider = new GRider(lsProdctID);

        if (!poGRider.loadUser(lsProdctID, lsUserIDxx)){
            System.out.println(poGRider.getMessage() + poGRider.getErrMsg());
            System.exit(1);
        }
        
        XMUpload instance = new XMUpload(poGRider);
        
        if (instance.UploadRequests()){
            System.out.println(instance.getMessage());
            System.exit(0);
        } else{
            System.out.println(instance.getMessage());
            System.exit(1); 
        }
    }
}
