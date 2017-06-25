/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package ru.radiomayak.http.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import ru.radiomayak.http.HttpEntityEnclosingRequest;
import ru.radiomayak.http.HttpEntity;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpServerConnection;
import ru.radiomayak.http.impl.entity.DisallowIdentityContentLengthStrategy;
import ru.radiomayak.http.impl.io.DefaultHttpRequestParserFactory;
import ru.radiomayak.http.impl.io.DefaultHttpResponseWriterFactory;
import ru.radiomayak.http.io.HttpMessageWriter;
import ru.radiomayak.http.io.HttpMessageWriterFactory;
import ru.radiomayak.http.util.Args;
import ru.radiomayak.http.impl.entity.StrictContentLengthStrategy;

import ru.radiomayak.http.HttpException;

/**
 * Default implementation of {@link HttpServerConnection}.
 *
 * @since 4.3
 */
public class DefaultBHttpServerConnection extends BHttpConnectionBase implements HttpServerConnection {

    private final ru.radiomayak.http.io.HttpMessageParser<HttpRequest> requestParser;
    private final HttpMessageWriter<HttpResponse> responseWriter;

    /**
     * Creates new instance of DefaultBHttpServerConnection.
     *
     * @param buffersize buffer size. Must be a positive number.
     * @param fragmentSizeHint fragment size hint.
     * @param chardecoder decoder to be used for decoding HTTP protocol elements.
     *   If {@code null} simple type cast will be used for byte to char conversion.
     * @param charencoder encoder to be used for encoding HTTP protocol elements.
     *   If {@code null} simple type cast will be used for char to byte conversion.
     * @param constraints Message constraints. If {@code null}
     *   {@link ru.radiomayak.http.config.MessageConstraints#DEFAULT} will be used.
     * @param incomingContentStrategy incoming content length strategy. If {@code null}
     *   {@link DisallowIdentityContentLengthStrategy#INSTANCE} will be used.
     * @param outgoingContentStrategy outgoing content length strategy. If {@code null}
     *   {@link StrictContentLengthStrategy#INSTANCE} will be used.
     * @param requestParserFactory request parser factory. If {@code null}
     *   {@link DefaultHttpRequestParserFactory#INSTANCE} will be used.
     * @param responseWriterFactory response writer factory. If {@code null}
     *   {@link DefaultHttpResponseWriterFactory#INSTANCE} will be used.
     */
    public DefaultBHttpServerConnection(
            final int buffersize,
            final int fragmentSizeHint,
            final CharsetDecoder chardecoder,
            final CharsetEncoder charencoder,
            final ru.radiomayak.http.config.MessageConstraints constraints,
            final ru.radiomayak.http.entity.ContentLengthStrategy incomingContentStrategy,
            final ru.radiomayak.http.entity.ContentLengthStrategy outgoingContentStrategy,
            final ru.radiomayak.http.io.HttpMessageParserFactory<HttpRequest> requestParserFactory,
            final HttpMessageWriterFactory<HttpResponse> responseWriterFactory) {
        super(buffersize, fragmentSizeHint, chardecoder, charencoder, constraints,
                incomingContentStrategy != null ? incomingContentStrategy :
                    DisallowIdentityContentLengthStrategy.INSTANCE, outgoingContentStrategy);
        this.requestParser = (requestParserFactory != null ? requestParserFactory :
            DefaultHttpRequestParserFactory.INSTANCE).create(getSessionInputBuffer(), constraints);
        this.responseWriter = (responseWriterFactory != null ? responseWriterFactory :
            DefaultHttpResponseWriterFactory.INSTANCE).create(getSessionOutputBuffer());
    }

    public DefaultBHttpServerConnection(
            final int buffersize,
            final CharsetDecoder chardecoder,
            final CharsetEncoder charencoder,
            final ru.radiomayak.http.config.MessageConstraints constraints) {
        this(buffersize, buffersize, chardecoder, charencoder, constraints, null, null, null, null);
    }

    public DefaultBHttpServerConnection(final int buffersize) {
        this(buffersize, buffersize, null, null, null, null, null, null, null);
    }

    protected void onRequestReceived(final HttpRequest request) {
    }

    protected void onResponseSubmitted(final HttpResponse response) {
    }

    @Override
    public void bind(final Socket socket) throws IOException {
        super.bind(socket);
    }

    @Override
    public HttpRequest receiveRequestHeader()
            throws HttpException, IOException {
        ensureOpen();
        final HttpRequest request = this.requestParser.parse();
        onRequestReceived(request);
        incrementRequestCount();
        return request;
    }

    @Override
    public void receiveRequestEntity(final HttpEntityEnclosingRequest request)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        ensureOpen();
        final HttpEntity entity = prepareInput(request);
        request.setEntity(entity);
    }

    @Override
    public void sendResponseHeader(final HttpResponse response)
            throws HttpException, IOException {
        Args.notNull(response, "HTTP response");
        ensureOpen();
        this.responseWriter.write(response);
        onResponseSubmitted(response);
        if (response.getStatusLine().getStatusCode() >= 200) {
            incrementResponseCount();
        }
    }

    @Override
    public void sendResponseEntity(final HttpResponse response)
            throws HttpException, IOException {
        Args.notNull(response, "HTTP response");
        ensureOpen();
        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            return;
        }
        final OutputStream outstream = prepareOutput(response);
        entity.writeTo(outstream);
        outstream.close();
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
        doFlush();
    }

}
