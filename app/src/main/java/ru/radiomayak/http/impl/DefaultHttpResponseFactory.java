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

import java.util.Locale;

import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpResponseFactory;
import ru.radiomayak.http.StatusLine;
import ru.radiomayak.http.annotation.Contract;

import ru.radiomayak.http.ProtocolVersion;
import ru.radiomayak.http.ReasonPhraseCatalog;

/**
 * Default factory for creating {@link HttpResponse} objects.
 *
 * @since 4.0
 */
@Contract(threading = ru.radiomayak.http.annotation.ThreadingBehavior.IMMUTABLE_CONDITIONAL)
public class DefaultHttpResponseFactory implements HttpResponseFactory {

    public static final DefaultHttpResponseFactory INSTANCE = new DefaultHttpResponseFactory();

    /**
     * The catalog for looking up reason phrases.
     */
    protected final ReasonPhraseCatalog reasonCatalog;


    /**
     * Creates a new response factory with the given catalog.
     *
     * @param catalog the catalog of reason phrases
     */
    public DefaultHttpResponseFactory(final ReasonPhraseCatalog catalog) {
        this.reasonCatalog = ru.radiomayak.http.util.Args.notNull(catalog, "Reason phrase catalog");
    }

    /**
     * Creates a new response factory with the default catalog.
     * The default catalog is {@link EnglishReasonPhraseCatalog}.
     */
    public DefaultHttpResponseFactory() {
        this(EnglishReasonPhraseCatalog.INSTANCE);
    }


    // non-javadoc, see interface HttpResponseFactory
    @Override
    public HttpResponse newHttpResponse(ProtocolVersion ver, int status, ru.radiomayak.http.protocol.HttpContext context) {
        ru.radiomayak.http.util.Args.notNull(ver, "HTTP version");
        final Locale loc = determineLocale(context);
        final String reason = this.reasonCatalog.getReason(status, loc);
        final StatusLine statusline = new ru.radiomayak.http.message.BasicStatusLine(ver, status, reason);
        return new ru.radiomayak.http.message.BasicHttpResponse(statusline, this.reasonCatalog, loc);
    }


    // non-javadoc, see interface HttpResponseFactory
    @Override
    public HttpResponse newHttpResponse(
            final StatusLine statusline,
            final ru.radiomayak.http.protocol.HttpContext context) {
        ru.radiomayak.http.util.Args.notNull(statusline, "Status line");
        return new ru.radiomayak.http.message.BasicHttpResponse(statusline, this.reasonCatalog, determineLocale(context));
    }

    /**
     * Determines the locale of the response.
     * The implementation in this class always returns the default locale.
     *
     * @param context the context from which to determine the locale, or
     *                {@code null} to use the default locale
     * @return the locale for the response, never {@code null}
     */
    protected Locale determineLocale(final ru.radiomayak.http.protocol.HttpContext context) {
        return Locale.getDefault();
    }

}
