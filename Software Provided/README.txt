
AutoInSure Web Service
Paulo Ferreira <paulofe@ifi.uio.no>

AutoInSure Web Service
Miguel Ribeiro <jose.miguel.ribeiro@tecnico.ulisboa.pt>

Based on:
Rodrigo Bruno <rbruno@gsd.inesc-id.pt>
Nuno Santos <nuno.santos@inesc-id.pt>


Instructions to run the project
------------------------------------------

This project contains AutoInSureWSServer.
It implements a set of Web Services. 

To test this project, you need to install Eclipse for Java for EE Developers

Next, start by opening the AutoInSureWSServer project with Eclipse JAVA EE for Developers. 
Then, all you have to do is to build and run the project. To make sure that the Web 
Service is up, open a browser and visit the following URL:

	http://localhost:8080/AutoInSureWS?WSDL


Note 1: You can change the file data/insuredata.json to load the database with different data.
Note 2: If the changes in the DB file data/insuredata.json don't show in the application, make sure you configured the "DATAFILE" variable in DataProvider.java correctly
Note 3: If you want to run the system without the DB file you can change the flag MEMORYSOURCE in DataProvider.java to only store the changes in memory, and not storing them in the file. (this resets the DB to the DataProvider defaults everytime you start the server)