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
public class LoanDataVerificationState implements LinearState,Serializable {
    private Party bankNode;
    private Party creditRatingAgency;
    private boolean isEligibleForLoanFlag;
    private String companyName;
    private int amount;
    private final UniqueIdentifier linearId;
    private  UniqueIdentifier linearIdLoanReqDataState;

    @ConstructorForDeserialization
    public LoanDataVerificationState(int amount, Party bankNode, Party creditRatingAgency, boolean isEligibleForLoanFlag, String companyName, UniqueIdentifier linearId, UniqueIdentifier linearIdLoanReqDataState) {
        this.amount = amount;
        this.bankNode = bankNode;
        this.creditRatingAgency = creditRatingAgency;
        this.isEligibleForLoanFlag = isEligibleForLoanFlag;
        this.companyName = companyName;
        this.linearId = linearId;
        this.linearIdLoanReqDataState = linearIdLoanReqDataState;
    }

    public LoanDataVerificationState(Party bankNode, UniqueIdentifier linearId, UniqueIdentifier linearIdLoanReqDataState) {
        this.amount = amount;
        this.bankNode = bankNode;
        this.linearId =linearId;
        this.linearIdLoanReqDataState = linearIdLoanReqDataState;
    }


    public Party getBankNode() {
        return bankNode;
    }

    public Party getCreditRatingAgency()
    {
        return creditRatingAgency;
    }

    public boolean getLoanEligibleFlag()
    {
        return isEligibleForLoanFlag;
    }

    public void setEligibleForLoanFlag(boolean eligibleForLoanFlag) {
        this.isEligibleForLoanFlag = eligibleForLoanFlag;
    }

    public boolean isEligibleForLoanFlag() {
        return isEligibleForLoanFlag;
    }

    public void setLinearIdLoanReqDataState(UniqueIdentifier linearIdLoanReqDataState) {
        this.linearIdLoanReqDataState = linearIdLoanReqDataState;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public UniqueIdentifier getLinearIdLoanReqDataState() {
        return linearIdLoanReqDataState;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public UniqueIdentifier getLinearId()
    {
        return linearId;
    }
    @Override
    public List<AbstractParty> getParticipants()
    {
        return ImmutableList.of(bankNode,creditRatingAgency);
    }
}
