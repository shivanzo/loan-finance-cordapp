package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.LoanReqContract;
import com.example.state.LoanRequestState;
import com.example.state.LoanVerificationState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.ArrayList;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class LoanResponseFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Party financeParty;
        private String companyName;
        private int amount;
        private boolean isEligibleForLoan;
        UniqueIdentifier linearIdLoanDataVerState = null;
        UniqueIdentifier linearIdLoanReqDataState = null;

        // This linearId variable serves as a temporary varible which will have the copy of prev state linear id */
        UniqueIdentifier linearId = null;

        /* This Constructor is called from REST API */
        public Initiator(Party otherParty, UniqueIdentifier linearIdLoanReqDataState, UniqueIdentifier linearIdLoanDataVerState) {
            this.financeParty = otherParty;
            this.linearIdLoanReqDataState = linearIdLoanReqDataState;
            this.linearIdLoanDataVerState = linearIdLoanDataVerState;
        }

        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step BANK_RESPONSE = new ProgressTracker.Step("Sending response to Finance agency from Bank");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");

        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.")
        {
            public ProgressTracker childProgressTracker()
            {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };

        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            public ProgressTracker childProgressTracker()
            {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                BANK_RESPONSE,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public UniqueIdentifier getLinearIdLoanDataVerState() {
            return linearIdLoanDataVerState;
        }

        public void setLinearIdLoanDataVerState(UniqueIdentifier linearIdLoanDataVerState) {
            this.linearIdLoanDataVerState = linearIdLoanDataVerState;
        }

        public UniqueIdentifier getLinearIdLoanReqDataState() {
            return linearIdLoanReqDataState;
        }

        public boolean isEligibleForLoan() {
            return isEligibleForLoan;
        }

        public void setEligibleForLoan(boolean eligibleForLoan) {
            isEligibleForLoan = eligibleForLoan;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            Party bankParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            StateAndRef<LoanRequestState> inputState = null;
            StateAndRef<LoanVerificationState> bankCreditStateQuery = null;
            LoanVerificationState bankState = new LoanVerificationState(bankParty, linearIdLoanDataVerState, linearIdLoanReqDataState);

            QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(linearIdLoanReqDataState),
                    Vault.StateStatus.UNCONSUMED,
                    null);

            List<UniqueIdentifier> financeStateListValidationResult = new ArrayList<UniqueIdentifier>();
            List<UniqueIdentifier> bankAndCreditStateRetrieve = new ArrayList<UniqueIdentifier>();

            List<StateAndRef<LoanRequestState>> financeStateListResults = getServiceHub().getVaultService().queryBy(LoanRequestState.class,criteria).getStates();

            if (financeStateListResults.isEmpty()) {
                throw new FlowException("Linearid with id %s not found."+ linearIdLoanReqDataState);
            }
            else {
                linearId = linearIdLoanReqDataState;
                inputState = financeStateListResults.get(0);
            }

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            progressTracker.setCurrentStep(BANK_RESPONSE);
            //Generate an unsigned transaction

            /** adding the linear id of unconsumed Previous LoanRequestState **/
            linearId = linearIdLoanReqDataState;
            LoanRequestState loanRequestState = null;

            QueryCriteria criteriaForBankVault = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(linearIdLoanDataVerState),
                    Vault.StateStatus.UNCONSUMED,
                    null);

            List<StateAndRef<LoanVerificationState>>  bankAndCreditStateList = getServiceHub().getVaultService().queryBy(LoanVerificationState.class,criteriaForBankVault).getStates();

            if(!bankAndCreditStateList.isEmpty()) {
                bankCreditStateQuery = bankAndCreditStateList.get(0);
            }
            else {
                throw new FlowException("Exception while fetching FinanceID : "+ linearIdLoanDataVerState + "size "+bankAndCreditStateList.size());
            }

            isEligibleForLoan = bankCreditStateQuery.getState().getData().getLoanEligibleFlag();
            amount = bankCreditStateQuery.getState().getData().getAmount();
            companyName = bankCreditStateQuery.getState().getData().getCompanyName();
            loanRequestState = new LoanRequestState(financeParty,bankParty,companyName,amount,linearId,isEligibleForLoan, linearIdLoanDataVerState);
            /********* NEED TO QUERY FROM BANK STATE THE FLAG ****/
            loanRequestState.setEligibleForLoan(bankCreditStateQuery.getState().getData().getLoanEligibleFlag());
            inputState.getState().getData().setEligibleForLoan(bankCreditStateQuery.getState().getData().getLoanEligibleFlag());

            final Command<LoanReqContract.Commands.LoanNotification> loanNotificationCommand = new Command<LoanReqContract.Commands.LoanNotification>(new LoanReqContract.Commands.LoanNotification(), ImmutableList.of(loanRequestState.getBankNode().getOwningKey(), loanRequestState.getFinanceNode().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addOutputState(loanRequestState, LoanReqContract.LOANREQUEST_CONTRACT_ID).addCommand(loanNotificationCommand);

            //step 2
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            txBuilder.verify(getServiceHub());
            //stage 3
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            //Stage 4
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(financeParty);
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

            class SignTxFlow extends  SignTransactionFlow {
                public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                    super(otherSideSession, progressTracker);

                }

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("Error ...!! This must be Bank's transaction to Finance Agency (LoanRequestState transaction).", output instanceof LoanRequestState);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
