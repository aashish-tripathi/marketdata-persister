package com.md.persisters.chronical;

import com.ashish.marketdata.avro.MarketPrice;
import com.ashish.marketdata.avro.Quote;
import com.md.persisters.Persister;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class ChQuotePersister implements Persister<Quote> {

    private volatile boolean running=true;
    private SingleChronicleQueue queue;
    private BlockingQueue<Quote> marketPriceQueue;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChQuotePersister.class);

    public ChQuotePersister(String path, BlockingQueue<Quote> marketPriceQueue) {
        this.queue = SingleChronicleQueueBuilder.binary(path).build();
        this.marketPriceQueue = marketPriceQueue;

        LOGGER.info("MarketPrice persister connected with {} ");
        new Thread(this).start();
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        ExcerptAppender appender = queue.acquireAppender();
        while (isRunning()){
            Quote marketPrice = marketPriceQueue.poll();
            if(marketPrice!=null){
                appender.writeText(marketPrice.toString());
                LOGGER.info("persisted new quote to {}");
            }
        }
    }

    @Override
    public void addMarketData(Quote quote) {
        try {
            marketPriceQueue.put(quote);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LOGGER.error("Thread has been interrupted ");
        }
    }
}
