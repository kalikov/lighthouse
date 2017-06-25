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

import ru.radiomayak.http.HttpStatus;
import ru.radiomayak.http.ReasonPhraseCatalog;
import ru.radiomayak.http.annotation.Contract;

/**
 * English reason phrases for HTTP status codes.
 * All status codes defined in RFC1945 (HTTP/1.0), RFC2616 (HTTP/1.1), and
 * RFC2518 (WebDAV) are supported.
 *
 * @since 4.0
 */
@Contract(threading = ru.radiomayak.http.annotation.ThreadingBehavior.IMMUTABLE)
public class EnglishReasonPhraseCatalog implements ReasonPhraseCatalog {

    // static array with english reason phrases defined below

    /**
     * The default instance of this catalog.
     * This catalog is thread safe, so there typically
     * is no need to create other instances.
     */
    public final static EnglishReasonPhraseCatalog INSTANCE = new EnglishReasonPhraseCatalog();


    /**
     * Restricted default constructor, for derived classes.
     * If you need an instance of this class, use {@link #INSTANCE INSTANCE}.
     */
    protected EnglishReasonPhraseCatalog() {
        // no body
    }


    /**
     * Obtains the reason phrase for a status code.
     *
     * @param status the status code, in the range 100-599
     * @param loc    ignored
     * @return the reason phrase, or {@code null}
     */
    @Override
    public String getReason(final int status, final Locale loc) {
        ru.radiomayak.http.util.Args.check(status >= 100 && status < 600, "Unknown category for status code " + status);

        String reason = null;
        HttpStatus httpStatus = HttpStatus.fromCode(status);
        if (httpStatus != null) {
            reason = httpStatus.getReason();
        }

        return reason;
    }
}
