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
public class FinanceAndBankState implements LinearState,Serializable {

    private Party finance;
    private Party bank;
    private String companyName;
    private int amount;
    private boolean loanEligibleFlag;
    private  UniqueIdentifier linearIdBankAndCreditState;
    private final UniqueIdentifier linearId;

    @ConstructorForDeserialization
    public FinanceAndBankState(Party finance, Party bank, String companyName, int amount, UniqueIdentifier linearId, boolean loanEligibleFlag,UniqueIdentifier linearIdBankAndCreditState) {
        this.finance = finance;
        this.bank = bank;
        this.companyName = companyName;
        this.amount = amount;
        this.linearId = linearId;
        this.loanEligibleFlag = loanEligibleFlag;
        this.linearIdBankAndCreditState = linearIdBankAndCreditState;
    }

    public Party getfinance() {
        return finance;
    }

    public Party getBank() {
        return bank;
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

    public boolean isLoanEligibleFlag() {
        return loanEligibleFlag;
    }

    public void setLoanEligibleFlag(boolean loanEligibleFlag) {
        this.loanEligibleFlag = loanEligibleFlag;
    }

    public void setFinance(Party finance) {
        this.finance = finance;
    }

    public UniqueIdentifier getLinearIdBankAndCreditState() {
        return linearIdBankAndCreditState;
    }


    public void setLinearIdBankAndCreditState(UniqueIdentifier linearIdBankAndCreditState) {
        this.linearIdBankAndCreditState = linearIdBankAndCreditState;
    }

    @Override
    public UniqueIdentifier getLinearId()
    {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants()
    {
        return ImmutableList.of(finance,bank);
    }

}