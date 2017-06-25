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

import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.annotation.Contract;
import ru.radiomayak.http.io.HttpMessageWriterFactory;
import ru.radiomayak.http.message.BasicLineFormatter;

/**
 * Default factory for request message writers.
 *
 * @since 4.3
 */
@Contract(threading = ru.radiomayak.http.annotation.ThreadingBehavior.IMMUTABLE_CONDITIONAL)
public class DefaultHttpRequestWriterFactory implements HttpMessageWriterFactory<HttpRequest> {

    public static final DefaultHttpRequestWriterFactory INSTANCE = new DefaultHttpRequestWriterFactory();

    private final ru.radiomayak.http.message.LineFormatter lineFormatter;

    public DefaultHttpRequestWriterFactory(final ru.radiomayak.http.message.LineFormatter lineFormatter) {
        super();
        this.lineFormatter = lineFormatter != null ? lineFormatter : BasicLineFormatter.INSTANCE;
    }

    public DefaultHttpRequestWriterFactory() {
        this(null);
    }

    @Override
    public ru.radiomayak.http.io.HttpMessageWriter<HttpRequest> create(final ru.radiomayak.http.io.SessionOutputBuffer buffer) {
        return new DefaultHttpRequestWriter(buffer, lineFormatter);
    }

}
