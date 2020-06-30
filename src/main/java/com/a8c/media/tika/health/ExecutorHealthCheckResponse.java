package com.a8c.media.tika.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class ExecutorHealthCheckResponse {

    @Getter
    @Setter
    @JsonProperty
    private int activeThreads;

    @Getter
    @Setter
    @JsonProperty
    private int poolSize;

    @Getter
    @Setter
    @JsonProperty
    private long taskCount;

    @Getter
    @Setter
    @JsonProperty
    private long completedTasks;

    @Getter
    @Setter
    @JsonProperty
    private int queueSize;

    @Getter
    @Setter
    @JsonProperty
    private int remainingQueueCapacity;
}
