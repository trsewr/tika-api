package com.a8c.media.tika;

import com.a8c.media.tika.core.MediaProcessorModule;
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

    @Override
    public void run(final TikaMediaServerConfiguration configuration, final Environment environment) {
        final TikaMediaServerHealthCheck healthCheck = new TikaMediaServerHealthCheck();
        ExecutorService executorService =
                environment.lifecycle().executorService("tikaapi").maxThreads(configuration.maxProcessingThreads).build();
        final MediaProcessorModule mediaProcessorModule = new MediaProcessorModule(executorService, configuration);
        Injector injector = Guice.createInjector(mediaProcessorModule);

        environment.getAdminContext().addServlet(MetricsServlet.class,"/metrics");
        environment.healthChecks().register("health", healthCheck);
        environment.jersey().register(injector.getInstance(TikaMediaProcessingResource.class));
        environment.jersey().register(injector.getInstance(TikaMetaInformationResource.class));
    }

}
