package com.a8c.media.tika.core;

import com.a8c.media.tika.api.MediaProcessingResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;


/**
 * <p> Wrapper to Apache Tika Library, to extract text from Inputstream </p>
 */
@Slf4j
public class TikaMediaProcessor {
    @Getter
    private long defaultProcessingTimeout;
    @Getter
    private long defaultOCRTimeOut;
    @Getter
    private int maxProcessingQueueDepth;
    @Getter
    private ExecutorService executorService;
    @Getter
    AbstractParser defaultParser;
    @Getter
    boolean ocrEnabled = false;
    @Getter
    private List<String> allowedSources = new ArrayList<>();

    /**
     * Builder helper to return TikaMediaProcessor
     */
    public static class Builder {
        @Getter
        private ExecutorService executorService;
        @Getter
        private int maxProcessingQueueDepth;
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

        public Builder withMaxProcessingQueueDepth(int maxProcessingQueueDepth) {
            this.maxProcessingQueueDepth = maxProcessingQueueDepth;
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

        /**
         * Returns TikaMediaProcessor based on supplied params
         * @return
         */
        public TikaMediaProcessor build() {
            TikaMediaProcessor tikaMediaProcessor = new TikaMediaProcessor();
            tikaMediaProcessor.executorService = this.executorService;
            tikaMediaProcessor.maxProcessingQueueDepth = this.maxProcessingQueueDepth;
            tikaMediaProcessor.defaultOCRTimeOut = this.defaultOCRTimeout;
            tikaMediaProcessor.defaultProcessingTimeout = this.defaultProcessingTimeout;
            tikaMediaProcessor.defaultParser = this.parser;
            tikaMediaProcessor.allowedSources = this.allowedSources;
            return tikaMediaProcessor;
        }
    }

    private TikaMediaProcessor() {
    }

    // Sets parser context for OCR - will throw exception in case of invalid language
    // or language model missing in server
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

    /**
     * <p> Processes inputstream to extract text content, optionally using OCR</p>
     * @param inputStream inputstream
     * @param ocrEnabled controls whether OCR is used
     * @param langs language models to be used for OCR
     * @return
     * @throws TikaException
     * @throws SAXException
     * @throws IOException
     * @throws URISyntaxException
     */
    public MediaProcessingResponse processMedia(InputStream inputStream, boolean ocrEnabled, List<String> langs)
            throws TikaException, SAXException, IOException, URISyntaxException {
        MediaProcessingResponse mediaProcessingResponse = new MediaProcessingResponse();
        try {
            BodyContentHandler saxStream = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            try (InputStream stream = new BufferedInputStream(inputStream)) {
                if (ocrEnabled) {
                    log.info("OCR is enabled in request , setting OCR context params ");
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
            log.error("Error in processing with Tika - ", e);
            throw e;
        } finally {
            inputStream.close();
        }
    }

    /**
     * <p>Detects mimetype of inputstream</p>
     * @param inputStream
     * @return
     * @throws IOException
     */
    public String detectMimeType(InputStream inputStream) throws IOException {
        try {
            log.info("Downloaded for mime type detection ");
            return new Tika().detect(inputStream);
        } finally {
            inputStream.close();
        }
    }

}
