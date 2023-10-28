package org.rmj.approvalcode.tokenize;

import org.rmj.appdriver.GRider;

public interface iNotification {
    public void setGRider(GRider foApp);
    public void setSourceCode(String fsSourceCd);
    public void setRequestType(String fsRqstType);
    public void setTransNox(String fsTransNox);
    
    public boolean SendNotification();
    
    public String getMessage();
}
