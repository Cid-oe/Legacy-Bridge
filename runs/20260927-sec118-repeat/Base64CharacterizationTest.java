package org.apache.commons.codec.binary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for {@link Base64}.
 *
 * These tests pin the ACTUAL behaviour of the code as-written, including quirks
 * that disagree with the Javadoc. They must all pass against the original source
 * without modification.
 */
class Base64CharacterizationTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static byte[] bytes(String ascii) {
        return ascii.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static String str(byte[] b) {
        return new String(b, java.nio.charset.StandardCharsets.US_ASCII);
    }

    // -----------------------------------------------------------------------
    // encodeBase64 – empty and null
    // -----------------------------------------------------------------------

    /** Empty input produces empty output (no padding at all). */
    @Test
    void encode_emptyArray_returnsEmptyArray() {
        byte[] result = Base64.encodeBase64(new byte[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    /** Null input blows up immediately – no null guard in the source. */
    @Test
    void encode_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> Base64.encodeBase64(null));
    }

    // -----------------------------------------------------------------------
    // encodeBase64 – 1-byte inputs (fewerThan24bits == EIGHTBIT branch)
    // -----------------------------------------------------------------------

    /** Single zero byte → "AA==" (two A's then two pads). */
    @Test
    void encode_singleZeroByte_returnsAA_pad_pad() {
        assertArrayEquals(bytes("AA=="),
                Base64.encodeBase64(new byte[]{0}));
    }

    /**
     * Single 0xFF byte.
     * val1 = ((-1 >> 2) ^ 0xC0) & 0xFF = 63 → '/'
     * k    = 3  → k<<4 = 48 → lookUp[48] = 'w'   (26..51 → 'a'..'z', index 48 = 'a'+22 = 'w')
     * Pads fill positions 2 and 3.
     */
    @Test
    void encode_singleMaxByte_returnsSlashW_pad_pad() {
        assertArrayEquals(bytes("/w=="),
                Base64.encodeBase64(new byte[]{(byte) 0xFF}));
    }

    /** Single 0x01 byte → "AQ==". */
    @Test
    void encode_singleByte_0x01() {
        // b1=1, val1=0 → 'A';  k=1, k<<4=16 → lookUp[16]='Q';  then PAD PAD
        assertArrayEquals(bytes("AQ=="),
                Base64.encodeBase64(new byte[]{(byte) 0x01}));
    }

    // -----------------------------------------------------------------------
    // encodeBase64 – 2-byte inputs (fewerThan24bits == SIXTEENBIT branch)
    // -----------------------------------------------------------------------

    /** Two zero bytes → "AAA=". */
    @Test
    void encode_twoZeroBytes_returnsAAA_pad() {
        assertArrayEquals(bytes("AAA="),
                Base64.encodeBase64(new byte[]{0, 0}));
    }

    /**
     * Two 0xFF bytes → "//8=".
     * val1=63→'/', combined-val2=63→'/', l<<2=60→lookUp[60]='8', PAD.
     */
    @Test
    void encode_twoMaxBytes_returnsSlashSlash8_pad() {
        assertArrayEquals(bytes("//8="),
                Base64.encodeBase64(new byte[]{(byte) 0xFF, (byte) 0xFF}));
    }

    // -----------------------------------------------------------------------
    // encodeBase64 – 3-byte inputs (complete triplet, no remainder)
    // -----------------------------------------------------------------------

    /** Three zero bytes → "AAAA". */
    @Test
    void encode_threeZeroBytes_returnsAAAA() {
        assertArrayEquals(bytes("AAAA"),
                Base64.encodeBase64(new byte[]{0, 0, 0}));
    }

    /** Three 0xFF bytes → "////". */
    @Test
    void encode_threeMaxBytes_returnsFourSlashes() {
        assertArrayEquals(bytes("////"),
                Base64.encodeBase64(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));
    }

    /** RFC 2045 example: "Man" → "TWFu". */
    @Test
    void encode_Man_returnsTWFu() {
        assertArrayEquals(bytes("TWFu"),
                Base64.encodeBase64(new byte[]{'M', 'a', 'n'}));
    }

    // -----------------------------------------------------------------------
    // encodeBase64 – output length formula
    // -----------------------------------------------------------------------

    /** 4 bytes: 1 full triplet + 1 remainder byte → 8 encoded bytes. */
    @Test
    void encode_fourBytes_outputLengthIs8() {
        assertEquals(8, Base64.encodeBase64(new byte[4]).length);
    }

    /** 6 bytes: 2 full triplets, no remainder → 8 encoded bytes. */
    @Test
    void encode_sixBytes_outputLengthIs8() {
        assertEquals(8, Base64.encodeBase64(new byte[6]).length);
    }

    /** 57 bytes: exactly 19 triplets → 76 encoded bytes (no chunking). */
    @Test
    void encode_57Bytes_noChunk_outputLengthIs76() {
        assertEquals(76, Base64.encodeBase64(new byte[57]).length);
    }

    /** 58 bytes: 19 triplets + 1 remainder → 80 encoded bytes (no chunking). */
    @Test
    void encode_58Bytes_noChunk_outputLengthIs80() {
        assertEquals(80, Base64.encodeBase64(new byte[58]).length);
    }

    // -----------------------------------------------------------------------
    // encodeBase64Chunked – chunk separator insertion
    // -----------------------------------------------------------------------

    /** Chunked empty array → empty output. */
    @Test
    void encodeChunked_emptyArray_returnsEmptyArray() {
        assertEquals(0, Base64.encodeBase64Chunked(new byte[0]).length);
    }

    /**
     * 1-byte input chunked: rawLen=4, nbrChunks=1, total=6.
     * Content is "AA==" followed by CRLF.
     */
    @Test
    void encodeChunked_oneByte_appendsCRLF() {
        byte[] result = Base64.encodeBase64Chunked(new byte[]{0});
        assertEquals(6, result.length);
        assertArrayEquals(bytes("AA==\r\n"), result);
    }

    /**
     * 57-byte input: rawLen=76, nbrChunks=ceil(76/76)=1, total=78.
     * All 76 encoded characters on one line followed by CRLF.
     */
    @Test
    void encodeChunked_57Bytes_oneChunkPlusCRLF() {
        byte[] result = Base64.encodeBase64Chunked(new byte[57]);
        assertEquals(78, result.length);
        // last two bytes are CR LF
        assertEquals('\r', result[76]);
        assertEquals('\n', result[77]);
    }

    /**
     * 58-byte input: rawLen=80, nbrChunks=2, total=84.
     * Layout: [76 chars][CRLF][4 chars][CRLF]
     */
    @Test
    void encodeChunked_58Bytes_twoChunks() {
        byte[] result = Base64.encodeBase64Chunked(new byte[58]);
        assertEquals(84, result.length);
        assertEquals('\r', result[76]);
        assertEquals('\n', result[77]);
        assertEquals('\r', result[82]);
        assertEquals('\n', result[83]);
    }

    /**
     * 3-byte input chunked: rawLen=4, nbrChunks=1, total=6.
     * Verify "AAAA\r\n".
     */
    @Test
    void encodeChunked_3Bytes_appendsCRLF() {
        assertArrayEquals(bytes("AAAA\r\n"),
                Base64.encodeBase64Chunked(new byte[]{0, 0, 0}));
    }

    // -----------------------------------------------------------------------
    // decodeBase64 – empty and edge cases
    // -----------------------------------------------------------------------

    /** Empty input returns empty output. */
    @Test
    void decode_emptyArray_returnsEmptyArray() {
        byte[] result = Base64.decodeBase64(new byte[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    /** All-PAD input "====" → empty output (lastData reaches 0). */
    @Test
    void decode_allPads_returnsEmptyArray() {
        byte[] result = Base64.decodeBase64(bytes("===="));
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    // -----------------------------------------------------------------------
    // decodeBase64 – standard vectors
    // -----------------------------------------------------------------------

    /** "AAAA" → three zero bytes. */
    @Test
    void decode_AAAA_returnsThreeZeroBytes() {
        assertArrayEquals(new byte[]{0, 0, 0},
                Base64.decodeBase64(bytes("AAAA")));
    }

    /** "AA==" → single zero byte. */
    @Test
    void decode_AA_pad_pad_returnsSingleZeroByte() {
        assertArrayEquals(new byte[]{0},
                Base64.decodeBase64(bytes("AA==")));
    }

    /** "AAA=" → two zero bytes. */
    @Test
    void decode_AAA_pad_returnsTwoZeroBytes() {
        assertArrayEquals(new byte[]{0, 0},
                Base64.decodeBase64(bytes("AAA=")));
    }

    /** "TWFu" → "Man". */
    @Test
    void decode_TWFu_returnsMan() {
        assertArrayEquals(new byte[]{'M', 'a', 'n'},
                Base64.decodeBase64(bytes("TWFu")));
    }

    /** "/w==" → single 0xFF byte. */
    @Test
    void decode_slashW_pad_pad_returns0xFF() {
        assertArrayEquals(new byte[]{(byte) 0xFF},
                Base64.decodeBase64(bytes("/w==")));
    }

    /** "//8=" → two 0xFF bytes. */
    @Test
    void decode_slashSlash8_pad_returnsTwoFF() {
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF},
                Base64.decodeBase64(bytes("//8=")));
    }

    /** "////" → three 0xFF bytes. */
    @Test
    void decode_fourSlashes_returnsThreeFF() {
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF},
                Base64.decodeBase64(bytes("////")));
    }

    // -----------------------------------------------------------------------
    // decodeBase64 – non-base64 characters are silently discarded
    // -----------------------------------------------------------------------

    /**
     * Spaces inside the encoded string are stripped by discardNonBase64
     * before decoding (RFC 2045: ignore all non-base64 characters).
     */
    @Test
    void decode_spacesAreStripped() {
        // "AA ==" with an embedded space still decodes to a single zero byte
        assertArrayEquals(new byte[]{0},
                Base64.decodeBase64(bytes("AA ==")));
    }

    /** CRLF inside encoded data is ignored. */
    @Test
    void decode_crlfIsStripped() {
        assertArrayEquals(new byte[]{0},
                Base64.decodeBase64(bytes("AA\r\n==")));
    }

    /** Punctuation characters not in the alphabet are silently dropped. */
    @Test
    void decode_nonBase64CharsIgnored() {
        // "!" and "," are not in the alphabet; result is same as "AAAA"
        assertArrayEquals(new byte[]{0, 0, 0},
                Base64.decodeBase64(bytes("A!A,AA")));
    }

    // -----------------------------------------------------------------------
    // decodeBase64 – round-trip with encodeBase64
    // -----------------------------------------------------------------------

    /** Round-trip for every possible single byte value 0x00..0x7F. */
    @Test
    void roundTrip_allSingleBytes_0_to_127() {
        for (int i = 0; i <= 127; i++) {
            byte[] original = new byte[]{(byte) i};
            byte[] encoded = Base64.encodeBase64(original);
            byte[] decoded = Base64.decodeBase64(encoded);
            assertArrayEquals(original, decoded,
                    "Round-trip failed for byte value " + i);
        }
    }

    /** Round-trip for three-byte inputs covering full triplet path. */
    @Test
    void roundTrip_threeBytesAllZero() {
        byte[] original = new byte[]{0, 0, 0};
        assertArrayEquals(original,
                Base64.decodeBase64(Base64.encodeBase64(original)));
    }

    /** Round-trip of a 100-byte sequence. */
    @Test
    void roundTrip_100Bytes() {
        byte[] original = new byte[100];
        for (int i = 0; i < 100; i++) {
            original[i] = (byte) i;
        }
        assertArrayEquals(original,
                Base64.decodeBase64(Base64.encodeBase64(original)));
    }

    /** Round-trip of a 114-byte sequence (exact multiple of 3). */
    @Test
    void roundTrip_114Bytes_exactMultipleOf3() {
        byte[] original = new byte[114];
        for (int i = 0; i < 114; i++) {
            original[i] = (byte) (i * 2);
        }
        assertArrayEquals(original,
                Base64.decodeBase64(Base64.encodeBase64(original)));
    }

    /** Chunked encode then decode round-trips correctly. */
    @Test
    void roundTrip_chunkedEncodeDecodesCorrectly() {
        byte[] original = new byte[200];
        for (int i = 0; i < 200; i++) {
            original[i] = (byte) i;
        }
        byte[] encoded = Base64.encodeBase64Chunked(original);
        byte[] decoded = Base64.decodeBase64(encoded);
        assertArrayEquals(original, decoded);
    }

    // -----------------------------------------------------------------------
    // isArrayByteBase64
    // -----------------------------------------------------------------------

    /** Empty array → true (code comments even note this oddity). */
    @Test
    void isArrayByteBase64_emptyArray_returnsTrue() {
        assertTrue(Base64.isArrayByteBase64(new byte[0]));
    }

    /** All whitespace → stripped to empty → true. */
    @Test
    void isArrayByteBase64_allWhitespace_returnsTrue() {
        assertTrue(Base64.isArrayByteBase64(bytes(" \t\r\n")));
    }

    /** Valid base64 alphabet characters return true. */
    @Test
    void isArrayByteBase64_validChars_returnsTrue() {
        assertTrue(Base64.isArrayByteBase64(bytes("TWFu")));
        assertTrue(Base64.isArrayByteBase64(bytes("AA==")));
        assertTrue(Base64.isArrayByteBase64(bytes("////")));
        assertTrue(Base64.isArrayByteBase64(bytes("+/aZ09")));
    }

    /** PAD character '=' is treated as base64 (isBase64 returns true for PAD). */
    @Test
    void isArrayByteBase64_padChar_returnsTrue() {
        assertTrue(Base64.isArrayByteBase64(bytes("=")));
        assertTrue(Base64.isArrayByteBase64(bytes("====")));
    }

    /** Characters not in the base64 alphabet cause false. */
    @Test
    void isArrayByteBase64_invalidChar_returnsFalse() {
        assertFalse(Base64.isArrayByteBase64(bytes("!")));
        assertFalse(Base64.isArrayByteBase64(bytes("TWFu!")));
    }

    /** Whitespace is stripped before validation, so "TW Fu" is valid. */
    @Test
    void isArrayByteBase64_whitespaceStrippedBeforeCheck() {
        assertTrue(Base64.isArrayByteBase64(bytes("TW Fu")));
    }

    /**
     * Lower-case letters are valid base64 characters (they map to values 26–51).
     * Upper-case is also valid (values 0–25). Mixed is fine.
     */
    @Test
    void isArrayByteBase64_mixedCase_returnsTrue() {
        assertTrue(Base64.isArrayByteBase64(bytes("aAbBzZ")));
    }

    /**
     * Byte value 0xFF (-1 signed) passed to isArrayByteBase64 causes an
     * ArrayIndexOutOfBoundsException because base64Alphabet has length 255
     * (indices 0-254) and -1 is an invalid index.
     */
    @Test
    void isArrayByteBase64_byte0xFF_throwsArrayIndexOutOfBounds() {
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> Base64.isArrayByteBase64(new byte[]{(byte) 0xFF}));
    }

