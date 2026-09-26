# Bob session: base64-generate-tests

- Started: 20260926T192119Z
- Command: `bob -p <prompt>`
- Duration: 287.4s
- Bob cost: 0.381

## Prompt

````
You are writing characterization tests for a legacy Java class before it is modernized.

Goal: pin down what the code DOES today, including odd behaviour, so any change in behaviour
during modernization makes a test fail. Do not test what the comments say it should do; test
what it actually does. Where the code and its javadoc disagree, the code wins.

Requirements:
- One JUnit 5 test class named `Base64CharacterizationTest` in package `org.apache.commons.codec.binary`.
- Use only JUnit 5 (org.junit.jupiter.api) and the JDK. No other libraries.
- Call only the public API of `Base64` shown below; keep test inputs deterministic.
- Cover boundaries, special cases and branches, input normalisation (whitespace, letter case,
  null, empty, very short input), error cases, and anything the code treats specially.
- Every test must pass against the code exactly as written below.
- Reply with the complete test class in a single ```java code block. Keep every line under 100 characters.

```java
/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.apache.commons.codec.binary;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;

/**
 * Provides Base64 encoding and decoding as defined by RFC 2045.
 * 
 * <p>This class implements section <cite>6.8. Base64 Content-Transfer-Encoding</cite> 
 * from RFC 2045 <cite>Multipurpose Internet Mail Extensions (MIME) Part One: 
 * Format of Internet Message Bodies</cite> by Freed and Borenstein.</p> 
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>
 * @author Apache Software Foundation
 * @since 1.0-dev
 * @version $Id: Base64.java,v 1.20 2004/05/24 00:21:24 ggregory Exp $
 */
public class Base64 implements BinaryEncoder, BinaryDecoder {

    /**
     * Chunk size per RFC 2045 section 6.8.
     * 
     * <p>The {@value} character limit does not count the trailing CRLF, but counts 
     * all other characters, including any equal signs.</p>
     * 
     * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045 section 6.8</a>
     */
    static final int CHUNK_SIZE = 76;

    /**
     * Chunk separator per RFC 2045 section 2.1.
     * 
     * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045 section 2.1</a>
     */
    static final byte[] CHUNK_SEPARATOR = "\r\n".getBytes();

    /**
     * The base length.
     */
    static final int BASELENGTH = 255;

    /**
     * Lookup length.
     */
    static final int LOOKUPLENGTH = 64;

    /**
     * Used to calculate the number of bits in a byte.
     */
    static final int EIGHTBIT = 8;

    /**
     * Used when encoding something which has fewer than 24 bits.
     */
    static final int SIXTEENBIT = 16;

    /**
     * Used to determine how many bits data contains.
     */
    static final int TWENTYFOURBITGROUP = 24;

    /**
     * Used to get the number of Quadruples.
     */
    static final int FOURBYTE = 4;

    /**
     * Used to test the sign of a byte.
     */
    static final int SIGN = -128;
    
    /**
     * Byte used to pad output.
     */
    static final byte PAD = (byte) '=';

    // Create arrays to hold the base64 characters and a 
    // lookup for base64 chars
    private static byte[] base64Alphabet = new byte[BASELENGTH];
    private static byte[] lookUpBase64Alphabet = new byte[LOOKUPLENGTH];

    // Populating the lookup and character arrays
    static {
        for (int i = 0; i < BASELENGTH; i++) {
            base64Alphabet[i] = (byte) -1;
        }
        for (int i = 'Z'; i >= 'A'; i--) {
            base64Alphabet[i] = (byte) (i - 'A');
        }
        for (int i = 'z'; i >= 'a'; i--) {
            base64Alphabet[i] = (byte) (i - 'a' + 26);
        }
        for (int i = '9'; i >= '0'; i--) {
            base64Alphabet[i] = (byte) (i - '0' + 52);
        }

        base64Alphabet['+'] = 62;
        base64Alphabet['/'] = 63;

        for (int i = 0; i <= 25; i++) {
            lookUpBase64Alphabet[i] = (byte) ('A' + i);
        }

        for (int i = 26, j = 0; i <= 51; i++, j++) {
            lookUpBase64Alphabet[i] = (byte) ('a' + j);
        }

        for (int i = 52, j = 0; i <= 61; i++, j++) {
            lookUpBase64Alphabet[i] = (byte) ('0' + j);
        }

        lookUpBase64Alphabet[62] = (byte) '+';
        lookUpBase64Alphabet[63] = (byte) '/';
    }

