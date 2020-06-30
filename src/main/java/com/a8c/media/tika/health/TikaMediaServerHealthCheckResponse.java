package com.a8c.media.tika.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class TikaMediaServerHealthCheckResponse {
    @Getter
    @Setter
    @JsonProperty
    private ExecutorHealthCheckResponse executorHealthCheck;
}
