package com.example.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import java.util.List;

@CordaSerializable
public class LoanVerificationState implements LinearState {
    private Party bankNode;
    private Party creditAgencyNode;
    private boolean isEligibleForLoan;
    private String companyName;
    private int amount;
    private final UniqueIdentifier linearIdLoanVer;
    private final UniqueIdentifier linearIdLoanReq;

    @ConstructorForDeserialization
    public LoanVerificationState(int amount, Party bankNode, Party creditAgencyNode, boolean isEligibleForLoan, String companyName, UniqueIdentifier linearIdLoanVer, UniqueIdentifier linearIdLoanReq) {
        this.amount = amount;
        this.bankNode = bankNode;
        this.creditAgencyNode = creditAgencyNode;
        this.isEligibleForLoan = isEligibleForLoan;
        this.companyName = companyName;
        this.linearIdLoanVer = linearIdLoanVer;
        this.linearIdLoanReq = linearIdLoanReq;
    }

    public LoanVerificationState(Party bankNode, UniqueIdentifier linearIdLoanVer, UniqueIdentifier linearIdLoanReq) {
        this.amount = amount;
        this.bankNode = bankNode;
        this.linearIdLoanVer = linearIdLoanVer;
        this.linearIdLoanReq = linearIdLoanReq;
    }


    public Party getBankNode() {
        return bankNode;
    }

    public Party getCreditAgencyNode() {
        return creditAgencyNode;
    }

    public boolean getLoanEligibleFlag() {
        return isEligibleForLoan;
    }

    public void setEligibleForLoan(boolean eligibleForLoan) {
        this.isEligibleForLoan = eligibleForLoan;
    }

    public UniqueIdentifier getLinearIdLoanVer() {
        return linearIdLoanVer;
    }

    public boolean isEligibleForLoan() {
        return isEligibleForLoan;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public UniqueIdentifier getLinearIdLoanReq() {
        return linearIdLoanReq;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearIdLoanVer;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(bankNode, creditAgencyNode);
    }
}