    private static boolean isBase64(byte octect) {
        if (octect == PAD) {
            return true;
        } else if (base64Alphabet[octect] == -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Tests a given byte array to see if it contains
     * only valid characters within the Base64 alphabet.
     *
     * @param arrayOctect byte array to test
     * @return true if all bytes are valid characters in the Base64
     *         alphabet or if the byte array is empty; false, otherwise
     */
    public static boolean isArrayByteBase64(byte[] arrayOctect) {

        arrayOctect = discardWhitespace(arrayOctect);

        int length = arrayOctect.length;
        if (length == 0) {
            // shouldn't a 0 length array be valid base64 data?
            // return false;
            return true;
        }
        for (int i = 0; i < length; i++) {
            if (!isBase64(arrayOctect[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encodes binary data using the base64 algorithm but
     * does not chunk the output.
     *
     * @param binaryData binary data to encode
     * @return Base64 characters
     */
    public static byte[] encodeBase64(byte[] binaryData) {
        return encodeBase64(binaryData, false);
    }

    /**
     * Encodes binary data using the base64 algorithm and chunks
     * the encoded output into 76 character blocks
     *
     * @param binaryData binary data to encode
     * @return Base64 characters chunked in 76 character blocks
     */
    public static byte[] encodeBase64Chunked(byte[] binaryData) {
        return encodeBase64(binaryData, true);
    }


    /**
     * Decodes an Object using the base64 algorithm.  This method
     * is provided in order to satisfy the requirements of the
     * Decoder interface, and will throw a DecoderException if the
     * supplied object is not of type byte[].
     *
     * @param pObject Object to decode
     * @return An object (of type byte[]) containing the 
     *         binary data which corresponds to the byte[] supplied.
     * @throws DecoderException if the parameter supplied is not
     *                          of type byte[]
     */
    public Object decode(Object pObject) throws DecoderException {
        if (!(pObject instanceof byte[])) {
            throw new DecoderException("Parameter supplied to Base64 decode is not a byte[]");
        }
        return decode((byte[]) pObject);
    }

    /**
     * Decodes a byte[] containing containing
     * characters in the Base64 alphabet.
     *
     * @param pArray A byte array containing Base64 character data
     * @return a byte array containing binary data
     */
    public byte[] decode(byte[] pArray) {
        return decodeBase64(pArray);
    }

    /**
     * Encodes binary data using the base64 algorithm, optionally
     * chunking the output into 76 character blocks.
     *
     * @param binaryData Array containing binary data to encode.
     * @param isChunked if isChunked is true this encoder will chunk
     *                  the base64 output into 76 character blocks
     * @return Base64-encoded data.
     */
    public static byte[] encodeBase64(byte[] binaryData, boolean isChunked) {
        int lengthDataBits = binaryData.length * EIGHTBIT;
        int fewerThan24bits = lengthDataBits % TWENTYFOURBITGROUP;
        int numberTriplets = lengthDataBits / TWENTYFOURBITGROUP;
        byte encodedData[] = null;
        int encodedDataLength = 0;
        int nbrChunks = 0;

        if (fewerThan24bits != 0) {
            //data not divisible by 24 bit
            encodedDataLength = (numberTriplets + 1) * 4;
        } else {
            // 16 or 8 bit
            encodedDataLength = numberTriplets * 4;
        }

        // If the output is to be "chunked" into 76 character sections, 
        // for compliance with RFC 2045 MIME, then it is important to 
        // allow for extra length to account for the separator(s)
        if (isChunked) {

            nbrChunks =
                (CHUNK_SEPARATOR.length == 0 ? 0 : (int) Math.ceil((float) encodedDataLength / CHUNK_SIZE));
            encodedDataLength += nbrChunks * CHUNK_SEPARATOR.length;
        }

        encodedData = new byte[encodedDataLength];

        byte k = 0, l = 0, b1 = 0, b2 = 0, b3 = 0;

        int encodedIndex = 0;
        int dataIndex = 0;
        int i = 0;
        int nextSeparatorIndex = CHUNK_SIZE;
        int chunksSoFar = 0;

        //log.debug("number of triplets = " + numberTriplets);
        for (i = 0; i < numberTriplets; i++) {
            dataIndex = i * 3;
            b1 = binaryData[dataIndex];
            b2 = binaryData[dataIndex + 1];
            b3 = binaryData[dataIndex + 2];

            //log.debug("b1= " + b1 +", b2= " + b2 + ", b3= " + b3);

            l = (byte) (b2 & 0x0f);
            k = (byte) (b1 & 0x03);

            byte val1 =
                ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);
            byte val2 =
                ((b2 & SIGN) == 0) ? (byte) (b2 >> 4) : (byte) ((b2) >> 4 ^ 0xf0);
            byte val3 =
                ((b3 & SIGN) == 0) ? (byte) (b3 >> 6) : (byte) ((b3) >> 6 ^ 0xfc);

            encodedData[encodedIndex] = lookUpBase64Alphabet[val1];
            //log.debug( "val2 = " + val2 );
            //log.debug( "k4   = " + (k<<4) );
            //log.debug(  "vak  = " + (val2 | (k<<4)) );
            encodedData[encodedIndex + 1] =
                lookUpBase64Alphabet[val2 | (k << 4)];
            encodedData[encodedIndex + 2] =
                lookUpBase64Alphabet[(l << 2) | val3];
            encodedData[encodedIndex + 3] = lookUpBase64Alphabet[b3 & 0x3f];

            encodedIndex += 4;

            // If we are chunking, let's put a chunk separator down.
            if (isChunked) {
                // this assumes that CHUNK_SIZE % 4 == 0
                if (encodedIndex == nextSeparatorIndex) {
                    System.arraycopy(
                        CHUNK_SEPARATOR,
                        0,
                        encodedData,
                        encodedIndex,
                        CHUNK_SEPARATOR.length);
                    chunksSoFar++;
                    nextSeparatorIndex =
                        (CHUNK_SIZE * (chunksSoFar + 1)) + 
                        (chunksSoFar * CHUNK_SEPARATOR.length);
                    encodedIndex += CHUNK_SEPARATOR.length;
                }
            }
        }

        // form integral number of 6-bit groups
        dataIndex = i * 3;

        if (fewerThan24bits == EIGHTBIT) {
            b1 = binaryData[dataIndex];
            k = (byte) (b1 & 0x03);
            //log.debug("b1=" + b1);
            //log.debug("b1<<2 = " + (b1>>2) );
            byte val1 =
                ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);
            encodedData[encodedIndex] = lookUpBase64Alphabet[val1];
            encodedData[encodedIndex + 1] = lookUpBase64Alphabet[k << 4];
            encodedData[encodedIndex + 2] = PAD;
            encodedData[encodedIndex + 3] = PAD;
        } else if (fewerThan24bits == SIXTEENBIT) {

            b1 = binaryData[dataIndex];
            b2 = binaryData[dataIndex + 1];
            l = (byte) (b2 & 0x0f);
            k = (byte) (b1 & 0x03);

            byte val1 =
                ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);
            byte val2 =
                ((b2 & SIGN) == 0) ? (byte) (b2 >> 4) : (byte) ((b2) >> 4 ^ 0xf0);

            encodedData[encodedIndex] = lookUpBase64Alphabet[val1];
            encodedData[encodedIndex + 1] =
                lookUpBase64Alphabet[val2 | (k << 4)];
            encodedData[encodedIndex + 2] = lookUpBase64Alphabet[l << 2];
            encodedData[encodedIndex + 3] = PAD;
        }

        if (isChunked) {
            // we also add a separator to the end of the final chunk.
            if (chunksSoFar < nbrChunks) {
                System.arraycopy(
                    CHUNK_SEPARATOR,
                    0,
                    encodedData,
                    encodedDataLength - CHUNK_SEPARATOR.length,
                    CHUNK_SEPARATOR.length);
            }
        }

        return encodedData;
    }

    /**
     * Decodes Base64 data into octects
     *
     * @param base64Data Byte array containing Base64 data
     * @return Array containing decoded data.
     */
    public static byte[] decodeBase64(byte[] base64Data) {
        // RFC 2045 requires that we discard ALL non-Base64 characters
        base64Data = discardNonBase64(base64Data);

        // handle the edge case, so we don't have to worry about it later
        if (base64Data.length == 0) {
            return new byte[0];
        }

        int numberQuadruple = base64Data.length / FOURBYTE;
        byte decodedData[] = null;
        byte b1 = 0, b2 = 0, b3 = 0, b4 = 0, marker0 = 0, marker1 = 0;

        // Throw away anything not in base64Data

        int encodedIndex = 0;
        int dataIndex = 0;
        {
            // this sizes the output array properly - rlw
            int lastData = base64Data.length;
            // ignore the '=' padding
            while (base64Data[lastData - 1] == PAD) {
                if (--lastData == 0) {
                    return new byte[0];
                }
            }
            decodedData = new byte[lastData - numberQuadruple];
        }
        
        for (int i = 0; i < numberQuadruple; i++) {
            dataIndex = i * 4;
            marker0 = base64Data[dataIndex + 2];
            marker1 = base64Data[dataIndex + 3];
            
            b1 = base64Alphabet[base64Data[dataIndex]];
            b2 = base64Alphabet[base64Data[dataIndex + 1]];
            
            if (marker0 != PAD && marker1 != PAD) {
                //No PAD e.g 3cQl
                b3 = base64Alphabet[marker0];
                b4 = base64Alphabet[marker1];
                
                decodedData[encodedIndex] = (byte) (b1 << 2 | b2 >> 4);
                decodedData[encodedIndex + 1] =
                    (byte) (((b2 & 0xf) << 4) | ((b3 >> 2) & 0xf));
                decodedData[encodedIndex + 2] = (byte) (b3 << 6 | b4);
            } else if (marker0 == PAD) {
                //Two PAD e.g. 3c[Pad][Pad]
                decodedData[encodedIndex] = (byte) (b1 << 2 | b2 >> 4);
            } else if (marker1 == PAD) {
                //One PAD e.g. 3cQ[Pad]
                b3 = base64Alphabet[marker0];
                
                decodedData[encodedIndex] = (byte) (b1 << 2 | b2 >> 4);
                decodedData[encodedIndex + 1] =
                    (byte) (((b2 & 0xf) << 4) | ((b3 >> 2) & 0xf));
            }
            encodedIndex += 3;
        }
        return decodedData;
    }
    
    /**
     * Discards any whitespace from a base-64 encoded block.
     *
     * @param data The base-64 encoded data to discard the whitespace
     * from.
     * @return The data, less whitespace (see RFC 2045).
     */
    static byte[] discardWhitespace(byte[] data) {
        byte groomedData[] = new byte[data.length];
        int bytesCopied = 0;
        
        for (int i = 0; i < data.length; i++) {
            switch (data[i]) {
            case (byte) ' ' :
            case (byte) '\n' :
            case (byte) '\r' :
            case (byte) '\t' :
                    break;
            default:
                    groomedData[bytesCopied++] = data[i];
            }
        }

        byte packedData[] = new byte[bytesCopied];

        System.arraycopy(groomedData, 0, packedData, 0, bytesCopied);

        return packedData;
    }

    /**
     * Discards any characters outside of the base64 alphabet, per
     * the requirements on page 25 of RFC 2045 - "Any characters
     * outside of the base64 alphabet are to be ignored in base64
     * encoded data."
     *
     * @param data The base-64 encoded data to groom
     * @return The data, less non-base64 characters (see RFC 2045).
     */
    static byte[] discardNonBase64(byte[] data) {
        byte groomedData[] = new byte[data.length];
        int bytesCopied = 0;

        for (int i = 0; i < data.length; i++) {
            if (isBase64(data[i])) {
                groomedData[bytesCopied++] = data[i];
            }
        }

        byte packedData[] = new byte[bytesCopied];

        System.arraycopy(groomedData, 0, packedData, 0, bytesCopied);

        return packedData;
    }


    // Implementation of the Encoder Interface

    /**
     * Encodes an Object using the base64 algorithm.  This method
     * is provided in order to satisfy the requirements of the
     * Encoder interface, and will throw an EncoderException if the
     * supplied object is not of type byte[].
     *
     * @param pObject Object to encode
     * @return An object (of type byte[]) containing the 
     *         base64 encoded data which corresponds to the byte[] supplied.
     * @throws EncoderException if the parameter supplied is not
     *                          of type byte[]
     */
    public Object encode(Object pObject) throws EncoderException {
        if (!(pObject instanceof byte[])) {
            throw new EncoderException(
                "Parameter supplied to Base64 encode is not a byte[]");
        }
        return encode((byte[]) pObject);
    }

    /**
     * Encodes a byte[] containing binary data, into a byte[] containing
     * characters in the Base64 alphabet.
     *
     * @param pArray a byte array containing binary data
     * @return A byte array containing only Base64 character data
     */
    public byte[] encode(byte[] pArray) {
        return encodeBase64(pArray, false);
    }

}

```

````

## Bob's answer

````
I'll analyze the code carefully to understand its actual behavior, then write characterization tests that pin down
exactly what it does.

Let me trace through the key behaviors:

Tool calls:
- spawn_subagent

Tool (1) 2026-09-26 19:24:55.767 UTC (+215.6s) Success

Tool: spawn_subagent
Args:
- name: general
- description:
Analyze this Base64 implementation and produce precise expected values for characterization tests.

Here is the full...




Now I have all the precise values needed. Let me also trace a few more subtle behaviors before writing the tests:

- decodeBase64("====") decoded size: numberQuadruple=1, lastData loop: 4→3→2→1→0 → returns new byte[0] ✓
- encodeBase64(new byte[]{(byte)0xFF, (byte)0xFF}, false): fewerThan24bits=16 (SIXTEENBIT). b1=-1, b2=-1. k=3, l=15.
  val1=63→'/'. val2=((−1>>4)^0xF0)|(3<<4) = (0xF^0xF0)|0x30 — wait: (byte)((b2)>>4^0xF0): b2=-1, -1>>4=-1 (0xFFFF...FF),
  ^0xF0 = ...0F, as byte = 0x0F=15. 15|(3<<4)=15|48=63→'/'. l<<2 = 15<<2=60→lookUpBase64Alphabet[60]='8'. PAD at end.
  Result: "//8="`
- Chunked structure: after 76 encoded characters, CRLF is inserted; the final chunk also gets a trailing CRLF.
- decode(Object) with String: DecoderException thrown since String is not byte[].
- isArrayByteBase64 with null: NullPointerException since it calls discardWhitespace(arrayOctect) with null →
  data.length NPE.

```java
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
```


Key characterization findings pinned by these tests:

┌──────────────────────────────────┬───────────────────────────────────────────────────────────────────────────────────┐
│ Behaviour                        │ What the code actually does                                                       │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ isArrayByteBase64(new            │ Throws ]8;;.\ArrayIndexOutOfBoundsException]8;;\ — array is only 255 elements (indices       │
│ byte[]{(byte)0xFF})              │ 0–254) but byte −1 is used as index 255                                           │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ decode(Object) null              │ Throws ]8;;.\DecoderException]8;;\ (not NPE) — the instanceof check catches null             │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ Empty input                      │ Encodes and decodes to empty byte array — not an error                            │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ isArrayByteBase64(new byte[0])   │ Returns true — the comment in the source says "shouldn't a 0 length array be      │
│                                  │ valid?" and leaves the return false commented out                                 │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ PAD '='                          │ ]8;;.\isBase64]8;;\ returns true for '=', so isArrayByteBase64(bytes("====")) is true and    │
│                                  │ decodes to empty                                                                  │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ Non-base64 chars in decode input │ Silently discarded by ]8;;.\discardNonBase64]8;;\ — RFC 2045 compliant                       │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ Chunked trailing CRLF            │ Every chunk, including the last one, always gets a CRLF appended                  │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ Whitespace in isArrayByteBase64  │ Stripped before alphabet check, so " " → true, "TW Fu" → true                     │
└──────────────────────────────────┴───────────────────────────────────────────────────────────────────────────────────┘

