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
    private final UniqueIdentifier linearIdLoanVerState;
    private final UniqueIdentifier linearIdLoanReqState;

    @ConstructorForDeserialization
    public LoanVerificationState(int amount, Party bankNode, Party creditAgencyNode, boolean isEligibleForLoan, String companyName, UniqueIdentifier linearIdLoanVerState, UniqueIdentifier linearIdLoanReqState) {
        this.amount = amount;
        this.bankNode = bankNode;
        this.creditAgencyNode = creditAgencyNode;
        this.isEligibleForLoan = isEligibleForLoan;
        this.companyName = companyName;
        this.linearIdLoanVerState = linearIdLoanVerState;
        this.linearIdLoanReqState = linearIdLoanReqState;
    }

    public LoanVerificationState(Party bankNode, UniqueIdentifier linearIdLoanVerState, UniqueIdentifier linearIdLoanReqState) {
        this.amount = amount;
        this.bankNode = bankNode;
        this.linearIdLoanVerState = linearIdLoanVerState;
        this.linearIdLoanReqState = linearIdLoanReqState;
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

    public UniqueIdentifier getLinearIdLoanVerState() {
        return linearIdLoanVerState;
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

    public UniqueIdentifier getLinearIdLoanReqState() {
        return linearIdLoanReqState;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearIdLoanVerState;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(bankNode, creditAgencyNode);
    }
}
