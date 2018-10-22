package com.example.contract;

import com.example.state.LoanVerificationState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

public class LoanVerificationContract implements Contract {

    public static final String LOANVERIFICATION_CONTRACT_ID = "com.example.contract.LoanVerificationContract";

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

            if(!(output instanceof LoanVerificationState))
                throw new IllegalArgumentException("Output must of BankAndCredit State");

            LoanVerificationState outputState = (LoanVerificationState)output;
            PublicKey BankKey = outputState.getBankNode().getOwningKey();
            PublicKey creditAgencyKey = outputState.getCreditAgencyNode().getOwningKey();

            if(!(requiredSigners.contains(BankKey)))
                throw new IllegalArgumentException("Bank should sign...!! Bank signature is mandatory");

            if(!(requiredSigners.contains(creditAgencyKey)))
                throw new IllegalArgumentException("Credit Agency should sign ...!! Credit agency signature is mandatory");
        }

        else if (commandType instanceof Commands.ReceiveCreditApproval) {
            if(tx.getInputStates().size() !=1)
                throw new IllegalArgumentException("Must have atleast one input state");

            if(tx.getOutputStates().size() !=1)
                throw new IllegalArgumentException("Must have one output state");

            ContractState input = tx.getInput(0);
            ContractState output = tx.getOutput(0);

            if(!(input instanceof LoanVerificationState))
                throw new IllegalArgumentException("input should be of LoanVerificationState");

            if(!(output instanceof LoanVerificationState))
                throw new IllegalArgumentException("Output must of BankAndCredit State");


            LoanVerificationState inputState = (LoanVerificationState) input;
            LoanVerificationState outputState = (LoanVerificationState) output;
            PublicKey creditAgencyKey = inputState.getCreditAgencyNode().getOwningKey();
            PublicKey bankKey = outputState.getBankNode().getOwningKey();

            if(!(requiredSigners.contains(creditAgencyKey)))
                throw new IllegalArgumentException("CreditAgency signature is required");
            if(!(requiredSigners.contains(bankKey)))
                throw new IllegalArgumentException("Bank signature is required");
        }
    }

    public interface Commands extends CommandData {
        public class SendForApproval implements LoanReqContract.Commands {}
        public class ReceiveCreditApproval implements LoanReqContract.Commands {}
    }
}
