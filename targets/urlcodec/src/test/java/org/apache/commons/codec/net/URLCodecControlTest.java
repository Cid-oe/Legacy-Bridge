package org.apache.commons.codec.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;

/**
 * Hand-written control tests: outputs of the unmodified Commons Codec 1.3 URLCodec,
 * one per behaviour that harness mutants break. Bob's suite is scored on the same mutants.
 */
class URLCodecControlTest {

    private final URLCodec codec = new URLCodec();

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    /** U1: high bytes are escaped one byte at a time and round-trip. */
    @Test
    void highBytesRoundTrip() throws Exception {
        byte[] data = {(byte) 0xFF, 0x00, (byte) 0x80};
        assertEquals("%FF%00%80", new String(codec.encode(data), StandardCharsets.US_ASCII));
        assertArrayEquals(data, codec.decode(b("%FF%00%80")));
    }

    /** U2: raw non-ASCII input to decode(String) becomes '?'. */
    @Test
    void decodeReadsInputAsAscii() throws Exception {
        assertEquals("caf?", codec.decode("café"));
        assertEquals("café", codec.decode("caf%C3%A9"));
    }

    /** U3: malformed escapes throw DecoderException. */
    @Test
    void malformedEscapeThrows() {
        assertThrows(DecoderException.class, () -> codec.decode("%zz"));
        assertThrows(DecoderException.class, () -> codec.decode("%+1"));
    }

    /** U4: a truncated escape throws DecoderException. */
    @Test
    void truncatedEscapeThrows() {
        assertThrows(DecoderException.class, () -> codec.decode("abc%2"));
        assertThrows(DecoderException.class, () -> codec.decode("abc%"));
    }

    /** U5: null in, null out. */
    @Test
    void nullPassesThrough() throws Exception {
        assertNull(codec.encode((String) null));
        assertNull(codec.decode((String) null));
    }

    /** U6: space becomes '+'; '*' stays; everything else unsafe is escaped in uppercase hex. */
    @Test
    void formEncoding() throws Exception {
        assertEquals("a+b*%7E%21", codec.encode("a b*~!"));
        assertEquals("caf%C3%A9", codec.encode("café"));
        assertEquals("a b", codec.decode("a+b"));
    }
}
