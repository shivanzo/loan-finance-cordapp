package com.example.bean;

import net.corda.core.identity.CordaX500Name;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DataBean {

    private String company;
    private int value;
    private CordaX500Name partyName;
    private String loanReqLinearId;
    private String loanVerLinearId;

    public void setCompany(String comapany) {
        this.company = comapany;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setPartyName(CordaX500Name partyName) {
        this.partyName = partyName;
    }

    public String getCompany() {
        return company;
    }

    public int getValue() {
        return value;
    }

    public CordaX500Name getPartyName() {
        return partyName;
    }

    public String getLoanReqLinearId() {
        return loanReqLinearId;
    }

    public String getLoanVerLinearId() {
        return loanVerLinearId;
    }

    @Override
    public String toString() {
        return "DetailBean [company=" + company + "value=" + value + "partyName=" + partyName + "loanReqLinearId=" + loanReqLinearId + "loanVerLinearId=" + loanVerLinearId + "]";
    }
}
