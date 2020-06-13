package com.a8c.media.tika;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import lombok.Getter;

import javax.validation.constraints.Min;
import java.util.ArrayList;

public class TikaMediaServerConfiguration extends Configuration {

    static long DEFAULT_TIME_OUT = 10 * 100_0;
    @JsonProperty
    @Getter
    @Min(10) long defaultProcessingTimeout = DEFAULT_TIME_OUT;

    @JsonProperty
    @Getter
    @Min(10) long defaultOCRTimeout = DEFAULT_TIME_OUT * 10;

    @JsonProperty
    @Getter
    int maxProcessingThreads = 10;

    @JsonProperty("sources")
    @Getter
    ArrayList<String> sources;

}