````

## Raw Bob output

````

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
User (1) 2026-09-26 19:21:20.728 UTC (+0.6s)

You are writing characterization tests for a legacy Java class before it is modernized.                                 
                                                                                                                        
Goal: pin down what the code DOES today, including odd behaviour, so any change in behaviour                            
during modernization makes a test fail. Do not test what the comments say it should do; test                            
what it actually does. Where the code and its javadoc disagree, the code wins.                                          
                                                                                                                        
Requirements:                                                                                                           
- One JUnit 5 test class named Base64CharacterizationTest in package org.apache.commons.codec.binary.                   
- Use only JUnit 5 (org.junit.jupiter.api) and the JDK. No other libraries.                                             
- Call only the public API of Base64 shown below; keep test inputs deterministic.                                       
- Cover boundaries, special cases and branches, input normalisation (whitespace, letter case,                           
  null, empty, very short input), error cases, and anything the code treats specially.                                  
- Every test must pass against the code exactly as written below.                                                       
- Reply with the complete test class in a single ```java code block. Keep every line under 100 characters.              
                                                                                                                        
```java                                                                                                                 
  /*                                                                                                                    
   * Copyright 2001-2004 The Apache Software Foundation.                                                                
   *                                                                                                                    
   * Licensed under the Apache License, Version 2.0 (the "License");                                                    
   * you may not use this file except in compliance with the License.                                                   
   * You may obtain a copy of the License at                                                                            
   *                                                                                                                    
   *      http://www.apache.org/licenses/LICENSE-2.0                                                                    
   *                                                                                                                    
   * Unless required by applicable law or agreed to in writing, software                                                
   * distributed under the License is distributed on an "AS IS" BASIS,                                                  
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.                                           
   * See the License for the specific language governing permissions and                                                
   * limitations under the License.                                                                                     
   */                                                                                                                   
                                                                                                                        
  package org.apache.commons.codec.binary;                                                                              
                                                                                                                        
  import org.apache.commons.codec.BinaryDecoder;                                                                        
  import org.apache.commons.codec.BinaryEncoder;                                                                        
  import org.apache.commons.codec.DecoderException;                                                                     
  import org.apache.commons.codec.EncoderException;                                                                     
                                                                                                                        
  /**                                                                                                                   
   * Provides Base64 encoding and decoding as defined by RFC 2045.                                                      
   *                                                                                                                    
   * <p>This class implements section <cite>6.8. Base64 Content-Transfer-Encoding</cite>                                
   * from RFC 2045 <cite>Multipurpose Internet Mail Extensions (MIME) Part One:                                         
   * Format of Internet Message Bodies</cite> by Freed and Borenstein.</p>                                              
   *                                                                                                                    
   * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>                                                    
   * @author Apache Software Foundation                                                                                 
   * @since 1.0-dev                                                                                                     
   * @version $Id: Base64.java,v 1.20 2004/05/24 00:21:24 ggregory Exp $                                                
   */                                                                                                                   
  public class Base64 implements BinaryEncoder, BinaryDecoder {                                                         
                                                                                                                        
      /**                                                                                                               
       * Chunk size per RFC 2045 section 6.8.                                                                           
       *                                                                                                                
       * <p>The {@value} character limit does not count the trailing CRLF, but counts                                   
       * all other characters, including any equal signs.</p>                                                           
       *                                                                                                                
       * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045 section 6.8</a>                                    
       */                                                                                                               
      static final int CHUNK_SIZE = 76;                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Chunk separator per RFC 2045 section 2.1.                                                                      
       *                                                                                                                
       * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045 section 2.1</a>                                    
       */                                                                                                               
      static final byte[] CHUNK_SEPARATOR = "\r\n".getBytes();                                                          
                                                                                                                        
      /**                                                                                                               
       * The base length.                                                                                               
       */                                                                                                               
      static final int BASELENGTH = 255;                                                                                
                                                                                                                        
      /**                                                                                                               
       * Lookup length.                                                                                                 
       */                                                                                                               
      static final int LOOKUPLENGTH = 64;                                                                               
                                                                                                                        
      /**                                                                                                               
       * Used to calculate the number of bits in a byte.                                                                
       */                                                                                                               
      static final int EIGHTBIT = 8;                                                                                    
                                                                                                                        
      /**                                                                                                               
       * Used when encoding something which has fewer than 24 bits.                                                     
       */                                                                                                               
      static final int SIXTEENBIT = 16;                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Used to determine how many bits data contains.                                                                 
       */                                                                                                               
      static final int TWENTYFOURBITGROUP = 24;                                                                         
                                                                                                                        
      /**                                                                                                               
       * Used to get the number of Quadruples.                                                                          
       */                                                                                                               
      static final int FOURBYTE = 4;                                                                                    
                                                                                                                        
      /**                                                                                                               
       * Used to test the sign of a byte.                                                                               
       */                                                                                                               
      static final int SIGN = -128;                                                                                     
                                                                                                                        
      /**                                                                                                               
       * Byte used to pad output.                                                                                       
       */                                                                                                               
      static final byte PAD = (byte) '=';                                                                               
                                                                                                                        
      // Create arrays to hold the base64 characters and a                                                              
      // lookup for base64 chars                                                                                        
      private static byte[] base64Alphabet = new byte[BASELENGTH];                                                      
      private static byte[] lookUpBase64Alphabet = new byte[LOOKUPLENGTH];                                              
                                                                                                                        
      // Populating the lookup and character arrays                                                                     
      static {                                                                                                          
          for (int i = 0; i < BASELENGTH; i++) {                                                                        
              base64Alphabet[i] = (byte) -1;                                                                            
          }                                                                                                             
          for (int i = 'Z'; i >= 'A'; i--) {                                                                            
              base64Alphabet[i] = (byte) (i - 'A');                                                                     
          }                                                                                                             
          for (int i = 'z'; i >= 'a'; i--) {                                                                            
              base64Alphabet[i] = (byte) (i - 'a' + 26);                                                                
          }                                                                                                             
          for (int i = '9'; i >= '0'; i--) {                                                                            
              base64Alphabet[i] = (byte) (i - '0' + 52);                                                                
          }                                                                                                             
                                                                                                                        
          base64Alphabet['+'] = 62;                                                                                     
          base64Alphabet['/'] = 63;                                                                                     
                                                                                                                        
          for (int i = 0; i <= 25; i++) {                                                                               
              lookUpBase64Alphabet[i] = (byte) ('A' + i);                                                               
          }                                                                                                             
                                                                                                                        
          for (int i = 26, j = 0; i <= 51; i++, j++) {                                                                  
              lookUpBase64Alphabet[i] = (byte) ('a' + j);                                                               
          }                                                                                                             
                                                                                                                        
          for (int i = 52, j = 0; i <= 61; i++, j++) {                                                                  
              lookUpBase64Alphabet[i] = (byte) ('0' + j);                                                               
          }                                                                                                             
                                                                                                                        
          lookUpBase64Alphabet[62] = (byte) '+';                                                                        
          lookUpBase64Alphabet[63] = (byte) '/';                                                                        
      }                                                                                                                 
                                                                                                                        
      private static boolean isBase64(byte octect) {                                                                    
          if (octect == PAD) {                                                                                          
              return true;                                                                                              
          } else if (base64Alphabet[octect] == -1) {                                                                    
              return false;                                                                                             
          } else {                                                                                                      
              return true;                                                                                              
          }                                                                                                             
      }                                                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Tests a given byte array to see if it contains                                                                 
       * only valid characters within the Base64 alphabet.                                                              
       *                                                                                                                
       * @param arrayOctect byte array to test                                                                          
       * @return true if all bytes are valid characters in the Base64                                                   
       *         alphabet or if the byte array is empty; false, otherwise                                               
       */                                                                                                               
      public static boolean isArrayByteBase64(byte[] arrayOctect) {                                                     
                                                                                                                        
          arrayOctect = discardWhitespace(arrayOctect);                                                                 
                                                                                                                        
          int length = arrayOctect.length;                                                                              
          if (length == 0) {                                                                                            
              // shouldn't a 0 length array be valid base64 data?                                                       
              // return false;                                                                                          
              return true;                                                                                              
          }                                                                                                             
          for (int i = 0; i < length; i++) {                                                                            
              if (!isBase64(arrayOctect[i])) {                                                                          
                  return false;                                                                                         
              }                                                                                                         
          }                                                                                                             
          return true;                                                                                                  
      }                                                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Encodes binary data using the base64 algorithm but                                                             
       * does not chunk the output.                                                                                     
       *                                                                                                                
       * @param binaryData binary data to encode                                                                        
       * @return Base64 characters                                                                                      
       */                                                                                                               
      public static byte[] encodeBase64(byte[] binaryData) {                                                            
          return encodeBase64(binaryData, false);                                                                       
      }                                                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Encodes binary data using the base64 algorithm and chunks                                                      
       * the encoded output into 76 character blocks                                                                    
       *                                                                                                                
       * @param binaryData binary data to encode                                                                        
       * @return Base64 characters chunked in 76 character blocks                                                       
       */                                                                                                               
      public static byte[] encodeBase64Chunked(byte[] binaryData) {                                                     
          return encodeBase64(binaryData, true);                                                                        
      }                                                                                                                 
                                                                                                                        
                                                                                                                        
      /**                                                                                                               
       * Decodes an Object using the base64 algorithm.  This method                                                     
       * is provided in order to satisfy the requirements of the                                                        
       * Decoder interface, and will throw a DecoderException if the                                                    
       * supplied object is not of type byte[].                                                                         
       *                                                                                                                
       * @param pObject Object to decode                                                                                
       * @return An object (of type byte[]) containing the                                                              
       *         binary data which corresponds to the byte[] supplied.                                                  
       * @throws DecoderException if the parameter supplied is not                                                      
       *                          of type byte[]                                                                        
       */                                                                                                               
      public Object decode(Object pObject) throws DecoderException {                                                    
          if (!(pObject instanceof byte[])) {                                                                           
              throw new DecoderException("Parameter supplied to Base64 decode is not a byte[]");                        
          }                                                                                                             
          return decode((byte[]) pObject);                                                                              
      }                                                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Decodes a byte[] containing containing                                                                         
       * characters in the Base64 alphabet.                                                                             
       *                                                                                                                
       * @param pArray A byte array containing Base64 character data                                                    
       * @return a byte array containing binary data                                                                    
       */                                                                                                               
      public byte[] decode(byte[] pArray) {                                                                             
          return decodeBase64(pArray);                                                                                  
      }                                                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Encodes binary data using the base64 algorithm, optionally                                                     
       * chunking the output into 76 character blocks.                                                                  
       *                                                                                                                
       * @param binaryData Array containing binary data to encode.                                                      
       * @param isChunked if isChunked is true this encoder will chunk                                                  
       *                  the base64 output into 76 character blocks                                                    
       * @return Base64-encoded data.                                                                                   
       */                                                                                                               
      public static byte[] encodeBase64(byte[] binaryData, boolean isChunked) {                                         
          int lengthDataBits = binaryData.length * EIGHTBIT;                                                            
          int fewerThan24bits = lengthDataBits % TWENTYFOURBITGROUP;                                                    
          int numberTriplets = lengthDataBits / TWENTYFOURBITGROUP;                                                     
          byte encodedData[] = null;                                                                                    
          int encodedDataLength = 0;                                                                                    
          int nbrChunks = 0;                                                                                            
                                                                                                                        
          if (fewerThan24bits != 0) {                                                                                   
              //data not divisible by 24 bit                                                                            
              encodedDataLength = (numberTriplets + 1) * 4;                                                             
          } else {                                                                                                      
              // 16 or 8 bit                                                                                            
              encodedDataLength = numberTriplets * 4;                                                                   
          }                                                                                                             
                                                                                                                        
          // If the output is to be "chunked" into 76 character sections,                                               
          // for compliance with RFC 2045 MIME, then it is important to                                                 
          // allow for extra length to account for the separator(s)                                                     
          if (isChunked) {                                                                                              
                                                                                                                        
              nbrChunks =                                                                                               
                  (CHUNK_SEPARATOR.length == 0 ? 0 : (int) Math.ceil((float) encodedDataLength / CHUNK_SIZE));          
              encodedDataLength += nbrChunks * CHUNK_SEPARATOR.length;                                                  
          }                                                                                                             
                                                                                                                        
          encodedData = new byte[encodedDataLength];                                                                    
                                                                                                                        
          byte k = 0, l = 0, b1 = 0, b2 = 0, b3 = 0;                                                                    
                                                                                                                        
          int encodedIndex = 0;                                                                                         
          int dataIndex = 0;                                                                                            
          int i = 0;                                                                                                    
          int nextSeparatorIndex = CHUNK_SIZE;                                                                          
          int chunksSoFar = 0;                                                                                          
                                                                                                                        
          //log.debug("number of triplets = " + numberTriplets);                                                        
          for (i = 0; i < numberTriplets; i++) {                                                                        
              dataIndex = i * 3;                                                                                        
              b1 = binaryData[dataIndex];                                                                               
              b2 = binaryData[dataIndex + 1];                                                                           
              b3 = binaryData[dataIndex + 2];                                                                           
                                                                                                                        
              //log.debug("b1= " + b1 +", b2= " + b2 + ", b3= " + b3);                                                  
                                                                                                                        
              l = (byte) (b2 & 0x0f);                                                                                   
              k = (byte) (b1 & 0x03);                                                                                   
                                                                                                                        
              byte val1 =                                                                                               
                  ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);                                    
              byte val2 =                                                                                               
                  ((b2 & SIGN) == 0) ? (byte) (b2 >> 4) : (byte) ((b2) >> 4 ^ 0xf0);                                    
              byte val3 =                                                                                               
                  ((b3 & SIGN) == 0) ? (byte) (b3 >> 6) : (byte) ((b3) >> 6 ^ 0xfc);                                    
                                                                                                                        
              encodedData[encodedIndex] = lookUpBase64Alphabet[val1];                                                   
              //log.debug( "val2 = " + val2 );                                                                          
              //log.debug( "k4   = " + (k<<4) );                                                                        
              //log.debug(  "vak  = " + (val2 | (k<<4)) );                                                              
              encodedData[encodedIndex + 1] =                                                                           
                  lookUpBase64Alphabet[val2 | (k << 4)];                                                                
              encodedData[encodedIndex + 2] =                                                                           
                  lookUpBase64Alphabet[(l << 2) | val3];                                                                
              encodedData[encodedIndex + 3] = lookUpBase64Alphabet[b3 & 0x3f];                                          
                                                                                                                        
              encodedIndex += 4;                                                                                        
                                                                                                                        
              // If we are chunking, let's put a chunk separator down.                                                  
              if (isChunked) {                                                                                          
                  // this assumes that CHUNK_SIZE % 4 == 0                                                              
                  if (encodedIndex == nextSeparatorIndex) {                                                             
                      System.arraycopy(                                                                                 
                          CHUNK_SEPARATOR,                                                                              
                          0,                                                                                            
                          encodedData,                                                                                  
                          encodedIndex,                                                                                 
                          CHUNK_SEPARATOR.length);                                                                      
                      chunksSoFar++;                                                                                    
                      nextSeparatorIndex =                                                                              
                          (CHUNK_SIZE * (chunksSoFar + 1)) +                                                            
                          (chunksSoFar * CHUNK_SEPARATOR.length);                                                       
                      encodedIndex += CHUNK_SEPARATOR.length;                                                           
                  }                                                                                                     
              }                                                                                                         
          }                                                                                                             
                                                                                                                        
          // form integral number of 6-bit groups                                                                       
          dataIndex = i * 3;                                                                                            
                                                                                                                        
          if (fewerThan24bits == EIGHTBIT) {                                                                            
              b1 = binaryData[dataIndex];                                                                               
              k = (byte) (b1 & 0x03);                                                                                   
              //log.debug("b1=" + b1);                                                                                  
              //log.debug("b1<<2 = " + (b1>>2) );                                                                       
              byte val1 =                                                                                               
                  ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);                                    
              encodedData[encodedIndex] = lookUpBase64Alphabet[val1];                                                   
              encodedData[encodedIndex + 1] = lookUpBase64Alphabet[k << 4];                                             
              encodedData[encodedIndex + 2] = PAD;                                                                      
              encodedData[encodedIndex + 3] = PAD;                                                                      
          } else if (fewerThan24bits == SIXTEENBIT) {                                                                   
                                                                                                                        
              b1 = binaryData[dataIndex];                                                                               
              b2 = binaryData[dataIndex + 1];                                                                           
              l = (byte) (b2 & 0x0f);                                                                                   
              k = (byte) (b1 & 0x03);                                                                                   
                                                                                                                        
              byte val1 =                                                                                               
                  ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);                                    
              byte val2 =                                                                                               
                  ((b2 & SIGN) == 0) ? (byte) (b2 >> 4) : (byte) ((b2) >> 4 ^ 0xf0);                                    
                                                                                                                        
              encodedData[encodedIndex] = lookUpBase64Alphabet[val1];                                                   
              encodedData[encodedIndex + 1] =                                                                           
                  lookUpBase64Alphabet[val2 | (k << 4)];                                                                
              encodedData[encodedIndex + 2] = lookUpBase64Alphabet[l << 2];                                             
              encodedData[encodedIndex + 3] = PAD;                                                                      
          }                                                                                                             
                                                                                                                        
          if (isChunked) {                                                                                              
              // we also add a separator to the end of the final chunk.                                                 
              if (chunksSoFar < nbrChunks) {                                                                            
                  System.arraycopy(                                                                                     
                      CHUNK_SEPARATOR,                                                                                  
                      0,                                                                                                
                      encodedData,                                                                                      
                      encodedDataLength - CHUNK_SEPARATOR.length,                                                       
                      CHUNK_SEPARATOR.length);                                                                          
              }                                                                                                         
          }                                                                                                             
                                                                                                                        
          return encodedData;                                                                                           
      }                                                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Decodes Base64 data into octects                                                                               
       *                                                                                                                
       * @param base64Data Byte array containing Base64 data                                                            
       * @return Array containing decoded data.                                                                         
       */                                                                                                               
      public static byte[] decodeBase64(byte[] base64Data) {                                                            
          // RFC 2045 requires that we discard ALL non-Base64 characters                                                
          base64Data = discardNonBase64(base64Data);                                                                    
                                                                                                                        
          // handle the edge case, so we don't have to worry about it later                                             
          if (base64Data.length == 0) {                                                                                 
              return new byte[0];                                                                                       
          }                                                                                                             
                                                                                                                        
          int numberQuadruple = base64Data.length / FOURBYTE;                                                           
          byte decodedData[] = null;                                                                                    
          byte b1 = 0, b2 = 0, b3 = 0, b4 = 0, marker0 = 0, marker1 = 0;                                                
                                                                                                                        
          // Throw away anything not in base64Data                                                                      
                                                                                                                        
          int encodedIndex = 0;                                                                                         
          int dataIndex = 0;                                                                                            
          {                                                                                                             
              // this sizes the output array properly - rlw                                                             
              int lastData = base64Data.length;                                                                         
              // ignore the '=' padding                                                                                 
              while (base64Data[lastData - 1] == PAD) {                                                                 
                  if (--lastData == 0) {                                                                                
                      return new byte[0];                                                                               
                  }                                                                                                     
              }                                                                                                         
              decodedData = new byte[lastData - numberQuadruple];                                                       
          }                                                                                                             
                                                                                                                        
          for (int i = 0; i < numberQuadruple; i++) {                                                                   
              dataIndex = i * 4;                                                                                        
              marker0 = base64Data[dataIndex + 2];                                                                      
              marker1 = base64Data[dataIndex + 3];                                                                      
                                                                                                                        
              b1 = base64Alphabet[base64Data[dataIndex]];                                                               
              b2 = base64Alphabet[base64Data[dataIndex + 1]];                                                           
                                                                                                                        
              if (marker0 != PAD && marker1 != PAD) {                                                                   
                  //No PAD e.g 3cQl                                                                                     
                  b3 = base64Alphabet[marker0];                                                                         
                  b4 = base64Alphabet[marker1];                                                                         
                                                                                                                        
                  decodedData[encodedIndex] = (byte) (b1 << 2 | b2 >> 4);                                               
                  decodedData[encodedIndex + 1] =                                                                       
                      (byte) (((b2 & 0xf) << 4) | ((b3 >> 2) & 0xf));                                                   
                  decodedData[encodedIndex + 2] = (byte) (b3 << 6 | b4);                                                
              } else if (marker0 == PAD) {                                                                              
                  //Two PAD e.g. 3c[Pad][Pad]                                                                           
                  decodedData[encodedIndex] = (byte) (b1 << 2 | b2 >> 4);                                               
              } else if (marker1 == PAD) {                                                                              
                  //One PAD e.g. 3cQ[Pad]                                                                               
                  b3 = base64Alphabet[marker0];                                                                         
                                                                                                                        
                  decodedData[encodedIndex] = (byte) (b1 << 2 | b2 >> 4);                                               
                  decodedData[encodedIndex + 1] =                                                                       
                      (byte) (((b2 & 0xf) << 4) | ((b3 >> 2) & 0xf));                                                   
              }                                                                                                         
              encodedIndex += 3;                                                                                        
          }                                                                                                             
          return decodedData;                                                                                           
      }                                                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Discards any whitespace from a base-64 encoded block.                                                          
       *                                                                                                                
       * @param data The base-64 encoded data to discard the whitespace                                                 
       * from.                                                                                                          
       * @return The data, less whitespace (see RFC 2045).                                                              
       */                                                                                                               
      static byte[] discardWhitespace(byte[] data) {                                                                    
          byte groomedData[] = new byte[data.length];                                                                   
          int bytesCopied = 0;                                                                                          
                                                                                                                        
          for (int i = 0; i < data.length; i++) {                                                                       
              switch (data[i]) {                                                                                        
              case (byte) ' ' :                                                                                         
              case (byte) '\n' :                                                                                        
              case (byte) '\r' :                                                                                        
              case (byte) '\t' :                                                                                        
                      break;                                                                                            
              default:                                                                                                  
                      groomedData[bytesCopied++] = data[i];                                                             
              }                                                                                                         
          }                                                                                                             
                                                                                                                        
          byte packedData[] = new byte[bytesCopied];                                                                    
                                                                                                                        
          System.arraycopy(groomedData, 0, packedData, 0, bytesCopied);                                                 
                                                                                                                        
          return packedData;                                                                                            
      }                                                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Discards any characters outside of the base64 alphabet, per                                                    
       * the requirements on page 25 of RFC 2045 - "Any characters                                                      
       * outside of the base64 alphabet are to be ignored in base64                                                     
       * encoded data."                                                                                                 
       *                                                                                                                
       * @param data The base-64 encoded data to groom                                                                  
       * @return The data, less non-base64 characters (see RFC 2045).                                                   
       */                                                                                                               
      static byte[] discardNonBase64(byte[] data) {                                                                     
          byte groomedData[] = new byte[data.length];                                                                   
          int bytesCopied = 0;                                                                                          
                                                                                                                        
          for (int i = 0; i < data.length; i++) {                                                                       
              if (isBase64(data[i])) {                                                                                  
                  groomedData[bytesCopied++] = data[i];                                                                 
              }                                                                                                         
          }                                                                                                             
                                                                                                                        
          byte packedData[] = new byte[bytesCopied];                                                                    
                                                                                                                        
          System.arraycopy(groomedData, 0, packedData, 0, bytesCopied);                                                 
                                                                                                                        
          return packedData;                                                                                            
      }                                                                                                                 
                                                                                                                        
                                                                                                                        
      // Implementation of the Encoder Interface                                                                        
                                                                                                                        
      /**                                                                                                               
       * Encodes an Object using the base64 algorithm.  This method                                                     
       * is provided in order to satisfy the requirements of the                                                        
       * Encoder interface, and will throw an EncoderException if the                                                   
       * supplied object is not of type byte[].                                                                         
       *                                                                                                                
       * @param pObject Object to encode                                                                                
       * @return An object (of type byte[]) containing the                                                              
       *         base64 encoded data which corresponds to the byte[] supplied.                                          
       * @throws EncoderException if the parameter supplied is not                                                      
       *                          of type byte[]                                                                        
       */                                                                                                               
      public Object encode(Object pObject) throws EncoderException {                                                    
          if (!(pObject instanceof byte[])) {                                                                           
              throw new EncoderException(                                                                               
                  "Parameter supplied to Base64 encode is not a byte[]");                                               
          }                                                                                                             
          return encode((byte[]) pObject);                                                                              
      }                                                                                                                 
                                                                                                                        
      /**                                                                                                               
       * Encodes a byte[] containing binary data, into a byte[] containing                                              
       * characters in the Base64 alphabet.                                                                             
       *                                                                                                                
       * @param pArray a byte array containing binary data                                                              
       * @return A byte array containing only Base64 character data                                                     
       */                                                                                                               
      public byte[] encode(byte[] pArray) {                                                                             
          return encodeBase64(pArray, false);                                                                           
      }                                                                                                                 
                                                                                                                        
  }                                                                                                                     
                                                                                                                        
