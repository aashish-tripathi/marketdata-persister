package com.md.service;

import com.md.receivers.MarketPriceReceiver;
import com.md.receivers.QuoteReceiver;
import com.md.receivers.TradeReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StartMarketDataPersister {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartMarketDataPersister.class);

    public static void main(String[] args) throws IOException, JMSException {
        String configPath = null;
        if (args.length == 0) {
            LOGGER.warn("Config file not provided, loading file from default directory");
            configPath = "/md-persister.properties";
        } else {
            configPath = args[0];
        }

        final boolean kafkaAsCarrier = true;

        Properties properties = new Properties();
        InputStream inputStream = StartMarketDataPersister.class.getResourceAsStream(configPath);
        properties.load(inputStream);
        String serverUrl = kafkaAsCarrier ? properties.getProperty("exsim.kafka.bootstrap.servers") : properties.getProperty("exsim.tibcoems.serverurl");

        final String type = properties.getProperty("exsim.persistence.type");
        final boolean mongo = type.equalsIgnoreCase("mongo") ? true : false;

        final String orderTopic = properties.getProperty("exsim.nse.ordertopic");
        final String tradeTopic = properties.getProperty("exsim.nse.tradetopic");
        final String quoteTopic = properties.getProperty("exsim.nse.quotestopic");
        final String marketPriceTopic = properties.getProperty("exsim.nse.marketpricetopic");
        final String marketByPriceTopic = properties.getProperty("exsim.nse.marketbypricetopic");
        final String executionTopic = properties.getProperty("exsim.nse.executiontopic=");

        ExecutorService executorService = Executors.newFixedThreadPool(5);

        if (mongo) {
            final String mongoServerUrl = properties.getProperty("exsim.mongodb.servers");
            final String mongoDb = properties.getProperty("exsim.mongodb.dbname");

            final String orderCollection = properties.getProperty("exsim.mongo.ordercollection");
            final String tradeCollection = properties.getProperty("exsim.mongo.tradecollection");
            final String quoteCollection = properties.getProperty("exsim.mongo.quotescollection");
            final String marketPriceCollection = properties.getProperty("exsim.mongo.marketpricecollection");
            final String marketByPriceCollection = properties.getProperty("exsim.mongo.marketbypricecollection");
            final String executionCollection = properties.getProperty("exsim.mongo.executionscollection");
            executorService.submit(new TradeReceiver(serverUrl, tradeTopic, kafkaAsCarrier, mongoServerUrl, mongoDb, mongo, tradeCollection));
            executorService.submit(new QuoteReceiver(serverUrl, quoteTopic, kafkaAsCarrier, mongoServerUrl, mongoDb, mongo, quoteCollection));
            executorService.submit(new MarketPriceReceiver(serverUrl, marketPriceTopic, kafkaAsCarrier, mongoServerUrl, mongoDb, mongo, marketPriceCollection));
        } else {
            final String orderQueue = properties.getProperty("exsim.chronical.orderqueue");
            final String tradeQueue = properties.getProperty("exsim.chronical.tradequeue");
            final String quoteQueue = properties.getProperty("exsim.chronical.quotesqueue");
            final String marketPriceQueue = properties.getProperty("exsim.chronical.marketpricequeue");
            final String marketByPriceQueue = properties.getProperty("exsim.chronical.marketbypricequeue");
            final String executionQueue = properties.getProperty("exsim.chronical.executionsqueue");
            executorService.submit(new TradeReceiver(serverUrl, tradeTopic, kafkaAsCarrier, null, null, false, tradeQueue));
            executorService.submit(new QuoteReceiver(serverUrl, quoteTopic, kafkaAsCarrier, null, null, false, quoteQueue));
            executorService.submit(new MarketPriceReceiver(serverUrl, marketPriceTopic, kafkaAsCarrier, null, null, false, marketPriceQueue));
        }
    }
}