    // -----------------------------------------------------------------------
    // Instance methods: encode(byte[]) and decode(byte[])
    // -----------------------------------------------------------------------

    /** Instance encode delegates to encodeBase64(data, false). */
    @Test
    void instanceEncode_delegatesToStaticNonChunked() {
        Base64 codec = new Base64();
        assertArrayEquals(bytes("TWFu"),
                codec.encode(new byte[]{'M', 'a', 'n'}));
    }

    /** Instance decode delegates to decodeBase64. */
    @Test
    void instanceDecode_delegatesToStaticDecode() {
        Base64 codec = new Base64();
        assertArrayEquals(new byte[]{'M', 'a', 'n'},
                codec.decode(bytes("TWFu")));
    }

    // -----------------------------------------------------------------------
    // Object encode/decode (BinaryEncoder / BinaryDecoder interfaces)
    // -----------------------------------------------------------------------

    /** encode(Object) with byte[] succeeds. */
    @Test
    void objectEncode_byteArray_succeeds() throws Exception {
        Base64 codec = new Base64();
        byte[] result = (byte[]) codec.encode((Object) new byte[]{'M', 'a', 'n'});
        assertArrayEquals(bytes("TWFu"), result);
    }

    /** encode(Object) with a non-byte[] throws EncoderException. */
    @Test
    void objectEncode_nonByteArray_throwsEncoderException() {
        Base64 codec = new Base64();
        assertThrows(org.apache.commons.codec.EncoderException.class,
                () -> codec.encode("not a byte array"));
    }

