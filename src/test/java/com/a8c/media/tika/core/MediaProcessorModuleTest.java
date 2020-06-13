package com.a8c.media.tika.core;

import com.a8c.media.tika.TikaMediaServerConfiguration;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaProcessorModuleTest {

    @Test
    void provideTikaMediaProcessor() {
        TikaMediaServerConfiguration tikaMediaServerConfiguration = new TikaMediaServerConfiguration();
        MediaProcessorModule module = new MediaProcessorModule(null,tikaMediaServerConfiguration);
        assert (module.provideTikaMediaProcessor() != null);
        assertTrue(module.provideTikaMediaProcessor().defaultParser instanceof AutoDetectParser);
        AutoDetectParser autoDetectParser = (AutoDetectParser)module.provideTikaMediaProcessor().defaultParser;
        assertTrue(autoDetectParser.getParsers().size() > 5);
        assertTrue(autoDetectParser.getParsers().containsKey(MediaType.parse("application/pdf"))
                ,"Should contain pdf parser");
    }
}