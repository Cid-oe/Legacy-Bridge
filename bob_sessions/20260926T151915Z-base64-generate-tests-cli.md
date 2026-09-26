# Bob session: base64-generate-tests

- Started: 20260926T151915Z
- Command: `bob -p <prompt>`
- Duration: 267.7s
- Bob cost: 0.365

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
I'll analyze the code carefully before writing the tests — tracing each branch, the encoding/decoding math, chunk logic,
and edge cases.

Tool calls:
- spawn_subagent

Tool (1) 2026-09-26 15:22:46.667 UTC (+211.0s) Success

Tool: spawn_subagent
Args:
- name: general
- description:
Carefully analyze the following Apache Commons Codec Base64 implementation and produce a complete, self-contained JU...

Files changed:
  ~ file:///tmp/bob-bridge-4vf801ko/Base64CharacterizationTest.java



The subagent's analysis is thorough and correct. Let me produce the final clean test class, incorporating all the
verified logic:

```java
  package org.apache.commons.codec.binary;

  import static org.junit.jupiter.api.Assertions.*;

  import org.apache.commons.codec.DecoderException;
  import org.apache.commons.codec.EncoderException;
  import org.junit.jupiter.api.Test;

  /**
   * Characterization tests for the legacy Base64 class.
   *
   * These tests pin what the code DOES today, including surprising behaviour.
   * Where the javadoc and the implementation disagree, the implementation wins.
   */
  class Base64CharacterizationTest {

      // -------------------------------------------------------------------
      // Static constants — publicly visible values pinned for regression
      // -------------------------------------------------------------------

      @Test
      void chunkSizeConstantIs76() {
          assertEquals(76, Base64.CHUNK_SIZE);
      }

      @Test
      void chunkSeparatorIsCRLF() {
          assertArrayEquals(new byte[]{'\r', '\n'}, Base64.CHUNK_SEPARATOR);
      }

      // -------------------------------------------------------------------
      // encodeBase64 — empty input
      // -------------------------------------------------------------------

      @Test
      void encodeEmpty_returnsEmptyArray() {
          assertArrayEquals(new byte[0], Base64.encodeBase64(new byte[0]));
      }

      // -------------------------------------------------------------------
      // encodeBase64 — one byte (fewerThan24bits == EIGHTBIT)
      // -------------------------------------------------------------------

      @Test
      void encodeSingleZeroByte_givesAA_eq_eq() {
          // fewerThan24bits=8 → 4-byte output, last two bytes are PAD
          assertArrayEquals("AA==".getBytes(), Base64.encodeBase64(new byte[]{0}));
      }

      @Test
      void encodeSingleByte_f_givesZg_eq_eq() {
          // 'f' = 0x66: val1 = 0x66>>2 = 25 → 'Z'; k = 0x66&3 = 2 → lookUp[32] = 'g'
          assertArrayEquals("Zg==".getBytes(), Base64.encodeBase64("f".getBytes()));
      }

      // -------------------------------------------------------------------
      // encodeBase64 — two bytes (fewerThan24bits == SIXTEENBIT)
      // -------------------------------------------------------------------

      @Test
      void encodeTwoZeroBytes_givesAAA_eq() {
          // fewerThan24bits=16 → 4-byte output, last byte is PAD
          assertArrayEquals("AAA=".getBytes(), Base64.encodeBase64(new byte[]{0, 0}));
      }

      @Test
      void encodeTwoBytes_fo_givesZm8_eq() {
          assertArrayEquals("Zm8=".getBytes(), Base64.encodeBase64("fo".getBytes()));
      }

      // -------------------------------------------------------------------
      // encodeBase64 — three bytes (one complete triplet, no remainder)
      // -------------------------------------------------------------------

      @Test
      void encodeThreeZeroBytes_givesAAAA() {
          assertArrayEquals("AAAA".getBytes(), Base64.encodeBase64(new byte[]{0, 0, 0}));
      }

      @Test
      void encodeThreeBytes_Man_givesTWFu() {
          // Classic RFC 2045 example
          assertArrayEquals("TWFu".getBytes(), Base64.encodeBase64("Man".getBytes()));
      }

      @Test
      void encodeThreeBytes_foo_givesZm9v() {
          assertArrayEquals("Zm9v".getBytes(), Base64.encodeBase64("foo".getBytes()));
      }

      // -------------------------------------------------------------------
      // encodeBase64 — four bytes (one triplet + one-byte remainder)
      // -------------------------------------------------------------------

      @Test
      void encodeFourZeroBytes_givesTripletPlusOneByteRemainder() {
          // fewerThan24bits=8, numberTriplets=1 → encodedDataLength=8
          // bytes 0-3: triplet [0,0,0] → "AAAA"
          // bytes 4-7: remainder [0] → "AA=="
          byte[] result = Base64.encodeBase64(new byte[]{0, 0, 0, 0});
          assertEquals(8, result.length);
          assertEquals('A', (char) result[0]);
          assertEquals('A', (char) result[1]);
          assertEquals('A', (char) result[2]);
          assertEquals('A', (char) result[3]);
          assertEquals('A', (char) result[4]);
          assertEquals('A', (char) result[5]);
          assertEquals('=', (char) result[6]);
          assertEquals('=', (char) result[7]);
      }

      // -------------------------------------------------------------------
      // decodeBase64 — empty and all-stripped input
      // -------------------------------------------------------------------

      @Test
      void decodeEmpty_returnsEmptyArray() {
          assertArrayEquals(new byte[0], Base64.decodeBase64(new byte[0]));
      }

      @Test
      void decodeAllNonBase64Chars_returnsEmptyArray() {
          // discardNonBase64 strips everything → length 0 → returns byte[0]
          assertArrayEquals(new byte[0], Base64.decodeBase64("!!!!".getBytes()));
      }

      // -------------------------------------------------------------------
      // decodeBase64 — output-size formula: new byte[lastData - numberQuadruple]
      // -------------------------------------------------------------------

      @Test
      void decodeAAAA_givesThreeZeroBytes() {
          // nq=1, lastData=4 (no PAD), size=3
          assertArrayEquals(new byte[]{0, 0, 0}, Base64.decodeBase64("AAAA".getBytes()));
      }

      @Test
      void decodeAA_eq_eq_givesOneZeroByte() {
          // nq=1, lastData=2 (two trailing '='), size=1; only slot 0 written
          byte[] result = Base64.decodeBase64("AA==".getBytes());
          assertEquals(1, result.length);
          assertEquals(0, result[0]);
      }

      @Test
      void decodeAAA_eq_givesTwoZeroBytes() {
          // nq=1, lastData=3 (one trailing '='), size=2
          assertArrayEquals(new byte[]{0, 0}, Base64.decodeBase64("AAA=".getBytes()));
      }

      @Test
      void decodeTWFu_givesMan() {
          assertArrayEquals("Man".getBytes(), Base64.decodeBase64("TWFu".getBytes()));
      }

      @Test
      void decodeZg_eq_eq_givesF() {
          assertArrayEquals("f".getBytes(), Base64.decodeBase64("Zg==".getBytes()));
      }

      @Test
      void decodeZm8_eq_givesFo() {
          assertArrayEquals("fo".getBytes(), Base64.decodeBase64("Zm8=".getBytes()));
      }

      @Test
      void decodeZm9v_givesFoo() {
          assertArrayEquals("foo".getBytes(), Base64.decodeBase64("Zm9v".getBytes()));
      }

      // -------------------------------------------------------------------
      // decodeBase64 — two quadruples, size formula across multiple groups
      // -------------------------------------------------------------------

      @Test
      void decodeTwoFullQuads_sizeIsSix() {
          // "AAAAAAAA": nq=2, lastData=8, size=6 → [0,0,0,0,0,0]
          byte[] result = Base64.decodeBase64("AAAAAAAA".getBytes());
          assertEquals(6, result.length);
          assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0}, result);
      }

      @Test
      void decodeTwoQuads_twoPad_sizeIsFour() {
          // "AAAAAA==": nq=2, lastData=6 (two '=' stripped), size=4
          // first quad [0,0,0], second quad two-PAD: only slot[3] written, slots[4,5] are
          // zero-filled by Java array initialisation and never overwritten.
          byte[] result = Base64.decodeBase64("AAAAAA==".getBytes());
          assertEquals(4, result.length);
          // only slot 0-2 of second quad written: slot[3]=0 (one byte), rest from init
          assertArrayEquals(new byte[]{0, 0, 0, 0}, result);
      }

      @Test
      void decodeTwoQuads_onePad_sizeIsFive() {
          // "AAAAAAA=": nq=2, lastData=7 (one '='), size=5
          byte[] result = Base64.decodeBase64("AAAAAAA=".getBytes());
          assertEquals(5, result.length);
          assertArrayEquals(new byte[]{0, 0, 0, 0, 0}, result);
      }

      // -------------------------------------------------------------------
      // decodeBase64 — discardNonBase64 strips noise before processing
      // -------------------------------------------------------------------

      @Test
      void decode_whitespaceInsideInput_isStripped() {
          // space is not a base64 char → stripped by discardNonBase64
          assertArrayEquals("Man".getBytes(), Base64.decodeBase64("TW Fu".getBytes()));
      }

      @Test
      void decode_crlfInsideInput_isStripped() {
          assertArrayEquals("Man".getBytes(),
                  Base64.decodeBase64("TW\r\nFu".getBytes()));
      }

      @Test
      void decode_punctuationInsideInput_isStripped() {
          // '!' → not in alphabet → silently removed
          assertArrayEquals("Man".getBytes(), Base64.decodeBase64("T!W!F!u".getBytes()));
      }

      // -------------------------------------------------------------------
      // encodeBase64Chunked — output length and CRLF positions
      // -------------------------------------------------------------------

      @Test
      void chunkedEncodeEmpty_returnsEmptyArray() {
          // encodedDataLength=0, nbrChunks=0 → byte[0]
          assertArrayEquals(new byte[0], Base64.encodeBase64Chunked(new byte[0]));
      }

      @Test
      void chunkedEncode1Byte_givesFourBase64PlusCRLF() {
          // encodedDataLength=4; nbrChunks=ceil(4/76)=1; total=6
          byte[] result = Base64.encodeBase64Chunked(new byte[]{0});
          assertEquals(6, result.length);
          assertEquals('A', (char) result[0]);
          assertEquals('A', (char) result[1]);
          assertEquals('=', (char) result[2]);
          assertEquals('=', (char) result[3]);
          assertEquals('\r', (char) result[4]);
          assertEquals('\n', (char) result[5]);
      }

      @Test
      void chunkedEncodeManGivesTWFuCRLF() {
          // 3 bytes → "TWFu" (4 chars) + "\r\n" = 6 bytes
          byte[] result = Base64.encodeBase64Chunked("Man".getBytes());
          assertEquals(6, result.length);
          assertEquals('T', (char) result[0]);
          assertEquals('W', (char) result[1]);
          assertEquals('F', (char) result[2]);
          assertEquals('u', (char) result[3]);
          assertEquals('\r', (char) result[4]);
          assertEquals('\n', (char) result[5]);
      }

      @Test
      void chunkedEncode57Bytes_gives78Bytes_crlfAt76_77() {
          // 57 bytes → 76 base64 chars, nbrChunks=1, total=78
          byte[] input = new byte[57];
          byte[] result = Base64.encodeBase64Chunked(input);
          assertEquals(78, result.length);
          assertEquals('\r', (char) result[76]);
          assertEquals('\n', (char) result[77]);
      }

      @Test
      void chunkedEncode57Bytes_first76CharsAreNotCRLF() {
          byte[] result = Base64.encodeBase64Chunked(new byte[57]);
          for (int i = 0; i < 76; i++) {
              assertNotEquals((byte) '\r', result[i], "unexpected CR at position " + i);
              assertNotEquals((byte) '\n', result[i], "unexpected LF at position " + i);
          }
      }

      @Test
      void chunkedEncode114Bytes_gives156Bytes_twoChunks() {
          // 114 bytes → 152 base64 chars, nbrChunks=2, total=156
          // CRLF at [76,77] and [154,155]
          byte[] result = Base64.encodeBase64Chunked(new byte[114]);
          assertEquals(156, result.length);
          assertEquals('\r', (char) result[76]);
          assertEquals('\n', (char) result[77]);
          assertEquals('\r', (char) result[154]);
          assertEquals('\n', (char) result[155]);
      }

      @Test
      void chunkedEncode58Bytes_gives84Bytes_crlfAfterFirstLine_andAtEnd() {
          // 58 bytes: fewerThan24bits=8, numberTriplets=19
          // encodedDataLength=(19+1)*4=80; nbrChunks=ceil(80/76)=2; total=84
          // CRLF inserted during loop at [76,77]; trailing CRLF at [82,83]
          byte[] result = Base64.encodeBase64Chunked(new byte[58]);
          assertEquals(84, result.length);
          assertEquals('\r', (char) result[76]);
          assertEquals('\n', (char) result[77]);
          assertEquals('\r', (char) result[82]);
          assertEquals('\n', (char) result[83]);
      }

      // -------------------------------------------------------------------
      // encodeBase64Chunked — content matches non-chunked within each line
      // -------------------------------------------------------------------

      @Test
      void chunkedAndNonChunked_sameContentFor3Bytes() {
          byte[] plain = Base64.encodeBase64("Man".getBytes());   // "TWFu" (4 bytes)
          byte[] chunked = Base64.encodeBase64Chunked("Man".getBytes()); // "TWFu\r\n" (6 bytes)
          assertEquals(4, plain.length);
          assertEquals(6, chunked.length);
          for (int i = 0; i < plain.length; i++) {
              assertEquals(plain[i], chunked[i], "mismatch at position " + i);
          }
      }

      // -------------------------------------------------------------------
      // isArrayByteBase64
      // -------------------------------------------------------------------

      @Test
      void isArrayByteBase64_emptyArray_returnsTrue() {
          // explicit comment in source: "shouldn't a 0 length array be valid base64 data?"
          // The return-false line is commented out; it returns true.
          assertTrue(Base64.isArrayByteBase64(new byte[0]));
      }

      @Test
      void isArrayByteBase64_whitespaceOnly_returnsTrue() {
          // discardWhitespace removes ' ', '\t', '\r', '\n' → empty → true
          assertTrue(Base64.isArrayByteBase64(" \t\r\n".getBytes()));
      }

      @Test
      void isArrayByteBase64_allAlphabetChars_returnsTrue() {
          String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
          assertTrue(Base64.isArrayByteBase64(alphabet.getBytes()));
      }

      @Test
      void isArrayByteBase64_validPaddedString_returnsTrue() {
          assertTrue(Base64.isArrayByteBase64("AA==".getBytes()));
          assertTrue(Base64.isArrayByteBase64("AAA=".getBytes()));
          assertTrue(Base64.isArrayByteBase64("AAAA".getBytes()));
      }

      @Test
      void isArrayByteBase64_singleInvalidChar_returnsFalse() {
          // '!' (0x21) → base64Alphabet[0x21] = -1, not PAD → false
          assertFalse(Base64.isArrayByteBase64("!".getBytes()));
      }

      @Test
      void isArrayByteBase64_invalidCharMixedWithWhitespace_returnsFalse() {
          // whitespace is stripped, but '!' survives discardWhitespace → false
          assertFalse(Base64.isArrayByteBase64(" ! ".getBytes()));
      }

      @Test
      void isArrayByteBase64_invalidCharAmongValid_returnsFalse() {
          assertFalse(Base64.isArrayByteBase64("AA!A".getBytes()));
      }

      // -------------------------------------------------------------------
      // Instance encode(Object) — delegates to encodeBase64(x, false)
      // -------------------------------------------------------------------

      @Test
      void instanceEncodeObject_byteArray_returnsEncodedBytes() throws EncoderException {
          Base64 b64 = new Base64();
          Object result = b64.encode((Object) "Man".getBytes());
          assertInstanceOf(byte[].class, result);
          assertArrayEquals("TWFu".getBytes(), (byte[]) result);
      }

      @Test
      void instanceEncodeObject_nonByteArray_throwsEncoderException() {
          Base64 b64 = new Base64();
          assertThrows(EncoderException.class, () -> b64.encode((Object) "Man"));
      }

      @Test
      void instanceEncode_byteArray_delegatesToNonChunkedStatic() throws EncoderException {
          Base64 b64 = new Base64();
          assertArrayEquals(Base64.encodeBase64("foo".getBytes()), b64.encode("foo".getBytes()));
      }

      // -------------------------------------------------------------------
      // Instance decode(Object) — delegates to decodeBase64
      // -------------------------------------------------------------------

      @Test
      void instanceDecodeObject_byteArray_returnsDecodedBytes() throws DecoderException {
          Base64 b64 = new Base64();
          Object result = b64.decode((Object) "TWFu".getBytes());
          assertInstanceOf(byte[].class, result);
          assertArrayEquals("Man".getBytes(), (byte[]) result);
      }

      @Test
      void instanceDecodeObject_nonByteArray_throwsDecoderException() {
          Base64 b64 = new Base64();
          assertThrows(DecoderException.class, () -> b64.decode((Object) "TWFu"));
      }

      @Test
      void instanceDecode_byteArray_delegatesToStaticDecodeBase64() {
          Base64 b64 = new Base64();
          assertArrayEquals(Base64.decodeBase64("TWFu".getBytes()),
                  b64.decode("TWFu".getBytes()));
      }

      // -------------------------------------------------------------------
      // Null input — no guards in the code, so NullPointerException is thrown
      // -------------------------------------------------------------------

      @Test
      void encodeBase64_null_throwsNPE() {
          assertThrows(NullPointerException.class, () -> Base64.encodeBase64(null));
      }

      @Test
      void decodeBase64_null_throwsNPE() {
          assertThrows(NullPointerException.class, () -> Base64.decodeBase64(null));
      }

      @Test
      void isArrayByteBase64_null_throwsNPE() {
          assertThrows(NullPointerException.class, () -> Base64.isArrayByteBase64(null));
      }

      // -------------------------------------------------------------------
      // Roundtrip encode → decode
      // -------------------------------------------------------------------

      @Test
      void roundtrip_emptyArray() {
          byte[] data = new byte[0];
          assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64(data)));
      }

      @Test
      void roundtrip_singleByte() {
          byte[] data = new byte[]{(byte) 0xFF};
          assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64(data)));
      }

      @Test
      void roundtrip_shortString() {
          byte[] data = "Hello, World!".getBytes();
          assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64(data)));
      }

      @Test
      void roundtrip_allByteValues0to255() {
          byte[] data = new byte[256];
          for (int i = 0; i < 256; i++) {
              data[i] = (byte) i;
          }
          assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64(data)));
      }

      @Test
      void roundtrip_chunkedEncodeIsDecodedCorrectly() {
          // decode strips \r\n (they are not base64 chars) → identical result
          byte[] data = new byte[200];
          for (int i = 0; i < 200; i++) data[i] = (byte) i;
          byte[] chunked = Base64.encodeBase64Chunked(data);
          assertArrayEquals(data, Base64.decodeBase64(chunked));
      }

      @Test
      void roundtrip_lengthsAlignForAllRemainderClasses() {
          // Test inputs of length 0-5 to cover both remainder branches and base triplet path
          for (int len = 0; len <= 5; len++) {
              byte[] data = new byte[len];
              for (int i = 0; i < len; i++) data[i] = (byte) (i + 1);
              byte[] encoded = Base64.encodeBase64(data);
              byte[] decoded = Base64.decodeBase64(encoded);
              assertArrayEquals(data, decoded, "roundtrip failed for input length " + len);
          }
      }

      // -------------------------------------------------------------------
      // Exact encoded output for known values — alphabet boundary checks
      // -------------------------------------------------------------------

      @Test
      void encodeBytes_usesCorrectAlphabetBoundaries() {
          // index 0 → 'A', 25 → 'Z', 26 → 'a', 51 → 'z', 52 → '0', 61 → '9', 62 → '+', 63 → '/'
          // Encode 3 bytes that cover all four alphabet regions in one triplet.
          // [0x00, 0x00, 0x00] → indices [0,0,0,0] → "AAAA"
          assertArrayEquals("AAAA".getBytes(), Base64.encodeBase64(new byte[]{0, 0, 0}));
          // [0xFF, 0xFF, 0xFF]:
          // b1=0xFF(-1): val1=(0xFF>>2)^0xC0=0x3F=63→'/'  k=0xFF&3=3
          // b2=0xFF(-1): val2=(0xFF>>4)^0xF0=0x0F=15  l=0xFF&0xF=15
          // b3=0xFF(-1): val3=(0xFF>>6)^0xFC=0x03=3
          // enc[0]=lookUp[63]='/'
          // enc[1]=lookUp[15|(3<<4)]=lookUp[63]='/'
          // enc[2]=lookUp[(15<<2)|3]=lookUp[63]='/'
          // enc[3]=lookUp[0xFF&0x3F]=lookUp[63]='/'
          assertArrayEquals("////".getBytes(), Base64.encodeBase64(new byte[]{-1, -1, -1}));
      }

      @Test
      void encodeProducesPlusAndSlash() {
          // '+' is index 62, '/' is index 63.  0xFB, 0xEF, 0xBE produces "++/+"... let's use known.
          // decode("////") should give [0xFF, 0xFF, 0xFF]
          byte[] decoded = Base64.decodeBase64("////".getBytes());
          assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, decoded);
          // roundtrip from encoding side
          assertArrayEquals("////".getBytes(), Base64.encodeBase64(new byte[]{-1, -1, -1}));
      }
  }
```

