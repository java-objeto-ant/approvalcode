package org.rmj.approvalcode.sms;

import org.rmj.appdriver.GRider;

/**
 *
 * @author Michael Cuison
 *      2020.12.08 Started creating this object.
 */
public interface iApproval {
    public void setGRider(GRider foApp);
    public void setSender(String fsValue);
    public void setSMS(String fsValue);
    
    public boolean ProcessApproval();
    
    public String getMessage();
    public String getErrorCode();
}
