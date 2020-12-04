package com.md.receivers;

import com.ashish.marketdata.avro.Quote;
import com.md.brokers.EMSBroker;
import com.md.brokers.KafkaBroker;
import com.md.persisters.Persister;
import com.md.persisters.chronical.ChQuotePersister;
import com.md.persisters.chronical.ChTradePersister;
import com.md.persisters.mongo.MnQuotePersister;
import com.md.persisters.mongo.MnTradePersister;
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

public class QuoteReceiver implements Runnable{

    private boolean kafka;
    private String topic;
    private Persister persister;
    private EMSBroker emsBroker;
    private KafkaConsumer<String, String> kafkaConsumer;
    private volatile boolean running = true;

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeReceiver.class);

    public QuoteReceiver(final String serverUrl, final String topic, final boolean kafka, String mongoDBUrl, String mongoDBName, boolean mongo, String collectionOrQueueName) throws JMSException {
        this.topic = topic;
        this.kafka = kafka;
        if (!kafka) {
            emsBroker = new EMSBroker(serverUrl, null, null);
            emsBroker.createConsumer(topic, true);
        } else {
            this.kafkaConsumer = new KafkaBroker(serverUrl).createConsumer(null);
            this.kafkaConsumer.subscribe(Arrays.asList(topic));
        }
        if(mongo){
            persister = new MnQuotePersister(mongoDBUrl, mongoDBName,collectionOrQueueName);
            LOGGER.info("Quote receiver thread started with {}",persister);
        }else{
            persister = new ChQuotePersister(collectionOrQueueName);
            LOGGER.info("Quote receiver thread started with {}",persister);
        }
    }

    @Override
    public void run() {
        int ackMode = Session.AUTO_ACKNOWLEDGE;
        while (isRunning()) {
            if (!kafka) {
                try {
                    receiveFromEMS();
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage());
                }
            } else {
                try {
                    receiveFromKafka();
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage());
                }
            }
        }
        LOGGER.warn("Thread {} received shutdown signal ", Thread.currentThread().getId());
        LOGGER.warn("Thread {} shutdown completed ", Thread.currentThread().getId());
    }

    private void receiveFromEMS() throws Exception {
        Message msg = emsBroker.consumer().receive();
        if (msg == null)
            return;
        if (msg instanceof TextMessage) {
            TextMessage message = (TextMessage) msg;
            byte[] decoded = Base64.getDecoder().decode(message.getText());
            Quote quote = deSerealizeAvroHttpRequestJSON(decoded);
            persister.addMarketData(quote);
        }
    }

    private void receiveFromKafka() throws Exception {
        ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(10));
        for (ConsumerRecord<String, String> record : records) {
            String symbol = record.key();
            String data = record.value();
            byte[] decoded = Base64.getDecoder().decode(data);
            Quote quote = deSerealizeAvroHttpRequestJSON(decoded);
            persister.addMarketData(quote);
            //LOGGER.info("Key: " + symbol + ", Value:" + data);
            //LOGGER.info("Partition:" + record.partition() + ",Offset:" + record.offset());
        }
    }

    public Quote deSerealizeAvroHttpRequestJSON(byte[] data) {
        DatumReader<Quote> reader
                = new SpecificDatumReader<>(Quote.class);
        Decoder decoder = null;
        try {
            decoder = DecoderFactory.get().jsonDecoder(Quote.getClassSchema(), new String(data));
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
        persister.stop(this.running);
    }
}