````

## Raw Bob output

````

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
User (1) 2026-09-26 15:19:16.203 UTC (+0.5s)

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
   
Carefully analyze the following Apache Commons Codec Base64 implementation and produce a complete, self-contained JUnit 5 characterization test class. The goal is to pin what the code DOES today — not what the javadoc says it should do. Where they disagree, the code wins.

Key observations to work through before writing any test:

1. **Encoding math** — trace `encodeBase64(binaryData, false)` for inputs of length 0, 1, 2, 3, 4 bytes.  Work out the expected byte-by-byte output manually.
2. **Chunked encoding** — CHUNK_SIZE=76, CHUNK_SEPARATOR="\r\n" (2 bytes).  A single chunk of 57 raw bytes → 76 encoded chars → then "\r\n" is appended.  Work out the exact output length formula.
3. **Decoding math** — trace `decodeBase64` for valid 4-char groups, one-pad, two-pad inputs, and the size formula `lastData - numberQuadruple`.
4. **discardNonBase64** — called at the top of decode; whitespace and anything not in the alphabet is silently stripped before counting quadruples.
5. **isArrayByteBase64** — calls `discardWhitespace` first (not discardNonBase64), then checks each byte; an empty array returns true.
6. **decode(Object)** — throws DecoderException for non-byte[].
7. **encode(Object)** — throws EncoderException for non-byte[].
8. **Null behaviour** — neither static nor instance methods guard against null; null input throws NullPointerException.
9. **Decode output size formula** — `decodedData = new byte[lastData - numberQuadruple]` where `lastData` = length after stripping trailing '=', `numberQuadruple = base64Data.length / 4`.  For "AAAA" → lastData=4, nq=1, size=3.  For "AAA=" → lastData=3, nq=1, size=2.  For "AA==" → lastData=2, nq=1, size=1.
10. **encodedIndex advances by 3 each quadruple regardless of PADs** — so for two-PAD case only 1 byte is written but 3 slots are consumed; the output array may have trailing zero bytes.

