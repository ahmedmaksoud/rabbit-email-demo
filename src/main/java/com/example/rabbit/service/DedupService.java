package com.example.rabbit.service;

public interface DedupService {
    /** @return true if this is the FIRST time we see the key (i.e., not a dup). */
    boolean firstTimeSeen(String key);
}