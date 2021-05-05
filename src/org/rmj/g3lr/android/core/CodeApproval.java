package org.rmj.g3lr.android.core;

import java.util.Date;
import org.rmj.appdriver.StringHelperMisc;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.StringHelper;

public class CodeApproval {
    private CodeGenConst p_oSCA_Const = new CodeGenConst();
    private xObject poRaw = new xObject();
    private xObject poResult = new xObject();

    public String Result(){
        if (!"".equals(poRaw.Branch)){
            return poResult.Misc + poResult.IssuedBy + poResult.Branch + poResult.System + poResult.Date;
        }else{
            return poResult.Misc + poResult.IssuedBy + poResult.System + poResult.Date;
        }
    }

    public void Branch(String fsBranchCd){
        poRaw.Branch = fsBranchCd;
    }

    public void DateRequested(String fdRequestxx){
        poRaw.Date = fdRequestxx;
    }

    public void IssuedBy(String fcIssuedBy){
        poRaw.IssuedBy = fcIssuedBy;
    }

    public void System(String fsSystem){
        poRaw.System = fsSystem;
    }

    public void MiscInfo(String fsValue){
        poRaw.Misc = fsValue;
    }

    public boolean Encode(){
    	if ("".equals(poRaw.System)){return false;}
    	if ("".equals(poRaw.Date)){return false;}
    	if ("".equals(poRaw.IssuedBy)){return false;}
    	
        if (poRaw.System.equals(p_oSCA_Const.pxeManualLog)){
            if (!StringHelperMisc.isNumeric(poRaw.Misc)){
                return false;
            }

            poResult.Misc = String.valueOf(StringHelperMisc.Rand(0, 9)) +
                            StringHelper.prepad(StringHelperMisc.Hex(Integer.parseInt(poRaw.Misc)), 2, '0');
            
        } else if (poRaw.System.equals(p_oSCA_Const.pxeOfficeRebate) || 
                    poRaw.System.equals(p_oSCA_Const.pxeFieldRebate) ||
                    poRaw.System.equals(p_oSCA_Const.pxeMCDiscount) || 
                    poRaw.System.equals(p_oSCA_Const.pxePartsDiscount) ||
                    poRaw.System.equals(p_oSCA_Const.pxeSPPurcDelivery) ||
                    poRaw.System.equals(p_oSCA_Const.pxeIssueORNotPR) ||
                    poRaw.System.equals(p_oSCA_Const.pxeIssueORNotSI) ||
                    poRaw.System.equals(p_oSCA_Const.pxeMCIssuance) ||
                    poRaw.System.equals(p_oSCA_Const.pxeMPDiscount) ||
                    poRaw.System.equals(p_oSCA_Const.pxeMCTransfer)){

            if (!StringHelperMisc.isNumeric(poRaw.Misc)){
                return false;
            }

            poResult.Misc = StringHelper.prepad(StringHelperMisc.Hex(StringHelperMisc.TotalStr(poRaw.Misc)), 3, '0');

        } else if (poRaw.System.equals(p_oSCA_Const.pxeDay2Day)){

            poResult.Misc = (char)StringHelperMisc.Rand(65, 90) + 
                            StringHelper.prepad(StringHelperMisc.Hex(Integer.parseInt(poRaw.Misc) + 70), 2, '0');

        } else if (poRaw.System.equals(p_oSCA_Const.pxeForgot2Log) || 
                    poRaw.System.equals(p_oSCA_Const.pxeBusinessTrip) ||
                    poRaw.System.equals(p_oSCA_Const.pxeBusinessTripLog) || 
                    poRaw.System.equals(p_oSCA_Const.pxeLeave) || 
                    poRaw.System.equals(p_oSCA_Const.pxeOvertime) ||
                    poRaw.System.equals(p_oSCA_Const.pxeShift) || 
                    poRaw.System.equals(p_oSCA_Const.pxeDayOff) ||
                    poRaw.System.equals(p_oSCA_Const.pxeTardiness) ||
                    poRaw.System.equals(p_oSCA_Const.pxeUndertime) ||
                    poRaw.System.equals(p_oSCA_Const.pxeCreditInv) ||
                    poRaw.System.equals(p_oSCA_Const.pxeCreditApp) ||
                    poRaw.System.equals(p_oSCA_Const.pxeCashBalance) ||
                    poRaw.System.equals(p_oSCA_Const.pxeMCClusteringDelivery) ||
                    poRaw.System.equals(p_oSCA_Const.pxeFSEPActivation) ||
                    poRaw.System.equals(p_oSCA_Const.pxeFSEXActivation) ||
                    poRaw.System.equals(p_oSCA_Const.pxeGCardRedemption) ||
                    poRaw.System.equals(p_oSCA_Const.pxeHotModel)) {
            
            //if length is greater than 30, trim it ang limit to 30 chars
            if (poRaw.Misc.length() >= 30)
                poResult.Misc = StringHelper.prepad(StringHelperMisc.Hex(StringHelperMisc.TotalStr(poRaw.Misc.toLowerCase().substring(0, 29))), 3, '0');
            else
                poResult.Misc = StringHelper.prepad(StringHelperMisc.Hex(StringHelperMisc.TotalStr(poRaw.Misc.toLowerCase())), 3, '0');
        } else if (poRaw.System.equals(p_oSCA_Const.pxeAdditional) ||
                    poRaw.System.equals(p_oSCA_Const.pxeByahengFiesta) ||
                    poRaw.System.equals(p_oSCA_Const.pxeTeleMarketing) ||
                    poRaw.System.equals(p_oSCA_Const.pxePreApproved)) {
            poResult.Misc = StringHelper.prepad(StringHelperMisc.Hex(StringHelperMisc.TotalStr(StringHelper.prepad(poRaw.Misc.toLowerCase(), 30))), 3, '0');
        } else {
                return false;
        }

        //Generate result date
        String date[] = StringHelper.split(poRaw.Date, "-"); //yyyy-MM-dd

        if (!"".equals(poRaw.Branch)){
            poResult.Branch = StringHelper.prepad(StringHelperMisc.Hex(StringHelperMisc.TotalStr(poRaw.Branch.substring(1))), 2);
            poResult.Date = StringHelper.prepad(StringHelperMisc.Hex(Integer.parseInt(date[1]) +
                                                                        Integer.parseInt(date[2]) +
                                                                        Integer.parseInt(date[0].substring(2))), 2) ;
        } else {
            poResult.Date = StringHelperMisc.Hex(Integer.parseInt(StringHelper.prepad(date[2], 2) + StringHelper.prepad(date[1], 2) + date[0].substring(2)));
        }

        poResult.System = StringHelper.prepad(StringHelperMisc.Hex(StringHelperMisc.TotalStr(poRaw.System)), 2);
        poResult.IssuedBy = poRaw.IssuedBy;
        return true;
    }
    
    
    /**
     * Checks if approval code is valid.
     * 
     * @param fsCode1 New issued approval
     * @param fsCode2 Old issued approval
     * @return 0 if matched; -100 if not
     */
    public int isEqual(String fsCode1, String fsCode2) {
        if (fsCode1.length() != 10){return -100;}
        if (fsCode2.length() != 10){return -100;}

        fsCode1 = fsCode1.toUpperCase();
        fsCode2 = fsCode2.toUpperCase();

        //Requesting branch is different from the given code                      
        if (!fsCode1.substring(4, 6).equals(fsCode2.substring(4, 6))){return -100;}

        //System approval request is different from the given code            
        if (!fsCode1.substring(6, 8).equals(fsCode2.substring(6, 8))){return -100;}

        //Date requested is different from the given code
        if (!fsCode1.substring(8, 10).equals(fsCode2.substring(8, 10))){return -100;}

        //Issuing Department/Person is different from the given code            
        if (fsCode1.charAt(3) != fsCode2.charAt(3)){return -100;}
        
        if (fsCode1.substring(6, 8).equals(StringHelper.postpad(StringHelperMisc.Hex(TotalStr(p_oSCA_Const.pxeDay2Day)), 2, '0'))){
            return Integer.parseInt(fsCode2.substring(1, 3)) - Integer.parseInt(fsCode1.substring(1, 3));
        } else if (fsCode1.substring(6, 8).equals(StringHelper.postpad(StringHelperMisc.Hex(TotalStr(p_oSCA_Const.pxeManualLog)), 2, '0'))){
            if (!fsCode2.substring(1, 3).equals(fsCode1.substring(1, 3))) return -100;
        } else{
            if (!fsCode2.substring(0, 3).equals(fsCode1.substring(0, 3))) return -100;
        }
        return 0;
    }
    
