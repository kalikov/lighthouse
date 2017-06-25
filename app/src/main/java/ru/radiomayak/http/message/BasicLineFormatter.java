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

package ru.radiomayak.http.message;

import ru.radiomayak.http.FormattedHeader;
import ru.radiomayak.http.Header;
import ru.radiomayak.http.ProtocolVersion;
import ru.radiomayak.http.RequestLine;
import ru.radiomayak.http.StatusLine;

/**
 * Interface for formatting elements of the HEAD section of an HTTP message.
 * This is the complement to {@link LineParser}.
 * There are individual methods for formatting a request line, a
 * status line, or a header line. The formatting does <i>not</i> include the
 * trailing line break sequence CR-LF.
 * The formatted lines are returned in memory, the formatter does not depend
 * on any specific IO mechanism.
 * Instances of this interface are expected to be stateless and thread-safe.
 *
 * @since 4.0
 */
@ru.radiomayak.http.annotation.Contract(threading = ru.radiomayak.http.annotation.ThreadingBehavior.IMMUTABLE)
public class BasicLineFormatter implements LineFormatter {

    /**
     * A default instance of this class, for use as default or fallback.
     * Note that {@link BasicLineFormatter} is not a singleton, there can
     * be many instances of the class itself and of derived classes.
     * The instance here provides non-customized, default behavior.
     */
    public final static BasicLineFormatter INSTANCE = new BasicLineFormatter();

    public BasicLineFormatter() {
        super();
    }

    /**
     * Obtains a buffer for formatting.
     *
     * @param charBuffer a buffer already available, or {@code null}
     *
     * @return  the cleared argument buffer if there is one, or
     *          a new empty buffer that can be used for formatting
     */
    protected ru.radiomayak.http.util.CharArrayBuffer initBuffer(final ru.radiomayak.http.util.CharArrayBuffer charBuffer) {
        ru.radiomayak.http.util.CharArrayBuffer buffer = charBuffer;
        if (buffer != null) {
            buffer.clear();
        } else {
            buffer = new ru.radiomayak.http.util.CharArrayBuffer(64);
        }
        return buffer;
    }


    /**
     * Formats a protocol version.
     *
     * @param version           the protocol version to format
     * @param formatter         the formatter to use, or
     *                          {@code null} for the
     *                          {@link #INSTANCE default}
     *
     * @return  the formatted protocol version
     */
    public static
        String formatProtocolVersion(final ProtocolVersion version,
                                     final LineFormatter formatter) {
        return (formatter != null ? formatter : BasicLineFormatter.INSTANCE)
                .appendProtocolVersion(null, version).toString();
    }


    // non-javadoc, see interface LineFormatter
    @Override
    public ru.radiomayak.http.util.CharArrayBuffer appendProtocolVersion(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                                 final ProtocolVersion version) {
        ru.radiomayak.http.util.Args.notNull(version, "Protocol version");
        // can't use initBuffer, that would clear the argument!
        ru.radiomayak.http.util.CharArrayBuffer result = buffer;
        final int len = estimateProtocolVersionLen(version);
        if (result == null) {
            result = new ru.radiomayak.http.util.CharArrayBuffer(len);
        } else {
            result.ensureCapacity(len);
        }

        result.append(version.getProtocol());
        result.append('/');
        result.append(Integer.toString(version.getMajor()));
        result.append('.');
        result.append(Integer.toString(version.getMinor()));

        return result;
    }


    /**
     * Guesses the length of a formatted protocol version.
     * Needed to guess the length of a formatted request or status line.
     *
     * @param version   the protocol version to format, or {@code null}
     *
     * @return  the estimated length of the formatted protocol version,
     *          in characters
     */
    protected int estimateProtocolVersionLen(final ProtocolVersion version) {
        return version.getProtocol().length() + 4; // room for "HTTP/1.1"
    }


    /**
     * Formats a request line.
     *
     * @param reqline           the request line to format
     * @param formatter         the formatter to use, or
     *                          {@code null} for the
     *                          {@link #INSTANCE default}
     *
     * @return  the formatted request line
     */
    public static String formatRequestLine(final RequestLine reqline,
                                           final LineFormatter formatter) {
        return (formatter != null ? formatter : BasicLineFormatter.INSTANCE)
                .formatRequestLine(null, reqline).toString();
    }


