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

package ru.radiomayak.http.protocol;

import java.io.IOException;

import ru.radiomayak.http.HttpStatus;

import ru.radiomayak.http.Header;
import ru.radiomayak.http.HttpEntity;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpResponseInterceptor;
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.ProtocolVersion;

/**
 * ResponseConnControl is responsible for adding {@code Connection} header
 * to the outgoing responses, which is essential for managing persistence of
 * {@code HTTP/1.0} connections. This interceptor is recommended for
 * server side protocol processors.
 *
 * @since 4.0
 */
@ru.radiomayak.http.annotation.Contract(threading = ru.radiomayak.http.annotation.ThreadingBehavior.IMMUTABLE)
public class ResponseConnControl implements HttpResponseInterceptor {

    public ResponseConnControl() {
        super();
    }

    @Override
    public void process(final HttpResponse response, final HttpContext context)
            throws HttpException, IOException {
        ru.radiomayak.http.util.Args.notNull(response, "HTTP response");

        final HttpCoreContext corecontext = HttpCoreContext.adapt(context);

        // Always drop connection after certain type of responses
        final int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.BAD_REQUEST.getCode() ||
                status == HttpStatus.REQUEST_TIMEOUT.getCode() ||
                status == HttpStatus.LENGTH_REQUIRED.getCode() ||
                status == HttpStatus.REQUEST_ENTITY_TOO_LARGE.getCode() ||
                status == HttpStatus.REQUEST_URI_TOO_LONG.getCode() ||
                status == HttpStatus.SERVICE_UNAVAILABLE.getCode() ||
                status == HttpStatus.NOT_IMPLEMENTED.getCode()) {
            response.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
            return;
        }
        final Header explicit = response.getFirstHeader(HTTP.CONN_DIRECTIVE);
        if (explicit != null && HTTP.CONN_CLOSE.equalsIgnoreCase(explicit.getValue())) {
            // Connection persistence explicitly disabled
            return;
        }
        // Always drop connection for HTTP/1.0 responses and below
        // if the content body cannot be correctly delimited
        final HttpEntity entity = response.getEntity();
        if (entity != null) {
            final ProtocolVersion ver = response.getStatusLine().getProtocolVersion();
            if (entity.getContentLength() < 0 &&
                    (!entity.isChunked() || ver.lessEquals(HttpVersion.HTTP_1_0))) {
                response.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
                return;
            }
        }
        // Drop connection if requested by the client or request was <= 1.0
        final HttpRequest request = corecontext.getRequest();
        if (request != null) {
            final Header header = request.getFirstHeader(HTTP.CONN_DIRECTIVE);
            if (header != null) {
                response.setHeader(HTTP.CONN_DIRECTIVE, header.getValue());
            } else if (request.getProtocolVersion().lessEquals(HttpVersion.HTTP_1_0)) {
                response.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
            }
        }
    }

}
