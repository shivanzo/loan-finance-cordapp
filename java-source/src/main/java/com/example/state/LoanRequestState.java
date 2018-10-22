package com.example.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import java.io.Serializable;
import java.util.List;

@CordaSerializable
public class LoanRequestState implements LinearState,Serializable {

    private Party financeNode;
    private Party bankNode;
    private String companyName;
    private int amount;
    private boolean isEligibleForLoan;
    private final UniqueIdentifier linearIdVerificationState;
    private final UniqueIdentifier linearIdLoanReqState;

    @ConstructorForDeserialization
    public LoanRequestState(Party financeNode, Party bankNode, String companyName, int amount, UniqueIdentifier linearIdLoanReqState, boolean isEligibleForLoan, UniqueIdentifier linearIdVerificationState) {
        this.financeNode = financeNode;
        this.bankNode = bankNode;
        this.companyName = companyName;
        this.amount = amount;
        this.linearIdLoanReqState = linearIdLoanReqState;
        this.isEligibleForLoan = isEligibleForLoan;
        this.linearIdVerificationState = linearIdVerificationState;
    }

    public Party getFinanceNode() {
        return financeNode;
    }

    public Party getBankNode() {
        return bankNode;
    }

    public String getCompanyName()
    {
        return companyName;
    }

    public int getAmount()
    {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public boolean isEligibleForLoan() {
        return isEligibleForLoan;
    }

    public void setEligibleForLoan(boolean isEligibleForLoanFlag) {
        this.isEligibleForLoan = isEligibleForLoanFlag;
    }

    public void setFinanceNode(Party financeNode) {
        this.financeNode = financeNode;
    }

    public UniqueIdentifier getLinearIdVerificationState() {
        return linearIdVerificationState;
    }


    @Override
    public UniqueIdentifier getLinearId()
    {
        return linearIdLoanReqState;
    }

    @Override
    public List<AbstractParty> getParticipants()
    {
        return ImmutableList.of(financeNode, bankNode);
    }

}