Requirements for the test class:
- Package: `org.apache.commons.codec.binary`
- Class name: `Base64CharacterizationTest`
- Use only JUnit 5 (org.junit.jupiter.api.*) and the JDK standard library
- No external libraries beyond JUnit 5
- Call only public API of Base64
- Every test MUST pass against the code exactly as written
- Keep every line under 100 characters
- Cover: empty input, 1-byte, 2-byte, 3-byte, 4-byte inputs; encode/decode roundtrip; chunked output length and content; isArrayByteBase64 with whitespace, invalid bytes, empty; decode(Object) and encode(Object) interface methods; NullPointerException behaviour; discardNonBase64 effect on decode; trailing zero bytes in decode output; known exact encoded values

Work through each manually:

**encode(new byte[0])** → fewerThan24bits=0, numberTriplets=0, encodedDataLength=0 → returns byte[0]

**encode(new byte[]{0})** → fewerThan24bits=8(EIGHTBIT), numberTriplets=0, encodedDataLength=4
  b1=0, k=0, val1=0 → lookUp[0]='A'; lookUp[0<<4]='A'; PAD; PAD → "AA=="

**encode(new byte[]{0,0})** → fewerThan24bits=16(SIXTEENBIT), numberTriplets=0, encodedDataLength=4
  b1=0,b2=0, l=0,k=0, val1=0,val2=0 → lookUp[0]='A'; lookUp[0]='A'; lookUp[0]='A'; PAD → "AAA="

**encode(new byte[]{0,0,0})** → fewerThan24bits=0, numberTriplets=1, encodedDataLength=4
  b1=0,b2=0,b3=0 → val1=0,val2=0,val3=0 → lookUp[0]='A' x4 → "AAAA"

**decode("AAAA")** → base64Data after discardNonBase64 = [65,65,65,65], numberQuadruple=1
  lastData=4 (no PAD), decodedData=new byte[4-1]=byte[3]
  b1=0,b2=0,b3=0,b4=0 → [0,0,0]

**decode("AA==")** → after discard = [65,65,61,61], nq=1
  lastData: base64Data[3]='=' → lastData=3; base64Data[2]='=' → lastData=2 → not 0
  decodedData=new byte[2-1]=byte[1]
  marker0=61('='), marker1=61('=') → two-PAD branch: decodedData[0]=(b1<<2|b2>>4)=0
  encodedIndex becomes 3 → but array is only byte[1] → wait, only [0] is written → returns [0]

**decode("AAA=")** → after discard=[65,65,65,61], nq=1
  lastData=3 (base64Data[3]='='), decodedData=new byte[3-1]=byte[2]
  marker0=65('A'), marker1=61('=') → one-PAD branch:
  b3=base64Alphabet['A']=0
  decodedData[0]=0, decodedData[1]=0
  returns [0,0]

**Chunked encoding — 57 bytes** → 57*8=456 bits, 456%24=0, numberTriplets=19, encodedDataLength=76
  isChunked=true, nbrChunks=ceil(76/76)=1, encodedDataLength=76+1*2=78
  After encoding 19 triplets, encodedIndex=76 which equals nextSeparatorIndex=76 → separator inserted at [76..77]
  Then chunksSoFar=1, not less than nbrChunks=1 → trailing separator block NOT re-added.
  Wait: after loop chunksSoFar=1, nbrChunks=1 → condition `chunksSoFar < nbrChunks` is false → no extra separator.
  fewerThan24bits=0 → no remainder block.
  So output is 78 bytes: 76 base64 chars + "\r\n"