```                                                                                                                     

Starting subagent: general
   
Analyze this Base64 implementation and produce precise expected values for characterization tests.

Here is the full source code:

```java
static final int CHUNK_SIZE = 76;
static final byte[] CHUNK_SEPARATOR = "\r\n".getBytes();
static final int BASELENGTH = 255;
static final int EIGHTBIT = 8;
static final int SIXTEENBIT = 16;
static final int TWENTYFOURBITGROUP = 24;
static final int FOURBYTE = 4;
static final int SIGN = -128;
static final byte PAD = (byte) '=';
```

Key questions to answer precisely:

1. **encodeBase64(new byte[0], false)** — what does it return? Trace: lengthDataBits=0, fewerThan24bits=0, numberTriplets=0, encodedDataLength=0. Result: empty byte array `new byte[0]`. Output: `[]`

2. **encodeBase64(new byte[]{0}, false)** — 1 byte. lengthDataBits=8, fewerThan24bits=8 (EIGHTBIT), numberTriplets=0. encodedDataLength=4. b1=0, k=0, val1=0. encodedData[0]=lookUpBase64Alphabet[0]='A', encodedData[1]=lookUpBase64Alphabet[0]='A', encodedData[2]=PAD='=', encodedData[3]=PAD='='. Result: "AA=="

3. **encodeBase64(new byte[]{0,0}, false)** — 2 bytes. lengthDataBits=16, fewerThan24bits=16 (SIXTEENBIT), numberTriplets=0. encodedDataLength=4. b1=0,b2=0, l=0,k=0, val1=0,val2=0. encodedData[0]='A',encodedData[1]='A',encodedData[2]=lookUpBase64Alphabet[0]='A',encodedData[3]=PAD. Result: "AAA="

4. **encodeBase64(new byte[]{0,0,0}, false)** — 3 bytes. lengthDataBits=24, fewerThan24bits=0, numberTriplets=1. encodedDataLength=4. b1=0,b2=0,b3=0. val1=0,val2=0,val3=0. k=0,l=0. encodedData[0]='A',encodedData[1]='A',encodedData[2]='A',encodedData[3]='A'. Result: "AAAA"

5. **encodeBase64(new byte[]{(byte)0xFF}, false)** — 1 byte, value 255 = -1 as signed byte. b1=-1. SIGN=-128. (b1 & SIGN) = (-1 & -128) = -128 != 0, so val1 = (byte)((b1) >> 2 ^ 0xc0). b1>>2 in Java: -1 >> 2 = -1 (arithmetic shift). -1 ^ 0xc0 = -1 ^ 192. In binary: -1 is all 1s = 0xFF. 0xc0 = 11000000. XOR: 0xFF ^ 0xC0 = 0x3F = 63. So val1=63. k=(byte)(b1 & 0x03) = (-1 & 3) = 3. encodedData[0]=lookUpBase64Alphabet[63]='/', encodedData[1]=lookUpBase64Alphabet[3<<4]=lookUpBase64Alphabet[48]. lookUpBase64Alphabet[48] = '0'+48-52... wait: for i=52 to 61, lookUpBase64Alphabet[i] = '0'+(i-52). So [52]='0',[53]='1',...[61]='9'. But 48 < 52 and 48 > 25 and 48 > 51? Let me check: indices 26-51 map to 'a'+'j'-'z' etc. lookUpBase64Alphabet[48] = 'a' + (48-26) = 'a'+22 = 'w'. So encodedData[1]='w'. encodedData[2]=PAD, encodedData[3]=PAD. Result: "/w=="

6. **decodeBase64(new byte[0])** — base64Data after discardNonBase64 is empty (length 0). Returns new byte[0].

7. **decodeBase64("AAAA".getBytes())** — numberQuadruple=1. lastData=4 (no PAD). decodedData = new byte[4-1] = new byte[3]. b1=0,b2=0,b3=0,b4=0. decodedData[0]=0,decodedData[1]=0,decodedData[2]=0. encodedIndex becomes 3. Return 3-byte array of zeros.

8. **decodeBase64("AA==".getBytes())** — after discardNonBase64: keeps A,A,=,=. numberQuadruple=1. lastData: base64Data[3]='=' so lastData=3, base64Data[2]='=' so lastData=2. lastData==2 > 0. decodedData = new byte[2-1] = new byte[1]. Loop i=0: marker0='=', marker1='='. Since marker0==PAD: decodedData[0] = (byte)(b1<<2 | b2>>4) = (byte)(0<<2 | 0>>4) = 0. Return byte[]{0}.

9. **decodeBase64("AAA=".getBytes())** — numberQuadruple=1. lastData: base64Data[3]='=' so lastData=3. No more PAD at [2]. decodedData = new byte[3-1] = new byte[2]. Loop: marker0='A'='A', marker1='='. So marker1==PAD branch: b3=base64Alphabet['A']=0. decodedData[0]=(byte)(0<<2|0>>4)=0, decodedData[1]=(byte)(((0&0xf)<<4)|((0>>2)&0xf))=0. Return byte[]{0,0}.

10. **Chunking behavior**: encodeBase64Chunked with input of exactly 57 bytes (57*8=456 bits, 456/24=19 triplets, no remainder, encodedDataLength=76). isChunked=true. nbrChunks = ceil(76/76) = 1. encodedDataLength += 1*2 = 78. So output is 78 bytes: 76 encoded chars + CRLF.

11. **Chunking with 58 bytes**: 58*8=464 bits, 464/24=19 triplets (remainder=8 bits, EIGHTBIT), encodedDataLength=(19+1)*4=80. nbrChunks=ceil(80/76)=2. encodedDataLength=80+4=84. After the loop for 19 triplets: after i=18 (the 19th triplet, encodedIndex becomes 76), check if encodedIndex==nextSeparatorIndex (76==76), yes. Copy CHUNK_SEPARATOR, chunksSoFar=1, nextSeparatorIndex = 76*(1+1) + 1*2 = 152+2=154. encodedIndex=78. Then fewerThan24bits=8, handle last byte, encodedIndex=82. isChunked and chunksSoFar(1) < nbrChunks(2): copy CHUNK_SEPARATOR to encodedData[84-2]=encodedData[82]. Result: 76 chars + CRLF + 4 chars + CRLF = 84 bytes total. 

12. **isArrayByteBase64** behavior: discards whitespace first, then checks. Empty after whitespace strip returns true. Non-base64 chars like '!' return false.

13. **encode(Object) with non-byte[]** throws EncoderException.

14. **decode(Object) with non-byte[]** throws DecoderException.

15. **decodeBase64 with all-PAD input "===="**: discardNonBase64 keeps '=' (isBase64('=') returns true since octect==PAD). After discard: "====". numberQuadruple=1. lastData=4, base64Data[3]='=' so lastData=3, base64Data[2]='=' so lastData=2, base64Data[1]='=' so lastData=1, base64Data[0]='=' so lastData=0. Returns new byte[0].

16. **decodeBase64 with whitespace in input** like "AA  ==": discardNonBase64 keeps base64 chars. Space byte=32. isBase64(32)? base64Alphabet[32]=-1 and 32!='=', so false. So spaces are discarded. "AA==" becomes "AA==". Decodes to byte[]{0}.

17. **isArrayByteBase64 with whitespace**: whitespace is stripped first, then remaining chars checked. So " " (just spaces) after strip is empty → returns true. "A B" → strip → "AB" → isBase64('A')=true, isBase64('B')=true → true.

18. **encodeBase64 with exactly 76 bytes input**: 76*8=608 bits, 608/24=25 triplets remainder 8 bits. encodedDataLength=(25+1)*4=104. Not chunked.

19. **What about chunked with 57 bytes?** Already analyzed above: 78 bytes output (76 data + CRLF).

20. **decodeBase64 ignores non-base64**: "Hello, World!" encoded would decode after stripping non-base64 chars.

21. **Large input round-trip**: encodeBase64(data, false) then decodeBase64 should round-trip for any byte array length.

22. **Known vector**: "Man" → bytes {77, 97, 110}. b1=77, b2=97, b3=110. 
- val1: (77 & -128)=0, so val1=(byte)(77>>2)=(byte)19. lookUpBase64Alphabet[19]='T'
- k=(byte)(77 & 3)=1. l=(byte)(97 & 0x0f)=1.
- val2: (97 & -128)=0, so val2=(byte)(97>>4)=(byte)6. val2|(k<<4) = 6|16=22. lookUpBase64Alphabet[22]='W'
- val3: (110 & -128)=0, so val3=(byte)(110>>6)=(byte)1. (l<<2)|val3 = 4|1=5. lookUpBase64Alphabet[5]='F'  wait: lookUpBase64Alphabet[4]='E', [5]='F'... actually 'A'+5='F'. Yes.
- b3&0x3f = 110 & 63 = 46. lookUpBase64Alphabet[46]= 'a'+(46-26)='a'+20='u'. Wait: 'a'=97, 97+20=117='u'.
- Result: "TWFu" ✓ (standard base64 for "Man")

23. **The BASELENGTH constant is 255**, but the alphabet array is `new byte[255]`. So valid indices are 0-254. If a byte value is 255 (unsigned) = -1 signed, accessing base64Alphabet[-1 & 0xFF] would be out of bounds... Actually in isBase64(byte octect), it accesses base64Alphabet[octect] where octect is a byte (signed). So octect=-1 → array index -1 → ArrayIndexOutOfBoundsException! But wait, only chars with byte values 0-254 can be valid. Let me check: the alphabet is size 255, indices 0-254. If a byte value is -1 (0xFF), base64Alphabet[-1] would throw AIOOBE. But actually this can't happen through public API since discardNonBase64 is called first... Actually isBase64 could be called with octect=-1 in isArrayByteBase64 path if the input bytes contain 0xFF. Let me re-check discardNonBase64: it calls isBase64 on each byte. If byte is 0xFF (-1 signed), isBase64(0xFF): octect==PAD? '='=61. No. base64Alphabet[octect] where octect=-1: array index -1! That would throw ArrayIndexOutOfBoundsException! So passing a byte[] containing 0xFF to isArrayByteBase64 or decodeBase64 would throw AIOOBE.

Actually wait - let me re-examine. The base64Alphabet array has size BASELENGTH=255. Indices 0-254. If byte value is 255 (=-1 signed in Java), then base64Alphabet[-1] → AIOOBE. So:
- isArrayByteBase64(new byte[]{(byte)0xFF}) → AIOOBE
- encodeBase64 with bytes containing 0xFF → does NOT call isBase64, directly accesses lookUpBase64Alphabet with computed values. Should be fine.
- decodeBase64 with a byte 0xFF that is NOT stripped... actually discardNonBase64 calls isBase64 on every byte, so if 0xFF is in the input, it throws AIOOBE before decoding starts.

Wait, actually let me reconsider. The byte -1 as an int is 0xFFFFFFFF. base64Alphabet is byte[255] indexed from 0 to 254. In Java, array access with negative index throws ArrayIndexOutOfBoundsException. So yes, passing 0xFF bytes through paths that call isBase64 will throw.

But actually, in practice, the only way to get 0xFF in input is from binary data to the decoder. A legitimate base64 string won't have byte 255. The encoder handles 0xFF bytes fine since it uses bit manipulation directly, not the lookup.

Let me reconsider: what about byte value 127 (0x7F)? That's a valid index (0-254), base64Alphabet[127] was initialized to -1 (not set for any base64 char), so isBase64(127)=false. Fine.

Please provide:
1. Exact byte-by-byte output of encodeBase64(new byte[]{77,97,110}, false) → should be "TWFu" in ASCII
2. Exact output of encodeBase64(new byte[]{(byte)255,(byte)255,(byte)255}, false)
3. What happens with encodeBase64(null, false)? → NullPointerException at `binaryData.length`
4. The chunked output length formula verification for various input sizes
5. What isArrayByteBase64(new byte[]{(byte)0xFF}) does - confirm AIOOBE



Subagent completed | 164.8s | 4 tool call(s) | 0.267


────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
Assistant (1) 2026-09-26 19:21:22.520 UTC (+2.4s)

I'll analyze the code carefully to understand its actual behavior, then write characterization tests that pin down      
exactly what it does.                                                                                                   
                                                                                                                        
Let me trace through the key behaviors:                                                                                 

Tool calls:
- spawn_subagent

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
Tool (1) 2026-09-26 19:24:55.767 UTC (+215.6s) Success

Tool: spawn_subagent
Args:
- name: general
- description: 
Analyze this Base64 implementation and produce precise expected values for characterization tests.

Here is the full...


────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
Assistant (2) 2026-09-26 19:24:57.169 UTC (+217.0s)

Now I have all the precise values needed. Let me also trace a few more subtle behaviors before writing the tests:       
                                                                                                                        
- decodeBase64("====") decoded size: numberQuadruple=1, lastData loop: 4→3→2→1→0 → returns new byte[0] ✓                
- encodeBase64(new byte[]{(byte)0xFF, (byte)0xFF}, false): fewerThan24bits=16 (SIXTEENBIT). b1=-1, b2=-1. k=3, l=15.    
  val1=63→'/'. val2=((−1>>4)^0xF0)|(3<<4) = (0xF^0xF0)|0x30 — wait: (byte)((b2)>>4^0xF0): b2=-1, -1>>4=-1 (0xFFFF...FF),
  ^0xF0 = ...0F, as byte = 0x0F=15. 15|(3<<4)=15|48=63→'/'. l<<2 = 15<<2=60→lookUpBase64Alphabet[60]='8'. PAD at end.   
  Result: "//8="`                                                                                                       
