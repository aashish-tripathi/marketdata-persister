package com.md.persisters.mongo;

import com.ashish.marketdata.avro.MarketPrice;

import com.md.persisters.Persister;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MnMarketPricePersister implements Persister<MarketPrice> {

    private volatile boolean running=true;
    private final String dbUrl;
    private final String dbName;
    private String collectionName;
    private MongoDatabase mongoDb;
    private BlockingQueue<MarketPrice> marketPriceQueue = new ArrayBlockingQueue<>(1024);;

    private static final Logger LOGGER = LoggerFactory.getLogger(MnMarketPricePersister.class);

    public MnMarketPricePersister(String dbUrl, String dbName, String collectionName) {
        this.dbUrl = dbUrl;
        this.dbName = dbName;
        this.collectionName = collectionName;
        mongoDb = connect(dbUrl, dbName);
        new Thread(this).start();
        LOGGER.info("MarketPrice persistence started with {} ",mongoDb);
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
            LOGGER.error("Thread has been interrupted ");
        }
    }

    @Override
    public void stop(boolean flag) {
        this.running=false;
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
                LOGGER.info("added new marketprice to {}", collectionName);
            }
            LOGGER.info("Queue is empty");
        }
    }

    public static MongoDatabase connect(final String url, final String dbName) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }

}
