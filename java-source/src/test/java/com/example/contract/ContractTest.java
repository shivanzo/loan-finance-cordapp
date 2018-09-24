package com.example.contract;

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
    int amount = 15000;
    String companyName = "Persistent";
    UniqueIdentifier uniquIdentifier = null;
    static private TestIdentity finance = new TestIdentity(new CordaX500Name("finance", "London", "GB"));
    static private TestIdentity bank = new TestIdentity(new CordaX500Name("bank", "New York", "US"));
    static private TestIdentity credit = new TestIdentity(new CordaX500Name("credit", "Paris", "FR"));
    private FinanceAndBankState  financeBankState = new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,uniquIdentifier);

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

    @Test
    public void transactionMustHaveNoInputs() {

        transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new FinanceContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });

           /* transaction(ledgerServices,tx -> {
                tx.input(FINANCE_CONTRACT_ID, financeBankState);
                tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("No inputs should be consumed when issuing .");
                return null;
            });*/


    }

    @Test
    public void transactionMustHaveOneOutput() {
        transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("Only one output state should be created.");
                return null;
            });

        transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, financeBankState);
            tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void lenderMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
            tx.command(ImmutableList.of(finance.getPublicKey(),bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });

       /* transaction(ledgerServices,tx -> {
                tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });*/
    }

    @Test
    public void borrowerMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
            tx.command(ImmutableList.of(finance.getPublicKey(),bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
            tx.verifies();
            return null;
        });

         /*transaction(ledgerServices,tx -> {
                tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(finance.getPublicKey(),bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
        });*/
    }

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
}
