package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.LoanReqContract;
import com.example.state.LoanRequestState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class RequestForLoanFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final Party bankParty;
        private int amount;
        private String companyName;
        private UniqueIdentifier linearIdLoanReqState;

        /**
         * This constructor is being called from REST API
         **/
        public Initiator(Party bankParty, int amount, String companyName) {
            this.bankParty = bankParty;
            this.amount = amount;
            this.companyName = companyName;
        }

        public UniqueIdentifier getLinearIdLoanReqState() {
            return linearIdLoanReqState;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        private final ProgressTracker.Step LOAN_REQUEST = new ProgressTracker.Step("Finance Agency sending Loan application for bank");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step LOAN_ELIGIBILITY = new ProgressTracker.Step("Sending Loan application to credit rating agecny to check loan eligibilty and CIBIL score");
        private final ProgressTracker.Step LOAN_ELIGIBILITY_RESPONSE = new ProgressTracker.Step("Response from credit rating agency about loan eligibility and approval");
        private final ProgressTracker.Step BANK_RESPONSE = new ProgressTracker.Step("Sending response to Finance agency from Bank");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };

        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                LOAN_REQUEST,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                LOAN_ELIGIBILITY,
                LOAN_ELIGIBILITY_RESPONSE,
                BANK_RESPONSE,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            //Stage 1
            progressTracker.setCurrentStep(LOAN_REQUEST);

            //Generate an unsigned transaction
            Party financeParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            LoanRequestState financeBankState = new LoanRequestState(financeParty, bankParty, companyName, amount, new UniqueIdentifier(), false);
            final Command<LoanReqContract.Commands.InitiateLoan> initiateLoanCommand = new Command<LoanReqContract.Commands.InitiateLoan>(new LoanReqContract.Commands.InitiateLoan(), ImmutableList.of(financeBankState.getBankNode().getOwningKey(), financeBankState.getFinanceNode().getOwningKey()));

            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(financeBankState, LoanReqContract.LOANREQUEST_CONTRACT_ID)
                    .addCommand(initiateLoanCommand);

            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

            txBuilder.verify(getServiceHub());
            //stage 3
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            //Stage 4
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(bankParty);
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));
            //stage 5
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            //Notarise and record the transaction in both party vaults.
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                    super(otherSideSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be a transaction between bank and finance Agency (LoanRequestState transaction).", output instanceof LoanRequestState);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
