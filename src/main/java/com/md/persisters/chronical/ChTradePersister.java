package com.md.persisters.chronical;

import com.ashish.marketdata.avro.Trade;
import com.md.persisters.Persister;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class ChTradePersister implements Persister<Trade> {

    private volatile boolean running=true;
    private SingleChronicleQueue queue;
    private BlockingQueue<Trade> tradeQueue;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChTradePersister.class);

    public ChTradePersister(String path, BlockingQueue<Trade> tradeBlockingQueue) {
        this.queue = SingleChronicleQueueBuilder.binary(path).build();
        this.tradeQueue = tradeBlockingQueue;

        LOGGER.info("Trade persister connected with {} ", queue);
        new Thread(this).start();
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        ExcerptAppender appender = queue.acquireAppender();
        while (isRunning()){
            Trade marketPrice = tradeQueue.poll();
            if(marketPrice!=null){

                appender.writeText(marketPrice.toString());
                LOGGER.info("persisted new trade to {}",queue);
            }
        }
    }

    @Override
    public void addMarketData(Trade trade) {
        try {
            tradeQueue.put(trade);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LOGGER.error("Thread has been interrupted ");
        }
    }

}
