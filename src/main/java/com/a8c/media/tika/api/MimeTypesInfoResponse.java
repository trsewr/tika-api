package com.a8c.media.tika.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class MimeTypesInfoResponse {

    @Getter
    @Setter
    @JsonProperty
    private List<String> mimeTypes;
}
