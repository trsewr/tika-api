package com.a8c.media.tika.health;

import com.codahale.metrics.health.HealthCheck;

public class TikaMediaServerHealthCheck extends HealthCheck {
    @Override
    protected Result check() throws Exception {
        return Result.healthy("I'm alive, but might be busy - cant say that for sure");
    }

}