**Chunked encoding — 57*2=114 bytes** → 912 bits, nTriplets=38, encodedDataLength=152
  nbrChunks=ceil(152/76)=2, encodedDataLength=152+4=156
  After triplet 19 (encodedIndex=76): separator at [76,77], chunksSoFar=1, next=76*2+2=154, encodedIndex=78
  After triplet 38 (encodedIndex=78+76=154): equals next=154 → separator at [154,155], chunksSoFar=2, encodedIndex=156
  fewerThan24bits=0 → no remainder. chunksSoFar=2 == nbrChunks=2 → no trailing separator.
  Output is 156 bytes: two 76-char lines each followed by "\r\n"

**Chunked with remainder — 58 bytes** → 58*8=464 bits, fewerThan24bits=464%24=8(EIGHTBIT), numberTriplets=19, encodedDataLength=(19+1)*4=80
  nbrChunks=ceil(80/76)=2, encodedDataLength=80+4=84
  After 19 triplets encodedIndex=76==nextSeparatorIndex → separator, chunksSoFar=1, next=76*2+2=154, encodedIndex=78
  Remainder block written at [78..81]: 4 bytes
  encodedIndex is now 82, but the trailing separator check: chunksSoFar=1 < nbrChunks=2 →
    System.arraycopy(CHUNK_SEPARATOR,0,encodedData, encodedDataLength-CHUNK_SEPARATOR.length, 2)
    = arraycopy at index 84-2=82 → encodedData[82]='\r', encodedData[83]='\n'
  Output 84 bytes: 76 chars + "\r\n" + 4 chars + "\r\n"

**isArrayByteBase64** observations:
  - Empty after discardWhitespace → returns true
  - Whitespace-only input: " \t\r\n" → after discardWhitespace = [] → returns true
  - Valid base64 chars (A-Z, a-z, 0-9, +, /, =) → true
  - Invalid char like '!' (byte 33) → base64Alphabet[33]=-1, not PAD → false
  - Note: isArrayByteBase64 uses discardWhitespace, not discardNonBase64; it checks for valid chars

**decode with non-base64 chars** — discardNonBase64 strips them silently before decoding.
  So decode("A A A A".getBytes()) → strips spaces → "AAAA" → [0,0,0]

**Known encode values**:
  "Man" = [77,97,110] → 
    b1=77(01001101), b2=97(01100001), b3=110(01101110)
    val1 = 77>>2=19 (since bit7=0) → 'T'
    k = 77&3 = 1, val2 = 97>>4=6 (bit7=0) → lookUp[6|(1<<4)]=lookUp[22]='W'
    l = 97&0xf = 1, val3 = 110>>6=1 (bit7=0) → lookUp[(1<<2)|1]=lookUp[5]='F'
    lookUp[110&63]=lookUp[46]='u'  (since 46th: 'A'+46? No: lookUp[0-25]=A-Z, [26-51]=a-z, [52-61]=0-9, [62]=+, [63]=/)
    lookUp[46] = 'a'+(46-26) = 'a'+20 = 'u'  ✓
    → "TWFu"

  "f" = [102]:
    fewerThan24bits=8, b1=102(01100110)
    val1=102>>2=25 → lookUp[25]='Z'
    k=102&3=2 → lookUp[2<<4]=lookUp[32]='g'  (32-26=6, 'a'+6='g')
    → "Zg=="

  "fo" = [102,111]:
    fewerThan24bits=16, b1=102,b2=111
    val1=25→'Z'; k=102&3=2; val2=111>>4=6(bit7=0)→ lookUp[6|(2<<4)]=lookUp[38]
    lookUp[38]='a'+(38-26)='a'+12='m' → 'm'
    l=111&0xf=15 → lookUp[15<<2]=lookUp[60]='8' (52+8=60 → '0'+8='8')
    PAD
    → "Zm8="

  "foo" = [102,111,111]:
    b1=102,b2=111,b3=111
    val1=25→'Z'; k=2, val2=6→lookUp[6|32]=lookUp[38]='m'
    l=15, val3=111>>6=1(bit7=0)→lookUp[(15<<2)|1]=lookUp[61]='9'
    lookUp[111&63]=lookUp[47]? 47>44? lookUp[63]='/' → lookUp[47]='v'? 
    47-26=21, 'a'+21='v' → lookUp[111&63]=lookUp[47]='v'? Wait 111&63=111-64=47 ✓
    → "Zm9v"

Now write the complete test class. Here it is:

