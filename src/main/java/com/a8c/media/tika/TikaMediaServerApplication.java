package com.a8c.media.tika;

import com.a8c.media.tika.core.MediaFetcher;
import com.a8c.media.tika.core.MediaProcessorModule;
import com.a8c.media.tika.core.TikaMediaProcessor;
import com.a8c.media.tika.health.TikaMediaServerHealthCheck;
import com.a8c.media.tika.resources.TikaMediaProcessingResource;
import com.a8c.media.tika.resources.TikaMetaInformationResource;
import com.codahale.metrics.servlets.MetricsServlet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;

public class TikaMediaServerApplication extends Application<TikaMediaServerConfiguration> {

    public static void main(final String[] args) throws Exception {
        new TikaMediaServerApplication().run(args);
    }

    public void initialize(Bootstrap<TikaMediaServerConfiguration> bootstrap) {
    }

    @Override
    public String getName() {
        return "TikaMediaServer";
    }

    /**
     * <p>Dropwizard app entry point, Initialize dependency injection module</p>
     * <p> Registers all resources</p>
     * <p>Registers health check and metrics endpoints</p>
     *
     * @param configuration Configuration, app generates it by parsing config.yml given as part of CLI
     * @param environment   Servlet context provided by dropwizard
     */
    @Override
    public void run(final TikaMediaServerConfiguration configuration, final Environment environment) {
        ExecutorService executorService =
                environment.lifecycle().executorService("tikaapi").maxThreads(configuration.getMaxProcessingThreads())
                        .workQueue(new LinkedBlockingDeque<>(configuration.getMaxProcessingQueueDepth())).build();
        final TikaMediaServerHealthCheck healthCheck = new TikaMediaServerHealthCheck(executorService,configuration);
        final MediaProcessorModule mediaProcessorModule = new MediaProcessorModule(executorService, configuration);
        Injector injector = Guice.createInjector(mediaProcessorModule);
        environment.getAdminContext().addServlet(MetricsServlet.class, "/metrics");
        environment.healthChecks().register("health", healthCheck);
        TikaMediaProcessingResource tikaMediaProcessingResource =
                new TikaMediaProcessingResource(injector.getInstance(TikaMediaProcessor.class),
                        new MediaFetcher(configuration.getSources()),
                        configuration,
                        executorService);
        environment.jersey().register(tikaMediaProcessingResource);
        environment.jersey().register(injector.getInstance(TikaMetaInformationResource.class));
    }

}
