package com.a8c.media.tika.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class MediaDetectionRequest {

    @Getter
    @Setter
    @JsonProperty
    private String resourcePath;

    @Getter
    @Setter
    @JsonProperty
    private long processingTimeout;

    @Getter
    @Setter
    @JsonProperty
    private long fileDownloadTimeout = -1;

}
