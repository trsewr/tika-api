package com.a8c.media.tika.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class MediaDetectionResponse {
    @Getter
    @Setter
    @JsonProperty
    public String mimeType;
}
