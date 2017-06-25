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

package ru.radiomayak.http.impl.io;

import java.io.IOException;

import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpRequestFactory;
import ru.radiomayak.http.ParseException;
import ru.radiomayak.http.RequestLine;

import ru.radiomayak.http.io.SessionInputBuffer;
import ru.radiomayak.http.message.BasicLineParser;

/**
 * HTTP request parser that obtain its input from an instance
 * of {@link SessionInputBuffer}.
 *
 * @since 4.2
 */
public class DefaultHttpRequestParser extends AbstractMessageParser<HttpRequest> {

    private final HttpRequestFactory requestFactory;
    private final ru.radiomayak.http.util.CharArrayBuffer lineBuf;

    /**
     * Creates new instance of DefaultHttpRequestParser.
     *
     * @param buffer the session input buffer.
     * @param lineParser the line parser. If {@code null}
     *   {@link BasicLineParser#INSTANCE} will be used.
     * @param requestFactory the response factory. If {@code null}
     *   {@link ru.radiomayak.http.impl.DefaultHttpRequestFactory#INSTANCE} will be used.
     * @param constraints the message constraints. If {@code null}
     *   {@link ru.radiomayak.http.config.MessageConstraints#DEFAULT} will be used.
     *
     * @since 4.3
     */
    public DefaultHttpRequestParser(
            final SessionInputBuffer buffer,
            final ru.radiomayak.http.message.LineParser lineParser,
            final HttpRequestFactory requestFactory,
            final ru.radiomayak.http.config.MessageConstraints constraints) {
        super(buffer, lineParser, constraints);
        this.requestFactory = requestFactory != null ? requestFactory :
            ru.radiomayak.http.impl.DefaultHttpRequestFactory.INSTANCE;
        this.lineBuf = new ru.radiomayak.http.util.CharArrayBuffer(128);
    }

    /**
     * @since 4.3
     */
    public DefaultHttpRequestParser(
            final SessionInputBuffer buffer,
            final ru.radiomayak.http.config.MessageConstraints constraints) {
        this(buffer, null, null, constraints);
    }

    /**
     * @since 4.3
     */
    public DefaultHttpRequestParser(final SessionInputBuffer buffer) {
        this(buffer, null, null, ru.radiomayak.http.config.MessageConstraints.DEFAULT);
    }

    @Override
    protected HttpRequest parseHead(
            final SessionInputBuffer sessionBuffer)
        throws IOException, HttpException, ParseException {

        this.lineBuf.clear();
        final int i = sessionBuffer.readLine(this.lineBuf);
        if (i == -1) {
            throw new ru.radiomayak.http.ConnectionClosedException("Client closed connection");
        }
        final ru.radiomayak.http.message.ParserCursor cursor = new ru.radiomayak.http.message.ParserCursor(0, this.lineBuf.length());
        final RequestLine requestline = this.lineParser.parseRequestLine(this.lineBuf, cursor);
        return this.requestFactory.newHttpRequest(requestline);
    }

}
