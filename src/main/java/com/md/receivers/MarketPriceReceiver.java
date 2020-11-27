package com.md.receivers;

import com.ashish.marketdata.avro.MarketPrice;
import com.md.brokers.EMSBroker;
import com.md.brokers.KafkaBroker;
import com.md.persisters.MarketPricePersister;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MarketPriceReceiver implements Runnable {

    private boolean kafka;
    private String topic;
    private MarketPricePersister persister;
    private EMSBroker emsBroker;
    private KafkaConsumer<String, String> kafkaConsumer;
    private volatile boolean running = true;
    private BlockingQueue<MarketPrice> quoteBlockingQueue;

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeReceiver.class);

    public MarketPriceReceiver(final String serverUrl, final String topic, final boolean kafka, String dbUrl, String dbName) throws JMSException {
        this.topic = topic;
        this.kafka = kafka;
        if (!kafka) {
            emsBroker = new EMSBroker(serverUrl, null, null);
            emsBroker.createConsumer(topic, true);
        } else {
            this.kafkaConsumer = new KafkaBroker(serverUrl).createConsumer(null);
            this.kafkaConsumer.subscribe(Arrays.asList(topic));
        }
        quoteBlockingQueue = new ArrayBlockingQueue<>(1024);
        persister = new MarketPricePersister(dbUrl, dbName, "trades", quoteBlockingQueue);
        LOGGER.info("Quote receiver started ");
    }

    @Override
    public void run() {
        int ackMode = Session.AUTO_ACKNOWLEDGE;
        while (isRunning()) {
            if (!kafka) {
                try {
                    persistFromEMS();
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage());
                }
            } else {
                try {
                    persistFromKafka();
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage());
                }
            }
        }
        LOGGER.warn("Thread {} received shutdown signal ", Thread.currentThread().getId());
        LOGGER.warn("Thread {} shutdown completed ", Thread.currentThread().getId());
    }

    private void persistFromEMS() throws Exception {
        Message msg = emsBroker.consumer().receive();
        if (msg == null)
            return;
        if (msg instanceof TextMessage) {
            TextMessage message = (TextMessage) msg;
            byte[] decoded = Base64.getDecoder().decode(message.getText());
            MarketPrice marketPricequote = deSerealizeAvroHttpRequestJSON(decoded);
            quoteBlockingQueue.put(marketPricequote);
        }
    }

    private void persistFromKafka() throws Exception {
        ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(10));
        for (ConsumerRecord<String, String> record : records) {
            String symbol = record.key();
            String data = record.value();
            byte[] decoded = Base64.getDecoder().decode(data);
            MarketPrice marketPricequote = deSerealizeAvroHttpRequestJSON(decoded);
            quoteBlockingQueue.put(marketPricequote);
            LOGGER.info("Key: " + symbol + ", Value:" + data);
            LOGGER.info("Partition:" + record.partition() + ",Offset:" + record.offset());
        }
    }

    public MarketPrice deSerealizeAvroHttpRequestJSON(byte[] data) {
        DatumReader<MarketPrice> reader
                = new SpecificDatumReader<>(MarketPrice.class);
        Decoder decoder = null;
        try {
            decoder = DecoderFactory.get().jsonDecoder(MarketPrice.getClassSchema(), new String(data));
            return reader.read(null, decoder);
        } catch (IOException e) {
            LOGGER.error("Deserialization error:" + e.getMessage());
        }
        return null;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}