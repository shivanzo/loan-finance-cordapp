# email : shivansawant1992@gmail.com

# LOAN APPLICATION POC

Problem Statement
*	There is total of 3 Parties + 1 Notary.
*	Party A is a Finance Agency which sends the loan application to the bank
*	Party B is a Bank which Lends/Approves the loan
*	Party C is a Credit Rating Agency which checks the credit score of the applicant/Institution in the loan application.
*	Finance Agency should send the loan application to the bank which contains Individual name/company name and amount (Loan Amount).
*	Bank will receive the application and forward it to Credit rating agency to check the eligibility of loan applicant/ loan application (Example CIBIL score of applicant).
*	Credit rating agency will respond back to bank with the eligibility of the loan application.
*	Bank will receive it and decide whether to lend the loan or not by acknowledging its response to the Finance agency.
*	Write APIs using REST API to implement and initiate is flow using POSTMAN.

## NOTE: Communication between Bank and credit rating agency or any data exchanged between Bank and credit rating agency should remain private to Finance Agency. Finance agency should not hear what Bank and Credit rating agency does.


* http://localhost:10009/api/loanApp/create-loan?company=Persistent&value=10000&partyName=O=PartyB,L=New York,C=US  (Finance to bank)

* http://localhost:10012/api/loanApp/send-loanEligibilityCheck?company=Persistent&value=10000&partyName=O=PartyC,L=Paris,C=FR&linearId=2e62f0a6-646d-4be7-980e-1ea057979896          (Bank to credit Agency)

* http://localhost:10015/api/loanApp/credit-response?company=Persistent&value=10000&partyName=O=PartyB,L=New York,C=US&financeLinearid=2e62f0a6-646d-4be7-980e-1ea057979896&bankLinearid=8d67023b-9487-4d44-8c09-f8d32a84bb7c

(Credit Agency to Bank)

* http://localhost:10012/api/loanApp/ackwd-finance?company=Persistent&value=10000&partyName=O=PartyA,L=London,C=GB&linearid=(linear id of fina
(Bank to Finance)

* 10009 : PARTY A Port (Finance Agency)
* 10012 : PARTY B Port (Bank)
* 10015 : PARTY C Port (Credit Rating Agency)

## This Application is accessible only through APIs (REST API) and not UI.


