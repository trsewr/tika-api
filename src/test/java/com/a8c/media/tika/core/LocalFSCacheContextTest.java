package com.a8c.media.tika.core;

import com.google.common.collect.Lists;
import io.dropwizard.util.Strings;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFSCacheContextTest {

    @Test
    void download() throws IOException {
        LocalFSCacheContext localFSCacheContext = new LocalFSCacheContext(Lists.newArrayList("https://automattic" +
                ".com/"));
        localFSCacheContext.download("https://automattic.com/about");
        assertTrue(localFSCacheContext.tempFile.exists());
        assertTrue(FileUtils.readFileToString(localFSCacheContext.tempFile).contains("WordPress"));
    }

    @Test
    void close() throws IOException {
        LocalFSCacheContext localFSCacheContext = new LocalFSCacheContext(Lists.newArrayList("https://automattic" +
                ".com/"));
        localFSCacheContext.download("https://automattic.com/about");
        localFSCacheContext.close();
        assertFalse(localFSCacheContext.tempFile.exists());
    }

    @Test
    void getFileName() throws IOException {
        LocalFSCacheContext localFSCacheContext = new LocalFSCacheContext(Lists.newArrayList("https://automattic" +
                ".com/"));
        localFSCacheContext.download("https://automattic.com/about");
        assertTrue(!Strings.isNullOrEmpty(localFSCacheContext.getFileName()));
    }
}