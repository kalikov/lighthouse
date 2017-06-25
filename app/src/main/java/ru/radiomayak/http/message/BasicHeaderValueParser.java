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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import ru.radiomayak.http.HeaderElement;
import ru.radiomayak.http.NameValuePair;
import ru.radiomayak.http.ParseException;
import ru.radiomayak.http.annotation.ThreadingBehavior;
import ru.radiomayak.http.annotation.Contract;
import ru.radiomayak.http.util.Args;

/**
 * Basic implementation for parsing header values into elements.
 * Instances of this class are stateless and thread-safe.
 * Derived classes are expected to maintain these properties.
 *
 * @since 4.0
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class BasicHeaderValueParser implements ru.radiomayak.http.message.HeaderValueParser {

    /**
     * A default instance of this class, for use as default or fallback.
     * Note that {@link BasicHeaderValueParser} is not a singleton, there
     * can be many instances of the class itself and of derived classes.
     * The instance here provides non-customized, default behavior.
     */
    public final static BasicHeaderValueParser INSTANCE = new BasicHeaderValueParser();

    private final static char PARAM_DELIMITER                = ';';
    private final static char ELEM_DELIMITER                 = ',';

    // IMPORTANT!
    // These private static variables must be treated as immutable and never exposed outside this class
    private static final BitSet TOKEN_DELIMS = TokenParser.INIT_BITSET('=', PARAM_DELIMITER, ELEM_DELIMITER);
    private static final BitSet VALUE_DELIMS = TokenParser.INIT_BITSET(PARAM_DELIMITER, ELEM_DELIMITER);

    private final TokenParser tokenParser;

    public BasicHeaderValueParser() {
        this.tokenParser = TokenParser.INSTANCE;
    }

    /**
     * Parses elements with the given parser.
     *
     * @param value     the header value to parse
     * @param parser    the parser to use, or {@code null} for default
     *
     * @return  array holding the header elements, never {@code null}
     * @throws ParseException in case of a parsing error
     */
    public static
        HeaderElement[] parseElements(final String value,
                                      final ru.radiomayak.http.message.HeaderValueParser parser) throws ParseException {
        Args.notNull(value, "Value");

        final ru.radiomayak.http.util.CharArrayBuffer buffer = new ru.radiomayak.http.util.CharArrayBuffer(value.length());
        buffer.append(value);
        final ru.radiomayak.http.message.ParserCursor cursor = new ru.radiomayak.http.message.ParserCursor(0, value.length());
        return (parser != null ? parser : BasicHeaderValueParser.INSTANCE)
            .parseElements(buffer, cursor);
    }


    // non-javadoc, see interface HeaderValueParser
    @Override
    public HeaderElement[] parseElements(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                         final ru.radiomayak.http.message.ParserCursor cursor) {
        Args.notNull(buffer, "Char array buffer");
        Args.notNull(cursor, "Parser cursor");
        final List<HeaderElement> elements = new ArrayList<HeaderElement>();
        while (!cursor.atEnd()) {
            final HeaderElement element = parseHeaderElement(buffer, cursor);
            if (!(element.getName().length() == 0 && element.getValue() == null)) {
                elements.add(element);
            }
        }
        return elements.toArray(new HeaderElement[elements.size()]);
    }


    /**
     * Parses an element with the given parser.
     *
     * @param value     the header element to parse
     * @param parser    the parser to use, or {@code null} for default
     *
     * @return  the parsed header element
     */
    public static
        HeaderElement parseHeaderElement(final String value,
                                         final ru.radiomayak.http.message.HeaderValueParser parser) throws ParseException {
        Args.notNull(value, "Value");

        final ru.radiomayak.http.util.CharArrayBuffer buffer = new ru.radiomayak.http.util.CharArrayBuffer(value.length());
        buffer.append(value);
        final ru.radiomayak.http.message.ParserCursor cursor = new ru.radiomayak.http.message.ParserCursor(0, value.length());
        return (parser != null ? parser : BasicHeaderValueParser.INSTANCE)
                .parseHeaderElement(buffer, cursor);
    }


    // non-javadoc, see interface HeaderValueParser
    @Override
    public HeaderElement parseHeaderElement(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                            final ru.radiomayak.http.message.ParserCursor cursor) {
        Args.notNull(buffer, "Char array buffer");
        Args.notNull(cursor, "Parser cursor");
        final NameValuePair nvp = parseNameValuePair(buffer, cursor);
        NameValuePair[] params = null;
        if (!cursor.atEnd()) {
            final char ch = buffer.charAt(cursor.getPos() - 1);
            if (ch != ELEM_DELIMITER) {
                params = parseParameters(buffer, cursor);
            }
        }
        return createHeaderElement(nvp.getName(), nvp.getValue(), params);
    }


    /**
     * Creates a header element.
     * Called from {@link #parseHeaderElement}.
     *
     * @return  a header element representing the argument
     */
    protected HeaderElement createHeaderElement(
            final String name,
            final String value,
            final NameValuePair[] params) {
        return new ru.radiomayak.http.message.BasicHeaderElement(name, value, params);
    }


    /**
     * Parses parameters with the given parser.
     *
     * @param value     the parameter list to parse
     * @param parser    the parser to use, or {@code null} for default
     *
     * @return  array holding the parameters, never {@code null}
     */
    public static
        NameValuePair[] parseParameters(final String value,
                                        final ru.radiomayak.http.message.HeaderValueParser parser) throws ParseException {
        Args.notNull(value, "Value");

        final ru.radiomayak.http.util.CharArrayBuffer buffer = new ru.radiomayak.http.util.CharArrayBuffer(value.length());
        buffer.append(value);
        final ru.radiomayak.http.message.ParserCursor cursor = new ru.radiomayak.http.message.ParserCursor(0, value.length());
        return (parser != null ? parser : BasicHeaderValueParser.INSTANCE)
                .parseParameters(buffer, cursor);
    }



    // non-javadoc, see interface HeaderValueParser
    @Override
    public NameValuePair[] parseParameters(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                           final ru.radiomayak.http.message.ParserCursor cursor) {
        Args.notNull(buffer, "Char array buffer");
        Args.notNull(cursor, "Parser cursor");
        tokenParser.skipWhiteSpace(buffer, cursor);
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        while (!cursor.atEnd()) {
            final NameValuePair param = parseNameValuePair(buffer, cursor);
            params.add(param);
            final char ch = buffer.charAt(cursor.getPos() - 1);
            if (ch == ELEM_DELIMITER) {
                break;
            }
        }
        return params.toArray(new NameValuePair[params.size()]);
    }

    /**
     * Parses a name-value-pair with the given parser.
     *
     * @param value     the NVP to parse
     * @param parser    the parser to use, or {@code null} for default
     *
     * @return  the parsed name-value pair
     */
    public static
       NameValuePair parseNameValuePair(final String value,
                                        final ru.radiomayak.http.message.HeaderValueParser parser) throws ParseException {
        Args.notNull(value, "Value");

        final ru.radiomayak.http.util.CharArrayBuffer buffer = new ru.radiomayak.http.util.CharArrayBuffer(value.length());
        buffer.append(value);
        final ru.radiomayak.http.message.ParserCursor cursor = new ru.radiomayak.http.message.ParserCursor(0, value.length());
        return (parser != null ? parser : BasicHeaderValueParser.INSTANCE)
                .parseNameValuePair(buffer, cursor);
    }


    // non-javadoc, see interface HeaderValueParser
    @Override
    public NameValuePair parseNameValuePair(final ru.radiomayak.http.util.CharArrayBuffer buffer,
                                            final ru.radiomayak.http.message.ParserCursor cursor) {
        Args.notNull(buffer, "Char array buffer");
        Args.notNull(cursor, "Parser cursor");

        final String name = tokenParser.parseToken(buffer, cursor, TOKEN_DELIMS);
        if (cursor.atEnd()) {
            return new ru.radiomayak.http.message.BasicNameValuePair(name, null);
        }
        final int delim = buffer.charAt(cursor.getPos());
        cursor.updatePos(cursor.getPos() + 1);
        if (delim != '=') {
            return createNameValuePair(name, null);
        }
        final String value = tokenParser.parseValue(buffer, cursor, VALUE_DELIMS);
        if (!cursor.atEnd()) {
            cursor.updatePos(cursor.getPos() + 1);
        }
        return createNameValuePair(name, value);
    }

    /**
     * Creates a name-value pair.
     * Called from {@link #parseNameValuePair}.
     *
     * @param name      the name
     * @param value     the value, or {@code null}
     *
     * @return  a name-value pair representing the arguments
     */
    protected NameValuePair createNameValuePair(final String name, final String value) {
        return new ru.radiomayak.http.message.BasicNameValuePair(name, value);
    }

}

