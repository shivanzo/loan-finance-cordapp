package com.example.client;

import com.example.state.LoanRequestState;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.services.Vault;
import net.corda.core.utilities.NetworkHostAndPort;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.ExecutionException;

/**
 * Demonstration of using the CordaRPCClient to connect to a Corda Node and
 * steam some State data from the node.
 */
public class ExampleClientRPC {
    private static final Logger logger = LoggerFactory.getLogger(ExampleClientRPC.class);

    private static void logState(StateAndRef<LoanRequestState> state) {
        logger.info("{}", state.getState().getData());
    }

    public static void main(String[] args) throws ActiveMQException, InterruptedException, ExecutionException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ExampleClientRPC <node address>");
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);

        // Can be amended in the com.example.Main file.
        final CordaRPCOps proxy = client.start("user1", "test").getProxy();

        // Grab all existing and future states in the vault.
        final DataFeed<Vault.Page<LoanRequestState>, Vault.Update<LoanRequestState>> dataFeed = proxy.vaultTrack(LoanRequestState.class);
        final Vault.Page<LoanRequestState> snapshot = dataFeed.getSnapshot();
        final Observable<Vault.Update<LoanRequestState>> updates = dataFeed.getUpdates();

        // Log the 'placed' listen for new ones.
        snapshot.getStates().forEach(ExampleClientRPC::logState);
        updates.toBlocking().subscribe(update -> update.getProduced().forEach(ExampleClientRPC::logState));
    }
}
