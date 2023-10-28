package org.rmj.approvalcode.tokenize;

import org.rmj.appdriver.GRider;

public class NotificationFactory {          
    public static iNotification make(GRider foGRider, String fsSourceCd, String fsRqstType){
        switch(fsSourceCd){
            case "PO": //purchase order
                switch (fsRqstType){
                    case "EP": //engineering purchasing
                        PO_EP instance = new PO_EP();
                        instance.setGRider(foGRider);
                        instance.setSourceCode(fsSourceCd);
                        instance.setRequestType(fsRqstType);
                        return instance;
                    default: return null;
                }
            default: return null;    
        }
    }
}
