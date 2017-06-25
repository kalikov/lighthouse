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

import ru.radiomayak.http.Header;
import ru.radiomayak.http.HeaderIterator;
import ru.radiomayak.http.HttpHeaders;
import ru.radiomayak.http.HttpStatus;

import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.ParseException;
import ru.radiomayak.http.ProtocolVersion;
import ru.radiomayak.http.TokenIterator;

import ru.radiomayak.http.annotation.Contract;
import ru.radiomayak.http.annotation.ThreadingBehavior;
import ru.radiomayak.http.protocol.HTTP;
import ru.radiomayak.http.util.Args;

/**
 * Default implementation of a strategy deciding about connection re-use.
 * The default implementation first checks some basics, for example
 * whether the connection is still open or whether the end of the
 * request entity can be determined without closing the connection.
 * If these checks pass, the tokens in the {@code Connection} header will
 * be examined. In the absence of a {@code Connection} header, the
 * non-standard but commonly used {@code Proxy-Connection} header takes
 * it's role. A token {@code close} indicates that the connection cannot
 * be reused. If there is no such token, a token {@code keep-alive}
 * indicates that the connection should be re-used. If neither token is found,
 * or if there are no {@code Connection} headers, the default policy for
 * the HTTP version is applied. Since {@code HTTP/1.1}, connections are
 * re-used by default. Up until {@code HTTP/1.0}, connections are not
 * re-used by default.
 *
 * @since 4.0
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class DefaultConnectionReuseStrategy implements ru.radiomayak.http.ConnectionReuseStrategy {

    public static final DefaultConnectionReuseStrategy INSTANCE = new DefaultConnectionReuseStrategy();

    public DefaultConnectionReuseStrategy() {
        super();
    }

    // see interface ConnectionReuseStrategy
    @Override
    public boolean keepAlive(final HttpResponse response, final ru.radiomayak.http.protocol.HttpContext context) {
        Args.notNull(response, "HTTP response");
        Args.notNull(context, "HTTP context");

        final HttpRequest request = (HttpRequest) context.getAttribute(ru.radiomayak.http.protocol.HttpCoreContext.HTTP_REQUEST);
        if (request != null) {
            try {
                final TokenIterator ti = new ru.radiomayak.http.message.BasicTokenIterator(request.headerIterator(HttpHeaders.CONNECTION));
                while (ti.hasNext()) {
                    final String token = ti.nextToken();
                    if (HTTP.CONN_CLOSE.equalsIgnoreCase(token)) {
                        return false;
                    }
                }
            } catch (final ParseException px) {
                // invalid connection header. do not re-use
                return false;
            }
        }

        // Check for a self-terminating entity. If the end of the entity will
        // be indicated by closing the connection, there is no keep-alive.
        final ProtocolVersion ver = response.getStatusLine().getProtocolVersion();
        final Header teh = response.getFirstHeader(HTTP.TRANSFER_ENCODING);
        if (teh != null) {
            if (!HTTP.CHUNK_CODING.equalsIgnoreCase(teh.getValue())) {
                return false;
            }
        } else {
            if (canResponseHaveBody(request, response)) {
                final Header[] clhs = response.getHeaders(HTTP.CONTENT_LEN);
                // Do not reuse if not properly content-length delimited
                if (clhs.length == 1) {
                    final Header clh = clhs[0];
                    try {
                        final int contentLen = Integer.parseInt(clh.getValue());
                        if (contentLen < 0) {
                            return false;
                        }
                    } catch (final NumberFormatException ex) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        // Check for the "Connection" header. If that is absent, check for
        // the "Proxy-Connection" header. The latter is an unspecified and
        // broken but unfortunately common extension of HTTP.
        HeaderIterator headerIterator = response.headerIterator(HTTP.CONN_DIRECTIVE);
        if (!headerIterator.hasNext()) {
            headerIterator = response.headerIterator("Proxy-Connection");
        }

        // Experimental usage of the "Connection" header in HTTP/1.0 is
        // documented in RFC 2068, section 19.7.1. A token "keep-alive" is
        // used to indicate that the connection should be persistent.
        // Note that the final specification of HTTP/1.1 in RFC 2616 does not
        // include this information. Neither is the "Connection" header
        // mentioned in RFC 1945, which informally describes HTTP/1.0.
        //
        // RFC 2616 specifies "close" as the only connection token with a
        // specific meaning: it disables persistent connections.
        //
        // The "Proxy-Connection" header is not formally specified anywhere,
        // but is commonly used to carry one token, "close" or "keep-alive".
        // The "Connection" header, on the other hand, is defined as a
        // sequence of tokens, where each token is a header name, and the
        // token "close" has the above-mentioned additional meaning.
        //
        // To get through this mess, we treat the "Proxy-Connection" header
        // in exactly the same way as the "Connection" header, but only if
        // the latter is missing. We scan the sequence of tokens for both
        // "close" and "keep-alive". As "close" is specified by RFC 2068,
        // it takes precedence and indicates a non-persistent connection.
        // If there is no "close" but a "keep-alive", we take the hint.

        if (headerIterator.hasNext()) {
            try {
                final TokenIterator ti = new ru.radiomayak.http.message.BasicTokenIterator(headerIterator);
                boolean keepalive = false;
                while (ti.hasNext()) {
                    final String token = ti.nextToken();
                    if (HTTP.CONN_CLOSE.equalsIgnoreCase(token)) {
                        return false;
                    } else if (HTTP.CONN_KEEP_ALIVE.equalsIgnoreCase(token)) {
                        // continue the loop, there may be a "close" afterwards
                        keepalive = true;
                    }
                }
                if (keepalive) {
                    return true;
                // neither "close" nor "keep-alive", use default policy
                }

            } catch (final ParseException px) {
                // invalid connection header. do not re-use
                return false;
            }
        }

        // default since HTTP/1.1 is persistent, before it was non-persistent
        return !ver.lessEquals(HttpVersion.HTTP_1_0);
    }


    /**
     * Creates a token iterator from a header iterator.
     * This method can be overridden to replace the implementation of
     * the token iterator.
     *
     * @param hit       the header iterator
     *
     * @return  the token iterator
     */
    protected TokenIterator createTokenIterator(final HeaderIterator hit) {
        return new ru.radiomayak.http.message.BasicTokenIterator(hit);
    }

    private boolean canResponseHaveBody(final HttpRequest request, final HttpResponse response) {
        if (request != null && request.getRequestLine().getMethod().equalsIgnoreCase("HEAD")) {
            return false;
        }
        final int status = response.getStatusLine().getStatusCode();
        return status >= HttpStatus.OK.getCode()
            && status != HttpStatus.NO_CONTENT.getCode()
            && status != HttpStatus.NOT_MODIFIED.getCode()
            && status != HttpStatus.RESET_CONTENT.getCode();
    }

}
