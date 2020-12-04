package com.md.persisters.chronical;

import com.ashish.marketdata.avro.MarketPrice;
import com.md.persisters.Persister;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ChMarketPricePersister implements Persister<MarketPrice> {

    private volatile boolean running=true;
    private SingleChronicleQueue queue;
    private BlockingQueue<MarketPrice> marketPriceQueue = new ArrayBlockingQueue<>(1024);;;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChMarketPricePersister.class);

    public ChMarketPricePersister(String path) {
        this.queue = SingleChronicleQueueBuilder.binary(path).build();
        new Thread(this).start();
        LOGGER.info("MarketPrice persistence started with {} ",queue);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void stop(boolean flag) {
        this.running=flag;
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
    public void run() {
        ExcerptAppender appender = queue.acquireAppender();
        while (isRunning()){
            MarketPrice marketPrice = marketPriceQueue.poll();
            if(marketPrice!=null){
                appender.writeText(marketPrice.toString());
                LOGGER.info("Added new marketprice to {}", queue);
            }
        }
    }

}
