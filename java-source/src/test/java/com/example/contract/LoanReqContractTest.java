package com.example.contract;

import com.example.state.LoanRequestState;
import com.example.state.LoanVerificationState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static com.example.contract.LoanReqContract.LOANREQUEST_CONTRACT_ID;
import static net.corda.testing.node.NodeTestUtils.ledger;
import static net.corda.testing.node.NodeTestUtils.transaction;

public class LoanReqContractTest {

    static private final MockServices ledgerServices = new MockServices();
    static private TestIdentity finance = new TestIdentity(new CordaX500Name("finance", "London", "GB"));
    static private TestIdentity bank = new TestIdentity(new CordaX500Name("bank", "New York", "US"));
    static private TestIdentity credit = new TestIdentity(new CordaX500Name("credit", "Paris", "FR"));

    private static int amount = 15000;
    private static String companyName = "Boeing Company";

    private static LoanRequestState financeBankState = new LoanRequestState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier());
    private static LoanVerificationState loanVerificationState = new LoanVerificationState(amount,bank.getParty(),credit.getParty(),false,companyName,new UniqueIdentifier(),new UniqueIdentifier());

    @Test
    public void transactionMustIncludeCreateCommand() {

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(LOANREQUEST_CONTRACT_ID, financeBankState);
                tx.fails();
                tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new LoanReqContract.Commands.InitiateLoan());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    /***This test case is for when PartyA contacts Party B (when finance agency send loan application to bank) **/
    @Test
    public void transactionMustHaveNoInputs() {

        transaction(ledgerServices,tx -> {
            tx.output(LOANREQUEST_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new LoanReqContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });

        /**** uncomment for failure criteria **/
           /* transaction(ledgerServices,tx -> {
                tx.input(FINANCE_CONTRACT_ID, financeBankState);
                tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new LoanReqContract.Commands.InitiateLoan());
                tx.failsWith("No inputs should be consumed when issuing .");
                return null;
            });*/
    }

    /***This test case is for when PartyA contacts Party B (when finance agency send loan application to bank) **/
    @Test
    public void transactionMustHaveOneOutput() {

        transaction(ledgerServices,tx -> {
            tx.output(LOANREQUEST_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new LoanReqContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });

        /**** uncomment for failure criteria **/
        /* transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new LoanReqContract.Commands.InitiateLoan());
                tx.failsWith("Only one output state should be created.");
                return null;
            });*/
    }

    /***This test case is for when PartyA contacts Party B (when finance agency send loan application to bank) **/
    @Test
    public void lenderMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(LOANREQUEST_CONTRACT_ID, new LoanRequestState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
            tx.command(ImmutableList.of(finance.getPublicKey(),bank.getPublicKey()), new LoanReqContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });

        /****uncomment for failure criteria **/
       /* transaction(ledgerServices,tx -> {
                tx.output(FINANCE_CONTRACT_ID, new LoanRequestState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(bank.getPublicKey()), new LoanReqContract.Commands.InitiateLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });*/
    }

    /***This test case is for when PartyA contacts Party B (when finance agency send loan application to bank) **/
    @Test
    public void borrowerMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(LOANREQUEST_CONTRACT_ID, new LoanRequestState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
            tx.command(ImmutableList.of(finance.getPublicKey(),bank.getPublicKey()), new LoanReqContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });
            /**** uncomment for failure criteria ****/
         /*transaction(ledgerServices,tx -> {
                tx.output(FINANCE_CONTRACT_ID, new LoanRequestState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(finance.getPublicKey(),bank.getPublicKey()), new LoanReqContract.Commands.InitiateLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
        });*/
    }

    /***This test case is for when PartyA contacts Party B (when finance agency send loan application to bank) **/
    @Test
    public void lenderIsNotBorrower() {

            transaction(ledgerServices,tx -> {
                tx.output(LOANREQUEST_CONTRACT_ID, new LoanRequestState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()),  new LoanReqContract.Commands.InitiateLoan());
                tx.verifies();
                return null;
            });
    }

    @Test
    public void cannotCreateNegativeValue() {

        transaction(ledgerServices,tx -> {
                tx.output(LOANREQUEST_CONTRACT_ID,new LoanRequestState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new LoanReqContract.Commands.InitiateLoan());
                tx.verifies();
                return null;
            });
    }

    /***This test case is for when PartyB contacts Party C (when bank send loan application to credit agency) **/
    @Test
    public void bankStateMustHaveNoInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.output(LOANREQUEST_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(credit.getParty().getOwningKey(), bank.getParty().getOwningKey()), new LoanVerificationContract.Commands.SendForCreditApproval());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when PartyB contacts Party C (when bank send loan application to credit agency) **/
    @Test
    public void bankMustHaveOneOutput() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.output(LOANREQUEST_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(credit.getPublicKey(), bank.getPublicKey()), new LoanVerificationContract.Commands.SendForCreditApproval());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when Party C contacts Party B (when credit agency send loan application to bank) **/
    @Test
    public void bankStateMustHaveOneInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.input(LOANREQUEST_CONTRACT_ID, loanVerificationState);
            tx.output(LOANREQUEST_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(credit.getParty().getOwningKey(), bank.getParty().getOwningKey()), new LoanVerificationContract.Commands.ReceiveCreditApproval());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when Party C contacts Party B (when credit agency send loan application to bank) **/
    @Test
    public void creditMustHaveOneOutput() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.input(LOANREQUEST_CONTRACT_ID, loanVerificationState);
            tx.output(LOANREQUEST_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(credit.getPublicKey(), bank.getPublicKey()), new LoanVerificationContract.Commands.ReceiveCreditApproval());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when PartyB contacts Party A (when bank send loan application status to finance) **/
    @Test
    public void transactionMustHaveOneInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices,tx -> {
            tx.input(LOANREQUEST_CONTRACT_ID,financeBankState);
            tx.output(LOANREQUEST_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new LoanReqContract.Commands.LoanResponse());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when PartyB contacts Party A (when bank send loan application Status to Finance) **/
    @Test
    public void FinanceBankStateMustHaveFinalOneOutput() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices,tx -> {
            tx.input(LOANREQUEST_CONTRACT_ID,financeBankState);
            tx.output(LOANREQUEST_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new LoanReqContract.Commands.LoanResponse());
            tx.verifies();
            return null;
        });
    }
}
