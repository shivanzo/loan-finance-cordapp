package com.example.contract;

import com.example.state.LoanRequestState;
import com.example.state.LoanVerificationState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class LoanVerificationContract implements Contract {

    public static final String LOANVERIFICATION_CONTRACT_ID = "com.example.contract.LoanVerificationContract";


    public interface Commands extends CommandData {
        public class SendForCreditApproval implements Commands {
        }

        public class ReceiveCreditApproval implements LoanReqContract.Commands {
        }
    }


    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        if (tx != null && tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if (commandType instanceof Commands.SendForCreditApproval) {
            verifySendForCreditApproval(tx, requiredSigners);
        } else if (commandType instanceof Commands.ReceiveCreditApproval) {
            verifyReceiveCreditApproval(tx, requiredSigners);
        }
    }

    private void verifySendForCreditApproval(LedgerTransaction tx, List<PublicKey> signers) {

        requireThat(req -> {

            req.using("No input should be consumed while initiating loan", tx.getInputStates().isEmpty());
            req.using("Only one output should be created during the process of initiating loan", tx.getOutputStates().size() == 1);

            ContractState output = tx.getOutput(0);

            req.using(" Ouput must be a LoanRequestState", output instanceof LoanVerificationState);

            LoanVerificationState loanVerState = (LoanVerificationState) output;
            PublicKey bankKey = loanVerState.getBankNode().getOwningKey();
            PublicKey creidtAgencyKey = loanVerState.getCreditAgencyNode().getOwningKey();

            req.using("Bank's signature is mandatory for completion of transaction ", signers.contains(bankKey));
            req.using("CreditAgency's signature is mandatory for completion of transaction ", signers.contains(creidtAgencyKey));

            return null;
        });
    }

    private void verifyReceiveCreditApproval(LedgerTransaction tx, List<PublicKey> signers) {

        requireThat(req -> {

            req.using("Only one input should be consumed while giving response from credit agency to Bank", tx.getInputStates().size() == 1);
            req.using("Only one output should be created ", tx.getOutputStates().size() == 1);

            ContractState input = tx.getInput(0);
            ContractState output = tx.getOutput(0);

            req.using("input should only be of type LoanVerificationState ", input instanceof LoanVerificationState);
            req.using("output shoud be of the type LoanVerificationState", output instanceof LoanVerificationState);

            LoanVerificationState inputState = (LoanVerificationState) input;
            LoanVerificationState outputState = (LoanVerificationState) output;

            PublicKey bankKey = inputState.getBankNode().getOwningKey();
            PublicKey creditAgencyKey = ((LoanVerificationState) output).getCreditAgencyNode().getOwningKey();

            req.using("bank must sign the transaction", signers.contains(bankKey));
            req.using("creditAgency must sign the transaction", signers.contains(creditAgencyKey));
            return null;
        });
    }
}





