/******** @author : Shivan Sawant *******/
package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.FinanceContract;
import com.example.state.FinanceAndBankState;
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

import java.util.Arrays;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class BankAndFinanceFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Party otherParty;
        private String companyName;
        private final int amount;
        private boolean loanflag;
        UniqueIdentifier linearId = null;
        UniqueIdentifier linearIdFinance = null;
        String id = null;

        public Initiator(int amount,Party otherParty,String companyName,UniqueIdentifier linearIdFinance) {
            this.amount = amount;
            this.otherParty = otherParty;
            this.companyName =companyName;
            this.linearIdFinance = linearIdFinance;
        }

        public Initiator(Party otherParty, String companyName, int amount, UniqueIdentifier uniqueIdentifier) {
            this.otherParty = otherParty;
            this.companyName = companyName;
            this.amount = amount;
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

        public boolean isLoanflag() {
            return loanflag;
        }
        public void setLoanflag(boolean loanflag) {
            this.loanflag = loanflag;
        }
        public void setLinearId(UniqueIdentifier linearId) {
            this.linearId = linearId;
        }

        public UniqueIdentifier getLinearIdFinance() {
            return linearIdFinance;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page<FinanceAndBankState> results  = getServiceHub().getVaultService().queryBy(FinanceAndBankState.class,criteria);
            List<StateAndRef<FinanceAndBankState>> inputStateList = results.getStates();
            if(inputStateList != null && !(inputStateList.isEmpty()) ) {
                inputStateList.get(inputStateList.size()-1);
            }
            else {
                throw new IllegalArgumentException("State Cannot be found");
            }

            /***** linear id of financeState Checking if it exist *****/
            try {
                QueryCriteria criteriaFinanceState = new QueryCriteria.LinearStateQueryCriteria(
                        null,
                        ImmutableList.of(linearIdFinance),
                        Vault.StateStatus.UNCONSUMED,
                        null);

                Vault.Page<FinanceAndBankState> resultsFinanceState  = getServiceHub().getVaultService().queryBy(FinanceAndBankState.class,criteria);
                List<StateAndRef<FinanceAndBankState>> financeStateListResults = resultsFinanceState.getStates();
                System.out.println("size of list financeStateListResults : "+financeStateListResults.size());
                if (financeStateListResults.size() < 1 && financeStateListResults.isEmpty()) {
                    System.out.println("SIZE : "+financeStateListResults.size());
                    throw new FlowException("Linearid with id %s not found."+ linearId);
                }
                else {
                    financeStateListResults.get(0);
                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            /*****END. linear id of financeState Checking if it exist *****/
            QueryCriteria.VaultQueryCriteria criteria1 = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            progressTracker.setCurrentStep(BANK_RESPONSE);
            //Generate an unsigned transaction
            Party me = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            final StateAndRef<FinanceAndBankState> stateAsInput =  inputStateList.get(inputStateList.size()-1);
            linearId = stateAsInput.getState().getData().getLinearId().copy(id,stateAsInput.getState().getData().getLinearId().getId());
            //FinanceAndBankState financeBankState = new FinanceAndBankState(me, otherParty,companyName,amount, linearId);
            List<String> blacklisted = Arrays.asList("Syntel","Mindtree","IBM","TechMahindra","TCS","J.P. Morgon","Bank of America");
            boolean contains = blacklisted.contains(companyName);
            FinanceAndBankState financeAndBankState = null;
            if(contains) {
                 financeAndBankState = new FinanceAndBankState(otherParty,me,companyName,amount,linearId,false);
                financeAndBankState.setLoanEligibleFlag(false);
                throw new IllegalArgumentException("This company is blacklisted for LOAN, Loan is rejected ..!!!");
            }
            else {
                 financeAndBankState = new FinanceAndBankState(otherParty,me,companyName,amount,linearId,false);
                 financeAndBankState.setLoanEligibleFlag(true);
            }

            final Command<FinanceContract.Commands.InitiateLoan> initiateLoanCommand = new Command<FinanceContract.Commands.InitiateLoan>(new FinanceContract.Commands.InitiateLoan(), ImmutableList.of(financeAndBankState.getBank().getOwningKey(), financeAndBankState.getfinance().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(stateAsInput)
                    .addOutputState(financeAndBankState, FinanceContract.TEMPLATE_CONTRACT_ID).addCommand(initiateLoanCommand);

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
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an FinanceAndBankState transaction.", output instanceof FinanceAndBankState);
                        FinanceAndBankState bankAndFinanceCheck = (FinanceAndBankState) output;
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
