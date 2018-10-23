package com.example.contract;

import com.example.state.LoanRequestState;
import com.example.state.LoanVerificationState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static com.example.contract.LoanVerificationContract.LOANVERIFICATION_CONTRACT_ID;
import static net.corda.testing.node.NodeTestUtils.ledger;
import static net.corda.testing.node.NodeTestUtils.transaction;

public class LoanDataVerContractTest {

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
                tx.output(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
                tx.fails();
                tx.command(ImmutableList.of(bank.getParty().getOwningKey(), credit.getParty().getOwningKey()), new LoanReqContract.Commands.InitiateLoan());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveNoInputs() {

        transaction(ledgerServices,tx -> {
            tx.output(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(bank.getParty().getOwningKey(), credit.getParty().getOwningKey()), new LoanReqContract.Commands.InitiateLoan());
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

    @Test
    public void transactionMustHaveOneOutput() {

        transaction(ledgerServices,tx -> {
            tx.output(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(bank.getPublicKey(), credit.getPublicKey()), new LoanReqContract.Commands.InitiateLoan());
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

    @Test
    public void lenderMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(LOANVERIFICATION_CONTRACT_ID, new LoanRequestState(bank.getParty(), credit.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
            tx.command(ImmutableList.of(bank.getPublicKey(),credit.getPublicKey()), new LoanReqContract.Commands.InitiateLoan());
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

    @Test
    public void borrowerMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(LOANVERIFICATION_CONTRACT_ID, new LoanVerificationState(amount,bank.getParty(),credit.getParty(),false,companyName,new UniqueIdentifier(),new UniqueIdentifier()));
            tx.command(ImmutableList.of(bank.getPublicKey(),credit.getPublicKey()), new LoanVerificationContract.Commands.SendForCreditApproval());
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

    @Test
    public void lenderIsNotBorrower() {

        transaction(ledgerServices,tx -> {
            tx.output(LOANVERIFICATION_CONTRACT_ID, new LoanVerificationState(amount,bank.getParty(),credit.getParty(),false,companyName,new UniqueIdentifier(),new UniqueIdentifier()));
            tx.command(ImmutableList.of(bank.getPublicKey(), credit.getPublicKey()),  new LoanVerificationContract.Commands.SendForCreditApproval());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void bankStateMustHaveNoInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.output(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(credit.getParty().getOwningKey(), bank.getParty().getOwningKey()), new LoanVerificationContract.Commands.SendForCreditApproval());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void bankMustHaveOneOutput() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.output(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(credit.getPublicKey(), bank.getPublicKey()), new LoanVerificationContract.Commands.SendForCreditApproval());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void bankStateMustHaveOneInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.input(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.output(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(credit.getParty().getOwningKey(), bank.getParty().getOwningKey()), new LoanVerificationContract.Commands.ReceiveCreditApproval());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void creditMustHaveOneOutput() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.input(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.output(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(credit.getPublicKey(), bank.getPublicKey()), new LoanVerificationContract.Commands.ReceiveCreditApproval());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void transactionMustHaveOneInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices,tx -> {
            tx.input(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.output(LOANVERIFICATION_CONTRACT_ID, loanVerificationState);
            tx.command(ImmutableList.of(bank.getParty().getOwningKey(), credit.getParty().getOwningKey()), new LoanVerificationContract.Commands.ReceiveCreditApproval());
            tx.verifies();
            return null;
        });
    }

}
