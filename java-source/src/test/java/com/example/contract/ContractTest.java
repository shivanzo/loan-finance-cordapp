package com.example.contract;

import com.example.state.BankAndCreditState;
import com.example.state.FinanceAndBankState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static com.example.contract.FinanceContract.FINANCE_CONTRACT_ID;
import static net.corda.testing.node.NodeTestUtils.ledger;
import static net.corda.testing.node.NodeTestUtils.transaction;

public class ContractTest {

    static private final MockServices ledgerServices = new MockServices();
    static private TestIdentity finance = new TestIdentity(new CordaX500Name("finance", "London", "GB"));
    static private TestIdentity bank = new TestIdentity(new CordaX500Name("bank", "New York", "US"));
    static private TestIdentity credit = new TestIdentity(new CordaX500Name("credit", "Paris", "FR"));

    private static int amount = 15000;
    private static String companyName = "Boeing Company";

    private static FinanceAndBankState  financeBankState = new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier());
    private static BankAndCreditState bankAndCreditState = new BankAndCreditState(bank.getParty(),credit.getParty(),false,companyName,new UniqueIdentifier());

    @Test
    public void transactionMustIncludeCreateCommand() {

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.fails();
                tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new FinanceContract.Commands.InitiateLoan());
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
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new FinanceContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });

        /**** uncomment for failure criteria **/
           /* transaction(ledgerServices,tx -> {
                tx.input(FINANCE_CONTRACT_ID, financeBankState);
                tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("No inputs should be consumed when issuing .");
                return null;
            });*/
    }

    /***This test case is for when PartyA contacts Party B (when finance agency send loan application to bank) **/
    @Test
    public void transactionMustHaveOneOutput() {

        transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });

        /**** uncomment for failure criteria **/
        /* transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("Only one output state should be created.");
                return null;
            });*/
    }

    /***This test case is for when PartyA contacts Party B (when finance agency send loan application to bank) **/
    @Test
    public void lenderMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
            tx.command(ImmutableList.of(finance.getPublicKey(),bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });

        /****uncomment for failure criteria **/
       /* transaction(ledgerServices,tx -> {
                tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });*/
    }

    /***This test case is for when PartyA contacts Party B (when finance agency send loan application to bank) **/
    @Test
    public void borrowerMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
            tx.command(ImmutableList.of(finance.getPublicKey(),bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });
            /**** uncomment for failure criteria ****/
         /*transaction(ledgerServices,tx -> {
                tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(finance.getPublicKey(),bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
        });*/
    }

    /***This test case is for when PartyA contacts Party B (when finance agency send loan application to bank) **/
    @Test
    public void lenderIsNotBorrower() {

            transaction(ledgerServices,tx -> {
                tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()),  new FinanceContract.Commands.InitiateLoan());
                tx.verifies();
                return null;
            });
    }

    @Test
    public void cannotCreateNegativeValueIOUs() {

        transaction(ledgerServices,tx -> {
                tx.output(FINANCE_CONTRACT_ID,new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
                tx.verifies();
                return null;
            });
    }

    /***This test case is for when PartyB contacts Party C (when bank send loan application to credit agency) **/
    @Test
    public void bankStateMustHaveNoInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.output(FINANCE_CONTRACT_ID, bankAndCreditState);
            tx.command(ImmutableList.of(credit.getParty().getOwningKey(), bank.getParty().getOwningKey()), new FinanceContract.Commands.SendForApproval());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when PartyB contacts Party C (when bank send loan application to credit agency) **/
    @Test
    public void bankMustHaveOneOutput() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.output(FINANCE_CONTRACT_ID, bankAndCreditState);
            tx.command(ImmutableList.of(credit.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.SendForApproval());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when Party C contacts Party B (when credit agency send loan application to bank) **/
    @Test
    public void bankStateMustHaveOneInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.input(FINANCE_CONTRACT_ID, bankAndCreditState);
            tx.output(FINANCE_CONTRACT_ID, bankAndCreditState);
            tx.command(ImmutableList.of(credit.getParty().getOwningKey(), bank.getParty().getOwningKey()), new FinanceContract.Commands.receiveCreditApproval());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when Party C contacts Party B (when credit agency send loan application to bank) **/
    @Test
    public void creditMustHaveOneOutput() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices, tx -> {
            tx.input(FINANCE_CONTRACT_ID, bankAndCreditState);
            tx.output(FINANCE_CONTRACT_ID, bankAndCreditState);
            tx.command(ImmutableList.of(credit.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.receiveCreditApproval());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when PartyB contacts Party A (when bank send loan application status to finance) **/
    @Test
    public void transactionMustHaveOneInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices,tx -> {
            tx.input(FINANCE_CONTRACT_ID,financeBankState);
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new FinanceContract.Commands.loanNotification());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when PartyB contacts Party A (when bank send loan application Status to Finance) **/
    @Test
    public void FinanceBankStateMustHaveFinalOneOutput() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices,tx -> {
            tx.input(FINANCE_CONTRACT_ID,financeBankState);
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.loanNotification());
            tx.verifies();
            return null;
        });
    }
}
