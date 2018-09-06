package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.FinanceContract;
import com.example.state.BankAndCreditState;
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
import java.security.PublicKey;
import java.util.List;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class BankCreditAgencyFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Party otherParty;
        private String companyName;
        private final int amount;
        private boolean loanEligibleFlag;
        private UniqueIdentifier linearId;
        private UniqueIdentifier linearIdRequestForLoan;

        public Initiator(int amount,Party otherParty,String companyName,UniqueIdentifier linearIdRequestForLoan) {
            System.out.println("Entered this constructor");
            this.amount = amount;
            this.otherParty = otherParty;
            this.companyName = companyName;
            this.linearIdRequestForLoan =linearIdRequestForLoan;
            System.out.println("linearIdRequestForLoan : "+linearIdRequestForLoan);
        }

        public UniqueIdentifier getLinearIdRequestForLoan() {
            return linearIdRequestForLoan;
        }

        public void setLinearIdRequestForLoan(UniqueIdentifier linearIdRequestForLoan) {
            this.linearIdRequestForLoan = linearIdRequestForLoan;
        }

        public boolean isLoanEligibleFlag() {
            return loanEligibleFlag;
        }

        public void setLoanEligibleFlag(boolean loanEligibleFlag) {
            this.loanEligibleFlag = loanEligibleFlag;
        }

        public int getAmount() {
            return amount;
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

        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.")
        {
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

            System.out.println("I reached here");
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            progressTracker.setCurrentStep(LOAN_ELIGIBILITY);
            Party me = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            /******Validation of linear id *****/
            try {
                QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(
                        null,
                        ImmutableList.of(linearIdRequestForLoan),
                        Vault.StateStatus.UNCONSUMED,
                        null);

                Vault.Page<FinanceAndBankState> results  = getServiceHub().getVaultService().queryBy(FinanceAndBankState.class,criteria);
                List<StateAndRef<FinanceAndBankState>> financeStateListResults = results.getStates();
                System.out.println("size of list financeStateListResults : "+financeStateListResults.size());
                if (financeStateListResults.size() < 1 && financeStateListResults.isEmpty()) {
                    System.out.println("SIZE : "+financeStateListResults.size());
                    throw new FlowException("Linearid with id %s not found."+ linearId);
                }
                else {
                    financeStateListResults.get(0);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            /*******Validation of linear id END *****/
            BankAndCreditState bankAndCreditState = new BankAndCreditState(me,otherParty, loanEligibleFlag, companyName,amount,new UniqueIdentifier());
            PublicKey bankKey = getServiceHub().getMyInfo().getLegalIdentities().get(0).getOwningKey();
            PublicKey creditAgencyKey = otherParty.getOwningKey();
            System.out.println("linearIdRequestForLoan : "+linearIdRequestForLoan);
            final Command<FinanceContract.Commands.SendForApproval> sendLoanApprovalCommand = new Command<FinanceContract.Commands.SendForApproval>(new FinanceContract.Commands.SendForApproval(),ImmutableList.of(bankKey,creditAgencyKey));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(bankAndCreditState,FinanceContract.TEMPLATE_CONTRACT_ID)
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
                        require.using("This must be an FinanceAndBankState transaction.", output instanceof BankAndCreditState);
                        BankAndCreditState bankAndCreditCheckOne = (BankAndCreditState) output;

                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