    /** encode(Object) with Integer throws EncoderException. */
    @Test
    void objectEncode_integer_throwsEncoderException() {
        Base64 codec = new Base64();
        assertThrows(org.apache.commons.codec.EncoderException.class,
                () -> codec.encode(42));
    }

    /** decode(Object) with byte[] succeeds. */
    @Test
    void objectDecode_byteArray_succeeds() throws Exception {
        Base64 codec = new Base64();
        byte[] result = (byte[]) codec.decode((Object) bytes("TWFu"));
        assertArrayEquals(new byte[]{'M', 'a', 'n'}, result);
    }

    /** decode(Object) with a String throws DecoderException. */
    @Test
    void objectDecode_string_throwsDecoderException() {
        Base64 codec = new Base64();
        assertThrows(org.apache.commons.codec.DecoderException.class,
                () -> codec.decode("TWFu"));
    }

    /** decode(Object) with null throws DecoderException (not NullPointerException). */
    @Test
    void objectDecode_null_throwsDecoderException() {
        Base64 codec = new Base64();
        assertThrows(org.apache.commons.codec.DecoderException.class,
                () -> codec.decode((Object) null));
    }

    // -----------------------------------------------------------------------
    // encodeBase64 output is always alphabet chars + PAD only (non-chunked)
    // -----------------------------------------------------------------------

