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

        String mongoServerUrl = properties.getProperty("exsim.mongodb.servers");
        String mongoDb = properties.getProperty("exsim.mongodb.dbname");
        final String orderTopic = properties.getProperty("exsim.nse.ordertopic");
        final String tradeTopic = properties.getProperty("exsim.nse.tradetopic");
        final String quoteTopic = properties.getProperty("exsim.nse.quotestopic");
        final String marketPriceTopic = properties.getProperty("exsim.nse.marketpricetopic");
        final String marketByPriceTopic = properties.getProperty("exsim.nse.marketbypricetopic");
        final String executionTopic = properties.getProperty("exsim.nse.marketbypricetopic");

        // start trade consumers
        TradeReceiver tradeReceiver = new TradeReceiver(serverUrl,tradeTopic,kafkaAsCarrier,mongoServerUrl,mongoDb);
        new Thread(tradeReceiver).start();

        // start quote consumers
        QuoteReceiver quoteReceiver = new QuoteReceiver(serverUrl,quoteTopic,kafkaAsCarrier,mongoServerUrl,mongoDb);
        new Thread(quoteReceiver).start();

        // start market price consumers
        MarketPriceReceiver marketPriceReceiver = new MarketPriceReceiver(serverUrl,marketPriceTopic,kafkaAsCarrier,mongoServerUrl,mongoDb);
        new Thread(marketPriceReceiver).start();


    }
}
