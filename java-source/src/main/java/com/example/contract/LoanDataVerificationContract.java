package com.example.contract;

import com.example.state.LoanDataVerificationState;
import com.example.state.LoanRequestDataState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

public class LoanDataVerificationContract implements Contract {

    public static final String BANK_CONTRACT_ID = "com.example.contract.LoanDataVerificationContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        if(tx != null && tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if(commandType instanceof Commands.SendForApproval) {
            if(tx.getInputStates().size() !=0)
                throw new IllegalArgumentException("Must have Zero input state");

            if(tx.getOutputStates().size() !=1)
                throw new IllegalArgumentException("Must have one output state");

            ContractState output = tx.getOutput(0);

            if(!(output instanceof LoanDataVerificationState))
                throw new IllegalArgumentException("Output must of BankAndCredit State");

            LoanDataVerificationState outputState = (LoanDataVerificationState)output;
            PublicKey BankKey = outputState.getBankNode().getOwningKey();
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

            if(!(input instanceof LoanDataVerificationState))
                throw new IllegalArgumentException("input should be of LoanDataVerificationState");

            if(!(output instanceof LoanDataVerificationState))
                throw new IllegalArgumentException("Output must of BankAndCredit State");

            List<String> blacklisted = Arrays.asList("jetsAirways","Kong airways","Hypermarket");
            boolean contains = blacklisted.contains(LoanRequestDataState.class);

            if(contains) {
                throw new IllegalArgumentException("Loan is not provided to defaulters");
            }

            LoanDataVerificationState inputState = (LoanDataVerificationState) input;
            LoanDataVerificationState outputState = (LoanDataVerificationState) output;
            PublicKey creditAgencyKey = inputState.getCreditRatingAgency().getOwningKey();
            PublicKey bankKey = outputState.getBankNode().getOwningKey();

            if(!(requiredSigners.contains(creditAgencyKey)))
                throw new IllegalArgumentException("CreditAgency signature is required");
            if(!(requiredSigners.contains(bankKey)))
                throw new IllegalArgumentException("Bank signature is required");
        }
    }

    public interface Commands extends CommandData {
        public class SendForApproval implements LoanReqDataContract.Commands {}
        public class receiveCreditApproval implements LoanReqDataContract.Commands {}
    }
}
