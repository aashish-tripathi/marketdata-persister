package com.md.persisters.chronical;

import com.ashish.marketdata.avro.MarketPrice;
import com.md.persisters.Persister;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.BlockingQueue;

public class ChMarketPricePersister implements Persister<MarketPrice> {

    private volatile boolean running=true;
    private SingleChronicleQueue queue;
    private BlockingQueue<MarketPrice> marketPriceQueue;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChMarketPricePersister.class);

    public ChMarketPricePersister(String path, BlockingQueue<MarketPrice> marketPriceQueue) {
        this.queue = SingleChronicleQueueBuilder.binary(path).build();
        this.marketPriceQueue = marketPriceQueue;

        LOGGER.info("MarketPrice persister connected with {} ");
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
                LOGGER.info("persisted new marketprice to {}");
            }
        }
    }

}
