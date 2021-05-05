/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.rmj.approvalcode.base;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.iface.GEntity;

/**
 *
 * @author Michael Cuison
 */
@Entity
@Table(name = "System_Code_Approval")
public class UnitSystemCodeApproval implements Serializable, GEntity{
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "sTransNox")
    private String sTransNox;

    @Basic(optional = false)
    @Column(name = "dTransact")
    @Temporal(TemporalType.DATE)
    private Date dTransact;

    @Column(name = "sSystemCD")
    private String sSystemCD;

    @Column(name = "sReqstdBy")
    private String sReqstdBy;
    
    @Basic(optional = false)
    @Column(name = "dReqstdxx")
    @Temporal(TemporalType.DATE)
    private Date dReqstdxx;

    @Column(name = "cIssuedBy")
    private String cIssuedBy;

    @Column(name = "sMiscInfo")
    private String sMiscInfo;

    @Column(name = "sRemarks1")
    private String sRemarks1;

    @Column(name = "sRemarks2")
    private String sRemarks2;

    @Column(name = "sApprCode")
    private String sApprCode;

    @Column(name = "sEntryByx")
    private String sEntryByx;

    @Column(name = "sApprvByx")
    private String sApprvByx;

    @Column(name = "sReasonxx")
    private String sReasonxx;

    @Column(name = "sReqstdTo")
    private String sReqstdTo;

    @Column(name = "cSendxxxx")
    private String cSendxxxx;

    @Column(name = "cTranStat")
    private String cTranStat;

    @Column(name = "sModified")
    private String sModified;

    @Basic(optional = false)
    @Column(name = "dModified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dModified;

    public UnitSystemCodeApproval() {
        this.sTransNox = "";
        this.sSystemCD = "";
        this.sReqstdBy = "";
        this.cIssuedBy = "";
        this.sMiscInfo = "";
        this.sRemarks1 = "";
        this.sRemarks2 = "";
        this.sApprCode = "";
        this.sEntryByx = "";
        this.sApprvByx = "";
        this.sReasonxx = "";
        this.sReqstdTo = "";
        this.cSendxxxx = "0";
        this.cTranStat = TransactionStatus.STATE_OPEN;

        //set vector for fields/col
        laColumns = new LinkedList();
        laColumns.add("sTransNox");
        laColumns.add("dTransact");
        laColumns.add("sSystemCD");
        laColumns.add("sReqstdBy");
        laColumns.add("dReqstdxx");
        laColumns.add("cIssuedBy");
        laColumns.add("sMiscInfo");
        laColumns.add("sRemarks1");
        laColumns.add("sRemarks2");
        laColumns.add("sApprCode");
        laColumns.add("sEntryByx");
        laColumns.add("sApprvByx");
        laColumns.add("sReasonxx");
        laColumns.add("sReqstdTo");
        laColumns.add("cSendxxxx");
        laColumns.add("cTranStat");
        laColumns.add("sModified");
        laColumns.add("dModified");

    }

    public String getTransactionNo() {
        return sTransNox;
    }

    public void setTransactionNo(String sTransNox) {
        this.sTransNox = sTransNox;
    }

    public Date getDateTransact() {
        return dTransact;
    }

    public void setDateTransact(Date dTransact) {
        this.dTransact = dTransact;
    }
   
    public String getSystemCode() {
        return sSystemCD;
    }

    public void setSystemCode(String sSystemCD) {
        this.sSystemCD = sSystemCD;
    }
   
    public String getRequestedBy() {
        return sReqstdBy;
    }

    public void setRequestedBy(String sReqstdBy) {
        this.sReqstdBy = sReqstdBy;
    }
   
    public Date getDateRequested() {
        return dReqstdxx;
    }

    public void setDateRequested(Date dReqstdxx) {
        this.dReqstdxx = dReqstdxx;
    }
    
    public String getIssuedBy() {
        return cIssuedBy;
    }

    public void setIssuedBy(String cIssuedBy) {
        this.cIssuedBy = cIssuedBy;
    }
   
    public String getMiscInfo() {
        return sMiscInfo;
    }

    public void setMiscInfo(String sMiscInfo) {
        this.sMiscInfo = sMiscInfo;
    }
    
    public String getRemarks1() {
        return sRemarks1;
    }

    public void setRemarks1(String sRemarks1) {
        this.sRemarks1 = sRemarks1;
    }
    
    public String getRemarks2() {
        return sRemarks2;
    }

    public void setRemarks2(String sRemarks2) {
        this.sRemarks2 = sRemarks2;
    }
    
    public String getApprovalCode() {
        return sApprCode;
    }

    public void setApprovalCode(String sApprCode) {
        this.sApprCode = sApprCode;
    }
    
    public String getEntryBy() {
        return sEntryByx;
    }

    public void setEntryBy(String sEntryByx) {
        this.sEntryByx = sEntryByx;
    }
    
    public String getApprovedBy() {
        return sApprvByx;
    }

    public void setApprovedBy(String sApprvByx) {
        this.sApprvByx = sApprvByx;
    }
    
    public String getReason() {
        return sReasonxx;
    }

    public void setReason(String sReasonxx) {
        this.sReasonxx = sReasonxx;
    }
    
    public String getRequestedTo() {
        return sReqstdTo;
    }

    public void setRequestedTo(String sReqstdTo) {
        this.sReqstdTo = sReqstdTo;
    }
    
    public String getSendStat() {
        return cSendxxxx;
    }

    public void setSendStat(String cSendxxxx) {
        this.cSendxxxx = cSendxxxx;
    }

    public String getTranStat() {
        return cTranStat;
    }

    public void setTranStat(String cTranStat) {
        this.cTranStat = cTranStat;
    }

    public String getModifiedBy() {
        return sModified;
    }

    public void setModifiedBy(String sModified) {
        this.sModified = sModified;
    }

    public Date getDateModified() {
        return dModified;
    }

    public void setDateModified(Date dModified) {
        this.dModified = dModified;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (sTransNox != null ? sTransNox.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UnitSystemCodeApproval)) {
           return false;
        }
        UnitSystemCodeApproval other = (UnitSystemCodeApproval) object;
        if ((this.sTransNox == null && other.sTransNox != null) || (this.sTransNox != null && !this.sTransNox.equals(other.sTransNox))) {
           return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.rmj.approvalcode.base.UnitSystemCodeApproval[sTransNox=" + sTransNox + "]";
    }

    public Object getValue(String fsColumn){
        int lnCol = getColumn(fsColumn);
        if(lnCol > 0){
           return getValue(lnCol);
        }
        else
          return null;
    }

    public Object getValue(int fnColumn) {
        switch(fnColumn){
            case 1: return this.sTransNox;
            case 2: return this.dTransact;
            case 3: return this.sSystemCD;
            case 4: return this.sReqstdBy;
            case 5: return this.dReqstdxx;
            case 6: return this.cIssuedBy;
            case 7: return this.sMiscInfo;
            case 8: return this.sRemarks1;
            case 9: return this.sRemarks2;
            case 10: return this.sApprCode;
            case 11: return this.sEntryByx;
            case 12: return this.sApprvByx;
            case 13: return this.sReasonxx;
            case 14: return this.sReqstdTo;
            case 15: return this.cSendxxxx;
            case 16: return this.cTranStat;
            case 17: return this.sModified;
            case 18: return this.dModified;
            default: return null;
        }
    }

    public String getTable() {
        return "System_Code_Approval";
    }

    public int getColumn(String fsCol) {
        return laColumns.indexOf(fsCol) + 1;
    }

    public String getColumn(int fnCol) {
        if(laColumns.size() < fnCol)
            return "";
        else
            return (String) laColumns.get(fnCol - 1);
    }

    public void setValue(String fsColumn, Object foValue){
        int lnCol = getColumn(fsColumn);
        if(lnCol > 0){
            setValue(lnCol, foValue);
        }
    }

    public void setValue(int fnColumn, Object foValue) {
        switch(fnColumn){
            case 1: this.sTransNox = (String) foValue; break;
            case 2: this.dTransact = (Date) foValue; break;
            case 3: this.sSystemCD = (String) foValue; break;
            case 4: this.sReqstdBy = (String) foValue; break;
            case 5: this.dReqstdxx = (Date) foValue; break;
            case 6: this.cIssuedBy = (String) foValue; break;
            case 7: this.sMiscInfo = (String) foValue; break;
            case 8: this.sRemarks1 = (String) foValue; break;
            case 9: this.sRemarks2 = (String) foValue; break;
            case 10: this.sApprCode = (String) foValue; break;
            case 11: this.sEntryByx = (String) foValue; break;
            case 12: this.sApprvByx = (String) foValue; break;
            case 13: this.sReasonxx = (String) foValue; break;
            case 14: this.sReqstdTo = (String) foValue; break;
            case 15: this.cSendxxxx = (String) foValue; break;
            case 16: this.cTranStat = (String) foValue; break;
            case 17: this.sModified = (String) foValue; break;
            case 18: this.dModified = (Date) foValue; break;
        }
    }

    public int getColumnCount() {
        return laColumns.size();
    }

    @Override
    public void list(){
        Stream.of(laColumns).forEach(System.out::println);        
    }
   
    //Member Variables here
    LinkedList laColumns = null;
}
