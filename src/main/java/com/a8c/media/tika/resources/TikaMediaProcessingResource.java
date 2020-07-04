package com.a8c.media.tika.resources;

import com.a8c.media.tika.TikaMediaServerConfiguration;
import com.a8c.media.tika.api.MediaDetectionRequest;
import com.a8c.media.tika.api.MediaProcessingRequest;
import com.a8c.media.tika.core.MediaFetcher;
import com.a8c.media.tika.core.TikaMediaProcessor;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Resource to serve REST requests to API
 * All methods in this resource works using async http
 * Following pattern is used :
 * - File to be processed is fetched using asyncio, worker thread blocks while waiting for IO ( to keep http
 * connection management simple, but this is an area for improvement)
 * - On Completion, Inputstream is processed by same thread
 * - On Thread Completion, async http response is returned
 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class TikaMediaProcessingResource {

    final TikaMediaProcessor tikaMediaProcessor;
    final MediaFetcher mediaFetcher;
    final TikaMediaServerConfiguration tikaMediaServerConfiguration;
    final ExecutorService executorService;

    @Inject
    public TikaMediaProcessingResource(TikaMediaProcessor tikaMediaProcessor, MediaFetcher mediaFetcher,
                                       TikaMediaServerConfiguration mediaServerConfiguration,
                                       ExecutorService executorService) {
        this.tikaMediaProcessor = tikaMediaProcessor;
        this.mediaFetcher = mediaFetcher;
        this.tikaMediaServerConfiguration = mediaServerConfiguration;
        this.executorService = executorService;
    }

    private long getRequestProcessingTimeout(long requestOverride, boolean isOCR) {
        long timeOut = requestOverride;
        if (timeOut == -1) {
            timeOut = isOCR ? tikaMediaServerConfiguration.getDefaultOCRTimeout() :
                    tikaMediaServerConfiguration.getDefaultProcessingTimeout();
        }
        return timeOut;
    }

    private long getDownloadTimeout(long requestOverride) {
        long timeOut = requestOverride;
        if (timeOut == -1) {
            timeOut = tikaMediaServerConfiguration.getDefaultDownloadTimeout();
        }
        return timeOut;
    }

    private void setTimeoutHandler(AsyncResponse asyncResponse, long timeout) {
        asyncResponse.setTimeout(timeout, TimeUnit.MILLISECONDS);
        asyncResponse.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse asyncResponse) {
                asyncResponse.resume(Response.serverError().status(Response.Status.REQUEST_TIMEOUT).build());
            }
        });
    }

    /**
     * <p>Method used by Jersey container to process media file and return response containing extracted text</p>
     *
     * @param mediaProcessingRequest Request Object with details on media to be processed and flags to be used
     * @param asyncResponse          ( Internal use only) Used by Jersey container for async http
     * @throws IOException
     * @throws URISyntaxException
     */
    @SneakyThrows
    @POST
    @Path("/process")
    @Timed(name = "time-processMedia")
    @Metered
    public void processMedia(MediaProcessingRequest mediaProcessingRequest, @Suspended AsyncResponse asyncResponse)
            throws IOException, URISyntaxException {
        setTimeoutHandler(asyncResponse,
                getRequestProcessingTimeout(getRequestProcessingTimeout(mediaProcessingRequest.getProcessingTimeout()
                        ,mediaProcessingRequest.isUseOCR()),
                mediaProcessingRequest.isUseOCR()));
        executorService.submit(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                MediaFetcher.MediaFetcherProcessor mediaFetcherProcessor =
                        mediaFetcher.withUrl(mediaProcessingRequest.getResourcePath())
                                .withTimeout(getDownloadTimeout(mediaProcessingRequest.getFileDownloadTimeout()))
                                .withMaxDownloadSizeInBytes(tikaMediaServerConfiguration.getMaxFileSizeInBytes())
                                .ignoreIfNoContentTypeHeader(
                                        tikaMediaServerConfiguration.isIgnoreIfNoContentLengthHeader());
                mediaFetcherProcessor.fetchStreamAndProcess(tikaMediaProcessor, mediaProcessingRequest, asyncResponse,
                        executorService);
            }
        });
    }

    /**
     * <p>Method used by Jersey container to process media file and return MIME type of media resource</p>
     *
     * @param mediaDetectionRequest Request Object with details on media to be processed
     * @param asyncResponse         ( Internal use only) Used by Jersey container for async http
     * @throws IOException
     * @throws URISyntaxException
     */
    @SneakyThrows
    @POST
    @Path("/detect")
    @Timed(name = "time-detectMedia")
    @Metered
    public void detectMedia(MediaDetectionRequest mediaDetectionRequest, @Suspended AsyncResponse asyncResponse)
            throws IOException, URISyntaxException {
        setTimeoutHandler(asyncResponse,
                getRequestProcessingTimeout(mediaDetectionRequest.getProcessingTimeout(), false));
        executorService.submit(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                MediaFetcher.MediaFetcherProcessor mediaFetcherProcessor =
                        mediaFetcher.withUrl(mediaDetectionRequest.getResourcePath())
                                .withTimeout(getDownloadTimeout(mediaDetectionRequest.getFileDownloadTimeout()))
                                .withMaxDownloadSizeInBytes(tikaMediaServerConfiguration.getMaxFileSizeInBytes())
                                .ignoreIfNoContentTypeHeader(
                                        tikaMediaServerConfiguration.isIgnoreIfNoContentLengthHeader());
                mediaFetcherProcessor.fetchStreamAndDetect(tikaMediaProcessor, mediaDetectionRequest, asyncResponse,
                        executorService);
            }
        });

    }
}
