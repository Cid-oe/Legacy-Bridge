# Target: URLCodec (Apache Commons Codec 1.3)

Real open-source legacy code: `URLCodec.java`, `StringEncodings.java` and the eight small
interfaces/exceptions they need are copied **unmodified** from `commons-codec-1.3-sources.jar` on
Maven Central (released 2005), under the Apache License 2.0 (see `LICENSE.txt`). Copyright The Apache
Software Foundation.

Why it is a hard test: the obvious ticket is "replace our hand-rolled URL codec with
`java.net.URLEncoder` / `URLDecoder`". On everyday text the two agree, so a quick check passes; the
differences only show up on raw bytes, non-ASCII input and malformed input. Measured on Java 21:

| Call | Commons Codec 1.3 | `URLEncoder` / `URLDecoder` (UTF-8) |
|---|---|---|
| `encode(new byte[]{(byte)0xFF})` | `%FF` | via a UTF-8 String: `%EF%BF%BD` |
| `decode("%FF".getBytes())` | byte `FF` | via a UTF-8 String: `EF BF BD` |
| `decode("café")` (raw non-ASCII) | `caf?` | `café` |
| `decode("%+1")` | throws `DecoderException` | returns `"\u0001"` |
| `decode("%zz")`, `decode("%2")` | throws `DecoderException` | throws `IllegalArgumentException` |
| `encode((String) null)` | `null` | throws `NullPointerException` |
| `encode("a b*~!")` | `a+b*%7E%21` | `a+b*%7E%21` (same) |

`URLCodecControlTest` pins the original behaviour; `mutants.json` breaks six behaviours one at a time.
