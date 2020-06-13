package com.a8c.media.tika.resources;

import com.a8c.media.tika.api.MimeTypesInfoResponse;
import com.google.common.collect.Lists;
import org.apache.tika.mime.MimeTypes;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/info")
@Produces(MediaType.APPLICATION_JSON)
public class TikaMetaInformationResource  {

    @Path("/supportedTypes")
    @GET
    public Response getSupportedMimeTypes(){
        MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();
        MimeTypesInfoResponse mimeTypesInfoResponse = new MimeTypesInfoResponse();
        List<String> mediaTypes = Lists.newArrayList();
        for(org.apache.tika.mime.MediaType mediaType: MimeTypes.getDefaultMimeTypes().getMediaTypeRegistry().getTypes()){
            mediaTypes.add(mediaType.toString());
        }
        mimeTypesInfoResponse.setMimeTypes(mediaTypes);
        return Response.ok(mimeTypesInfoResponse).build();
    }
}
