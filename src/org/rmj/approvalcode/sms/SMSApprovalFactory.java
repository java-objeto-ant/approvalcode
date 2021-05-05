package org.rmj.approvalcode.sms;

import org.rmj.appdriver.GRider;

/**
 *
 * @author Michael Cuison
 *      2020.12.08  Started creating this object.
 */
public class SMSApprovalFactory {
    private static final String CODEAPPR = "CODEAPPR";
    private static final String APP_RQST = "APP_RQST";
    
    public static iApproval make(GRider foApp, String fsMessagex, String fsSenderNo){
        switch(fsMessagex.substring(0, 8)){
            case CODEAPPR:
                return null;
            case APP_RQST:
                APP_RQST instance = new APP_RQST();
                instance.setGRider(foApp);
                instance.setSMS(fsMessagex);
                instance.setSender(fsSenderNo);
                
                return instance;
            default:
                return null;
        }
    }
}