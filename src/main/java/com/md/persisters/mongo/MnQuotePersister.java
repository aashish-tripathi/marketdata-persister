package com.md.persisters.mongo;

import com.ashish.marketdata.avro.Quote;
import com.md.persisters.Persister;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MnQuotePersister implements Persister<Quote> {

    private volatile boolean running = true;
    private final String dbUrl;
    private final String dbName;
    private String collectionName;
    private MongoDatabase mongoDb;
    private BlockingQueue<Quote>  blockingQueue = new ArrayBlockingQueue<>(1024);;

    private static final Logger LOGGER = LoggerFactory.getLogger(MnQuotePersister.class);

    public MnQuotePersister(String dbUrl, String dbName, String collectionName) {
        this.dbUrl = dbUrl;
        this.dbName = dbName;
        this.collectionName = collectionName;
        mongoDb = connect(dbUrl, dbName);
        new Thread(this).start();
        LOGGER.info("Quote persistence started with {} ",mongoDb);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void stop(boolean flag) {
        this.running=false;
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
        while (isRunning()) {
            Quote quote = blockingQueue.poll();
            if (quote != null) {
                MongoCollection<Document> quoteCollection = mongoDb.getCollection(collectionName).withWriteConcern(WriteConcern.MAJORITY).withReadPreference(ReadPreference.primaryPreferred());
                quoteCollection.insertOne(new Document("time", quote.getTime().longValue()).append("bidsize", quote.getBidsize().longValue())
                                                      .append("bidprice", quote.getBidprice().doubleValue()).append("asksize", quote.getAsksize().longValue())
                                                      .append("askprice", quote.getAskprice().doubleValue()).append("symbol", quote.getSymbol().toString())
                                                      .append("exchange", quote.getExchange().toString()));
                LOGGER.info("added new quote to {}", collectionName);

            }
        }
    }

    public static MongoDatabase connect(final String url, final String dbName) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }

}
