package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.LoanVerificationContract;
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

public class RequestCreditRatingFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Party creditParty;
        private String companyName;
        private boolean isEligibleForLoan;
        private int amount;
        private UniqueIdentifier linearId;
        private UniqueIdentifier linearIdRequestForLoan;

        /** This constructor is called from REST API **/
        public Initiator(Party creditParty, UniqueIdentifier linearIdRequestForLoan) {
            this.creditParty = creditParty;
            this.linearIdRequestForLoan = linearIdRequestForLoan;
        }

        public UniqueIdentifier getLinearIdRequestForLoan() {
            return linearIdRequestForLoan;
        }

        public void setLinearIdRequestForLoan(UniqueIdentifier linearIdRequestForLoan) {
            this.linearIdRequestForLoan = linearIdRequestForLoan;
        }

        public boolean isEligibleForLoan() {
            return isEligibleForLoan;
        }

        public void setEligibleForLoan(boolean eligibleForLoan) {
            this.isEligibleForLoan = eligibleForLoan;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }


        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step LOAN_ELIGIBILITY = new ProgressTracker.Step("Sending Loan application to credit rating agecny to check loan eligibilty and CIBIL score");
        private final ProgressTracker.Step LOAN_ELIGIBILITY_RESPONSE = new ProgressTracker.Step("Response from credit rating agency about loan eligibility and approval");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
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
                LOAN_ELIGIBILITY,
                LOAN_ELIGIBILITY_RESPONSE,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            progressTracker.setCurrentStep(LOAN_ELIGIBILITY);
            Party bankParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            StateAndRef<LoanVerificationState> financStateInstance = null;

            /******Validation of financeDataState linear id *****/
               QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(
                        null,
                        ImmutableList.of(linearIdRequestForLoan),
                        Vault.StateStatus.UNCONSUMED,
                        null);

                List<UniqueIdentifier> financeStateListValidationResult = new ArrayList<UniqueIdentifier>();
                List<StateAndRef<LoanRequestState>> financeStateListResults = getServiceHub().getVaultService().queryBy(LoanRequestState.class,criteria).getStates();
                if (financeStateListResults.isEmpty()) {
                    throw new FlowException("Linearid with id not found." + linearIdRequestForLoan );
                }



              /*** Getting the amount, companyName and loan-eligibility from the vault of Previous State **/
              amount = financeStateListResults.get(0).getState().getData().getAmount();
              companyName = financeStateListResults.get(0).getState().getData().getCompanyName();
              isEligibleForLoan = false;

            /******* Validation of financeDataState linear id *****/

            LoanVerificationState loanVerificationState = new LoanVerificationState(amount, bankParty, creditParty, isEligibleForLoan, companyName, new UniqueIdentifier(), linearIdRequestForLoan);
            final Command<LoanVerificationContract.Commands.SendForCreditApproval> sendLoanApprovalCommand = new Command<LoanVerificationContract.Commands.SendForCreditApproval>(new LoanVerificationContract.Commands.SendForCreditApproval(), ImmutableList.of(loanVerificationState.getBankNode().getOwningKey(), loanVerificationState.getCreditAgencyNode().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(loanVerificationState, LoanVerificationContract.LOANVERIFICATION_CONTRACT_ID)
                    .addCommand(sendLoanApprovalCommand);

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
            FlowSession otherPartySession = initiateFlow(creditParty);
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
                        require.using("This transaction should be between Credit Agency and bank (LoanDataVerifactionState).", output instanceof LoanVerificationState);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
