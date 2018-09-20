Author : Shivan Sawant
Company : Persistent Systems

http://localhost:10009/api/loans/loanapplications?company=Persistent&value=10000&partyName=O=Bank,L=New York,C=US

http://localhost:10012/api/loans/bankapplications?company=Persistent&partyName=O=Credit Rating Agency,L=Paris,C=FR&linearId=1101b51f-ba43-4a60-bf45-18f166f7a540         (Bank to credit Agency)

http://localhost:10015/api/loans/creditresponses?company=Persistent&partyName=O=Bank,L=New York,C=US&financeLinearid=1101b51f-ba43-4a60-bf45-18f166f7a540&bankLinearid=2fb83418-b11a-4729-99c9-df8cf8018ae3

(Credit Agency to Bank)

http://localhost:10012/api/loans/financeacknowledgments?company=Persistent&value=10000&partyName=O=Finance Agency Of London,L=London,C=GB&linearid=1101b51f-ba43-4a60-bf45-18f166f7a540&linearIdBank=2fb83418-b11a-4729-99c9-df8cf8018ae3

(Bank to Finance)

10009 : PARTY A Port (Finance Agency)
10012 : PARTY B Port (Bank)
10015 : PARTY C Port (Credit Rating Agency)

