package com.example.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

public class FinanceAndBankState implements LinearState
{
    private Party bajajFinance;
    private Party bank;
    private String companyName;
    private int amount;
    private final UniqueIdentifier linearId;


    public FinanceAndBankState(Party bajajFinance, Party bank, String companyName, int amount, UniqueIdentifier linearId)
    {
        this.bajajFinance = bajajFinance;
        this.bank = bank;
        this.companyName = companyName;
        this.amount = amount;
        this.linearId = linearId;
    }

    public Party getBajajFinance()
    {
        return bajajFinance;
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

    @Override
    public UniqueIdentifier getLinearId()
    {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants()
    {
        return ImmutableList.of(bank,bajajFinance);
    }

}