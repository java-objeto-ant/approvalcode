package org.rmj.approvalcode.tokenize;

import org.rmj.appdriver.GRider;
import org.rmj.replication.utility.LogWrapper;

public class SendNotification {
    public static void main(String [] args){
        LogWrapper logwrapr = new LogWrapper("SMS.Notification", "sms-token.log");       

        //Set important path configuration for this utility
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/mac/GGC_Java_Systems";
        }
        
        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);
        
        String lsProdctID = "IntegSys";
        String lsUserIDxx = "M001111122";
        
        GRider poGRider = new GRider(lsProdctID);

        if (!poGRider.loadUser(lsProdctID, lsUserIDxx)){
            System.out.println(poGRider.getMessage() + poGRider.getErrMsg());
            logwrapr.severe(poGRider.getMessage() + poGRider.getErrMsg());
            System.exit(1);
        }
        
        logwrapr.info("Start of Process.");
        
        iNotification instance = NotificationFactory.make(poGRider, "PO", "EP");        
        
        //assign the transaction number if passed
        if (args.length != 0) instance.setTransNox(args[0]);
        
        if (!instance.SendNotification()){
            logwrapr.severe(instance.getMessage());
            System.exit(1);
        }
        
        logwrapr.info(instance.getMessage());
        logwrapr.info("Thank you.");
        System.exit(0);
    }
}