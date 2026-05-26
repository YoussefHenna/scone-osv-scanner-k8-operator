package com.youssefhenna.policy.cas;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultCASClientFactory implements CASClientFactory {

    @Override
    public CASClient create(String casAddress, int casPort) {
        return new HttpCASClient(casAddress, casPort);
    }
}