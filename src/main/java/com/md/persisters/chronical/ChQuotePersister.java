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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ChQuotePersister implements Persister<Quote> {

    private volatile boolean running=true;
    private SingleChronicleQueue queue;
    private BlockingQueue<Quote>  blockingQueue = new ArrayBlockingQueue<>(1024);;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChQuotePersister.class);

    public ChQuotePersister(String path) {
        this.queue = SingleChronicleQueueBuilder.binary(path).build();
        new Thread(this).start();
        LOGGER.info("Quote persistence started with {} ",queue);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void stop(boolean flag) {
        this.running=flag;
    }

    @Override
    public void run() {
        ExcerptAppender appender = queue.acquireAppender();
        while (isRunning()){
            Quote marketPrice = blockingQueue.poll();
            if(marketPrice!=null){
                appender.writeText(marketPrice.toString());
                LOGGER.info("Added new quote to {}", queue);
            }
        }
    }

    @Override
    public void addMarketData(Quote quote) {
        try {
            blockingQueue.put(quote);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LOGGER.error("Thread has been interrupted ");
        }
    }
}
