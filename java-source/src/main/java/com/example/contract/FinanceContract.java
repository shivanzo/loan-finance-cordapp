/**** @author : Shivan Sawant ***/
package com.example.contract;

import com.example.state.BankAndCreditState;
import com.example.state.FinanceAndBankState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

public class FinanceContract implements Contract {
    public static final String FINANCE_CONTRACT_ID = "com.example.contract.FinanceContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        if(tx != null && tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if(commandType instanceof Commands.InitiateLoan) {
               if (tx.getInputStates().size() != 0)
                   throw new IllegalArgumentException("InitiateLoan should not have inputs : "+tx.getInputStates().size() +"getInput : "+tx.getInputStates().toString());
               if (tx.getOutputStates().size() != 1)
                   throw new IllegalArgumentException("InitiateLoan should have one output : "+tx.getOutputStates().size());

               ContractState outputState = tx.getOutput(0);
               if (!(outputState instanceof FinanceAndBankState))
                   throw new IllegalArgumentException("Ouput must be a FinanceAndBankState");

               FinanceAndBankState financAndBankState = (FinanceAndBankState) outputState;
               if ( (financAndBankState.getAmount() < 1000) && (financAndBankState.getAmount() > 100000000))
                   throw new IllegalArgumentException("Why : Loan amount is out of the range : "+financAndBankState.getAmount());

               if (financAndBankState.getCompanyName().equalsIgnoreCase("PNB Bank"))
                   throw new IllegalArgumentException("Loan is not provided to the defaulters");

               Party financeAgency = financAndBankState.getfinance();
               PublicKey financeAgencyKey = financeAgency.getOwningKey();
               PublicKey bankKey = financAndBankState.getBank().getOwningKey();

               if (!(requiredSigners.contains(financeAgencyKey)))
                   throw new IllegalArgumentException("Finance agency should sign the transaction");
            if (!(requiredSigners.contains(bankKey)))
                throw new IllegalArgumentException("Finance agency should sign the transaction");
        }
        else if(commandType instanceof Commands.SendForApproval) {
               if(tx.getInputStates().size() !=0)
                   throw new IllegalArgumentException("Must have atleast Zero input state");

               if(tx.getOutputStates().size() !=1)
                   throw new IllegalArgumentException("Must have one output state");

               ContractState output = tx.getOutput(0);

               if(!(output instanceof BankAndCreditState))
                   throw new IllegalArgumentException("Output must of BankAndCredit State");

               List<String> blacklisted = Arrays.asList("jetsAirways","Kong airways","Hypermarket");
               boolean contains = blacklisted.contains(FinanceAndBankState.class);

               if(contains) {
                   throw new IllegalArgumentException("Loan is not provided to defaulters");
               }

               BankAndCreditState outputState = (BankAndCreditState)output;
               PublicKey BankKey = outputState.getbank().getOwningKey();
               PublicKey creditAgencyKey = outputState.getCreditRatingAgency().getOwningKey();

               if(!(requiredSigners.contains(BankKey)))
                   throw new IllegalArgumentException("Bank should sign...!! Bank signature is mandatory");

               if(!(requiredSigners.contains(creditAgencyKey)))
                   throw new IllegalArgumentException("Credit Agency should sign ...!! Credit agency signature is mandatory");

        }
        else if (commandType instanceof Commands.receiveCreditApproval) {
            if(tx.getInputStates().size() !=1)
                throw new IllegalArgumentException("Must have atleast one input state");

            if(tx.getOutputStates().size() !=1)
                throw new IllegalArgumentException("Must have one output state");

            ContractState input = tx.getInput(0);
            ContractState output = tx.getOutput(0);

            if(!(input instanceof BankAndCreditState))
                throw new IllegalArgumentException("input should be of BankAndCreditState");

            if(!(output instanceof BankAndCreditState))
                throw new IllegalArgumentException("Output must of BankAndCredit State");

            List<String> blacklisted = Arrays.asList("jetsAirways","Kong airways","Hypermarket");
            boolean contains = blacklisted.contains(FinanceAndBankState.class);

            if(contains) {
                throw new IllegalArgumentException("Loan is not provided to defaulters");
            }

            BankAndCreditState inputState = (BankAndCreditState) input;
            BankAndCreditState outputState = (BankAndCreditState) output;
            PublicKey creditAgencyKey = inputState.getCreditRatingAgency().getOwningKey();
            PublicKey bankKey = outputState.getbank().getOwningKey();

            if(!(requiredSigners.contains(creditAgencyKey)))
                throw new IllegalArgumentException("CreditAgency signature is required");
            if(!(requiredSigners.contains(bankKey)))
                throw new IllegalArgumentException("Bank signature is required");
        }
        else if(commandType instanceof Commands.loanNotification) {
            if(tx.getInputStates().size() !=1)
                throw new IllegalArgumentException("Must have atleast one input state");

            if(tx.getOutputStates().size() !=1)
                throw new IllegalArgumentException("Must have one output state");

            ContractState input = tx.getInput(0);
            ContractState output = tx.getOutput(0);

            if(!(input instanceof FinanceAndBankState))
                throw new IllegalArgumentException("input should be of the type BankAndCreditState");

            if(!(output instanceof FinanceAndBankState))
                throw new IllegalArgumentException("output should be of the type BankAndCreditState");

            FinanceAndBankState inputState = (FinanceAndBankState) input;
            FinanceAndBankState outputState = (FinanceAndBankState) output;

            List<String> blacklisted = Arrays.asList("jetsAirways","Kong airways","Hypermarket");
            boolean contains = blacklisted.contains(FinanceAndBankState.class);

            if(contains) {
                throw new IllegalArgumentException("Loan is not provided to defaulters");
            }

            PublicKey bankKey = inputState.getBank().getOwningKey();
            PublicKey financeAgencyKey = outputState.getfinance().getOwningKey();

            if(!(requiredSigners.contains(financeAgencyKey)))
                throw new IllegalArgumentException("financeAgency signature is required");
            if(!(requiredSigners.contains(bankKey)))
                throw new IllegalArgumentException("Bank signature is required");
        }
        else {
            throw new IllegalArgumentException("Command Type not recognised");
        }
    }

    public interface Commands extends CommandData {
       public class InitiateLoan implements Commands {}
       public class SendForApproval implements Commands {}
       public class receiveCreditApproval implements Commands {}
       public class loanNotification implements Commands {}
    }
}