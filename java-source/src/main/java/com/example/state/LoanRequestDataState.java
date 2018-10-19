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
public class LoanRequestDataState implements LinearState,Serializable {

    private Party financeNode;
    private Party bankNode;
    private String companyName;
    private int amount;
    private boolean isEligibleForLoanFlag;
    private UniqueIdentifier linearIdDataVerState;
    private UniqueIdentifier linearId;

    @ConstructorForDeserialization
    public LoanRequestDataState(Party financeNode, Party bankNode, String companyName, int amount, UniqueIdentifier linearId, boolean isEligibleForLoanFlag, UniqueIdentifier linearIdDataVerState) {
        this.financeNode = financeNode;
        this.bankNode = bankNode;
        this.companyName = companyName;
        this.amount = amount;
        this.linearId = linearId;
        this.isEligibleForLoanFlag = isEligibleForLoanFlag;
        this.linearIdDataVerState = linearIdDataVerState;
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

    public boolean isEligibleForLoanFlag() {
        return isEligibleForLoanFlag;
    }

    public void setEligibleForLoanFlag(boolean isEligibleForLoanFlag) {
        this.isEligibleForLoanFlag = isEligibleForLoanFlag;
    }

    public void setFinanceNode(Party financeNode) {
        this.financeNode = financeNode;
    }

    public UniqueIdentifier getLinearIdDataVerState() {
        return linearIdDataVerState;
    }

    public void setLinearIdDataVerState(UniqueIdentifier linearIdDataVerState) {
        this.linearIdDataVerState = linearIdDataVerState;
    }


    @Override
    public UniqueIdentifier getLinearId()
    {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants()
    {
        return ImmutableList.of(financeNode, bankNode);
    }

}