    // non-javadoc, see interface LineFormatter
    @Override
    public ru.radiomayak.http.util.CharArrayBuffer formatRequestLine(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                             final RequestLine reqline) {
        ru.radiomayak.http.util.Args.notNull(reqline, "Request line");
        final ru.radiomayak.http.util.CharArrayBuffer result = initBuffer(buffer);
        doFormatRequestLine(result, reqline);

        return result;
    }


    /**
     * Actually formats a request line.
     * Called from {@link #formatRequestLine}.
     *
     * @param buffer    the empty buffer into which to format,
     *                  never {@code null}
     * @param reqline   the request line to format, never {@code null}
     */
    protected void doFormatRequestLine(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                       final RequestLine reqline) {
        final String method = reqline.getMethod();
        final String uri    = reqline.getUri();

        // room for "GET /index.html HTTP/1.1"
        final int len = method.length() + 1 + uri.length() + 1 +
            estimateProtocolVersionLen(reqline.getProtocolVersion());
        buffer.ensureCapacity(len);

        buffer.append(method);
        buffer.append(' ');
        buffer.append(uri);
        buffer.append(' ');
        appendProtocolVersion(buffer, reqline.getProtocolVersion());
    }



    /**
     * Formats a status line.
     *
     * @param statline          the status line to format
     * @param formatter         the formatter to use, or
     *                          {@code null} for the
     *                          {@link #INSTANCE default}
     *
     * @return  the formatted status line
     */
    public static String formatStatusLine(final StatusLine statline,
                                          final LineFormatter formatter) {
        return (formatter != null ? formatter : BasicLineFormatter.INSTANCE)
                .formatStatusLine(null, statline).toString();
    }


    // non-javadoc, see interface LineFormatter
    @Override
    public ru.radiomayak.http.util.CharArrayBuffer formatStatusLine(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                            final StatusLine statline) {
        ru.radiomayak.http.util.Args.notNull(statline, "Status line");
        final ru.radiomayak.http.util.CharArrayBuffer result = initBuffer(buffer);
        doFormatStatusLine(result, statline);

        return result;
    }


    /**
     * Actually formats a status line.
     * Called from {@link #formatStatusLine}.
     *
     * @param buffer    the empty buffer into which to format,
     *                  never {@code null}
     * @param statline  the status line to format, never {@code null}
     */
    protected void doFormatStatusLine(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                      final StatusLine statline) {

        int len = estimateProtocolVersionLen(statline.getProtocolVersion())
            + 1 + 3 + 1; // room for "HTTP/1.1 200 "
        final String reason = statline.getReasonPhrase();
        if (reason != null) {
            len += reason.length();
        }
        buffer.ensureCapacity(len);

        appendProtocolVersion(buffer, statline.getProtocolVersion());
        buffer.append(' ');
        buffer.append(Integer.toString(statline.getStatusCode()));
        buffer.append(' '); // keep whitespace even if reason phrase is empty
        if (reason != null) {
            buffer.append(reason);
        }
    }


    /**
     * Formats a header.
     *
     * @param header            the header to format
     * @param formatter         the formatter to use, or
     *                          {@code null} for the
     *                          {@link #INSTANCE default}
     *
     * @return  the formatted header
     */
    public static String formatHeader(final Header header,
                                      final LineFormatter formatter) {
        return (formatter != null ? formatter : BasicLineFormatter.INSTANCE)
                .formatHeader(null, header).toString();
    }


    // non-javadoc, see interface LineFormatter
    @Override
    public ru.radiomayak.http.util.CharArrayBuffer formatHeader(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                        final Header header) {
        ru.radiomayak.http.util.Args.notNull(header, "Header");
        final ru.radiomayak.http.util.CharArrayBuffer result;

        if (header instanceof FormattedHeader) {
            // If the header is backed by a buffer, re-use the buffer
            result = ((FormattedHeader)header).getBuffer();
        } else {
            result = initBuffer(buffer);
            doFormatHeader(result, header);
        }
        return result;

    } // formatHeader


    /**
     * Actually formats a header.
     * Called from {@link #formatHeader}.
     *
     * @param buffer    the empty buffer into which to format,
     *                  never {@code null}
     * @param header    the header to format, never {@code null}
     */
    protected void doFormatHeader(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                  final Header header) {
        final String name = header.getName();
        final String value = header.getValue();

        int len = name.length() + 2;
        if (value != null) {
            len += value.length();
        }
        buffer.ensureCapacity(len);

        buffer.append(name);
        buffer.append(": ");
        if (value != null) {
            buffer.append(value);
        }
    }


} // class BasicLineFormatter
