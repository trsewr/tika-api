package com.a8c.media.tika.core;

import com.a8c.media.tika.TikaMediaServerConfiguration;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.AutoDetectParser;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class MediaProcessorModule extends AbstractModule {
    final ExecutorService executorService;
    final TikaMediaServerConfiguration tikaMediaServerConfiguration;

    public MediaProcessorModule(ExecutorService executorService, TikaMediaServerConfiguration serverConfiguration) {
        this.executorService = executorService;
        this.tikaMediaServerConfiguration = serverConfiguration;
    }

    static AutoDetectParser autoDetectParser = new AutoDetectParser(TikaConfig.getDefaultConfig());

    @Override
    protected void configure() {
        bind(new TypeLiteral<List<String>>(){}).annotatedWith(Names.named("http-whitelist"))
                .toProvider(() -> tikaMediaServerConfiguration.getSources());

    }
    @Provides
    TikaMediaProcessor provideTikaMediaProcessor() {
        log.info("ocr timeout -" + tikaMediaServerConfiguration.getDefaultOCRTimeout());
        log.info("max processing threads -" + tikaMediaServerConfiguration.getMaxProcessingThreads());
        log.info("default processing timeout -" + tikaMediaServerConfiguration.getDefaultProcessingTimeout());
        return new TikaMediaProcessor.Builder()
                .withDefaultOCRTimeout(tikaMediaServerConfiguration.getDefaultOCRTimeout())
                .withExecutorService(executorService)
                .withMaxProcessingThreads(tikaMediaServerConfiguration.getMaxProcessingThreads())
                .withDefaultProcessingTimeout(tikaMediaServerConfiguration.getDefaultProcessingTimeout())
                .withTikaParser(autoDetectParser)
                .withAllowedSources(tikaMediaServerConfiguration.getSources())
                .build();

    }
}
