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
import java.net.SocketTimeoutException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import ru.radiomayak.http.HttpEntityEnclosingRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpStatus;
import ru.radiomayak.http.impl.io.DefaultHttpRequestWriterFactory;
import ru.radiomayak.http.impl.io.DefaultHttpResponseParserFactory;
import ru.radiomayak.http.io.HttpMessageParserFactory;
import ru.radiomayak.http.io.HttpMessageWriter;
import ru.radiomayak.http.io.HttpMessageWriterFactory;
import ru.radiomayak.http.util.Args;
import ru.radiomayak.http.impl.entity.LaxContentLengthStrategy;
import ru.radiomayak.http.impl.entity.StrictContentLengthStrategy;

import ru.radiomayak.http.HttpClientConnection;
import ru.radiomayak.http.HttpEntity;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpRequest;

/**
 * Default implementation of {@link HttpClientConnection}.
 *
 * @since 4.3
 */
public class DefaultBHttpClientConnection extends BHttpConnectionBase implements HttpClientConnection {

    private final ru.radiomayak.http.io.HttpMessageParser<HttpResponse> responseParser;
    private final HttpMessageWriter<HttpRequest> requestWriter;

    /**
     * Creates new instance of DefaultBHttpClientConnection.
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
     *   {@link LaxContentLengthStrategy#INSTANCE} will be used.
     * @param outgoingContentStrategy outgoing content length strategy. If {@code null}
     *   {@link StrictContentLengthStrategy#INSTANCE} will be used.
     * @param requestWriterFactory request writer factory. If {@code null}
     *   {@link DefaultHttpRequestWriterFactory#INSTANCE} will be used.
     * @param responseParserFactory response parser factory. If {@code null}
     *   {@link DefaultHttpResponseParserFactory#INSTANCE} will be used.
     */
    public DefaultBHttpClientConnection(
            final int buffersize,
            final int fragmentSizeHint,
            final CharsetDecoder chardecoder,
            final CharsetEncoder charencoder,
            final ru.radiomayak.http.config.MessageConstraints constraints,
            final ru.radiomayak.http.entity.ContentLengthStrategy incomingContentStrategy,
            final ru.radiomayak.http.entity.ContentLengthStrategy outgoingContentStrategy,
            final HttpMessageWriterFactory<HttpRequest> requestWriterFactory,
            final HttpMessageParserFactory<HttpResponse> responseParserFactory) {
        super(buffersize, fragmentSizeHint, chardecoder, charencoder,
                constraints, incomingContentStrategy, outgoingContentStrategy);
        this.requestWriter = (requestWriterFactory != null ? requestWriterFactory :
            DefaultHttpRequestWriterFactory.INSTANCE).create(getSessionOutputBuffer());
        this.responseParser = (responseParserFactory != null ? responseParserFactory :
            DefaultHttpResponseParserFactory.INSTANCE).create(getSessionInputBuffer(), constraints);
    }

    public DefaultBHttpClientConnection(
            final int buffersize,
            final CharsetDecoder chardecoder,
            final CharsetEncoder charencoder,
            final ru.radiomayak.http.config.MessageConstraints constraints) {
        this(buffersize, buffersize, chardecoder, charencoder, constraints, null, null, null, null);
    }

    public DefaultBHttpClientConnection(final int buffersize) {
        this(buffersize, buffersize, null, null, null, null, null, null, null);
    }

    protected void onResponseReceived(final HttpResponse response) {
    }

    protected void onRequestSubmitted(final HttpRequest request) {
    }

    @Override
    public void bind(final Socket socket) throws IOException {
        super.bind(socket);
    }

    @Override
    public boolean isResponseAvailable(final int timeout) throws IOException {
        ensureOpen();
        try {
            return awaitInput(timeout);
        } catch (final SocketTimeoutException ex) {
            return false;
        }
    }

    @Override
    public void sendRequestHeader(final HttpRequest request)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        ensureOpen();
        this.requestWriter.write(request);
        onRequestSubmitted(request);
        incrementRequestCount();
    }

    @Override
    public void sendRequestEntity(final HttpEntityEnclosingRequest request)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        ensureOpen();
        final HttpEntity entity = request.getEntity();
        if (entity == null) {
            return;
        }
        final OutputStream outstream = prepareOutput(request);
        entity.writeTo(outstream);
        outstream.close();
    }

    @Override
    public HttpResponse receiveResponseHeader() throws HttpException, IOException {
        ensureOpen();
        final HttpResponse response = this.responseParser.parse();
        onResponseReceived(response);
        if (response.getStatusLine().getStatusCode() >= HttpStatus.OK.getCode()) {
            incrementResponseCount();
        }
        return response;
    }

    @Override
    public void receiveResponseEntity(
            final HttpResponse response) throws HttpException, IOException {
        Args.notNull(response, "HTTP response");
        ensureOpen();
        final HttpEntity entity = prepareInput(response);
        response.setEntity(entity);
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
        doFlush();
    }

}