```java
package org.apache.commons.codec.binary;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class Base64CharacterizationTest {

    // -----------------------------------------------------------------------
    // encodeBase64 / encode — empty and short inputs
    // -----------------------------------------------------------------------

    @Test
    void encodeEmptyArrayReturnsEmptyArray() {
        byte[] result = Base64.encodeBase64(new byte[0]);
        assertArrayEquals(new byte[0], result);
    }

    @Test
    void encodeSingleZeroByteGivesAA_eq_eq() {
        // 1 byte → fewerThan24bits=8 → "AA=="
        byte[] result = Base64.encodeBase64(new byte[]{0});
        assertArrayEquals("AA==".getBytes(), result);
    }

    @Test
    void encodeTwoZeroBytesGivesAAA_eq() {
        // 2 bytes → fewerThan24bits=16 → "AAA="
        byte[] result = Base64.encodeBase64(new byte[]{0, 0});
        assertArrayEquals("AAA=".getBytes(), result);
    }

    @Test
    void encodeThreeZeroBytesGivesAAAA() {
        // 3 bytes → fewerThan24bits=0, 1 triplet → "AAAA"
        byte[] result = Base64.encodeBase64(new byte[]{0, 0, 0});
        assertArrayEquals("AAAA".getBytes(), result);
    }

    @Test
    void encodeManGivesTWFu() {
        // "Man" -> "TWFu" (classic RFC example)
        byte[] result = Base64.encodeBase64("Man".getBytes());
        assertArrayEquals("TWFu".getBytes(), result);
    }

    @Test
    void encodeSingleF_givesZg_eq_eq() {
        byte[] result = Base64.encodeBase64("f".getBytes());
        assertArrayEquals("Zg==".getBytes(), result);
    }

    @Test
    void encodeFo_givesZm8_eq() {
        byte[] result = Base64.encodeBase64("fo".getBytes());
        assertArrayEquals("Zm8=".getBytes(), result);
    }

    @Test
    void encodeFoo_givesZm9v() {
        byte[] result = Base64.encodeBase64("foo".getBytes());
        assertArrayEquals("Zm9v".getBytes(), result);
    }

    @Test
    void encodeFourBytesGivesTwoTripletOutput() {
        // 4 bytes: 1 triplet + 1 remainder byte → 8 chars
        byte[] result = Base64.encodeBase64(new byte[]{0, 0, 0, 0});
        assertEquals(8, result.length);
        // first 4 = AAAA, last 4 = AA==
        byte[] expected = "AAAAAAAA".getBytes();
        // wait: triplet[0,0,0]→"AAAA"; remainder[0]→"AA=="
        byte[] expectedActual = "AAAAAAAA==".getBytes();
        // recalc: encodedDataLength = (1+1)*4=8... that's wrong. Let me recalc.
        // 4 bytes: fewerThan24bits = (4*8)%24 = 32%24 = 8; numberTriplets=32/24=1
        // encodedDataLength = (1+1)*4 = 8
        // triplet [0,0,0] at positions [0-3]: "AAAA"
        // remainder: b1=0 → "AA==" at positions [4-7]
        // So result = "AAAAAAAA==" ... but length=8 not 10!
        // ERROR: "AA==" is 4 chars, total should be 4+4=8 bytes
        // result[4]='A', result[5]='A', result[6]='=', result[7]='='
        assertEquals(8, result.length);
        assertEquals('A', (char) result[0]);
        assertEquals('A', (char) result[4]);
        assertEquals('A', (char) result[5]);
        assertEquals('=', (char) result[6]);
        assertEquals('=', (char) result[7]);
    }

    // -----------------------------------------------------------------------
    // decodeBase64 / decode — edge cases
    // -----------------------------------------------------------------------

    @Test
    void decodeEmptyReturnsEmpty() {
        byte[] result = Base64.decodeBase64(new byte[0]);
        assertArrayEquals(new byte[0], result);
    }

    @Test
    void decodeAAAA_givesThreeZeroBytes() {
        // numberQuadruple=1, lastData=4, decodedData=byte[3]
        byte[] result = Base64.decodeBase64("AAAA".getBytes());
        assertArrayEquals(new byte[]{0, 0, 0}, result);
    }

    @Test
    void decodeAA_eq_eq_givesOneZeroByte() {
        // Two pads: lastData=2, nq=1, size=1; only decodedData[0] is written
        byte[] result = Base64.decodeBase64("AA==".getBytes());
        assertEquals(1, result.length);
        assertEquals(0, result[0]);
    }

    @Test
    void decodeAAA_eq_givesTwoZeroBytes() {
        // One pad: lastData=3, nq=1, size=2
        byte[] result = Base64.decodeBase64("AAA=".getBytes());
        assertArrayEquals(new byte[]{0, 0}, result);
    }

    @Test
    void decodeTWFu_givesMan() {
        byte[] result = Base64.decodeBase64("TWFu".getBytes());
        assertArrayEquals("Man".getBytes(), result);
    }

    @Test
    void decodeZg_eq_eq_givesF() {
        byte[] result = Base64.decodeBase64("Zg==".getBytes());
        assertArrayEquals("f".getBytes(), result);
    }

    @Test
    void decodeZm8_eq_givesFo() {
        byte[] result = Base64.decodeBase64("Zm8=".getBytes());
        assertArrayEquals("fo".getBytes(), result);
    }

    @Test
    void decodeZm9v_givesFoo() {
        byte[] result = Base64.decodeBase64("Zm9v".getBytes());
        assertArrayEquals("foo".getBytes(), result);
    }

    // The decode output array is sized [lastData - numberQuadruple].
    // encodedIndex advances by 3 per quadruple regardless of PADs.
    // Two-PAD group only writes 1 byte but the next encodedIndex is +3.
    // When there are multiple quadruples, trailing groups may leave
    // zero-filled slots in the output array.
    @Test
    void decodeTwoQuadruplesWithTrailingPads_sizeFormula() {
        // "AAAAAAAA" (8 chars, 0 PADs): nq=2, lastData=8, size=6
        byte[] result = Base64.decodeBase64("AAAAAAAA".getBytes());
        assertEquals(6, result.length);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0}, result);
    }

    @Test
    void decodeTwoQuadruplesSecondTwoPads_sizeFormula() {
        // "AAAAAAA=" (8 chars, 1 PAD): nq=2, lastData=7, size=5
        // first quad [0,0,0], second quad one-PAD writes [0,0] → encodedIndex=6
        // but slot [5] already has 0 from array init
        byte[] result = Base64.decodeBase64("AAAAAAA=".getBytes());
        assertEquals(5, result.length);
    }

    // -----------------------------------------------------------------------
    // Decode strips non-base64 characters (including whitespace)
    // -----------------------------------------------------------------------

    @Test
    void decodeStripsWhitespace() {
        // "TW Fu" → strips space → "TWFu" → "Man"
        byte[] result = Base64.decodeBase64("TW Fu".getBytes());
        assertArrayEquals("Man".getBytes(), result);
    }

    @Test
    void decodeStripsNonBase64Chars() {
        // Non-base64 chars like '!' are stripped
        byte[] result = Base64.decodeBase64("TW!Fu".getBytes());
        assertArrayEquals("Man".getBytes(), result);
    }

    @Test
    void decodeAllNonBase64ReturnsEmpty() {
        // After stripping, nothing left → length 0 → returns byte[0]
        byte[] result = Base64.decodeBase64("!!!!".getBytes());
        assertArrayEquals(new byte[0], result);
    }

    // -----------------------------------------------------------------------
    // Chunked encoding
    // -----------------------------------------------------------------------

    @Test
    void chunkedEncodeEmpty_returnsEmpty() {
        // 0 bytes → encodedDataLength=0, nbrChunks=0 → byte[0]
        byte[] result = Base64.encodeBase64Chunked(new byte[0]);
        assertArrayEquals(new byte[0], result);
    }

    @Test
    void chunkedEncode57Bytes_gives78Bytes() {
        // 57 bytes → 76 base64 chars + 2 (CRLF) = 78
        byte[] input = new byte[57];
        byte[] result = Base64.encodeBase64Chunked(input);
        assertEquals(78, result.length);
        assertEquals('\r', (char) result[76]);
        assertEquals('\n', (char) result[77]);
    }

    @Test
    void chunkedEncode57Bytes_firstLineIs76Chars() {
        byte[] input = new byte[57];
        byte[] result = Base64.encodeBase64Chunked(input);
        // first 76 bytes are base64 alphabet chars
        for (int i = 0; i < 76; i++) {
            assertNotEquals((byte) '\r', result[i], "position " + i + " should not be CR");
            assertNotEquals((byte) '\n', result[i], "position " + i + " should not be LF");
        }
    }

    @Test
    void chunkedEncode114Bytes_gives156Bytes() {
        // 114 bytes → 152 base64 chars + 4 (2 CRLFs) = 156
        byte[] input = new byte[114];
        byte[] result = Base64.encodeBase64Chunked(input);
        assertEquals(156, result.length);
        // CRLF at positions 76,77 and 154,155
        assertEquals('\r', (char) result[76]);
        assertEquals('\n', (char) result[77]);
        assertEquals('\r', (char) result[154]);
        assertEquals('\n', (char) result[155]);
    }

    @Test
    void chunkedEncode58Bytes_gives84Bytes() {
        // 58 bytes: 19 triplets + 1 remainder
        // encodedDataLength = 20*4=80; nbrChunks=ceil(80/76)=2; total=80+4=84
        byte[] input = new byte[58];
        byte[] result = Base64.encodeBase64Chunked(input);
        assertEquals(84, result.length);
        // CRLF after first 76 chars: positions 76,77
        assertEquals('\r', (char) result[76]);
        assertEquals('\n', (char) result[77]);
        // trailing CRLF at end: positions 82,83
        assertEquals('\r', (char) result[82]);
        assertEquals('\n', (char) result[83]);
    }

    @Test
    void chunkedEncode1Byte_gives6Bytes() {
        // 1 byte → encodedDataLength=4; nbrChunks=ceil(4/76)=1; total=4+2=6
        byte[] input = new byte[]{0};
        byte[] result = Base64.encodeBase64Chunked(input);
        assertEquals(6, result.length);
        assertEquals('A', (char) result[0]);
        assertEquals('A', (char) result[1]);
        assertEquals('=', (char) result[2]);
        assertEquals('=', (char) result[3]);
        assertEquals('\r', (char) result[4]);
        assertEquals('\n', (char) result[5]);
    }

    // -----------------------------------------------------------------------
    // encodeBase64Chunked vs encodeBase64 — same content, different wrapping
    // -----------------------------------------------------------------------

    @Test
    void nonChunkedAndChunkedSameContentFor3Bytes() {
        byte[] input = "Man".getBytes();
        byte[] plain = Base64.encodeBase64(input);
        byte[] chunked = Base64.encodeBase64Chunked(input);
        // plain = "TWFu" (4 bytes); chunked = "TWFu\r\n" (6 bytes)
        assertEquals(4, plain.length);
        assertEquals(6, chunked.length);
        for (int i = 0; i < 4; i++) {
            assertEquals(plain[i], chunked[i]);
        }
    }

    // -----------------------------------------------------------------------
    // isArrayByteBase64
    // -----------------------------------------------------------------------

    @Test
    void isArrayByteBase64_emptyArray_returnsTrue() {
        assertTrue(Base64.isArrayByteBase64(new byte[0]));
    }

    @Test
    void isArrayByteBase64_whitespaceOnly_returnsTrue() {
        // discardWhitespace removes all → empty → true
        assertTrue(Base64.isArrayByteBase64(" \t\r\n".getBytes()));
    }

    @Test
    void isArrayByteBase64_validBase64Chars_returnsTrue() {
        assertTrue(Base64.isArrayByteBase64("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnop"
            .getBytes()));
        assertTrue(Base64.isArrayByteBase64("qrstuvwxyz0123456789+/=".getBytes()));
    }

    @Test
    void isArrayByteBase64_paddingChar_returnsTrue() {
        assertTrue(Base64.isArrayByteBase64("AA==".getBytes()));
    }

    @Test
    void isArrayByteBase64_invalidChar_returnsFalse() {
        assertFalse(Base64.isArrayByteBase64("AA!A".getBytes()));
    }

    @Test
    void isArrayByteBase64_invalidCharAfterWhitespaceStrip_returnsFalse() {
        // '!' is not whitespace, survives discardWhitespace → false
        assertFalse(Base64.isArrayByteBase64("! ".getBytes()));
    }

    // -----------------------------------------------------------------------
    // Instance decode(Object) and encode(Object)
    // -----------------------------------------------------------------------

    @Test
    void instanceDecodeByteArray_works() throws DecoderException {
        Base64 b64 = new Base64();
        Object result = b64.decode((Object) "TWFu".getBytes());
        assertInstanceOf(byte[].class, result);
        assertArrayEquals("Man".getBytes(), (byte[]) result);
    }

    @Test
    void instanceDecodeNonByteArray_throwsDecoderException() {
        Base64 b64 = new Base64();
        assertThrows(DecoderException.class, () -> b64.decode((Object) "TWFu"));
    }

    @Test
    void instanceEncodeByteArray_works() throws EncoderException {
        Base64 b64 = new Base64();
        Object result = b64.encode((Object) "Man".getBytes());
        assertInstanceOf(byte[].class, result);
        assertArrayEquals("TWFu".getBytes(), (byte[]) result);
    }

    @Test
    void instanceEncodeNonByteArray_throwsEncoderException() {
        Base64 b64 = new Base64();
        assertThrows(EncoderException.class, () -> b64.encode((Object) "Man"));
    }

    // -----------------------------------------------------------------------
    // Instance encode/decode byte[] convenience methods
    // -----------------------------------------------------------------------

    @Test
    void instanceEncode_delegatesToNonChunked() throws EncoderException {
        Base64 b64 = new Base64();
        byte[] result = b64.encode("Man".getBytes());
        assertArrayEquals("TWFu".getBytes(), result);
    }

    @Test
    void instanceDecode_delegatesToDecodeBase64() {
        Base64 b64 = new Base64();
        byte[] result = b64.decode("TWFu".getBytes());
        assertArrayEquals("Man".getBytes(), result);
    }

    // -----------------------------------------------------------------------
    // Null throws NullPointerException (no null-guards in the code)
    // -----------------------------------------------------------------------

    @Test
    void encodeBase64_null_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Base64.encodeBase64(null));
    }

    @Test
    void decodeBase64_null_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Base64.decodeBase64(null));
    }

    @Test
    void isArrayByteBase64_null_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Base64.isArrayByteBase64(null));
    }

    // -----------------------------------------------------------------------
    // Decode output array size formula — trailing zero bytes
    // -----------------------------------------------------------------------

    @Test
    void decodeOutputSizeFormula_noTrailingPads() {
        // n quadruples, no PAD: lastData=4n, nq=n, size=4n-n=3n
        byte[] r = Base64.decodeBase64("AAAAAAAAAAAAAAAA".getBytes()); // 16 chars / 4 = 4 quads
        // nq=4, lastData=16, size=12
        assertEquals(12, r.length);
    }

    @Test
    void decodeOutputSizeFormula_onePad() {
        // "AAAAAAAAAAAAAAAA=" would be 17 chars — after discardNonBase64 PAD is kept,
        // but 17/4=4 quads; actually we need a valid 5-quad: "AAAAAAAAAAAAAAAAAAA="
        // 20 chars, 5 quads, lastData=19, size=19-5=14, one-PAD last quad writes 2 bytes
        // total written: 4*3 + 2 = 14 slots; encodedIndex advances 5*3=15 > array size
        // Actually: first 4 quads write to [0-11]; 5th quad one-PAD branch writes to [12,13]
        // encodedIndex after = 15, but array is byte[14] → safe (only [12],[13] written)
        byte[] r = Base64.decodeBase64("AAAAAAAAAAAAAAAAAAA=".getBytes());
        assertEquals(14, r.length);
    }

    // -----------------------------------------------------------------------
    // Roundtrip
    // -----------------------------------------------------------------------

    @Test
    void roundtrip_shortString() {
        byte[] original = "Hello, World!".getBytes();
        byte[] encoded = Base64.encodeBase64(original);
        byte[] decoded = Base64.decodeBase64(encoded);
        assertArrayEquals(original, decoded);
    }

    @Test
    void roundtrip_allByteValues() {
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }
        byte[] encoded = Base64.encodeBase64(original);
        byte[] decoded = Base64.decodeBase64(encoded);
        assertArrayEquals(original, decoded);
    }

    @Test
    void roundtrip_chunkedThenDecode() {
        byte[] original = new byte[200];
        for (int i = 0; i < 200; i++) original[i] = (byte) i;
        byte[] encoded = Base64.encodeBase64Chunked(original);
        // decode strips CRLF (non-base64 chars are stripped)
        byte[] decoded = Base64.decodeBase64(encoded);
        assertArrayEquals(original, decoded);
    }

    // -----------------------------------------------------------------------
    // CHUNK_SIZE and CHUNK_SEPARATOR constants are publicly visible
    // -----------------------------------------------------------------------

    @Test
    void chunkSizeIs76() {
        assertEquals(76, Base64.CHUNK_SIZE);
    }

    @Test
    void chunkSeparatorIsCRLF() {
        assertArrayEquals(new byte[]{'\r', '\n'}, Base64.CHUNK_SEPARATOR);
    }
}
```

