package com.md.persisters.mongo;

import com.ashish.marketdata.avro.MarketPrice;

import com.md.persisters.Persister;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class MnMarketPricePersister implements Persister<MarketPrice> {

    private volatile boolean running=true;
    private final String dbUrl;
    private final String dbName;
    private String collectionName;
    private MongoDatabase mongoDb;
    private BlockingQueue<MarketPrice> marketPriceQueue;

    private static final Logger LOGGER = LoggerFactory.getLogger(MnMarketPricePersister.class);

    public MnMarketPricePersister(String dbUrl, String dbName, String collectionName, BlockingQueue<MarketPrice> marketPriceQueue) {
        this.dbUrl = dbUrl;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.marketPriceQueue = marketPriceQueue;
        mongoDb = connect(dbUrl, dbName);
        LOGGER.info("MarketPrice persister connected with {} ",mongoDb);
        new Thread(this).start();
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
                MongoCollection<Document> marketPriceCollectio = mongoDb.getCollection(collectionName).withWriteConcern(WriteConcern.MAJORITY).withReadPreference(ReadPreference.primaryPreferred());
                //courseCollection.insertOne(new Document("name", studentName).append("age", age).append("gpa", gpa));
                marketPriceCollectio.insertOne(new Document(
                         "open", marketPrice.getOpen().doubleValue()).append("high", marketPrice.getHigh().doubleValue())
                        .append("low", marketPrice.getLow().doubleValue()).append("close", marketPrice.getClose().doubleValue())
                        .append("lastPrice", marketPrice.getLastPrice().doubleValue()).append("volume", marketPrice.getVolume().doubleValue())
                        .append("uperCircuit", marketPrice.getUperCircuit().doubleValue()).append("lowerCircuit", marketPrice.getLowerCircuit().doubleValue())
                        .append("lastTradeTime", marketPrice.getLastTradeTime().doubleValue()).append("lastTradeSize", marketPrice.getLastTradeSize().doubleValue())
                        .append("symbol", marketPrice.getSymbol().toString()).append("exchange", marketPrice.getExchange().toString()));
                LOGGER.info("persisted new marketprice to {}", collectionName);
            }
        }
    }

    public static MongoDatabase connect(final String url, final String dbName) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }

}