    /** Non-chunked output contains only alphabet characters and/or '=' pads. */
    @Test
    void encode_nonChunked_outputContainsOnlyBase64CharsAndPad() {
        byte[] input = new byte[50];
        for (int i = 0; i < 50; i++) {
            input[i] = (byte) i;
        }
        byte[] encoded = Base64.encodeBase64(input);
        for (byte b : encoded) {
            boolean valid = (b >= 'A' && b <= 'Z')
                    || (b >= 'a' && b <= 'z')
                    || (b >= '0' && b <= '9')
                    || b == '+' || b == '/' || b == '=';
            assertTrue(valid, "Unexpected byte in output: " + b);
        }
    }

    // -----------------------------------------------------------------------
    // Chunked output structure: CRLF separators only; no extra whitespace
    // -----------------------------------------------------------------------

    /** Chunked output for exactly 57 bytes is [76 base64 chars][CR][LF]. */
    @Test
    void encodeChunked_57Bytes_exactLayout() {
        byte[] result = Base64.encodeBase64Chunked(new byte[57]);
        // first 76 bytes must be valid base64 alphabet
        for (int i = 0; i < 76; i++) {
            byte b = result[i];
            boolean valid = (b >= 'A' && b <= 'Z')
                    || (b >= 'a' && b <= 'z')
                    || (b >= '0' && b <= '9')
                    || b == '+' || b == '/' || b == '=';
            assertTrue(valid, "Non-base64 char at position " + i);
        }
        assertEquals((byte) '\r', result[76]);
        assertEquals((byte) '\n', result[77]);
    }

