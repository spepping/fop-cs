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

/** Enum class for PDF versions. */
public final class PDFVersion {

    /** PDF 1.3 */
    public static final PDFVersion V_1_3 = new PDFVersion("1.3");
    /** PDF 1.4 */
    public static final PDFVersion V_1_4 = new PDFVersion("1.4");
    /** PDF 1.7 */
    public static final PDFVersion V_1_7 = new PDFVersion("1.7");

    private String name;

    /**
     * Constructor to add a new named item.
     * @param name Name of the item.
     */
    private PDFVersion(String name) {
        this.name = name;
    }

    /** @return the name of the enum */
    public String getName() {
        return this.name;
    }

    /** @return the (real number) value of the version */
    public float floatValue() {
        return Float.parseFloat ( name );
    }

    /**
     * Determine if version supports a PDF/A mode.
     * @param mode a PDF/A mode object
     * @return true if specified mode is suppported
     */
    public boolean supportsMode ( PDFAMode mode ) {
        return mode.supportedByVersion ( this );
    }

    /**
     * Determine if version supports a PDF/X mode.
     * @param mode a PDF/X mode object
     * @return true if specified mode is suppported
     */
    public boolean supportsMode ( PDFXMode mode ) {
        return mode.supportedByVersion ( this );
    }

    /**
     * Returns the mode enum object given a version string.
     * @param s the version string ("1.3", "1.4", "1.7", ...)
     * @return the PDFVersion enum object
     * @throws IllegalArgumentException if unknown version string
     */
    public static PDFVersion valueOf(String s) throws IllegalArgumentException {
        if (V_1_3.getName().equals(s)) {
            return V_1_3;
        } else if (V_1_4.getName().equals(s)) {
            return V_1_4;
        } else if (V_1_7.getName().equals(s)) {
            return V_1_7;
        } else {
            throw new IllegalArgumentException ( "unknown or unsupported PDF format version: " + s );
        }
    }

    /** {@inheritDoc} */
    public String toString() {
        return name;
    }

}
