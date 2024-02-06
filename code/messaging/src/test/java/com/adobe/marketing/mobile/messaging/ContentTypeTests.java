package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ContentTypeTests {

    @Test
    public void getValue_returnsCorrectValue_whenCalledOnApplicationJson() {
        ContentType contentType = ContentType.APPLICATION_JSON;
        int result = contentType.getValue();
        assertEquals(0, result);
    }

    @Test
    public void getValue_returnsCorrectValue_whenCalledOnTextHtml() {
        ContentType contentType = ContentType.TEXT_HTML;
        int result = contentType.getValue();
        assertEquals(1, result);
    }

    @Test
    public void getValue_returnsCorrectValue_whenCalledOnTextXml() {
        ContentType contentType = ContentType.TEXT_XML;
        int result = contentType.getValue();
        assertEquals(2, result);
    }

    @Test
    public void getValue_returnsCorrectValue_whenCalledOnTextPlain() {
        ContentType contentType = ContentType.TEXT_PLAIN;
        int result = contentType.getValue();
        assertEquals(3, result);
    }

    @Test
    public void getValue_returnsCorrectValue_whenCalledOnUnknown() {
        ContentType contentType = ContentType.UNKNOWN;
        int result = contentType.getValue();
        assertEquals(4, result);

    }
    @Test
    public void toString_returnsApplicationJson_whenContentTypeIsApplicationJson() {
        ContentType contentType = ContentType.APPLICATION_JSON;
        String result = contentType.toString();
        assertEquals(MessagingTestConstants.ContentTypes.APPLICATION_JSON, result);
    }

    @Test
    public void toString_returnsTextHtml_whenContentTypeIsTextHtml() {
        ContentType contentType = ContentType.TEXT_HTML;
        String result = contentType.toString();
        assertEquals(MessagingTestConstants.ContentTypes.TEXT_HTML, result);
    }

    @Test
    public void toString_returnsTextXml_whenContentTypeIsTextXml() {
        ContentType contentType = ContentType.TEXT_XML;
        String result = contentType.toString();
        assertEquals(MessagingTestConstants.ContentTypes.TEXT_XML, result);
    }

    @Test
    public void toString_returnsTextPlain_whenContentTypeIsTextPlain() {
        ContentType contentType = ContentType.TEXT_PLAIN;
        String result = contentType.toString();
        assertEquals(MessagingTestConstants.ContentTypes.TEXT_PLAIN, result);
    }

    @Test
    public void toString_returnsEmptyString_whenContentTypeIsUnknown() {
        ContentType contentType = ContentType.UNKNOWN;
        String result = contentType.toString();
        assertEquals("", result);
    }

    @Test
    public void fromString_returnsApplicationJson_whenInputIsApplicationJson() {
        ContentType result = ContentType.fromString(MessagingTestConstants.ContentTypes.APPLICATION_JSON);
        assertEquals(ContentType.APPLICATION_JSON, result);
    }

    @Test
    public void fromString_returnsTextHtml_whenInputIsTextHtml() {
        ContentType result = ContentType.fromString(MessagingTestConstants.ContentTypes.TEXT_HTML);
        assertEquals(ContentType.TEXT_HTML, result);
    }

    @Test
    public void fromString_returnsTextXml_whenInputIsTextXml() {
        ContentType result = ContentType.fromString(MessagingTestConstants.ContentTypes.TEXT_XML);
        assertEquals(ContentType.TEXT_XML, result);
    }

    @Test
    public void fromString_returnsTextPlain_whenInputIsTextPlain() {
        ContentType result = ContentType.fromString(MessagingTestConstants.ContentTypes.TEXT_PLAIN);
        assertEquals(ContentType.TEXT_PLAIN, result);
    }

    @Test
    public void fromString_returnsUnknown_whenInputIsUnknown() {
        ContentType result = ContentType.fromString("randomString");
        assertEquals(ContentType.UNKNOWN, result);
    }

    @Test
    public void fromString_returnsUnknown_whenInputIsNull() {
        ContentType result = ContentType.fromString(null);
        assertEquals(ContentType.UNKNOWN, result);
    }
}
