/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2020 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.httppanel.view.impl.models.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.network.HttpBody;
import org.parosproxy.paros.network.HttpHeader;
import org.parosproxy.paros.network.HttpMessage;

public abstract class BodyByteHttpPanelViewModelTest<T1 extends HttpHeader, T2 extends HttpBody> {

    private static final Charset DEFAULT_CHARSET = Charset.forName(HttpBody.DEFAULT_CHARSET);

    private static final byte[] BODY = "Body 123 ABC".getBytes(DEFAULT_CHARSET);
    private static final byte[] EMPTY_ARRAY = {};

    private AbstractHttpByteHttpPanelViewModel model;

    protected HttpMessage message;
    protected T1 header;
    protected T2 body;

    @BeforeEach
    void setup() {
        model = createModel();

        message = mock(HttpMessage.class);
        header = mock(getHeaderClass());
        body = mock(getBodyClass());

        given(body.getBytes()).willReturn(BODY);

        prepareMessage();
    }

    protected abstract AbstractHttpByteHttpPanelViewModel createModel();

    protected abstract Class<T1> getHeaderClass();

    protected abstract Class<T2> getBodyClass();

    protected abstract void prepareMessage();

    @Test
    void shouldGetEmptyDataFromNullMessage() {
        // Given
        model.setMessage(null);
        // When
        byte[] data = model.getData();
        // Then
        assertThat(data, is(EMPTY_ARRAY));
    }

    @Test
    void shouldGetDataFromBody() {
        // Given
        model.setMessage(message);
        // When
        byte[] data = model.getData();
        // Then
        assertThat(data, is(equalTo(BODY)));
    }

    @Test
    void shouldGetDataFromBodyGzipDecoded() {
        // Given
        given(header.getHeader(HttpHeader.CONTENT_ENCODING)).willReturn("gzip");
        given(body.getCharset()).willReturn(DEFAULT_CHARSET.name());
        given(body.getBytes()).willReturn(gzip(BODY));
        model.setMessage(message);
        // When
        byte[] data = model.getData();
        // Then
        assertThat(data, is(equalTo(BODY)));
    }

    @Test
    void shouldNotSetDataWithNullMessage() {
        // Given
        model.setMessage(null);
        // When / Then
        assertDoesNotThrow(() -> model.setData(BODY));
    }

    @Test
    void shouldSetDataIntoBody() {
        // Given
        model.setMessage(message);
        byte[] otherBodyContent = "Other Body".getBytes(DEFAULT_CHARSET);
        given(body.length()).willReturn(otherBodyContent.length);
        // When
        model.setData(otherBodyContent);
        // Then
        verify(body).setBody(otherBodyContent);
        verify(header, times(0)).setContentLength(anyInt());
    }

    @Test
    void shouldSetDataIntoBodyGzipEncoded() {
        // Given
        model.setMessage(message);
        given(header.getHeader(HttpHeader.CONTENT_ENCODING)).willReturn("gzip");
        given(body.getCharset()).willReturn(DEFAULT_CHARSET.name());
        byte[] otherBodyContent = "Other Body".getBytes(DEFAULT_CHARSET);
        byte[] encodedBody = gzip(otherBodyContent);
        given(body.length()).willReturn(encodedBody.length);
        // When
        model.setData(otherBodyContent);
        // Then
        verify(body).setBody(encodedBody);
        verify(header, times(0)).setContentLength(anyInt());
    }

    private static byte[] gzip(byte[] value) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gis = new GZIPOutputStream(baos)) {
            gis.write(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
