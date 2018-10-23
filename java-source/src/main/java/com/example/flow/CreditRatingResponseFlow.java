package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.LoanVerificationContract;
import com.example.state.LoanVerificationState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.List;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CreditRatingResponseFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Party bankParty;
        private  String companyName;
        private boolean isEligibleForLoanFlag;
        private int amount;
        UniqueIdentifier linearId = null;
        UniqueIdentifier linearIdLoanReqState = null;
        UniqueIdentifier linearIdLoanDataVerState = null;
        final String[] values = {"JETSAIRWAYS","AMERICONAIRWAYS","SAHARAAIRLINES","JETBLUEAIRLINE"};

        /* This constructor is called from REST API **/
        public Initiator(Party bankParty, UniqueIdentifier linearIdLoanDataVerState) {
            this.bankParty = bankParty;
            this.linearIdLoanDataVerState = linearIdLoanDataVerState;
        }

        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step LOAN_ELIGIBILITY_RESPONSE = new ProgressTracker.Step("Response from credit rating agency about loan eligibility and approval");
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
                LOAN_ELIGIBILITY_RESPONSE,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public UniqueIdentifier getLinearId() {
            return linearId;
        }

        public void setLinearId(UniqueIdentifier linearId) {
            this.linearId = linearId;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public UniqueIdentifier getLinearIdLoanReqState() {
            return linearIdLoanReqState;
        }

        public boolean isEligibleForLoanFlag() {
            return isEligibleForLoanFlag;
        }

        public UniqueIdentifier getLinearIdLoanDataVerState() {
            return linearIdLoanDataVerState;
        }

        public void setLinearIdLoanDataVerState(UniqueIdentifier linearIdLoanDataVerState) {
            this.linearIdLoanDataVerState = linearIdLoanDataVerState;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            LoanVerificationState loanVerificationStates = null;
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party creditParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

            /* Querying LoanVerificationState from vault using linear id */
            QueryCriteria criteriaBankState = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(linearIdLoanDataVerState),
                    Vault.StateStatus.UNCONSUMED,
                    null);

            StateAndRef<LoanVerificationState> inputState = null;
            List<StateAndRef<LoanVerificationState>> inputStateList = getServiceHub().getVaultService().queryBy(LoanVerificationState.class, criteriaBankState).getStates();
            if (inputStateList == null || inputStateList.isEmpty()) {
                throw new IllegalArgumentException("State Cannot be found : " + inputStateList.size() + " " + linearIdLoanDataVerState);
            }

            linearId = linearIdLoanDataVerState;
            inputState = inputStateList.get(0);

            companyName = inputStateList.get(0).getState().getData().getCompanyName();
            amount = inputStateList.get(0).getState().getData().getAmount();
            linearIdLoanReqState = inputStateList.get(0).getState().getData().getLinearIdLoanReq();

            boolean contains = Arrays.stream(values).anyMatch(companyName::equals);

            /** Setting the loanEligibility flag in the state's vault **/
            if (contains) {
                loanVerificationStates = new LoanVerificationState(amount, bankParty, creditParty, false, companyName,linearId, linearIdLoanReqState);
            } else {
                 loanVerificationStates = new LoanVerificationState(amount, bankParty, creditParty, true, companyName,linearId, linearIdLoanReqState);
            }

            progressTracker.setCurrentStep(LOAN_ELIGIBILITY_RESPONSE);

            //LoanVerificationState loanVerificationStates = new LoanVerificationState(amount,bankParty,me,true, companyName,linearId, linearIdLoanReqState);
            final Command<LoanVerificationContract.Commands.ReceiveCreditApproval> receiveCreditApproval = new Command<LoanVerificationContract.Commands.ReceiveCreditApproval>(new LoanVerificationContract.Commands.ReceiveCreditApproval(),ImmutableList.of(loanVerificationStates.getCreditAgencyNode().getOwningKey(), loanVerificationStates.getBankNode().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addOutputState(loanVerificationStates, LoanVerificationContract.LOANVERIFICATION_CONTRACT_ID)
                    .addCommand(receiveCreditApproval);
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
            class SignTxFlow extends  SignTransactionFlow {
                public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                    super(otherSideSession, progressTracker);
                }
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException
                {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an credit agency transaction (LoanVerificationState).", output instanceof LoanVerificationState);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}