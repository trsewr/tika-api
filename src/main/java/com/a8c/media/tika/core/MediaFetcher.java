package com.a8c.media.tika.core;

import com.a8c.media.tika.api.MediaDetectionRequest;
import com.a8c.media.tika.api.MediaDetectionResponse;
import com.a8c.media.tika.api.MediaProcessingRequest;
import com.a8c.media.tika.api.MediaProcessingResponse;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

import javax.ws.rs.container.AsyncResponse;

import static org.asynchttpclient.Dsl.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Helper class to validate/initialize http download</p>
 */
@Slf4j
public class MediaFetcher {
    private boolean allowAll = false;
    private List<String> whiteListSources;

    public MediaFetcher(List<String> sourceWhiteList) {
        if (sourceWhiteList.size() == 1 && sourceWhiteList.get(0).equalsIgnoreCase("all")) {
            allowAll = true;
            log.warn("DANGER - RUNNING IN ALLOW-ALL SOURCE MODE");
        } else {
            whiteListSources = sourceWhiteList;
        }
    }

    public MediaFetcherProcessor withUrl(String source) {
        if (allowAll) {
            return new MediaFetcherProcessor(source);
        }
        for (String whiteListSource : whiteListSources) {
            if (source.toLowerCase().startsWith(whiteListSource.toLowerCase())) {
                return new MediaFetcherProcessor(source);
            }
        }
        throw new RuntimeException(
                "NOT_WHITELISTED: " + source + " not in source white list , not allowed to " + "access");
    }

    public class MediaFetcherProcessor {

        private final String path;
        private long timeout;
        private int maxFileSizeInBytes = 1000 * 1000 * 2 ;
        private boolean ignoreIfNoHeader = false;

        MediaFetcherProcessor(String url) {
            this.path = url;
        }

        public MediaFetcherProcessor withTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Max Allowed download size of file in bytes
         * @param maxDownloadSizeInBytes
         * @return
         */
        public MediaFetcherProcessor withMaxDownloadSizeInBytes(int maxDownloadSizeInBytes){
            this.maxFileSizeInBytes = maxDownloadSizeInBytes;
            return this;
        }

        /**
         * if no content-length is present in file response, size of file will not be limited
         * @param ignoreIfNoHeader true to ignore content-length error, if not present
         * @return
         */
        public MediaFetcherProcessor ignoreIfNoContentTypeHeader(boolean ignoreIfNoHeader){
            this.ignoreIfNoHeader = ignoreIfNoHeader;
            return this;
        }

        /**
         * <p> Fetches HTTP stream , checks for content-Length and then calls TikaMediaProessor to process</p>
         * <p> Sets response or error to AsyncResponse to complete response</p>
         *
         * @param mediaProcessor         TikaMediaProcessor which wraps Tika requests
         * @param mediaProcessingRequest Original request to Jersey
         * @param asyncResponse          Jersey AsyncResponse to be resumed
         * @param executorService        threadpool manager
         * @throws IOException
         * @throws URISyntaxException
         * @throws NoSuchAlgorithmException
         * @throws InterruptedException
         * @throws ExecutionException
         * @throws TimeoutException
         */
        public void fetchStreamAndProcess(TikaMediaProcessor mediaProcessor,
                                          MediaProcessingRequest mediaProcessingRequest, AsyncResponse asyncResponse,
                                          ExecutorService executorService)
                throws IOException, URISyntaxException, NoSuchAlgorithmException, InterruptedException,
                ExecutionException, TimeoutException {
            final int[] contentLength = new int[1];
            final boolean[] hadContentLengthHeader = new boolean[1];
            try (AsyncHttpClient ahc = asyncHttpClient()) {
                final ListenableFuture<Response> future = ahc.prepareGet(path).execute(
                        new AsyncCompletionHandlerWithContentLengthCheck<Response>(
                                maxFileSizeInBytes,
                                ignoreIfNoHeader,
                                mediaProcessingRequest.getResourcePath()) {

                            @Override
                            protected void onContentLengthRecvd(int cL, boolean hadCl) {
                                contentLength[0] = cL;
                                hadContentLengthHeader[0] = hadCl;
                            }

                            @Override
                            public Response onCompleted(Response response) throws Exception {
                                return response;
                            }
                        });
                Response response = future.get(timeout, TimeUnit.MILLISECONDS);
                if (response.getStatusCode() == javax.ws.rs.core.Response.Status.OK.getStatusCode()) {
                    log.info(mediaProcessingRequest.getResourcePath() + "  was too large to process with " +
                            "content-length " + contentLength[0] + " , content-Length value present in header => " +
                            hadContentLengthHeader[0]);
                    MediaProcessingResponse mediaProcessingResponse = mediaProcessor
                            .processMedia(response.getResponseBodyAsStream(), mediaProcessingRequest.isUseOCR(),
                                    mediaProcessingRequest.getOcrLangs());
                    asyncResponse.resume(javax.ws.rs.core.Response.ok().entity(mediaProcessingResponse).build());

                } else {
                    if (response.getStatusCode() ==
                            javax.ws.rs.core.Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                        log.error(mediaProcessingRequest.getResourcePath() + "  was too large to process with " +
                                "content-length " + contentLength[0] + " , content-Length value present in header => " +
                                hadContentLengthHeader[0]);
                    }
                    asyncResponse
                            .resume(javax.ws.rs.core.Response.serverError().status(response.getStatusCode()).build());
                }
            } catch (TimeoutException toe) {
                log.error("Timeout exception whie downloading file " + toe.getMessage());
                asyncResponse.resume(javax.ws.rs.core.Response.serverError()
                        .status(javax.ws.rs.core.Response.Status.REQUEST_TIMEOUT).build());
            } catch (Throwable throwable) {
                log.error("Exception while processing file " + mediaProcessingRequest.getResourcePath() + " => " +
                        throwable.toString());
                asyncResponse.resume(javax.ws.rs.core.Response.serverError().entity(throwable.getMessage() + " -> " +
                        "while downloading resource").build());
            }
        }

