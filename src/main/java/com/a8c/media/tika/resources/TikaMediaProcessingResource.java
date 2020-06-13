package com.a8c.media.tika.resources;

import com.a8c.media.tika.api.MediaDetectionRequest;
import com.a8c.media.tika.api.MediaDetectionResponse;
import com.a8c.media.tika.api.MediaProcessingRequest;
import com.a8c.media.tika.core.TikaMediaProcessor;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.name.Named;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class TikaMediaProcessingResource {

    final TikaMediaProcessor tikaMediaProcessor;
    final List<String> httpWhiteList;
    @Inject
    public TikaMediaProcessingResource(TikaMediaProcessor tikaMediaProcessor,
                                       @Named("http-whitelist") List<String> httpWhiteList) {
        this.tikaMediaProcessor = tikaMediaProcessor;
        this.httpWhiteList = httpWhiteList;
    }

    @POST
    @Path("/process")
    @Timed(name ="time-processMedia")
    @Metered
    public Response processMedia(MediaProcessingRequest mediaProcessingRequest) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return Response.ok( tikaMediaProcessor.processMedia(mediaProcessingRequest.getResoucePath(),
                    mediaProcessingRequest.getMimeType(),mediaProcessingRequest.getProcessingTimeout(),
                    mediaProcessingRequest.isUseOCR(), mediaProcessingRequest.getOcrLangs())).build();
        } catch (TimeoutException exception) {
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity(exception.toString()).build();
        } catch (Exception exception) {
            return Response.serverError().entity(exception.toString()).build();
        }
    }

    @POST
    @Path("/detect")
    @Timed(name ="time-detectMedia")
    @Metered
    public Response detectMedia(MediaDetectionRequest mediaDetectionRequest) throws InterruptedException,
            ExecutionException, TimeoutException {
        MediaDetectionResponse mediaDetectionResponse = new MediaDetectionResponse();
        try {
            mediaDetectionResponse.setMimeType(tikaMediaProcessor.detectMimeType(mediaDetectionRequest.getResoucePath(),mediaDetectionRequest.getProcessingTimeout()));
            return Response.ok(mediaDetectionResponse).build();
        } catch (TimeoutException exception) {
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity(exception.toString()).build();
        }catch (RejectedExecutionException rejected){
            return Response.status(Response.Status.TOO_MANY_REQUESTS).build();
        }
        catch (Exception exception) {
            return Response.serverError().entity(exception.toString()).build();
        }
    }


}
