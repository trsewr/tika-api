package com.a8c.media.tika.core;

import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;
import org.asynchttpclient.handler.ProgressAsyncHandler;
import org.asynchttpclient.uri.Uri;

import java.net.SocketAddress;

@Slf4j
public abstract class AsyncCompletionHandlerWithContentLengthCheck<T> implements ProgressAsyncHandler<T> {
    private final Response.ResponseBuilder builder = new Response.ResponseBuilder();
    private final int maxContentLength;
    private final boolean ignoreIfNoHeader;
    private final String path;

    public AsyncCompletionHandlerWithContentLengthCheck(int maxContentLength, boolean ignoreIfNoHeader, String path) {
        this.maxContentLength = maxContentLength;
        this.ignoreIfNoHeader = ignoreIfNoHeader;
        this.path = path;
    }

    public State onStatusReceived(HttpResponseStatus status) throws Exception {
        this.builder.reset();
        this.builder.accumulate(status);
        return State.CONTINUE;
    }


    public State onHeadersReceived(HttpHeaders headers) throws Exception {
        int contentLength = 0;
        if (headers.contains("Content-Length")) {
            contentLength = headers.getInt("Content-Length");
            onContentLengthRecvd(contentLength, true);
            log.info(path + " returned with content length of " + contentLength);
            if (contentLength > maxContentLength) {
                this.builder.accumulate(new HttpResponseStatus(Uri.create(path)) {
                    @Override
                    public int getStatusCode() {
                        return 413;
                    }

                    @Override
                    public String getStatusText() {
                        return "too Large file to process";
                    }

                    @Override
                    public String getProtocolName() {
                        return "http";
                    }

                    @Override
                    public int getProtocolMajorVersion() {
                        return 0;
                    }

                    @Override
                    public int getProtocolMinorVersion() {
                        return 0;
                    }

                    @Override
                    public String getProtocolText() {
                        return null;
                    }

                    @Override
                    public SocketAddress getRemoteAddress() {
                        return null;
                    }

                    @Override
                    public SocketAddress getLocalAddress() {
                        return null;
                    }
                });
                return State.ABORT;
            }
        } else {
            onContentLengthRecvd(-1, false);
            if (!ignoreIfNoHeader) {
                return State.ABORT;
            }
        }
        this.builder.accumulate(headers);
        return State.CONTINUE;
    }

    protected abstract void onContentLengthRecvd(int contentLength, boolean b);

    public State onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
        this.builder.accumulate(content);
        return State.CONTINUE;
    }

    public State onTrailingHeadersReceived(HttpHeaders headers) throws Exception {
        this.builder.accumulate(headers);
        return State.CONTINUE;
    }

    public final T onCompleted() throws Exception {
        return this.onCompleted(this.builder.build());
    }

    public void onThrowable(Throwable t) {
        log.debug(t.getMessage(), t);
    }

    public abstract T onCompleted(Response var1) throws Exception;

    public State onHeadersWritten() {
        return State.CONTINUE;
    }

    public State onContentWritten() {
        return State.CONTINUE;
    }

    public State onContentWriteProgress(long amount, long current, long total) {
        return State.CONTINUE;
    }
}