        /**
         * <p> Fetches HTTP stream , checks for content-Length and then calls TikaMediaProessor to detect MIME </p>
         * <p> Sets response or error to AsyncResponse to complete response</p>
         *
         * @param mediaProcessor        TikaMediaProcessor which wraps Tika requests
         * @param mediaDetectionRequest Original request to Jersey
         * @param asyncResponse         Jersey AsyncResponse to be resumed
         * @param executorService       threadpool manager managed by dropwizard
         * @throws IOException
         * @throws URISyntaxException
         * @throws NoSuchAlgorithmException
         * @throws InterruptedException
         * @throws ExecutionException
         * @throws TimeoutException
         */
        public void fetchStreamAndDetect(TikaMediaProcessor mediaProcessor, MediaDetectionRequest mediaDetectionRequest,
                                         AsyncResponse asyncResponse, ExecutorService executorService)
                throws IOException, URISyntaxException, NoSuchAlgorithmException, InterruptedException,
                ExecutionException, TimeoutException {
            final int[] contentLength = new int[1];
            final boolean[] hadContentLengthHeader = new boolean[1];
            try (AsyncHttpClient ahc = asyncHttpClient()) {
                final ListenableFuture<Response> future = ahc.prepareGet(path).execute(
                        new AsyncCompletionHandlerWithContentLengthCheck<Response>(
                                maxFileSizeInBytes,
                                ignoreIfNoHeader,
                                mediaDetectionRequest.getResourcePath()) {

                            @Override
                            protected void onContentLengthRecvd(int cL, boolean hadCl) {
                                contentLength[0] = cL;
                                hadContentLengthHeader[0] = hadCl;
                            }

                            @Override
                            public Response onCompleted(Response response) throws Exception {
                                return response;
                            }
                        });
                try {
                    Response response = future.get(timeout, TimeUnit.MILLISECONDS);
                    if (response.getStatusCode() == javax.ws.rs.core.Response.Status.OK.getStatusCode()) {
                        log.info(mediaDetectionRequest.getResourcePath() + "  was too large to detect with " +
                                "content-length " + contentLength[0] + " , content-Length value present in header => " +
                                hadContentLengthHeader[0]);
                        MediaDetectionResponse mediaProcessingResponse = new MediaDetectionResponse();
                        mediaProcessingResponse
                                .setMimeType(mediaProcessor.detectMimeType(response.getResponseBodyAsStream()));
                        asyncResponse.resume(javax.ws.rs.core.Response.ok().entity(mediaProcessingResponse).build());

                    } else {
                        if (response.getStatusCode() ==
                                javax.ws.rs.core.Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                            log.error(mediaDetectionRequest.getResourcePath() + "  was too large to detect with " +
                                    "content-length " + contentLength[0] +
                                    " , content-Length value present in header => " + hadContentLengthHeader[0]);
                        }
                        asyncResponse.resume(javax.ws.rs.core.Response.serverError().status(response.getStatusCode())
                                .build());
                    }
                } catch (Throwable throwable) {
                    log.error("Exception while detecting mimetype - " + throwable.toString());
                    asyncResponse
                            .resume(javax.ws.rs.core.Response.serverError().entity(throwable.getMessage()).build());
                }
            }
        }

    }

}
