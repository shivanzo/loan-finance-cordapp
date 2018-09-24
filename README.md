# LOAN-FINANCE 3-PARTY FLOW APPLICATION POC (JAVA)

This simple cordapp provides the facility for untrusting parties to interact and contract with one another to reach a final state of consensus where all parties can trust in the outcome without trusting one another and without the need for expensive out of band reconciliation.
This simple cordapp written purely in JAVA, shows how Finance Agencies(A third party which provides loan to individuals), Banks and credit rating agencies can use corda DLT for loan lending and processing of loan. This cordapp has 3 parties 
1. Finance Agency ( A third party which provides personal loans to individual) 
2. Banks 
3. Credit rating agency ( A third party which checks the loan eligibility of loan application/loan applicant.

## Cordapp structure
*	There are total of 3 Parties + 1 Notary.
* There are 2 Linear states 1) FinanceAndBankState 2) BankAndCreditState
*	Party A is a Finance Agency which sends the loan application to the bank 
*	Party B is a Bank which Lends/Approves the loan (port 10012 in my case)
*	Party C is a Credit Rating Agency which checks the credit eligibility of the applicant/institution in the loan application. 

## Cordapp flow
*	Finance Agency sends the loan application to the bank which contains individual name/company name and amount (Loan Amount).
*	Bank will receive the application and forward it to Credit rating agency to check the eligibility of loan applicant/ loan application (Example CIBIL score of applicant).
*	Credit rating agency will respond back to bank with the eligibility of the loan application.
*	Bank will receive it and decide whether to lend the loan or not by acknowledging its response to the Finance agency.
*	Use API endspoints to initiate flow using POSTMAN.

## NOTE: Communication between Bank and credit rating agency or any data exchanged between Bank and credit rating agency remains private to Finance Agency. Finance agency does not hear what Bank and Credit rating agency does.

* 10009 : PARTY A Port (Finance Agency)
* 10012 : PARTY B Port (Bank)
* 10015 : PARTY C Port (Credit Rating Agency)

## Minimum System Requirements
* 16 GB RAM (RAM is important to avoid BSOD and laggy system)
* Intel i5 and above processor
* latest version of JAVA 8 java 8u181 (Preferably, Corda and kotlin support latest version of java 8)
* Intellij 2018.1
* Postman (Chrome)
* http://docs.corda.r3.com/sizing-and-performance.html 

## Instructions for setting up
1. clone the repository
2. open it using IntelliJ IDEA 2018.1
3. do gradle build and run the application
4. initiate transactions using POSTMAN through api endpoints

## This Application is accessible only through APIs (REST API) and not UI.