Wait, I need to review the `encodeFourBytesGivesTwoTripletOutput` test — it has confused comments and a redundant assertion. Let me clean it up and also double check the `isArrayByteBase64_whitespaceOnly_returnsTrue` — since discardWhitespace removes ' ', '\n', '\r', '\t' but NOT other whitespace. The test uses exactly those characters so it's fine.

Also verify `decodeAA_eq_eq_givesOneZeroByte`: after discardNonBase64, '=' is kept (isBase64('=') returns true because PAD==('=')), so we get [65,65,61,61]. nq=1, lastData starts at 4, base64Data[3]='='==PAD → lastData=3; base64Data[2]='='==PAD → lastData=2; not 0. decodedData=byte[2-1]=byte[1]. Loop i=0: marker0=base64Data[2]='='=PAD, marker1=base64Data[3]='='=PAD → two-PAD branch: decodedData[0]=(b1<<2|b2>>4)=(0<<2|0>>4)=0. encodedIndex=3 (but array only has 1 slot → only slot 0 was written). Return byte[]{0}. ✓

Check `decodeStripsWhitespace`: "TW Fu" has 5 chars. After discardNonBase64: space is not in base64 alphabet (base64Alphabet[' '] = -1, and ' '!=PAD), so space is stripped → "TWFu" (4 chars). ✓

Now produce the final, clean test class with all tests verified and every line under 100 chars.



Subagent completed | 110.4s | 3 tool call(s) | 0.234


────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
Assistant (1) 2026-09-26 15:19:18.127 UTC (+2.4s)

I'll analyze the code carefully before writing the tests — tracing each branch, the encoding/decoding math, chunk logic,
and edge cases.                                                                                                         

Tool calls:
- spawn_subagent

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
Tool (1) 2026-09-26 15:22:46.667 UTC (+211.0s) Success

Tool: spawn_subagent
Args:
- name: general
- description: 
Carefully analyze the following Apache Commons Codec Base64 implementation and produce a complete, self-contained JU...

Files changed:
  ~ file:///tmp/bob-bridge-4vf801ko/Base64CharacterizationTest.java

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
Assistant (2) 2026-09-26 15:22:48.312 UTC (+212.6s)

The subagent's analysis is thorough and correct. Let me produce the final clean test class, incorporating all the       
verified logic:                                                                                                         
                                                                                                                        