    /**
     * Checks if approval code is valid depending on the given period.
     * 
     * @param fsCode1 New issued approval
     * @param fsCode2 Old issued approval
     * @return 0 if matched; -100 if not
     */
    public int isEqualx(String fsCode1, String fsCode2){
        if (fsCode1.length() != fsCode1.length()){return -100;}
        
        fsCode1 = fsCode1.toUpperCase();
        fsCode2 = fsCode2.toUpperCase();
        
        //Issuing Department/Person is different from the given code
        if (fsCode1.charAt(3) != fsCode2.charAt(3)) return -100;
        
        //System approval request is different from the given code
        if (!fsCode1.substring(4, 6).equals(fsCode2.substring(4, 6))) return -100;
        
        //Misc Information/Name
        if (!fsCode1.substring(0, 3).equals(fsCode2.substring(0, 3))) return -100;
        
        String lsDatex;
        Date ldDate1;
        Date ldDate2;
        
        lsDatex = StringHelper.prepad(String.valueOf(StringHelperMisc.Hex2Dec(fsCode2.substring(6))), 6, '0');
        lsDatex = lsDatex.substring(2, 4) + "/" + lsDatex.substring(0, 2) + "/" + lsDatex.substring(4, 6);
        ldDate2 = StringHelperMisc.toDate(lsDatex, "MM/dd/yyyy");
        
        lsDatex = StringHelper.prepad(String.valueOf(StringHelperMisc.Hex2Dec(fsCode1.substring(6))), 6, '0');
        lsDatex = lsDatex.substring(2, 4) + "/" + lsDatex.substring(0, 2) + "/" + lsDatex.substring(4, 6);
        ldDate1 = StringHelperMisc.toDate(lsDatex, "MM/dd/yyyy");
        
        if (fsCode1.substring(4, 6).equalsIgnoreCase(StringHelper.postpad(StringHelperMisc.Hex(TotalStr(p_oSCA_Const.pxeTeleMarketing)), 2, '0')) ||
                fsCode1.substring(4, 6).equalsIgnoreCase(StringHelper.postpad(StringHelperMisc.Hex(TotalStr(p_oSCA_Const.pxePreApproved)), 2, '0'))){
            
            if (CommonUtils.dateDiff(ldDate1, ldDate2) >= 0 && CommonUtils.dateDiff(ldDate1, ldDate2) <= 60){
                return 0;
            }
        } else if(fsCode1.substring(4, 6).equalsIgnoreCase(StringHelper.postpad(StringHelperMisc.Hex(TotalStr(p_oSCA_Const.pxeByahengFiesta)), 2, '0'))){
            if (CommonUtils.dateDiff(ldDate1, ldDate2) >= 0 && CommonUtils.dateDiff(ldDate1, ldDate2) <= 3){
                return 0;
            }
        } else if (fsCode1.substring(4, 6).equalsIgnoreCase(StringHelper.postpad(StringHelperMisc.Hex(TotalStr(p_oSCA_Const.pxeAdditional)), 2, '0'))){
            if (CommonUtils.dateDiff(ldDate1, ldDate2) >= 0 && CommonUtils.dateDiff(ldDate1, ldDate2) <= 3){
                return 0;
            }
        } else return -100;
        
        return -100;
    }   
    
