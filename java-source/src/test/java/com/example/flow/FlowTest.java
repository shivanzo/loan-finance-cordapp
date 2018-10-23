package com.example.flow;

import com.example.state.LoanRequestState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


import java.util.List;

import static org.junit.Assert.assertEquals;

public class FlowTest {


    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;
    private StartedMockNode nodeC;

    private static int amount = -1;
    private static String companyName = "Boeing Company";

    @Before
    public void setup() {
        network = new MockNetwork(ImmutableList.of("com.example.contract"));
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        nodeC = network.createPartyNode(null);
        for (StartedMockNode node : ImmutableList.of(nodeA, nodeB,nodeC)) {
            node.registerInitiatedFlow(RequestForLoanFlow.Acceptor.class);
        }
        network.runNetwork();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputTheInputIOU() throws Exception {
        RequestForLoanFlow.Initiator flow = new RequestForLoanFlow.Initiator( nodeB.getInfo().getLegalIdentities().get(0), amount,companyName);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(nodeA, nodeB)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (((List) txOutputs).size() == 1);

            LoanRequestState recordedState = (LoanRequestState) txOutputs.get(0).getData();
            assertEquals(recordedState.getFinanceNode(), nodeA.getInfo().getLegalIdentities().get(0));
            assertEquals(recordedState.getBankNode(), nodeB.getInfo().getLegalIdentities().get(0));
        }
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }


    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        RequestForLoanFlow.Initiator flow = new RequestForLoanFlow.Initiator( nodeB.getInfo().getLegalIdentities().get(0), amount,companyName);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(nodeA, nodeB)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        RequestForLoanFlow.Initiator flow = new RequestForLoanFlow.Initiator( nodeB.getInfo().getLegalIdentities().get(0), amount,companyName);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(nodeA.getInfo().getLegalIdentities().get(0).getOwningKey());
    }


    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        RequestForLoanFlow.Initiator flow = new RequestForLoanFlow.Initiator( nodeB.getInfo().getLegalIdentities().get(0), amount,companyName);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(nodeB.getInfo().getLegalIdentities().get(0).getOwningKey());
    }


}
