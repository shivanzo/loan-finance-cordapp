package com.example.api;

import com.example.flow.BankAndFinanceFlow;
import com.example.flow.BankCreditAgencyFlow;
import com.example.flow.CreditAgencyBankNotificationFlow;
import com.example.flow.FinanceFlow;
import com.example.state.BankAndCreditState;
import com.example.state.FinanceAndBankState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.bean.DataBean;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

@Path("loans")
public class LoanFinanceApi {

    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;
    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(LoanFinanceApi.class);

    public LoanFinanceApi(CordaRPCOps rpcOps) {
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
    @Path("finance")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFinacneBankQuery() {
        System.out.println("VaultQuery : "+rpcOps.vaultQuery(FinanceAndBankState.class).getStates() );
       return Response.status(200).entity(rpcOps.vaultQuery(FinanceAndBankState.class).getStates()).build();
    }

    @GET
    @Path("bank")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBankAndCreditQuery() {
        System.out.println("VaultQuery : "+rpcOps.vaultQuery(BankAndCreditState.class).getStates() );
        return Response.status(200).entity(rpcOps.vaultQuery(BankAndCreditState.class).getStates()).build();
    }

    @GET
    @Path("credit")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCreditBankQuery() {
        System.out.println("VaultQuery : "+rpcOps.vaultQuery(BankAndCreditState.class).getStates() );
        return Response.status(200).entity(rpcOps.vaultQuery(BankAndCreditState.class).getStates()).build();
    }

    /*******start of put request for query param. Shivan Sawant.***/
    @POST
    @Path("loanapplications")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response loanRequest(DataBean detail) throws InterruptedException, ExecutionException {

        CordaX500Name bankNode=detail.getPartyName();
        int value=detail.getValue();
        String company=detail.getCompany();

        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(bankNode);

        if (bankNode == null) {
            return Response.status(BAD_REQUEST).entity("parameter 'partyName' missing or has wrong format.\n").build();
        }

        if (value <= 0) {
            return Response.status(BAD_REQUEST).entity(" parameter 'Amount' must be non-negative.\n").build();
        }

        if(company == null) {
            return Response.status(BAD_REQUEST).entity("Company name is missing. \n").build();
        }

        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + bankNode + "cannot be found.\n").build();
        }

        try {
            FinanceFlow.Initiator initiator = new FinanceFlow.Initiator(value,otherParty,company);
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(initiator.getClass(), value, otherParty,company)
                    .getReturnValue()
                    .get();

            System.out.println("Current linear State : "+initiator.getLinearId());
            final String msg = String.format("FINANCE AGENCY OF WALES. \n Transaction id %s  is successfully committed to ledger.\n ", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();
        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    @POST
    @Path("bankapplications")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response loanEligibilityCheck(DataBean dataBean) throws InterruptedException, ExecutionException {

        String company = dataBean.getCompany();
        CordaX500Name creditAgencyNode = dataBean.getPartyName();
        String financeBankStateLinearId = dataBean.getFinanceLinearId();
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(creditAgencyNode);

        if (creditAgencyNode == null) {
            return Response.status(BAD_REQUEST).entity("parameter 'partyName' missing or has wrong format.\n").build();
        }

        if(company == null) {
            return  Response.status(BAD_REQUEST).entity("Company name is missing . \n").build();
        }

        if(financeBankStateLinearId == null) {
            return Response.status(BAD_REQUEST).entity("linear id of FinanceAndBank State is missing . \n").build();
        }

        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + creditAgencyNode + "cannot be found.\n").build();
        }

        UniqueIdentifier linearIdFinanceState = new UniqueIdentifier();
        UniqueIdentifier uuidFinanceState = linearIdFinanceState.copy(null,UUID.fromString(financeBankStateLinearId));

        try {
            BankCreditAgencyFlow.Initiator initiator = new BankCreditAgencyFlow.Initiator(otherParty,company,uuidFinanceState);
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(BankCreditAgencyFlow.Initiator.class, otherParty,company,uuidFinanceState)
                    .getReturnValue()
                    .get();

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
    @POST
    @Path("creditresponses")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response creditAgencyResponse(DataBean dataBean) throws InterruptedException, ExecutionException {

        String company = dataBean.getCompany();
        CordaX500Name partyName = dataBean.getPartyName();
        String financeBankStateLinearId = dataBean.getFinanceLinearId();
        String bankCreditStateLinearId = dataBean.getBankLinearId();

        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);

        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity(" parameter 'partyName' missing or has wrong format.\n").build();
        }

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

        System.out.println("Vault Query Finance State : "+rpcOps.vaultQuery(FinanceAndBankState.class).getStates() );
        UniqueIdentifier linearIdFinanceState = new UniqueIdentifier();
        UniqueIdentifier linearIdBankState = new UniqueIdentifier();
        UniqueIdentifier uuidFinanceState = linearIdFinanceState.copy(null,UUID.fromString(financeBankStateLinearId));
        UniqueIdentifier uuidBankState = linearIdBankState.copy(null,UUID.fromString(bankCreditStateLinearId));

        try {
            CreditAgencyBankNotificationFlow.Initiator initiator = new CreditAgencyBankNotificationFlow.Initiator(otherParty,company,uuidFinanceState,uuidBankState);
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(initiator.getClass(),otherParty,company,uuidFinanceState,uuidBankState)
                    .getReturnValue()
                    .get();

            final String msg = String.format("LONDON CREDIT RATING AGENCY Reponse.\n Transaction id %s is successfully committed to ledger. \n", signedTx.getId() + " "+ "The Loan Applicaiton id (linear id) is  : "+initiator.getLinearIdFinanceState());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    @POST
    @Path("financeacknowledgments")
    public Response bankLoanConfirmation(DataBean dataBean) throws InterruptedException, ExecutionException {

        String company = dataBean.getCompany();
        int value = dataBean.getValue();
        CordaX500Name partyName = dataBean.getPartyName();
        String financeBankStateLinearId = dataBean.getFinanceLinearId();
        String bankAndCreditStateLinearId = dataBean.getBankLinearId();

        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);

        if (value <= 0) {
            return Response.status(BAD_REQUEST).entity(" parameter 'Amount' must be non-negative.\n").build();
        }

        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity(" parameter 'partyName' missing or has wrong format.\n").build();
        }

        if(company == null) {
            return Response.status(BAD_REQUEST).entity("Company name should not be null. \n").build();
        }

        if(financeBankStateLinearId == null) {
            return Response.status(BAD_REQUEST).entity("linear id of FinanceAndBank State is missing . \n").build();
        }

        if(bankAndCreditStateLinearId == null) {
            return Response.status(BAD_REQUEST).entity("linear id of bank is needed for audit . \n").build();
        }

        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
        }

        UniqueIdentifier linearIdFinanceState = new UniqueIdentifier();
        UniqueIdentifier linearIdBankState = new UniqueIdentifier();
        UniqueIdentifier uuidFinanceState = linearIdFinanceState.copy(null,UUID.fromString(financeBankStateLinearId));
        UniqueIdentifier uuidBankState = linearIdBankState.copy(null,UUID.fromString(bankAndCreditStateLinearId));

        try {
            BankAndFinanceFlow.Initiator initiator = new BankAndFinanceFlow.Initiator(value,otherParty,company,uuidFinanceState,uuidBankState);
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(initiator.getClass(), value, otherParty,company,uuidFinanceState,uuidBankState)
                    .getReturnValue()
                    .get();

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
}
