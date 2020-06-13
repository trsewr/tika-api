package com.a8c.media.tika.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

@Slf4j
public class LocalFSCacheContext {
    File tempFile = null;
    boolean allowAll = false;
    List<String> whiteListSources;

    public LocalFSCacheContext(List<String> sourceWhiteList) {
        if (sourceWhiteList.size() == 1 && sourceWhiteList.get(0).equalsIgnoreCase("*")) {
            allowAll = true;
            log.warn("DANGER - RUNNING IN ALLOW-ALL SOURCE MODE");
        } else {
            whiteListSources = sourceWhiteList;
        }
    }

    private boolean isValidUrl(String source) {
        if (allowAll) {
            return true;
        }
        for (String whiteListSource : whiteListSources) {
            if (source.toLowerCase().startsWith(whiteListSource.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static File getTmpFile() throws IOException {
        return File.createTempFile("tika", null);
    }

    public void download(String path) throws IOException {
        if (!isValidUrl(path)) {
            throw new RuntimeException("NOT_WHITELISTED: " + path + " not in source white list , not allowed to " + "access");
        }
        tempFile = getTmpFile();
        tempFile.deleteOnExit();
        String filePath = path;
        FileUtils.copyURLToFile(new URL(filePath), tempFile);
        log.info("Downloaded file to cache directory " + filePath + "->" + tempFile.getAbsolutePath());
    }

    public void close() {
        if (tempFile != null) {
            if (tempFile.exists()) {
                log.info("Deleting" + "->" + tempFile.getAbsolutePath());
                tempFile.delete();
            }
        }
    }

    public String getFileName() {
        return tempFile.getAbsolutePath();
    }


}