    // -----------------------------------------------------------------------
    // Boundary: input lengths 0-6 encode length
    // -----------------------------------------------------------------------

    @Test
    void encode_lengthBoundaries_0to6() {
        int[] expected = {0, 4, 4, 4, 8, 8, 8};
        for (int len = 0; len <= 6; len++) {
            assertEquals(expected[len],
                    Base64.encodeBase64(new byte[len]).length,
                    "Encoded length mismatch for input length " + len);
        }
    }

    // -----------------------------------------------------------------------
    // decodeBase64: output array sizing
    // -----------------------------------------------------------------------

    /**
     * Decoded array size = (number of complete quadruples × 3) - (number of trailing PADs).
     * "AAAA" → 3 bytes, "AAA=" → 2 bytes, "AA==" → 1 byte.
     */
    @Test
    void decode_outputSizeReflectsPadCount() {
        assertEquals(3, Base64.decodeBase64(bytes("AAAA")).length);
        assertEquals(2, Base64.decodeBase64(bytes("AAA=")).length);
        assertEquals(1, Base64.decodeBase64(bytes("AA==")).length);
    }

    // -----------------------------------------------------------------------
    // decodeBase64: input with only whitespace / non-base64 chars
    // -----------------------------------------------------------------------

    /**
     * Input consisting entirely of non-base64 chars (after discardNonBase64
     * removes them, length becomes 0) → empty output.
     */
    @Test
    void decode_allNonBase64_returnsEmpty() {
        // '!' has value 33; base64Alphabet[33]==-1 and it's not PAD → stripped
        assertArrayEquals(new byte[0],
                Base64.decodeBase64(bytes("!!!")));
    }

    // -----------------------------------------------------------------------
    // Static constants are accessible and have expected values
    // -----------------------------------------------------------------------

    @Test
    void constants_chunkSizeIs76() {
        assertEquals(76, Base64.CHUNK_SIZE);
    }

    @Test
    void constants_chunkSeparatorIsCRLF() {
        assertArrayEquals(new byte[]{'\r', '\n'}, Base64.CHUNK_SEPARATOR);
    }

    @Test
    void constants_padByteIsEqualsSign() {
        assertEquals((byte) '=', Base64.PAD);
    }
}