```java                                                                                                                 
  package org.apache.commons.codec.binary;                                                                              
                                                                                                                        
  import static org.junit.jupiter.api.Assertions.*;                                                                     
                                                                                                                        
  import org.apache.commons.codec.DecoderException;                                                                     
  import org.apache.commons.codec.EncoderException;                                                                     
  import org.junit.jupiter.api.Test;                                                                                    
                                                                                                                        
  /**                                                                                                                   
   * Characterization tests for the legacy Base64 class.                                                                
   *                                                                                                                    
   * These tests pin what the code DOES today, including surprising behaviour.                                          
   * Where the javadoc and the implementation disagree, the implementation wins.                                        
   */                                                                                                                   
  class Base64CharacterizationTest {                                                                                    
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // Static constants — publicly visible values pinned for regression                                               
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void chunkSizeConstantIs76() {                                                                                    
          assertEquals(76, Base64.CHUNK_SIZE);                                                                          
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void chunkSeparatorIsCRLF() {                                                                                     
          assertArrayEquals(new byte[]{'\r', '\n'}, Base64.CHUNK_SEPARATOR);                                            
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // encodeBase64 — empty input                                                                                     
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void encodeEmpty_returnsEmptyArray() {                                                                            
          assertArrayEquals(new byte[0], Base64.encodeBase64(new byte[0]));                                             
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // encodeBase64 — one byte (fewerThan24bits == EIGHTBIT)                                                          
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void encodeSingleZeroByte_givesAA_eq_eq() {                                                                       
          // fewerThan24bits=8 → 4-byte output, last two bytes are PAD                                                  
          assertArrayEquals("AA==".getBytes(), Base64.encodeBase64(new byte[]{0}));                                     
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void encodeSingleByte_f_givesZg_eq_eq() {                                                                         
          // 'f' = 0x66: val1 = 0x66>>2 = 25 → 'Z'; k = 0x66&3 = 2 → lookUp[32] = 'g'                                   
          assertArrayEquals("Zg==".getBytes(), Base64.encodeBase64("f".getBytes()));                                    
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // encodeBase64 — two bytes (fewerThan24bits == SIXTEENBIT)                                                       
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void encodeTwoZeroBytes_givesAAA_eq() {                                                                           
          // fewerThan24bits=16 → 4-byte output, last byte is PAD                                                       
          assertArrayEquals("AAA=".getBytes(), Base64.encodeBase64(new byte[]{0, 0}));                                  
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void encodeTwoBytes_fo_givesZm8_eq() {                                                                            
          assertArrayEquals("Zm8=".getBytes(), Base64.encodeBase64("fo".getBytes()));                                   
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // encodeBase64 — three bytes (one complete triplet, no remainder)                                                
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void encodeThreeZeroBytes_givesAAAA() {                                                                           
          assertArrayEquals("AAAA".getBytes(), Base64.encodeBase64(new byte[]{0, 0, 0}));                               
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void encodeThreeBytes_Man_givesTWFu() {                                                                           
          // Classic RFC 2045 example                                                                                   
          assertArrayEquals("TWFu".getBytes(), Base64.encodeBase64("Man".getBytes()));                                  
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void encodeThreeBytes_foo_givesZm9v() {                                                                           
          assertArrayEquals("Zm9v".getBytes(), Base64.encodeBase64("foo".getBytes()));                                  
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // encodeBase64 — four bytes (one triplet + one-byte remainder)                                                   
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void encodeFourZeroBytes_givesTripletPlusOneByteRemainder() {                                                     
          // fewerThan24bits=8, numberTriplets=1 → encodedDataLength=8                                                  
          // bytes 0-3: triplet [0,0,0] → "AAAA"                                                                        
          // bytes 4-7: remainder [0] → "AA=="                                                                          
          byte[] result = Base64.encodeBase64(new byte[]{0, 0, 0, 0});                                                  
          assertEquals(8, result.length);                                                                               
          assertEquals('A', (char) result[0]);                                                                          
          assertEquals('A', (char) result[1]);                                                                          
          assertEquals('A', (char) result[2]);                                                                          
          assertEquals('A', (char) result[3]);                                                                          
          assertEquals('A', (char) result[4]);                                                                          
          assertEquals('A', (char) result[5]);                                                                          
          assertEquals('=', (char) result[6]);                                                                          
          assertEquals('=', (char) result[7]);                                                                          
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // decodeBase64 — empty and all-stripped input                                                                    
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void decodeEmpty_returnsEmptyArray() {                                                                            
          assertArrayEquals(new byte[0], Base64.decodeBase64(new byte[0]));                                             
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeAllNonBase64Chars_returnsEmptyArray() {                                                                
          // discardNonBase64 strips everything → length 0 → returns byte[0]                                            
          assertArrayEquals(new byte[0], Base64.decodeBase64("!!!!".getBytes()));                                       
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // decodeBase64 — output-size formula: new byte[lastData - numberQuadruple]                                       
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void decodeAAAA_givesThreeZeroBytes() {                                                                           
          // nq=1, lastData=4 (no PAD), size=3                                                                          
          assertArrayEquals(new byte[]{0, 0, 0}, Base64.decodeBase64("AAAA".getBytes()));                               
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeAA_eq_eq_givesOneZeroByte() {                                                                          
          // nq=1, lastData=2 (two trailing '='), size=1; only slot 0 written                                           
          byte[] result = Base64.decodeBase64("AA==".getBytes());                                                       
          assertEquals(1, result.length);                                                                               
          assertEquals(0, result[0]);                                                                                   
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeAAA_eq_givesTwoZeroBytes() {                                                                           
          // nq=1, lastData=3 (one trailing '='), size=2                                                                
          assertArrayEquals(new byte[]{0, 0}, Base64.decodeBase64("AAA=".getBytes()));                                  
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeTWFu_givesMan() {                                                                                      
          assertArrayEquals("Man".getBytes(), Base64.decodeBase64("TWFu".getBytes()));                                  
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeZg_eq_eq_givesF() {                                                                                    
          assertArrayEquals("f".getBytes(), Base64.decodeBase64("Zg==".getBytes()));                                    
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeZm8_eq_givesFo() {                                                                                     
          assertArrayEquals("fo".getBytes(), Base64.decodeBase64("Zm8=".getBytes()));                                   
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeZm9v_givesFoo() {                                                                                      
          assertArrayEquals("foo".getBytes(), Base64.decodeBase64("Zm9v".getBytes()));                                  
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // decodeBase64 — two quadruples, size formula across multiple groups                                             
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void decodeTwoFullQuads_sizeIsSix() {                                                                             
          // "AAAAAAAA": nq=2, lastData=8, size=6 → [0,0,0,0,0,0]                                                       
          byte[] result = Base64.decodeBase64("AAAAAAAA".getBytes());                                                   
          assertEquals(6, result.length);                                                                               
          assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0}, result);                                                      
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeTwoQuads_twoPad_sizeIsFour() {                                                                         
          // "AAAAAA==": nq=2, lastData=6 (two '=' stripped), size=4                                                    
          // first quad [0,0,0], second quad two-PAD: only slot[3] written, slots[4,5] are                              
          // zero-filled by Java array initialisation and never overwritten.                                            
          byte[] result = Base64.decodeBase64("AAAAAA==".getBytes());                                                   
          assertEquals(4, result.length);                                                                               
          // only slot 0-2 of second quad written: slot[3]=0 (one byte), rest from init                                 
          assertArrayEquals(new byte[]{0, 0, 0, 0}, result);                                                            
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeTwoQuads_onePad_sizeIsFive() {                                                                         
          // "AAAAAAA=": nq=2, lastData=7 (one '='), size=5                                                             
          byte[] result = Base64.decodeBase64("AAAAAAA=".getBytes());                                                   
          assertEquals(5, result.length);                                                                               
          assertArrayEquals(new byte[]{0, 0, 0, 0, 0}, result);                                                         
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // decodeBase64 — discardNonBase64 strips noise before processing                                                 
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void decode_whitespaceInsideInput_isStripped() {                                                                  
          // space is not a base64 char → stripped by discardNonBase64                                                  
          assertArrayEquals("Man".getBytes(), Base64.decodeBase64("TW Fu".getBytes()));                                 
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decode_crlfInsideInput_isStripped() {                                                                        
          assertArrayEquals("Man".getBytes(),                                                                           
                  Base64.decodeBase64("TW\r\nFu".getBytes()));                                                          
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decode_punctuationInsideInput_isStripped() {                                                                 
          // '!' → not in alphabet → silently removed                                                                   
          assertArrayEquals("Man".getBytes(), Base64.decodeBase64("T!W!F!u".getBytes()));                               
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // encodeBase64Chunked — output length and CRLF positions                                                         
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void chunkedEncodeEmpty_returnsEmptyArray() {                                                                     
          // encodedDataLength=0, nbrChunks=0 → byte[0]                                                                 
          assertArrayEquals(new byte[0], Base64.encodeBase64Chunked(new byte[0]));                                      
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void chunkedEncode1Byte_givesFourBase64PlusCRLF() {                                                               
          // encodedDataLength=4; nbrChunks=ceil(4/76)=1; total=6                                                       
          byte[] result = Base64.encodeBase64Chunked(new byte[]{0});                                                    
          assertEquals(6, result.length);                                                                               
          assertEquals('A', (char) result[0]);                                                                          
          assertEquals('A', (char) result[1]);                                                                          
          assertEquals('=', (char) result[2]);                                                                          
          assertEquals('=', (char) result[3]);                                                                          
          assertEquals('\r', (char) result[4]);                                                                         
          assertEquals('\n', (char) result[5]);                                                                         
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void chunkedEncodeManGivesTWFuCRLF() {                                                                            
          // 3 bytes → "TWFu" (4 chars) + "\r\n" = 6 bytes                                                              
          byte[] result = Base64.encodeBase64Chunked("Man".getBytes());                                                 
          assertEquals(6, result.length);                                                                               
          assertEquals('T', (char) result[0]);                                                                          
          assertEquals('W', (char) result[1]);                                                                          
          assertEquals('F', (char) result[2]);                                                                          
          assertEquals('u', (char) result[3]);                                                                          
          assertEquals('\r', (char) result[4]);                                                                         
          assertEquals('\n', (char) result[5]);                                                                         
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void chunkedEncode57Bytes_gives78Bytes_crlfAt76_77() {                                                            
          // 57 bytes → 76 base64 chars, nbrChunks=1, total=78                                                          
          byte[] input = new byte[57];                                                                                  
          byte[] result = Base64.encodeBase64Chunked(input);                                                            
          assertEquals(78, result.length);                                                                              
          assertEquals('\r', (char) result[76]);                                                                        
          assertEquals('\n', (char) result[77]);                                                                        
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void chunkedEncode57Bytes_first76CharsAreNotCRLF() {                                                              
          byte[] result = Base64.encodeBase64Chunked(new byte[57]);                                                     
          for (int i = 0; i < 76; i++) {                                                                                
              assertNotEquals((byte) '\r', result[i], "unexpected CR at position " + i);                                
              assertNotEquals((byte) '\n', result[i], "unexpected LF at position " + i);                                
          }                                                                                                             
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void chunkedEncode114Bytes_gives156Bytes_twoChunks() {                                                            
          // 114 bytes → 152 base64 chars, nbrChunks=2, total=156                                                       
          // CRLF at [76,77] and [154,155]                                                                              
          byte[] result = Base64.encodeBase64Chunked(new byte[114]);                                                    
          assertEquals(156, result.length);                                                                             
          assertEquals('\r', (char) result[76]);                                                                        
          assertEquals('\n', (char) result[77]);                                                                        
          assertEquals('\r', (char) result[154]);                                                                       
          assertEquals('\n', (char) result[155]);                                                                       
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void chunkedEncode58Bytes_gives84Bytes_crlfAfterFirstLine_andAtEnd() {                                            
          // 58 bytes: fewerThan24bits=8, numberTriplets=19                                                             
          // encodedDataLength=(19+1)*4=80; nbrChunks=ceil(80/76)=2; total=84                                           
          // CRLF inserted during loop at [76,77]; trailing CRLF at [82,83]                                             
          byte[] result = Base64.encodeBase64Chunked(new byte[58]);                                                     
          assertEquals(84, result.length);                                                                              
          assertEquals('\r', (char) result[76]);                                                                        
          assertEquals('\n', (char) result[77]);                                                                        
          assertEquals('\r', (char) result[82]);                                                                        
          assertEquals('\n', (char) result[83]);                                                                        
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // encodeBase64Chunked — content matches non-chunked within each line                                             
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void chunkedAndNonChunked_sameContentFor3Bytes() {                                                                
          byte[] plain = Base64.encodeBase64("Man".getBytes());   // "TWFu" (4 bytes)                                   
          byte[] chunked = Base64.encodeBase64Chunked("Man".getBytes()); // "TWFu\r\n" (6 bytes)                        
          assertEquals(4, plain.length);                                                                                
          assertEquals(6, chunked.length);                                                                              
          for (int i = 0; i < plain.length; i++) {                                                                      
              assertEquals(plain[i], chunked[i], "mismatch at position " + i);                                          
          }                                                                                                             
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // isArrayByteBase64                                                                                              
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void isArrayByteBase64_emptyArray_returnsTrue() {                                                                 
          // explicit comment in source: "shouldn't a 0 length array be valid base64 data?"                             
          // The return-false line is commented out; it returns true.                                                   
          assertTrue(Base64.isArrayByteBase64(new byte[0]));                                                            
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void isArrayByteBase64_whitespaceOnly_returnsTrue() {                                                             
          // discardWhitespace removes ' ', '\t', '\r', '\n' → empty → true                                             
          assertTrue(Base64.isArrayByteBase64(" \t\r\n".getBytes()));                                                   
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void isArrayByteBase64_allAlphabetChars_returnsTrue() {                                                           
          String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";                        
          assertTrue(Base64.isArrayByteBase64(alphabet.getBytes()));                                                    
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void isArrayByteBase64_validPaddedString_returnsTrue() {                                                          
          assertTrue(Base64.isArrayByteBase64("AA==".getBytes()));                                                      
          assertTrue(Base64.isArrayByteBase64("AAA=".getBytes()));                                                      
          assertTrue(Base64.isArrayByteBase64("AAAA".getBytes()));                                                      
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void isArrayByteBase64_singleInvalidChar_returnsFalse() {                                                         
          // '!' (0x21) → base64Alphabet[0x21] = -1, not PAD → false                                                    
          assertFalse(Base64.isArrayByteBase64("!".getBytes()));                                                        
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void isArrayByteBase64_invalidCharMixedWithWhitespace_returnsFalse() {                                            
          // whitespace is stripped, but '!' survives discardWhitespace → false                                         
          assertFalse(Base64.isArrayByteBase64(" ! ".getBytes()));                                                      
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void isArrayByteBase64_invalidCharAmongValid_returnsFalse() {                                                     
          assertFalse(Base64.isArrayByteBase64("AA!A".getBytes()));                                                     
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // Instance encode(Object) — delegates to encodeBase64(x, false)                                                  
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void instanceEncodeObject_byteArray_returnsEncodedBytes() throws EncoderException {                               
          Base64 b64 = new Base64();                                                                                    
          Object result = b64.encode((Object) "Man".getBytes());                                                        
          assertInstanceOf(byte[].class, result);                                                                       
          assertArrayEquals("TWFu".getBytes(), (byte[]) result);                                                        
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void instanceEncodeObject_nonByteArray_throwsEncoderException() {                                                 
          Base64 b64 = new Base64();                                                                                    
          assertThrows(EncoderException.class, () -> b64.encode((Object) "Man"));                                       
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void instanceEncode_byteArray_delegatesToNonChunkedStatic() throws EncoderException {                             
          Base64 b64 = new Base64();                                                                                    
          assertArrayEquals(Base64.encodeBase64("foo".getBytes()), b64.encode("foo".getBytes()));                       
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // Instance decode(Object) — delegates to decodeBase64                                                            
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void instanceDecodeObject_byteArray_returnsDecodedBytes() throws DecoderException {                               
          Base64 b64 = new Base64();                                                                                    
          Object result = b64.decode((Object) "TWFu".getBytes());                                                       
          assertInstanceOf(byte[].class, result);                                                                       
          assertArrayEquals("Man".getBytes(), (byte[]) result);                                                         
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void instanceDecodeObject_nonByteArray_throwsDecoderException() {                                                 
          Base64 b64 = new Base64();                                                                                    
          assertThrows(DecoderException.class, () -> b64.decode((Object) "TWFu"));                                      
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void instanceDecode_byteArray_delegatesToStaticDecodeBase64() {                                                   
          Base64 b64 = new Base64();                                                                                    
          assertArrayEquals(Base64.decodeBase64("TWFu".getBytes()),                                                     
                  b64.decode("TWFu".getBytes()));                                                                       
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // Null input — no guards in the code, so NullPointerException is thrown                                          
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void encodeBase64_null_throwsNPE() {                                                                              
          assertThrows(NullPointerException.class, () -> Base64.encodeBase64(null));                                    
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void decodeBase64_null_throwsNPE() {                                                                              
          assertThrows(NullPointerException.class, () -> Base64.decodeBase64(null));                                    
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void isArrayByteBase64_null_throwsNPE() {                                                                         
          assertThrows(NullPointerException.class, () -> Base64.isArrayByteBase64(null));                               
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // Roundtrip encode → decode                                                                                      
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void roundtrip_emptyArray() {                                                                                     
          byte[] data = new byte[0];                                                                                    
          assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64(data)));                                      
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void roundtrip_singleByte() {                                                                                     
          byte[] data = new byte[]{(byte) 0xFF};                                                                        
          assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64(data)));                                      
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void roundtrip_shortString() {                                                                                    
          byte[] data = "Hello, World!".getBytes();                                                                     
          assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64(data)));                                      
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void roundtrip_allByteValues0to255() {                                                                            
          byte[] data = new byte[256];                                                                                  
          for (int i = 0; i < 256; i++) {                                                                               
              data[i] = (byte) i;                                                                                       
          }                                                                                                             
          assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64(data)));                                      
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void roundtrip_chunkedEncodeIsDecodedCorrectly() {                                                                
          // decode strips \r\n (they are not base64 chars) → identical result                                          
          byte[] data = new byte[200];                                                                                  
          for (int i = 0; i < 200; i++) data[i] = (byte) i;                                                             
          byte[] chunked = Base64.encodeBase64Chunked(data);                                                            
          assertArrayEquals(data, Base64.decodeBase64(chunked));                                                        
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void roundtrip_lengthsAlignForAllRemainderClasses() {                                                             
          // Test inputs of length 0-5 to cover both remainder branches and base triplet path                           
          for (int len = 0; len <= 5; len++) {                                                                          
              byte[] data = new byte[len];                                                                              
              for (int i = 0; i < len; i++) data[i] = (byte) (i + 1);                                                   
              byte[] encoded = Base64.encodeBase64(data);                                                               
              byte[] decoded = Base64.decodeBase64(encoded);                                                            
              assertArrayEquals(data, decoded, "roundtrip failed for input length " + len);                             
          }                                                                                                             
      }                                                                                                                 
                                                                                                                        
      // -------------------------------------------------------------------                                            
      // Exact encoded output for known values — alphabet boundary checks                                               
      // -------------------------------------------------------------------                                            
                                                                                                                        
      @Test                                                                                                             
      void encodeBytes_usesCorrectAlphabetBoundaries() {                                                                
          // index 0 → 'A', 25 → 'Z', 26 → 'a', 51 → 'z', 52 → '0', 61 → '9', 62 → '+', 63 → '/'                        
          // Encode 3 bytes that cover all four alphabet regions in one triplet.                                        
          // [0x00, 0x00, 0x00] → indices [0,0,0,0] → "AAAA"                                                            
          assertArrayEquals("AAAA".getBytes(), Base64.encodeBase64(new byte[]{0, 0, 0}));                               
          // [0xFF, 0xFF, 0xFF]:                                                                                        
          // b1=0xFF(-1): val1=(0xFF>>2)^0xC0=0x3F=63→'/'  k=0xFF&3=3                                                   
          // b2=0xFF(-1): val2=(0xFF>>4)^0xF0=0x0F=15  l=0xFF&0xF=15                                                    
          // b3=0xFF(-1): val3=(0xFF>>6)^0xFC=0x03=3                                                                    
          // enc[0]=lookUp[63]='/'                                                                                      
          // enc[1]=lookUp[15|(3<<4)]=lookUp[63]='/'                                                                    
          // enc[2]=lookUp[(15<<2)|3]=lookUp[63]='/'                                                                    
          // enc[3]=lookUp[0xFF&0x3F]=lookUp[63]='/'                                                                    
          assertArrayEquals("////".getBytes(), Base64.encodeBase64(new byte[]{-1, -1, -1}));                            
      }                                                                                                                 
                                                                                                                        
      @Test                                                                                                             
      void encodeProducesPlusAndSlash() {                                                                               
          // '+' is index 62, '/' is index 63.  0xFB, 0xEF, 0xBE produces "++/+"... let's use known.                    
          // decode("////") should give [0xFF, 0xFF, 0xFF]                                                              
          byte[] decoded = Base64.decodeBase64("////".getBytes());                                                      
          assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, decoded);                                
          // roundtrip from encoding side                                                                               
          assertArrayEquals("////".getBytes(), Base64.encodeBase64(new byte[]{-1, -1, -1}));                            
      }                                                                                                                 
  }                                                                                                                     
```                                                                                                                     
────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task Summary

Total Cost:              0.365
Total Duration:          4m 27s


Assistant Messages:      2
Tool Calls:              1
Task ID:                 c60c6ce9e876d8bb70fd7cfb49ac7a64

````
