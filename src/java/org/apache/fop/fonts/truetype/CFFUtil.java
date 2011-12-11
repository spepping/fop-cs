/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fonts.truetype;

import java.io.IOException;
import java.util.Map;

/**
 * A utility class for working with Adobe Compact Font Format (CFF) data. See
 * Adobe Technical Note #5176 "The Compact Font Format Specification" for more
 * information.
 */
final class CFFUtil {

    private CFFUtil() {
    }

    /**
     * Extract glyph subset of CFF table ENTRY from input font IN according to
     * the specified GLYPHS.
     * @param in input font file reader
     * @param entry directory entry describing CFF table in input file
     * @param glyphs map of original glyph indices to subset indices
     * @returns an array of bytes representing a well formed CFF table containing
     * the specified glyph subset
     * @throws IOException in case of an I/O exception when reading from input font
     */
    public static byte[] extractGlyphSubset
        ( FontFileReader in, TTFDirTabEntry entry, Map<Integer, Integer> glyphs )
        throws IOException {

        // 1. read CFF data from IN, where ENTRY points at start of CFF table
        // 2. while reading CFF data, accumulate necessary information to output subset
        //    of glyphs, where GLYPHS.keySet() enumerates the desired glyphs, and
        //    GLYPHS.values() (for the keys) enumerates the new (output) glyph indices
        //    for the desired glyph subset
        // 3. return a BLOB containing a well-formed CFF font according to the Adobe
        //    spec and constrained as needed by http://www.microsoft.com/typography/otspec/cff.htm

        return new byte[] {};
    }

}
