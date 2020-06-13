package com.a8c.media.tika.core;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TikaMediaProcessorTest {

    @Test
    void processMetaData_TimeOutTest() {
        TikaMediaProcessor tikaMediaProcessor = new TikaMediaProcessor.Builder().withDefaults().
                build();

        boolean gotTimeOut = false;
        try {
            tikaMediaProcessor.processMedia("https://file-examples.com/wp-content/uploads/2017/10/file" +
                            "-example_PDF_500_kB.pdf",
                    "application" + "/pdf", 0,false, Lists.newArrayList());
        } catch (Exception e) {
            gotTimeOut = e instanceof TimeoutException;
        }
        assertTrue(gotTimeOut, "Should Throw timeout exception");
    }
}