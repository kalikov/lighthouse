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

import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpRequestInterceptor;
import ru.radiomayak.http.annotation.ThreadingBehavior;
import ru.radiomayak.http.annotation.Contract;
import ru.radiomayak.http.util.Args;

/**
 * RequestConnControl is responsible for adding {@code Connection} header
 * to the outgoing requests, which is essential for managing persistence of
 * {@code HTTP/1.0} connections. This interceptor is recommended for
 * client side protocol processors.
 *
 * @since 4.0
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class RequestConnControl implements HttpRequestInterceptor {

    public RequestConnControl() {
        super();
    }

    @Override
    public void process(final HttpRequest request, final HttpContext context)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");

        final String method = request.getRequestLine().getMethod();
        if (method.equalsIgnoreCase("CONNECT")) {
            return;
        }

        if (!request.containsHeader(ru.radiomayak.http.protocol.HTTP.CONN_DIRECTIVE)) {
            // Default policy is to keep connection alive
            // whenever possible
            request.addHeader(ru.radiomayak.http.protocol.HTTP.CONN_DIRECTIVE, ru.radiomayak.http.protocol.HTTP.CONN_KEEP_ALIVE);
        }
    }

}
