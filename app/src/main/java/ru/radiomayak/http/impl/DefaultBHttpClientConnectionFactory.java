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

import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.io.HttpMessageWriterFactory;
import ru.radiomayak.http.HttpClientConnection;

import java.io.IOException;
import java.net.Socket;

import ru.radiomayak.http.HttpConnectionFactory;
import ru.radiomayak.http.HttpRequest;

/**
 * Default factory for {@link HttpClientConnection}s.
 *
 * @since 4.3
 */
@ru.radiomayak.http.annotation.Contract(threading = ru.radiomayak.http.annotation.ThreadingBehavior.IMMUTABLE_CONDITIONAL)
public class DefaultBHttpClientConnectionFactory implements HttpConnectionFactory<DefaultBHttpClientConnection> {

    public static final DefaultBHttpClientConnectionFactory INSTANCE = new DefaultBHttpClientConnectionFactory();

    private final ru.radiomayak.http.config.ConnectionConfig cconfig;
    private final ru.radiomayak.http.entity.ContentLengthStrategy incomingContentStrategy;
    private final ru.radiomayak.http.entity.ContentLengthStrategy outgoingContentStrategy;
    private final HttpMessageWriterFactory<HttpRequest> requestWriterFactory;
    private final ru.radiomayak.http.io.HttpMessageParserFactory<HttpResponse> responseParserFactory;

    public DefaultBHttpClientConnectionFactory(
            final ru.radiomayak.http.config.ConnectionConfig cconfig,
            final ru.radiomayak.http.entity.ContentLengthStrategy incomingContentStrategy,
            final ru.radiomayak.http.entity.ContentLengthStrategy outgoingContentStrategy,
            final HttpMessageWriterFactory<HttpRequest> requestWriterFactory,
            final ru.radiomayak.http.io.HttpMessageParserFactory<HttpResponse> responseParserFactory) {
        super();
        this.cconfig = cconfig != null ? cconfig : ru.radiomayak.http.config.ConnectionConfig.DEFAULT;
        this.incomingContentStrategy = incomingContentStrategy;
        this.outgoingContentStrategy = outgoingContentStrategy;
        this.requestWriterFactory = requestWriterFactory;
        this.responseParserFactory = responseParserFactory;
    }

    public DefaultBHttpClientConnectionFactory(
            final ru.radiomayak.http.config.ConnectionConfig cconfig,
            final HttpMessageWriterFactory<HttpRequest> requestWriterFactory,
            final ru.radiomayak.http.io.HttpMessageParserFactory<HttpResponse> responseParserFactory) {
        this(cconfig, null, null, requestWriterFactory, responseParserFactory);
    }

    public DefaultBHttpClientConnectionFactory(final ru.radiomayak.http.config.ConnectionConfig cconfig) {
        this(cconfig, null, null, null, null);
    }

    public DefaultBHttpClientConnectionFactory() {
        this(null, null, null, null, null);
    }

    @Override
    public DefaultBHttpClientConnection createConnection(final Socket socket) throws IOException {
        final DefaultBHttpClientConnection conn = new DefaultBHttpClientConnection(
                this.cconfig.getBufferSize(),
                this.cconfig.getFragmentSizeHint(),
                ConnSupport.createDecoder(this.cconfig),
                ConnSupport.createEncoder(this.cconfig),
                this.cconfig.getMessageConstraints(),
                this.incomingContentStrategy,
                this.outgoingContentStrategy,
                this.requestWriterFactory,
                this.responseParserFactory);
        conn.bind(socket);
        return conn;
    }

}
