package com.a8c.media.tika.health;

import com.a8c.media.tika.TikaMediaServerConfiguration;
import com.codahale.metrics.health.HealthCheck;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class TikaMediaServerHealthCheck extends HealthCheck {
    ExecutorService executorService;
    TikaMediaServerConfiguration mediaServerConfiguration;

    public TikaMediaServerHealthCheck(ExecutorService executorService,
                                      TikaMediaServerConfiguration mediaServerConfiguration) {
        this.executorService = executorService;
        this.mediaServerConfiguration = mediaServerConfiguration;
    }

    @Override
    protected Result check() throws Exception {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        ExecutorHealthCheckResponse executorHealthCheckResponse = new ExecutorHealthCheckResponse();
        executorHealthCheckResponse.setActiveThreads(threadPoolExecutor.getActiveCount());
        executorHealthCheckResponse.setCompletedTasks(threadPoolExecutor.getCompletedTaskCount());
        executorHealthCheckResponse.setPoolSize(threadPoolExecutor.getPoolSize());
        executorHealthCheckResponse.setTaskCount(threadPoolExecutor.getTaskCount());
        executorHealthCheckResponse.setQueueSize(threadPoolExecutor.getQueue().size());
        executorHealthCheckResponse.setRemainingQueueCapacity(threadPoolExecutor.getQueue().remainingCapacity());

        return Result.builder().withDetail("executor", executorHealthCheckResponse)
                .withDetail("config", mediaServerConfiguration).build();

    }

}
