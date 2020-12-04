package com.md.persisters.chronical;

import com.ashish.marketdata.avro.Trade;
import com.md.persisters.Persister;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ChTradePersister implements Persister<Trade> {

    private volatile boolean running=true;
    private SingleChronicleQueue queue;
    private BlockingQueue<Trade>  tradeQueue = new ArrayBlockingQueue<>(1024);

    private static final Logger LOGGER = LoggerFactory.getLogger(ChTradePersister.class);

    public ChTradePersister(String path) {
        this.queue = SingleChronicleQueueBuilder.binary(path).build();
        new Thread(this).start();
        LOGGER.info("Trade persistence started with {} ",queue);
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
            Trade marketPrice = tradeQueue.poll();
            if(marketPrice!=null){
                appender.writeText(marketPrice.toString());
                LOGGER.info("added new trade to {}",queue);
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
