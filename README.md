![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

Author : Shivan Sawant
Company : Persistent Systems

http://localhost:10009/api/loanApp/create-loan?company=Persistent&value=10000&partyName=O=PartyB,L=New York,C=US  (Finance to bank)

http://localhost:10012/api/loanApp/send-loanEligibilityCheck?company=Persistent&value=10000&partyName=O=PartyC,L=Paris,C=FR&linearId=2e62f0a6-646d-4be7-980e-1ea057979896          (Bank to credit Agency)

http://localhost:10015/api/loanApp/credit-response?company=Persistent&value=10000&partyName=O=PartyB,L=New York,C=US&financeLinearid=2e62f0a6-646d-4be7-980e-1ea057979896&bankLinearid=8d67023b-9487-4d44-8c09-f8d32a84bb7c

(Credit Agency to Bank)

http://localhost:10012/api/loanApp/ackwd-finance?company=Persistent&value=10000&partyName=O=PartyA,L=London,C=GB&linearid=2e62f0a6-646d-4be7-980e-1ea057979896

(Bank to Finance)

10009 : PARTY A Port (Finance Agency)
10012 : PARTY B Port (Bank)
10015 : PARTY C Port (Credit Rating Agency)