- Chunked structure: after 76 encoded characters, CRLF is inserted; the final chunk also gets a trailing CRLF.          
- decode(Object) with String: DecoderException thrown since String is not byte[].                                       
- isArrayByteBase64 with null: NullPointerException since it calls discardWhitespace(arrayOctect) with null →           
  data.length NPE.                                                                                                      
                                                                                                                        
```java                                                                                                                 
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
```                                                                                                                     
                                                                                                                        
────────────────────────────────────────────────────────────────────────────────                                        
                                                                                                                        
Key characterization findings pinned by these tests:                                                                    
                                                                                                                        
┌──────────────────────────────────┬───────────────────────────────────────────────────────────────────────────────────┐
│ Behaviour                        │ What the code actually does                                                       │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ isArrayByteBase64(new            │ Throws ]8;;.\ArrayIndexOutOfBoundsException]8;;\ — array is only 255 elements (indices       │
│ byte[]{(byte)0xFF})              │ 0–254) but byte −1 is used as index 255                                           │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ decode(Object) null              │ Throws ]8;;.\DecoderException]8;;\ (not NPE) — the instanceof check catches null             │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ Empty input                      │ Encodes and decodes to empty byte array — not an error                            │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ isArrayByteBase64(new byte[0])   │ Returns true — the comment in the source says "shouldn't a 0 length array be      │
│                                  │ valid?" and leaves the return false commented out                                 │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ PAD '='                          │ ]8;;.\isBase64]8;;\ returns true for '=', so isArrayByteBase64(bytes("====")) is true and    │
│                                  │ decodes to empty                                                                  │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ Non-base64 chars in decode input │ Silently discarded by ]8;;.\discardNonBase64]8;;\ — RFC 2045 compliant                       │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ Chunked trailing CRLF            │ Every chunk, including the last one, always gets a CRLF appended                  │
├──────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
│ Whitespace in isArrayByteBase64  │ Stripped before alphabet check, so " " → true, "TW Fu" → true                     │
└──────────────────────────────────┴───────────────────────────────────────────────────────────────────────────────────┘
────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task Summary

Total Cost:              0.381
Total Duration:          4m 46s


Assistant Messages:      2
Tool Calls:              1
Task ID:                 27e4eeebde064d10e306a18636373f42

````
