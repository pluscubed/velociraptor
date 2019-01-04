package com.pluscubed.velociraptor.utils;

import java.util.ArrayList;

//https://stackoverflow.com/a/21047889/3579513
public class LimitedSizeQueue<K> extends ArrayList<K> {

    private int maxSize;

    public LimitedSizeQueue(int size) {
        this.maxSize = size;
    }

    public boolean add(K k) {
        boolean r = super.add(k);
        if (size() > maxSize) {
            removeRange(0, size() - maxSize - 1);
        }
        return r;
    }

    public K getYoungest() {
        return get(size() - 1);
    }

    public K getOldest() {
        return get(0);
    }
}