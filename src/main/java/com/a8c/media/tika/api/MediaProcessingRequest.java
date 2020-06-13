package com.a8c.media.tika.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class MediaProcessingRequest {
    @Getter
    @Setter
    @JsonProperty
    private String mimeType;

    @Getter
    @Setter
    @JsonProperty
    private String resoucePath;

    @Getter
    @Setter
    @JsonProperty
    private boolean useOCR;

    @Getter
    @Setter
    @JsonProperty
    private long processingTimeout;

    @Getter
    @Setter
    @JsonProperty
    private long ocrProcessingTimeout;

    /*
    Tesseract language codes are moslty ISO-639-2 (i.e,  `eng` not `en` for english)
    https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
     */
    @Getter
    @Setter
    @JsonProperty("ocrLangs")
    private ArrayList<String> ocrLangs;

    @Getter
    @Setter
    @JsonProperty
    private boolean autoDetectType;
}
