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

package org.apache.fop.pdf;

/** Enum class for PDF/X modes. */
public final class PDFXMode {

    /** PDF/X disabled */
    public static final PDFXMode DISABLED = new PDFXMode("PDF/X disabled");
    /** PDF/X-1:2001 enabled */
    public static final PDFXMode PDFX_1_2001 = new PDFXMode("PDF/X-1:2001");
    /** PDF/X-1a:2001 enabled */
    public static final PDFXMode PDFX_1A_2001 = new PDFXMode("PDF/X-1a:2001");
    /** PDF/X-1a:2003 enabled */
    public static final PDFXMode PDFX_1A_2003 = new PDFXMode("PDF/X-1a:2003");
    /** PDF/X-3:2002 enabled */
    public static final PDFXMode PDFX_3_2002 = new PDFXMode("PDF/X-3:2002");
    /** PDF/X-3:2003 enabled */
    public static final PDFXMode PDFX_3_2003 = new PDFXMode("PDF/X-3:2003");
    /** PDF/X-4:2008 enabled */
    public static final PDFXMode PDFX_4 = new PDFXMode("PDF/X-4");
    /** PDF/X-4p:2008 enabled */
    public static final PDFXMode PDFX_4P = new PDFXMode("PDF/X-4p");
    /** PDF/X-5:2008 enabled */
    public static final PDFXMode PDFX_5 = new PDFXMode("PDF/X-5");
    /** PDF/X-5g:2008 enabled */
    public static final PDFXMode PDFX_5G = new PDFXMode("PDF/X-5g");
    /** PDF/X-5pg:2008 enabled */
    public static final PDFXMode PDFX_5PG = new PDFXMode("PDF/X-5pg");
    /** PDF/X-5n:2008 enabled */
    public static final PDFXMode PDFX_5N = new PDFXMode("PDF/X-5n");

    private String name;

    /**
     * Constructor to add a new named item.
     * @param name Name of the item.
     */
    private PDFXMode(String name) {
        this.name = name;
    }

    /** @return the name of the enum */
    public String getName() {
        return this.name;
    }

    /**
     * Determine if this mode is supported by specified version.
     * @param version a PDF version object
     * @return true if specified version supports this mode
     */
    public boolean supportedByVersion ( PDFVersion version ) {
        float v = version.floatValue();
        if ( this == DISABLED ) {
            return true;
        } else if ( this == PDFX_1_2001 ) {
            return v >= 1.3F;
        } else if ( this == PDFX_1A_2001 ) {
            return v >= 1.3F;
        } else if ( this == PDFX_1A_2003 ) {
            return v >= 1.4F;
        } else if ( this == PDFX_3_2002 ) {
            return v >= 1.3F;
        } else if ( this == PDFX_3_2003 ) {
            return v >= 1.4F;
        } else if ( this == PDFX_4 ) {
            return v >= 1.4F;
        } else if ( this == PDFX_4P ) {
            return v >= 1.4F;
        } else if ( this == PDFX_5G ) {
            return v >= 1.4F;
        } else if ( this == PDFX_5PG ) {
            return v >= 1.4F;
        } else if ( this == PDFX_5N ) {
            return v >= 1.4F;
        } else {
            return false;
        }
    }

    /**
     * Returns the mode enum object given a String.
     * @param s the string
     * @return the PDFAMode enum object (DISABLED will be returned if no match is found)
     */
    public static PDFXMode valueOf(String s) {
        if (PDFX_1_2001.getName().equalsIgnoreCase(s)) {
            return PDFX_1_2001;
        } else if (PDFX_1A_2001.getName().equalsIgnoreCase(s)) {
            return PDFX_1A_2001;
        } else if (PDFX_1A_2003.getName().equalsIgnoreCase(s)) {
            return PDFX_1A_2003;
        } else if (PDFX_3_2002.getName().equalsIgnoreCase(s)) {
            return PDFX_3_2002;
        } else if (PDFX_3_2003.getName().equalsIgnoreCase(s)) {
            return PDFX_3_2003;
        } else if (PDFX_4.getName().equalsIgnoreCase(s)) {
            return PDFX_4;
        } else if (PDFX_4P.getName().equalsIgnoreCase(s)) {
            return PDFX_4P;
        } else if (PDFX_5.getName().equalsIgnoreCase(s)) {
            return PDFX_5;
        } else if (PDFX_5G.getName().equalsIgnoreCase(s)) {
            return PDFX_5G;
        } else if (PDFX_5PG.getName().equalsIgnoreCase(s)) {
            return PDFX_5PG;
        } else if (PDFX_5N.getName().equalsIgnoreCase(s)) {
            return PDFX_5N;
        } else {
            return DISABLED;
        }
    }

    /** {@inheritDoc} */
    public String toString() {
        return name;
    }

}
