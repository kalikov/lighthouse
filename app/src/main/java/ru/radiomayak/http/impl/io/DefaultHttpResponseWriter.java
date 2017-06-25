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

import ru.radiomayak.http.message.BasicLineFormatter;

import ru.radiomayak.http.HttpResponse;

/**
 * HTTP response writer that serializes its output to an instance of {@link ru.radiomayak.http.io.SessionOutputBuffer}.
 *
 * @since 4.3
 */
public class DefaultHttpResponseWriter extends AbstractMessageWriter<HttpResponse> {

    /**
     * Creates an instance of DefaultHttpResponseWriter.
     *
     * @param buffer the session output buffer.
     * @param formatter the line formatter If {@code null}
     *  {@link BasicLineFormatter#INSTANCE}
     *   will be used.
     */
    public DefaultHttpResponseWriter(
            final ru.radiomayak.http.io.SessionOutputBuffer buffer,
            final ru.radiomayak.http.message.LineFormatter formatter) {
        super(buffer, formatter);
    }

    public DefaultHttpResponseWriter(final ru.radiomayak.http.io.SessionOutputBuffer buffer) {
        super(buffer, null);
    }

    @Override
    protected void writeHeadLine(final HttpResponse message) throws IOException {
        lineFormatter.formatStatusLine(this.lineBuf, message.getStatusLine());
        this.sessionBuffer.writeLine(this.lineBuf);
    }

}
