package com.md.persisters;

import com.ashish.marketdata.avro.MarketPrice;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class MarketPricePersister implements Persister<MarketPrice>{

    private volatile boolean running=true;
    private final String dbUrl;
    private final String dbName;
    private String collectionName;
    private MongoDatabase mongoDb;
    private BlockingQueue<MarketPrice> marketPriceQueue;

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketPricePersister.class);

    public MarketPricePersister(String dbUrl,String dbName,String collectionName, BlockingQueue<MarketPrice> marketPriceQueue) {
        this.dbUrl = dbUrl;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.marketPriceQueue = marketPriceQueue;
        mongoDb = connect(MONGO_DB_URL, DB_NAME);
        LOGGER.info("MarketPrice persister connected with {} ",mongoDb);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void addMarketData(MarketPrice marketPrice) {
        try {
            marketPriceQueue.put(marketPrice);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LOGGER.error("Thread has been tnterrupted ");
        }
    }

    @Override
    public void run() {
        while (isRunning()){
            MarketPrice marketPrice = marketPriceQueue.poll();
            if(marketPrice!=null){
                MongoCollection<Document> collection = mongoDb.getCollection(collectionName).withWriteConcern(WriteConcern.MAJORITY).withReadPreference(ReadPreference.primaryPreferred());
                //courseCollection.insertOne(new Document("name", studentName).append("age", age).append("gpa", gpa));

            }
        }
    }

    public static MongoDatabase connect(final String url, final String dbName) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }

}
