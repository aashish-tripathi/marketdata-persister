# Market Data Persister
Following pre-requisites required to run this application

1 Linux/Ubuntu environment
2 Kafka/EMS installation in clustered mode
3 Mongo db installation in clustered mode

Mongo DB steps
 Install mongod in cluster mode https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/#install-mongodb-community-edition 
  a After mongo installation create separate data directory for each node under /var/lib/mongodb/ as mmentioned below
      /var/lib/mongodb/rs0-0
      /var/lib/mongodb/rs0-1
      /var/lib/mongodb/rs0-2
  b Start mongod and login to shell and run below (one time activity)
  rs.initiate(
  {
  _id:"rs0",
  members : [
         {
  	   _id : 0, 
  	   host : "127.0.0.1:27017"
  	   },
         {
  	   _id : 1,
  	   host : "127.0.0.1:27018"
  	   },
         {
  	   _id : 2, 
  	   host : "127.0.0.1:27019"
  	   }
     ]
  })
  c Now start all node one by one as given below
    sudo mongod --replSet "rs0" --bind_ip localhost --port 27017 --dbpath /var/lib/mongodb/rs0-0 --oplogSize 128
    
    sudo mongod --replSet "rs0" --bind_ip localhost --port 27018 --dbpath /var/lib/mongodb/rs0-1 --oplogSize 128
    
    sudo mongod --replSet "rs0" --bind_ip localhost --port 27019 --dbpath /var/lib/mongodb/rs0-2 --oplogSize 128
    
    
Kafka Steps
Setup cluster with port 9093, 9094 & 9095 with zookeeper on 2181 and create below topics   
exsim.nse.orders
exsim.nse.trades
exsim.nse.quotes
exsim.nse.marketprice
exsim.nse.marketbyprice

Start Persister.
