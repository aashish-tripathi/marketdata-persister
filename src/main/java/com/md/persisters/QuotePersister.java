package com.md.persisters;

import com.ashish.marketdata.avro.Quote;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class QuotePersister implements Persister<Quote>{

    private volatile boolean running=true;
    private final String dbUrl;
    private final String dbName;
    private String collectionName;
    private MongoDatabase mongoDb;
    private BlockingQueue<Quote> blockingQueue;

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotePersister.class);

    public QuotePersister(String dbUrl,String dbName,String collectionName, BlockingQueue<Quote> marketPriceQueue) {
        this.dbUrl = dbUrl;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.blockingQueue = marketPriceQueue;
        mongoDb = connect(MONGO_DB_URL, DB_NAME);
        LOGGER.info("Quote persister connected with {} ",mongoDb);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void addMarketData(Quote marketPrice) {
        try {
            blockingQueue.put(marketPrice);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LOGGER.error("Thread has been tnterrupted ");
        }
    }

    @Override
    public void run() {
        while (isRunning()){
            Quote quote = blockingQueue.poll();
            if(quote!=null){
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
