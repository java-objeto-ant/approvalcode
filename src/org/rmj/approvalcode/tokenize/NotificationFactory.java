package org.rmj.approvalcode.tokenize;

import org.rmj.appdriver.GRider;

public class NotificationFactory {          
    public static iNotification make(GRider foGRider, String fsSourceCd, String fsRqstType){
        iNotification object;
        
        switch(fsSourceCd){
            case "PO": //purchase order
                switch (fsRqstType){
                    case "EP": //engineering purchasing
                        object = new PO_EP();
                        object.setGRider(foGRider);
                        object.setSourceCode(fsSourceCd);
                        object.setRequestType(fsRqstType);
                        return object;
                    case "LP":
                        object = new PO_LP();
                        object.setGRider(foGRider);
                        object.setSourceCode(fsSourceCd);
                        object.setRequestType(fsRqstType);
                        return object;
                    default: return null;
                }
            default: return null;    
        }
    }
}
