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

public class ContractTest {
    static private final MockServices ledgerServices = new MockServices();
    int amount = 15000;
    String companyName = "Persistent";
    UniqueIdentifier uniquIdentifier = null;
    static private TestIdentity finance = new TestIdentity(new CordaX500Name("finance", "London", "GB"));
    static private TestIdentity bank = new TestIdentity(new CordaX500Name("bank", "New York", "US"));
    static private TestIdentity credit = new TestIdentity(new CordaX500Name("credit", "Paris", "FR"));
    private FinanceAndBankState financeBankState = new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,uniquIdentifier);

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

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(FINANCE_CONTRACT_ID, financeBankState);
                tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.command(ImmutableList.of(finance.getParty().getOwningKey(), bank.getParty().getOwningKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("No inputs should be consumed when issuing .");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveOneOutput() {

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.output(FINANCE_CONTRACT_ID, financeBankState);
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void lenderMustSignTransaction() {

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(finance.getPublicKey(), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void borrowerMustSignTransaction() {

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(bank.getPublicKey(), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void lenderIsNotBorrower() {

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FINANCE_CONTRACT_ID, new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()),  new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("The lender and the borrower cannot be the same entity.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cannotCreateNegativeValueIOUs() {

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FINANCE_CONTRACT_ID,new FinanceAndBankState(finance.getParty(), bank.getParty(), companyName,amount,new UniqueIdentifier(),false,new UniqueIdentifier()));
                tx.command(ImmutableList.of(finance.getPublicKey(), bank.getPublicKey()), new FinanceContract.Commands.InitiateLoan());
                tx.failsWith("The IOU's value must be non-negative.");
                return null;
            });
            return null;
        }));
    }
}
