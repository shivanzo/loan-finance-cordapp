# The Finance-Loan Application

This simple CorDapp provides the facility for un-trusting parties to interact and transact with one another to reach a final state of consensus where all parties can trust in the outcome without trusting one another and without the need for expensive out of band reconciliation.
This application shows how finance agencies(A third party which provides loan to individuals), Banks and credit rating agencies can use corda DLT for Loan lending process. Finance agency usually finances loans for the people.Finance agency forwards the loan application to the bank for loan. Bank internally contacts credit rating agency to check the eligibility of the applicant for the loan. Based on the feedback provided by credit rating agency, bank decides to approve or reject the loan application sent by the finance agency. Communication between Bank and credit rating agency or any data exchanged between Bank and credit rating agency remains private to Finance Agency. Finance agency does not have visibility what Bank and Credit rating agency does. This corDapp has 3 parties 

1. Finance Agency (A third party which provides personal loans to individual)
2. Banks (Bank sends the loan application to credit agency to check the credit worthyness of the loan application)
3. Credit rating agency (A third party which checks the credit worthiness of loan applicantion).

## CorDapp structure
*	There are 3 Parties and 1 Notary.
* There are 2 Linear states 1) LoanRequestState 2) LoanVerificationState
*	Party A is a Finance Agency which sends the loan application to the bank 
*	Party B is a Bank which Lends/Approves the loan 
*	Party C is a Credit Rating Agency which checks the credit eligibility of the applicant/institution in the loan application. 

## CorDapp flow
*	Finance Agency sends the loan application to the bank which contains individual's or company's name and the loan amount.
*	Bank will receive the application and forward it to Credit rating agency to check the eligibility of loan application. 
*	Credit rating agency will respond back to bank with the eligibility of the loan application as a response.
*	Bank will receive it and decide whether to lend the loan depending upon the positive or negative review from credit agency.
*	Use API endpoints to initiate flow using REST API.

## Minimum System Requirements
* 16 GB RAM preferably
* Latest version of JAVA 8 java 8u181 (Preferably, Corda and kotlin support latest version of java 8)
* http://docs.corda.r3.com/sizing-and-performance.html 

## Instructions for setting up
1. clone the repository https://github.com/shivanzo/Finance-Assignment-Final-Phase
2. To build on unix : ./gradlew deployNodes
3. To build on windows : gradlew.bat deployNodes
4. For running corDapp on unix ./runnodes --log-to-console --logging-level=DEBUG
5. For running corDapp on windows runnodes.bat --log-to-console --logging-level=DEBUG
6. Good to run in intellij

## Accessing over API endpoints 

| Node                  |    Port         |
| --------------------- | --------------- | 
| FinanceAgency         | localhost:10009 |
| Bank                  | localhost:10012 |      
| Credit Rating Agency  | localhost:10015 |   





