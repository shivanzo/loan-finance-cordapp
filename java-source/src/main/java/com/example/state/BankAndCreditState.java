package com.example.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

public class BankAndCreditState implements LinearState
{
    private Party stateBank;
    private Party creditRatingAgency;
    private boolean loanEligibleFlag;
    private String companyName;
    private int amount;
    private final UniqueIdentifier linearId;

    public BankAndCreditState(Party stateBank, Party creditRatingAgency, Boolean loanEligibleFlag, boolean loanEligibleFlag1, String companyName, int amount, UniqueIdentifier linearId)
    {
        this.stateBank = stateBank;
        this.creditRatingAgency = creditRatingAgency;
        this.loanEligibleFlag = loanEligibleFlag1;
        loanEligibleFlag = loanEligibleFlag;
        this.companyName = companyName;
        this.amount = amount;
        this.linearId = linearId;
    }

    public Party getStateBank()
    {
        return stateBank;
    }

    public Party getCreditRatingAgency()
    {
        return creditRatingAgency;
    }

    public boolean getLoanEligibleFlag()
    {
        return loanEligibleFlag;
    }

    public void setLoanEligibleFlag(boolean loanEligibleFlag) {
        this.loanEligibleFlag = loanEligibleFlag;
    }

    public String getCompanyName()
    {
        return companyName;
    }

    public int getAmount()
    {
        return amount;
    }

    @Override
    public UniqueIdentifier getLinearId()
    {
        return linearId;
    }
    @Override
    public List<AbstractParty> getParticipants()
    {
        return ImmutableList.of(stateBank,creditRatingAgency);
    }
}
