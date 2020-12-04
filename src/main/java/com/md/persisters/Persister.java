package com.md.persisters;

public interface Persister<T> extends Runnable{

    public void addMarketData(T t);
    public void stop(boolean flag);

}
