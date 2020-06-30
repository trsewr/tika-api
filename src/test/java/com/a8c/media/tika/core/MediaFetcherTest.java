package com.a8c.media.tika.core;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaFetcherTest {

    @Test
    void testMediaFetcher() {
        MediaFetcher.MediaFetcherProcessor mediaFetcherProcessor =
                new MediaFetcher(Lists.newArrayList("all")).withUrl("http://www.google" + ".com").withTimeout(100);
        try {
            mediaFetcherProcessor =
                    new MediaFetcher(Lists.newArrayList("http://www.yahoo.com")).withUrl("http://www.google" + ".com")
                            .withTimeout(100);
            fail("Expected exception");
        } catch (RuntimeException rte) {
            Assertions.assertTrue(rte.getMessage().contains("NOT_WHITELISTED"));
        }
    }
}