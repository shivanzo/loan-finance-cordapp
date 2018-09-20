package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.FinanceContract;
import com.example.state.BankAndCreditState;
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

public class CreditAgencyBankNotificationFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Party otherParty;
        private  String companyName;
        private boolean loanEligibleFlag;
        UniqueIdentifier linearId = null;
        UniqueIdentifier linearIdFinanceState = null;
        UniqueIdentifier linearIdBankState = null;

        public Initiator(Party otherParty,String companyName,UniqueIdentifier linearIdFinanceState,UniqueIdentifier linearIdBankState) {
            this.otherParty = otherParty;
            this.companyName = companyName;
            this.linearIdFinanceState = linearIdFinanceState;
            this.linearIdBankState = linearIdBankState;
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

        public UniqueIdentifier getLinearIdFinanceState() {
            return linearIdFinanceState;
        }

        public boolean isLoanEligibleFlag() {
            return loanEligibleFlag;
        }

        public UniqueIdentifier getLinearIdBankState() {
            return linearIdBankState;
        }

        public void setLinearIdBankState(UniqueIdentifier linearIdBankState) {
            this.linearIdBankState = linearIdBankState;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            QueryCriteria criteriaBankState = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(linearIdBankState),
                    Vault.StateStatus.UNCONSUMED,
                    null);

            StateAndRef<BankAndCreditState> inputState = null;
            List<StateAndRef<BankAndCreditState>> inputStateList = getServiceHub().getVaultService().queryBy(BankAndCreditState.class,criteriaBankState).getStates();
            if(inputStateList == null || inputStateList.isEmpty() || inputStateList.size() < 1 ) {
                throw new IllegalArgumentException("State Cannot be found : "+inputStateList.size());
            }
            else if(inputStateList.get(0).getState().getData().getLinearId().equals(linearIdBankState)) {
                linearId = linearIdBankState;
                inputState = inputStateList.get(0);
            }
            else {
                throw new IllegalArgumentException("Linear id is not valid : "+linearIdBankState);
            }

            List<String> blacklisted = Arrays.asList("jetsAirways","Kong airways","Hypermarket");
            boolean contains = blacklisted.contains(companyName);
            BankAndCreditState bankAndCreditState = new BankAndCreditState(linearId);
            if(contains) {
                bankAndCreditState.setLoanEligibleFlag(false);
                throw new IllegalArgumentException("This company is blacklisted for LOAN, Loan is rejected ..!!!");
            }
            else {
                bankAndCreditState.setLoanEligibleFlag(true);
            }

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            progressTracker.setCurrentStep(LOAN_ELIGIBILITY_RESPONSE);
            Party me = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            BankAndCreditState bankAndCreditStates = new BankAndCreditState(me,otherParty,true, companyName,linearId);
            final Command<FinanceContract.Commands.receiveCreditApproval> receiveCreditApproval = new Command<FinanceContract.Commands.receiveCreditApproval>(new FinanceContract.Commands.receiveCreditApproval(),ImmutableList.of(bankAndCreditStates.getCreditRatingAgency().getOwningKey(),bankAndCreditStates.getbank().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addOutputState(bankAndCreditStates,FinanceContract.FINANCE_CONTRACT_ID)
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
            FlowSession otherPartySession = initiateFlow(otherParty);
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
                        require.using("This must be an FinanceAndBankState transaction.", output instanceof BankAndCreditState);
                        BankAndCreditState bankAndCreditCheck = (BankAndCreditState) output;
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}