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
public class FinanceAndBankState implements LinearState {

    private Party finance;
    private Party bank;
    private String companyName;
    private int amount;
    private boolean loanEligibleFlag;
    private final UniqueIdentifier linearId;


    public FinanceAndBankState(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    @ConstructorForDeserialization
    public FinanceAndBankState(Party finance, Party bank, String companyName, int amount, UniqueIdentifier linearId) {
        this.finance = finance;
        this.bank = bank;
        this.companyName = companyName;
        this.amount = amount;
        this.linearId = linearId;
    }

    public Party getfinance()
    {
        return finance;
    }

    public Party getBank()
    {
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


    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public boolean isLoanEligibleFlag() {
        return loanEligibleFlag;
    }

    public void setLoanEligibleFlag(boolean loanEligibleFlag) {
        this.loanEligibleFlag = loanEligibleFlag;
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