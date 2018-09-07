/**** @author : Shivan Sawant  *****/
package com.example.api;

import com.example.flow.BankAndFinanceFlow;
import com.example.flow.BankCreditAgencyFlow;
import com.example.flow.CreditAgencyBankNotificationFlow;
import com.example.flow.FinanceFlow;
import com.example.schema.IOUSchemaV1;
import com.example.state.BankAndCreditState;
import com.example.state.FinanceAndBankState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("example")
public class ExampleApi {

    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;
    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(ExampleApi.class);

    public ExampleApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }
    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }


    /* by Shivan Sawant */
    @GET
    @Path("initiateLoan-query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFinacneBankQuery() {
        System.out.println("Shivan Sawant : "+rpcOps.vaultQuery(FinanceAndBankState.class).getStates() );
       return Response.status(200).entity(rpcOps.vaultQuery(FinanceAndBankState.class).getStates()).build();
    }

    @GET
    @Path("sendApprove-query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBankAndCreditQuery() {
        System.out.println("Shivan Sawant : "+rpcOps.vaultQuery(BankAndCreditState.class).getStates() );
        return Response.status(200).entity(rpcOps.vaultQuery(BankAndCreditState.class).getStates()).build();
    }

    @GET
    @Path("creditresponse-query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCreditBankQuery() {
        System.out.println("Shivan Sawant : "+rpcOps.vaultQuery(BankAndCreditState.class).getStates() );
        return Response.status(200).entity(rpcOps.vaultQuery(BankAndCreditState.class).getStates()).build();
    }

    @GET
    @Path("loanresponse-query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBankFinanceQuery() {

        System.out.println("Shivan Sawant : "+rpcOps.vaultQuery(FinanceAndBankState.class).getStates() );
        return Response.status(200).entity(rpcOps.vaultQuery(FinanceAndBankState.class).getStates()).build();
    }

    /* by Shivan Sawant */
    /*******start of put request for query param. Shivan Sawant.***/
    @PUT
    @Path("create-loan")
    public Response loanRequest(@QueryParam("company") String company, @QueryParam("value")int value ,@QueryParam("partyName") CordaX500Name bankNode) throws InterruptedException, ExecutionException {

        if (value <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'Amount' must be non-negative.\n").build();
        }

        if (bankNode == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }

        if(company == null) {
            return Response.status(BAD_REQUEST).entity("Company name is missing. \n").build();
        }
        System.out.println("Type 1 pass");
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(bankNode);
        System.out.println("Type 2 pass");

        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + bankNode + "cannot be found.\n").build();
        }
        System.out.println("Type 3 pass");

        try {
            FinanceFlow.Initiator initiator = new FinanceFlow.Initiator(value,otherParty,company);
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(initiator.getClass(), value, otherParty,company)
                    .getReturnValue()
                    .get();

            System.out.println("Type 4 pass");
            System.out.println("Current linear State : "+initiator.getLinearId());
            final String msg = String.format("BAJAJ FINSERV FINANCE. \n Transaction id %s  is successfully committed to ledger.\n ", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();
        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    /* by Shivan Sawant */
    @PUT
    @Path("send-loanEligibilityCheck")
    public Response loanEligibilityCheck(@QueryParam("company") String company, @QueryParam("value")int value ,@QueryParam("partyName") CordaX500Name creditAgencyNode,@QueryParam("linearId") String financeBankStateLinearId) throws InterruptedException, ExecutionException {

        if (value <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'Amount' must be non-negative.\n").build();
        }

        if (creditAgencyNode == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }

        if(company == null) {
            return  Response.status(BAD_REQUEST).entity("Company name is missing . \n").build();
        }

        if(financeBankStateLinearId == null) {
            return Response.status(BAD_REQUEST).entity("linear id of FinanceAndBank State is missing . \n").build();
        }

        System.out.println("Type 1 pass");
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(creditAgencyNode);
        System.out.println("Type 2 pass");
        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + creditAgencyNode + "cannot be found.\n").build();
        }
        System.out.println("Type 3 pass");
        UniqueIdentifier linearIdFinanceState = new UniqueIdentifier();
        UniqueIdentifier uuidFinanceState = linearIdFinanceState.copy("",UUID.fromString(financeBankStateLinearId));
        System.out.println("Actual Linear Id : "+uuidFinanceState);

        try {
            BankCreditAgencyFlow.Initiator initiator = new BankCreditAgencyFlow.Initiator(value,otherParty,company,uuidFinanceState);
            System.out.println("Before Type 4 Passes");
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(BankCreditAgencyFlow.Initiator.class, value, otherParty,company,uuidFinanceState)
                    .getReturnValue()
                    .get();

            System.out.println("Type 4 pass");
            System.out.println("linearId fetched : "+initiator.getLinearIdRequestForLoan() != null ? initiator.getLinearIdRequestForLoan() :"");
            final String msg = String.format("STANDARD CHARTERED BANK Response.\n Transaction id %s  is successfully committed to ledger. \n", signedTx.getId() +" \n" + "The Application Id (linear id) for loan is : "+initiator.getLinearIdRequestForLoan());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    /** code for creditAgency and Bank
     *
     */
    @PUT
    @Path("credit-response")
    public Response creditAgencyResponse(@QueryParam("company") String company, @QueryParam("value")int value ,@QueryParam("partyName") CordaX500Name partyName, @QueryParam("financeLinearid") String financeBankStateLinearId, @QueryParam("bankLinearid") String bankCreditStateLinearId) throws InterruptedException, ExecutionException {

        System.out.println("partyName : "+partyName);
        if (value <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'Amount' must be non-negative.\n").build();
        }

        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }
        System.out.println("Type 1 pass");
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);
        System.out.println("Type 2 pass");

        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
        }

        if(company == null) {
            return Response.status(BAD_REQUEST).entity("Company name should not be null. \n").build();
        }

        if(financeBankStateLinearId == null) {
            return Response.status(BAD_REQUEST).entity("linear id of FinanceAndBank State is missing . \n").build();
        }

        if(bankCreditStateLinearId == null) {
            return Response.status(BAD_REQUEST).entity("linear id of previous unconsumed state cannot be empty. \n").build();
        }

        UniqueIdentifier linearIdFinanceState = new UniqueIdentifier();
        UniqueIdentifier linearIdBankState = new UniqueIdentifier();
        UniqueIdentifier uuidFinanceState = linearIdFinanceState.copy(" ",UUID.fromString(financeBankStateLinearId));
        UniqueIdentifier uuidBankState = linearIdBankState.copy(" ",UUID.fromString(bankCreditStateLinearId));

        System.out.println("Type 3 pass");

        try {
            CreditAgencyBankNotificationFlow.Initiator initiator = new CreditAgencyBankNotificationFlow.Initiator(value,otherParty,company,uuidFinanceState,uuidBankState);
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(initiator.getClass(), value, otherParty,company,uuidFinanceState,uuidBankState)
                    .getReturnValue()
                    .get();

            System.out.println("Type 4 pass");
            System.out.println("Previous  Finance Linear Id is  : "+initiator.getLinearIdFinanceState()!=null ? initiator.getLinearIdFinanceState() :"");
            final String msg = String.format("BANK BAZAR CREDIT RATING AGENCY Reponse.\n Transaction id %s is successfully committed to ledger. \n", signedTx.getId() + " "+ "The Loan Applicaiton id (linear id) is  : "+initiator.getLinearIdFinanceState());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    @PUT
    @Path("ackwd-finance")
    public Response bankLoanConfirmation(@QueryParam("company") String company, @QueryParam("value")int value ,@QueryParam("partyName") CordaX500Name partyName,@QueryParam("linearid") String financeBankStateLinearId) throws InterruptedException, ExecutionException {
        System.out.println("partyName : "+partyName);

        if (value <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'Amount' must be non-negative.\n").build();
        }

        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }

        if(company == null) {
            return Response.status(BAD_REQUEST).entity("Company name should not be null. \n").build();
        }

        if(financeBankStateLinearId == null) {
            return Response.status(BAD_REQUEST).entity("linear id of FinanceAndBank State is missing . \n").build();
        }

        System.out.println("Type 1 pass");
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);
        System.out.println("Type 2 pass");

        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
        }
        System.out.println("Type 3 pass");

        UniqueIdentifier linearIdFinanceState = new UniqueIdentifier();
        UniqueIdentifier uuidFinanceState = linearIdFinanceState.copy(" ",UUID.fromString(financeBankStateLinearId));

        try {
            BankAndFinanceFlow.Initiator initiator = new BankAndFinanceFlow.Initiator(value,otherParty,company,uuidFinanceState);
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(initiator.getClass(), value, otherParty,company,uuidFinanceState)
                    .getReturnValue()
                    .get();

            System.out.println("Type 4 pass");
            System.out.println("Previous  Finance Linear Id is  : "+initiator.getLinearIdFinance());
            final String msg = String.format("Loan Application Response from STANDARD CHARTERED BANK. \n Transaction id %s is sucessfully  committed to ledger.\n", signedTx.getId() +" "+ " Application id is : "+initiator.getLinearIdFinance());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }


    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot.stream().map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * Displays all states that exist in the node's vault.
     */
    @GET
    @Path("states")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<FinanceAndBankState>> getIOUs() {
        return rpcOps.vaultQuery(FinanceAndBankState.class).getStates();
    }


	/**
     * Displays all IOU states that are created by Party.
     */
    @GET
    @Path("my-ious")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyIOUs() throws NoSuchFieldException {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        Field lender = IOUSchemaV1.PersistentIOU.class.getDeclaredField("lender");
        CriteriaExpression lenderIndex = Builder.equal(lender, myLegalName.toString());
        QueryCriteria lenderCriteria = new QueryCriteria.VaultCustomQueryCriteria(lenderIndex);
        QueryCriteria criteria = generalCriteria.and(lenderCriteria);
        List<StateAndRef<FinanceAndBankState>> results = rpcOps.vaultQueryByCriteria(criteria,FinanceAndBankState.class).getStates();
        return Response.status(OK).entity(results).build();
    }
}
