package com.md.persisters;

public interface Persister<T> extends Runnable{

    public static final String MONGO_DB_URL = "mongodb://127.0.0.1:27017,127.0.0.1:27018,127.0.0.1:27019/?replicaSet=rs0";
    public static final String DB_NAME = "online-school";

    public void addMarketData(T t);

}
