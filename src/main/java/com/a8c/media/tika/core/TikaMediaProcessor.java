package com.a8c.media.tika.core;

import com.a8c.media.tika.api.MediaProcessingResponse;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.ProbabilisticMimeDetectionSelector;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TikaMediaProcessor {
    @Getter
    private long defaultProcessingTimeout;
    @Getter
    private long defaultOCRTimeOut;
    @Getter
    private int maxProcessingThreads;
    @Getter
    private ExecutorService executorService;
    @Getter
    AbstractParser defaultParser;
    @Getter
    boolean ocrEnabled = false;
    @Getter
    private List<String> allowedSources = new ArrayList<>();


    private void isExecutorBusy() {
        if (((ThreadPoolExecutor) executorService).getActiveCount() > maxProcessingThreads) {
            throw new RejectedExecutionException("Too Busy");
        }
    }

    public static class Builder {
        @Getter
        private ExecutorService executorService;
        @Getter
        private int maxProcessingThreads;
        @Getter
        private long defaultOCRTimeout;
        @Getter
        private long defaultProcessingTimeout;
        @Getter
        private AbstractParser parser;
        @Getter
        private boolean ocrEnabled = false;
        @Getter
        private List<String> allowedSources = new ArrayList<>();

        public Builder withExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder withMaxProcessingThreads(int maxProcessingThreads) {
            this.maxProcessingThreads = maxProcessingThreads;
            return this;
        }

        public Builder withDefaultOCRTimeout(long timeout) {
            this.defaultOCRTimeout = timeout;
            return this;
        }

        public Builder withDefaultProcessingTimeout(long timeout) {
            this.defaultProcessingTimeout = timeout;
            return this;
        }

        public Builder withTikaParser(AbstractParser parser) {
            this.parser = parser;
            return this;
        }

        public Builder withAllowedSources(List<String> sources) {
            this.allowedSources = sources;
            return this;
        }


        public Builder withDefaults() {
            this.executorService = new ThreadPoolExecutor(5, 10, 100, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
            this.maxProcessingThreads = 10;
            this.defaultOCRTimeout = 10 * 10_00;
            this.defaultProcessingTimeout = 10_00;
            this.allowedSources = Lists.newArrayList("*");
            this.parser = new AutoDetectParser(TikaConfig.getDefaultConfig());
            return this;
        }

        public TikaMediaProcessor build() {
            TikaMediaProcessor tikaMediaProcessor = new TikaMediaProcessor();
            tikaMediaProcessor.executorService = this.executorService;
            tikaMediaProcessor.maxProcessingThreads = this.maxProcessingThreads;
            tikaMediaProcessor.defaultOCRTimeOut = this.defaultOCRTimeout;
            tikaMediaProcessor.defaultProcessingTimeout = this.defaultProcessingTimeout;
            tikaMediaProcessor.defaultParser = this.parser;
            tikaMediaProcessor.allowedSources = this.allowedSources;
            return tikaMediaProcessor;
        }
    }

    private TikaMediaProcessor() {
    }

    public MediaProcessingResponse processMedia(String url, String mimeType, long timeOut, boolean ocrEnabled, List<String> langs)
            throws InterruptedException, ExecutionException, TimeoutException {
        isExecutorBusy();
        Future<MediaProcessingResponse> future = executorService.submit(() -> processMediaInternal(url, mimeType, ocrEnabled, langs));
        long ttl = timeOut == -1 ? defaultProcessingTimeout : timeOut;
        return future.get(ttl, TimeUnit.MILLISECONDS);

    }

    private void setParserContextForOCR(ParseContext context, List<String> langs) {
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(true);
        TesseractOCRConfig tesserConfig = new TesseractOCRConfig();
        String ocrLangs = "eng";
        if (langs.size() == 0) {
            log.info("no OCR lang was set , defaulting to eng`");
        }
        if (langs.size() == 1) {
            ocrLangs = langs.get(0);
        } else {
            ocrLangs = StringUtils.join(langs, '+');
        }
        log.info("languages used in OCR " + ocrLangs);
        tesserConfig.setLanguage(ocrLangs);
        context.set(PDFParserConfig.class, pdfConfig);
        context.set(TesseractOCRConfig.class, tesserConfig);
    }

    private MediaProcessingResponse processMediaInternal(String url, String mimeType, boolean ocrEnabled, List<String> langs) throws TikaException, SAXException, IOException {
        MediaProcessingResponse mediaProcessingResponse = new MediaProcessingResponse();
        LocalFSCacheContext localFSCacheContext = new LocalFSCacheContext(allowedSources);
        try {
            localFSCacheContext.download(url);
            BodyContentHandler saxStream = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            try (InputStream stream = new FileInputStream(localFSCacheContext.tempFile)) {
                log.info("Input stream obtained, parsing with tika -" + url);
                if (ocrEnabled) {
                    log.info("OCR is enabled in request , setting OCR context params - " + url);
                    setParserContextForOCR(context, langs);
                }
                defaultParser.parse(stream, saxStream, metadata, context);
                mediaProcessingResponse.setContentText(saxStream.toString());
                Map<String, String> metaData = new HashMap<>();
                for (String name : metadata.names()) {
                    metaData.put(name, metadata.get(name));
                }
                mediaProcessingResponse.setMetaData(metaData);
                mediaProcessingResponse.setOcrIncluded(ocrEnabled);
            }
            return mediaProcessingResponse;

        } catch (Throwable e) {
            log.error("Error in processing with Tika - " + url, e);
            throw e;
        } finally {
            localFSCacheContext.close();
        }
    }

    public String detectMimeType(String url, long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        isExecutorBusy();
        Future<String> future = executorService.submit(() -> {
            try {
                return detectMimeTypeInternal(url);
            } catch (Throwable ex) {
                log.error("Error in detecting MimeType for " + url, ex);
                throw ex;
            }
        });
        long ttl = timeout == -1 ? defaultProcessingTimeout : timeout;
        return future.get(ttl, TimeUnit.MILLISECONDS);
    }

    private String detectMimeTypeInternal(String url) throws IOException {
        LocalFSCacheContext localFSCacheContext = new LocalFSCacheContext(allowedSources);
        try {
            localFSCacheContext.download(url);
            log.info("Downloaded for mime type detection -" + url);
            Detector detector = new ProbabilisticMimeDetectionSelector();
            return new Tika().detect(localFSCacheContext.tempFile);
        } finally {
            localFSCacheContext.close();
        }
    }

}
