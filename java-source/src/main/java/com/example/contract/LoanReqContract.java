package com.example.contract;

import com.example.state.LoanRequestState;
import com.example.state.LoanVerificationState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class LoanReqContract implements Contract {
    public static final String LOANREQUEST_CONTRACT_ID = "com.example.contract.LoanReqContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

        if (tx != null && tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if (commandType instanceof Commands.InitiateLoan) {
            verifyInitiateLoan(tx, requiredSigners);

        } else if (commandType instanceof Commands.LoanResponse) {
            verifyLoanResponse(tx, requiredSigners);
        }
    }

    private void verifyInitiateLoan(LedgerTransaction tx, List<PublicKey> signers) {

        requireThat(req -> {

            req.using("No input should be consumed while initiating loan", tx.getInputStates().isEmpty());
            req.using("Only one output should be created during the process of initiating loan", tx.getOutputStates().size() == 1);

            ContractState outputState = tx.getOutput(0);

            req.using(" Ouput must be a LoanRequestState", outputState instanceof LoanRequestState);

            LoanRequestState loanReqState = (LoanRequestState) outputState;

            req.using("Loan amount should not be zero", loanReqState.getAmount() > 0);

            Party financeAgency = loanReqState.getFinanceNode();

            PublicKey financeAgencyKey = financeAgency.getOwningKey();
            PublicKey bankKey = loanReqState.getBankNode().getOwningKey();

            req.using("Finance agency should sign the transaction", signers.contains(financeAgencyKey));

            req.using("Bank should sign the transaction", signers.contains(bankKey));

            return null;
        });
    }

        private void verifyLoanResponse(LedgerTransaction tx, List<PublicKey> signers) {

            requireThat(req -> {

                req.using("Only one input should be consumed while giving loan application response to finance agency", tx.getInputStates().size() == 1);
                req.using("Only one output should be created ", tx.getOutputStates().size() == 1);

                ContractState input = tx.getInput(0);
                ContractState output = tx.getOutput(0);

                req.using("input must be of type LoanRequestState ", input instanceof LoanRequestState);
                req.using("output must be of the type LoanRequestState ", output instanceof LoanRequestState);

                LoanRequestState inputState = (LoanRequestState) input;
                LoanRequestState outputState = (LoanRequestState) output;

                PublicKey bankKey = inputState.getBankNode().getOwningKey();
                PublicKey financeAgencyKey = outputState.getFinanceNode().getOwningKey();

                req.using("Finance agency should sign the transaction", signers.contains(financeAgencyKey));
                req.using("Bank should sign the transaction", signers.contains(bankKey));

                return null;
            });
        }


    public interface Commands extends CommandData {
        public class InitiateLoan implements Commands { }
       // public class SendForCreditApproval implements Commands { }
        public class LoanResponse implements Commands { }
    }
}