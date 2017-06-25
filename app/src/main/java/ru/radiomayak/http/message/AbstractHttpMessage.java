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

import ru.radiomayak.http.Header;
import ru.radiomayak.http.HeaderIterator;
import ru.radiomayak.http.HttpMessage;
import ru.radiomayak.http.util.Args;

/**
 * Basic implementation of {@link HttpMessage}.
 *
 * @since 4.0
 */
public abstract class AbstractHttpMessage implements HttpMessage {

    protected HeaderGroup headergroup;

    protected AbstractHttpMessage() {
        this.headergroup = new HeaderGroup();
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public boolean containsHeader(final String name) {
        return this.headergroup.containsHeader(name);
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public Header[] getHeaders(final String name) {
        return this.headergroup.getHeaders(name);
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public Header getFirstHeader(final String name) {
        return this.headergroup.getFirstHeader(name);
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public Header getLastHeader(final String name) {
        return this.headergroup.getLastHeader(name);
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public Header[] getAllHeaders() {
        return this.headergroup.getAllHeaders();
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public void addHeader(final Header header) {
        this.headergroup.addHeader(header);
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public void addHeader(final String name, final String value) {
        Args.notNull(name, "Header name");
        this.headergroup.addHeader(new BasicHeader(name, value));
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public void setHeader(final Header header) {
        this.headergroup.updateHeader(header);
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public void setHeader(final String name, final String value) {
        Args.notNull(name, "Header name");
        this.headergroup.updateHeader(new BasicHeader(name, value));
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public void setHeaders(final Header[] headers) {
        this.headergroup.setHeaders(headers);
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public void removeHeader(final Header header) {
        this.headergroup.removeHeader(header);
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public void removeHeaders(final String name) {
        if (name == null) {
            return;
        }
        for (final HeaderIterator i = this.headergroup.iterator(); i.hasNext(); ) {
            final Header header = i.nextHeader();
            if (name.equalsIgnoreCase(header.getName())) {
                i.remove();
            }
        }
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public HeaderIterator headerIterator() {
        return this.headergroup.iterator();
    }

    // non-javadoc, see interface HttpMessage
    @Override
    public HeaderIterator headerIterator(final String name) {
        return this.headergroup.iterator(name);
    }
}
