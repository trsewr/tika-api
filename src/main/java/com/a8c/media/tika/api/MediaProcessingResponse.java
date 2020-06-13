package com.a8c.media.tika.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


public class MediaProcessingResponse {
    @Getter
    @Setter
    @JsonProperty
    private boolean ocrIncluded;

    @Getter
    @Setter
    @JsonProperty
    private String contentText;

    @Getter
    @Setter
    @JsonProperty
    private String ocrText;

    @Getter
    @Setter
    @JsonProperty
    private Map<String, String> metaData;
}
