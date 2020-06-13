package com.a8c.media.tika.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class MediaDetectionRequest {
    @Getter
    @Setter
    @JsonProperty
    private String resoucePath;

    @Getter
    @Setter
    @JsonProperty
    private long processingTimeout;

}