    private int TotalStr(String fsStr){       
        fsStr = fsStr.replace(" ", "");
        fsStr = fsStr.replace(",", "");

        int lnTotal = 0;
        
        for (int x = 0; x <= fsStr.length()-1; x++){
            lnTotal = lnTotal + StringHelperMisc.CharToASCII(fsStr.charAt(x));
        }
        return lnTotal;
    }
}

class CodeGenConst{
    public String pxeDay2Day = "DT";
    public String pxeManualLog = "ML";
    public String pxeForgot2Log = "FL";
    public String pxeBusinessTrip = "OB";
    public String pxeBusinessTripLog = "OL";
    public String pxeLeave = "LV";
    public String pxeOvertime = "OT";
    public String pxeShift = "SH";
    public String pxeDayOff = "DO";
    public String pxeTardiness = "TD";
    public String pxeUndertime = "UD";
    public String pxeCreditInv = "CI";
    public String pxeCreditApp = "CA";
    public String pxeWholeSaleDisc = "WD";
    public String pxeCashBalance = "CB";
    public String pxeOfficeRebate = "R1";
    public String pxeFieldRebate = "R2";
    public String pxePartsDiscount = "SI";
    public String pxeMCDiscount = "DR";
    public String pxeSPPurcDelivery = "PD";
    public String pxeIssueORNotPR = "OR";
    public String pxeIssueORNotSI = "OX";
    public String pxeAdditional = "RS";
    public String pxeByahengFiesta = "BF";
    public String pxeTeleMarketing = "TM";
    public String pxeMCIssuance = "MI";
    public String pxeMCClusteringDelivery = "CD";
    public String pxeFSEPActivation = "FA";
    public String pxeFSEXActivation = "FX";
    public String pxeMPDiscount = "MD";
    public String pxePreApproved = "PA";
    public String pxeJobOrderWOGCard = "JG";
    public String pxeMCDownPayment = "DP";
    public String pxeMCTransfer = "MT";
    public String pxeGCardRedemption = "GR";
    public String pxeHotModel = "HM"; //2019-08-23
}

class xObject{
    String System;
    String Branch;
    String IssuedBy;
    String Date;
    String Misc;

    public void System(String system){
            this.System = system;
    }

    public void Branch(String branch){
            this.Branch = branch;
    }

    public void IssuedBy(String issuedby){
            this.IssuedBy = issuedby;
    }

    public void Misc(String misc){
            this.Misc = misc;
    }

    public String System(){
            return this.System;
    }

    public String Branch(){
            return this.Branch;
    }

    public String IssuedBy(){
            return this.IssuedBy;
    }

    public String Misc(){
            return this.Misc;
    }
}
