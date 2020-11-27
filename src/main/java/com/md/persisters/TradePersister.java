package com.md.persisters;

import com.ashish.marketdata.avro.Trade;
import com.md.receivers.TradeReceiver;
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

public class TradePersister implements Persister<Trade> {

    private volatile boolean running=true;
    private final String dbUrl;
    private final String dbName;
    private String collectionName;
    private MongoDatabase mongoDb;
    private BlockingQueue<Trade> tradeBlockingQueue;

    private static final Logger LOGGER = LoggerFactory.getLogger(TradePersister.class);

    public TradePersister(String dbUrl,String dbName,String collectionName, BlockingQueue<Trade> tradeBlockingQueue) {
        this.dbUrl = dbUrl;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.tradeBlockingQueue = tradeBlockingQueue;
        mongoDb = connect(dbUrl, dbName);
        LOGGER.info("Trade persister connected with {} ",mongoDb);
        new Thread(this).start();
    }

    public boolean isRunning() {
        return running;
    }

     @Override
    public void addMarketData(Trade trade) {
        try {
            tradeBlockingQueue.put(trade);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LOGGER.error("Thread has been interrupted ");
        }
    }

    @Override
    public void run() {
        while (isRunning()){
            Trade trade = tradeBlockingQueue.poll();
            if(trade!=null){
                MongoCollection<Document> tradeCollection = mongoDb.getCollection(collectionName).withWriteConcern(WriteConcern.MAJORITY).withReadPreference(ReadPreference.primaryPreferred());

                tradeCollection.insertOne(new Document("time", trade.getTime().longValue()).append("size", trade.getSize())
                                                       .append("price", trade.getPrice().doubleValue()).append("symbol", trade.getSymbol().toString())
                                                       .append("exchange", trade.getExchange().toString()));
                LOGGER.info("persisted new trade to {}", collectionName);
            }
        }
    }

    public static MongoDatabase connect(final String url, final String dbName) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }

}
