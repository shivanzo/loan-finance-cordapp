/**** @author : Shivan Sawant ***/
package com.example.contract;

import com.example.state.LoanRequestDataState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

public class LoanReqDataContract implements Contract {
    public static final String FINANCE_CONTRACT_ID = "com.example.contract.LoanReqDataContract";

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

               if (!(outputState instanceof LoanRequestDataState))
                   throw new IllegalArgumentException("Ouput must be a LoanRequestDataState");

               LoanRequestDataState financAndBankState = (LoanRequestDataState) outputState;

               if ( (financAndBankState.getAmount() < 1000) && (financAndBankState.getAmount() > 100000000))
                   throw new IllegalArgumentException("Loan amount is out of the range : "+financAndBankState.getAmount());

               Party financeAgency = financAndBankState.getFinanceNode();

               PublicKey financeAgencyKey = financeAgency.getOwningKey();
               PublicKey bankKey = financAndBankState.getBankNode().getOwningKey();

               if (!(requiredSigners.contains(financeAgencyKey)))
                   throw new IllegalArgumentException("Finance agency should sign the transaction");

               if (!(requiredSigners.contains(bankKey)))
                throw new IllegalArgumentException("Bank should sign the transaction");
        }

        else if(commandType instanceof Commands.loanNotification) {
            if(tx.getInputStates().size() !=1)
                throw new IllegalArgumentException("Must have atleast one input state");

            if(tx.getOutputStates().size() !=1)
                throw new IllegalArgumentException("Must have one output state");

            ContractState input = tx.getInput(0);
            ContractState output = tx.getOutput(0);

            if(!(input instanceof LoanRequestDataState))
                throw new IllegalArgumentException("Input should be of the type LoanDataVerificationState");

            if(!(output instanceof LoanRequestDataState))
                throw new IllegalArgumentException("output should be of the type LoanDataVerificationState");

            LoanRequestDataState inputState = (LoanRequestDataState) input;
            LoanRequestDataState outputState = (LoanRequestDataState) output;

            PublicKey bankKey = inputState.getBankNode().getOwningKey();
            PublicKey financeAgencyKey = outputState.getFinanceNode().getOwningKey();

            if(!(requiredSigners.contains(financeAgencyKey)))
                throw new IllegalArgumentException("FinanceAgency signature is required");
            if(!(requiredSigners.contains(bankKey)))
                throw new IllegalArgumentException("Bank signature is required");
        }
        else {
            throw new IllegalArgumentException("Command Type not recognised");
        }
    }

    public interface Commands extends CommandData {
       public class InitiateLoan implements Commands {}
       public class loanNotification implements Commands {}
    }
}