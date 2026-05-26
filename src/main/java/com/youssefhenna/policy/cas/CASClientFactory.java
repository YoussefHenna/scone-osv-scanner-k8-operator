package com.youssefhenna.policy.cas;

public interface CASClientFactory {
    CASClient create(String casAddress, int casPort);
}