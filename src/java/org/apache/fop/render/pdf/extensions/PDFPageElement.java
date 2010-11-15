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

package org.apache.fop.render.pdf.extensions;

import java.util.Map;
import java.util.Vector;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.fo.extensions.FilterPageExtensionAttachment;
import org.apache.fop.fo.pagination.SimplePageMaster;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFPage;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

// CSOFF: WhitespaceAfterCheck
// CSOFF: LineLengthCheck

/** class representing pdf:page element */
public class PDFPageElement extends PDFElement {

    /** name of element */
    static final String ELEMENT = "page";
    /** name of page-number attribute */
    private static final String ATT_PAGE_NUMBERS = "page-numbers";

    /** array page numbers expressed as closed intervals [S,E] */
    private int[][] pageIntervals;

    /**
     * Explicit constructor.
     * @param parent parent of this node
     */
    public PDFPageElement ( FONode parent ) {
        super ( parent );
    }

    /** {@inheritDoc} */
    public void processNode ( String elementName, Locator locator, Attributes attlist, PropertyList pList ) throws FOPException {
        // perform default processing
        super.processNode ( elementName, locator, attlist, pList );
        // set page number
        String pn = attlist.getValue ( ATT_PAGE_NUMBERS );
        if ( ( pn == null ) || ( pn.length() == 0 ) ) {
            this.missingAttributeError ( ATT_PAGE_NUMBERS );
        } else {
            try {
                this.pageIntervals = parsePageNumbers ( pn );
            } catch ( SAXException e ) {
                throw new FOPException ( e.getMessage() );
            }
        }
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return ELEMENT;
    }

    /** {@inheritDoc} */
    protected ExtensionAttachment instantiateExtensionAttachment() {
        return new Attachment();
    }

    /**
     * Determine if page index matches this page extension element.
     * @param pageIndex to match
     * @return true if page index matches a page number associated with this page extension element
     */
    public boolean matchesPageIndex ( int pageIndex ) {
        return inPageNumbers ( pageIndex + 1 );
    }

    /**
     * Augment page object using this page element extension.
     * @param page whose dictionary is to be augmented from this page element extension
     */
    public void augmentPage ( PDFPage page ) {
        if ( page != null ) {
            page.augment ( getDictionary(), new PageCombiner() );
        }
    }

    private int[][] parsePageNumbers ( String s ) throws SAXException {
        Vector v = new Vector();
        String[] ranges = s.split(",");
        for ( int i = 0, n = ranges.length; i < n; i++ ) {
            v.add ( parsePageNumberInterval ( ranges [ i ] ) );
        }
        return (int[][]) v.toArray ( new int [ v.size() ] [] );
    }

    private int[] parsePageNumberInterval ( String interval ) throws SAXException {
        int s, e;
        if ( interval.equals("*") ) {
            s = 1;
            e = -1;
        } else {
            String[] ca = interval.split ( "-" );
            if ( ca.length == 1 ) {
                s = parsePageNumberIntervalComponent ( ca[0] );
                e = s;
            } else if ( ca.length == 2 ) {
                s = parsePageNumberIntervalComponent ( ca[0] );
                if ( ca[1].equals("LAST") ) {
                    e = -1;
                } else {
                    e = parsePageNumberIntervalComponent ( ca[1] );
                }
            } else {
                throw new SAXException ( "bad page number interval syntax '" + interval + "', must contain one integer or two integers separated by '-'" );
            }
        }
        if ( s <= 0 ) {
            throw new SAXException ( "bad page number interval start component '" + interval + "', must be greater than zero" );
        } else if ( ( e >= 0 ) && ( e < s ) ) {
            throw new SAXException ( "bad page number interval end component '" + interval + "', must be negative or greater than or equal to start component" );
        }
        return new int[] { s, e };
    }

    private int parsePageNumberIntervalComponent ( String component ) throws SAXException {
        try {
            return Integer.parseInt ( component );
        } catch ( NumberFormatException e ) {
            throw new SAXException ( "bad page number interval component syntax '" + component + "', must contain integer" );
        }
    }

    private boolean inPageNumbers ( int pn ) {
        if ( pageIntervals == null ) {
            return false;
        } else {
            for ( int i = 0, n = pageIntervals.length; i < n; i++ ) {
                int[] pi = pageIntervals [ i ];
                int s = pi[0];
                int e = pi[1];
                if ( pn >= s ) {
                    if ( ( e < 0 ) || ( pn <= e ) ) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private class PageCombiner implements PDFDictionary.Combiner {
        /** {@inheritDoc} */
        public Object combine ( String name, Object vCur, Object vNew ) {
            if ( name.equals ( "Type" ) ) {
                if ( ( vNew == null ) || ! vNew.toString().equals ( vCur.toString() ) ) {
                    throw new IllegalStateException ( "inconsistent Type entry" );
                } else {
                    return vCur;
                }
            } else if ( disallowCombination ( name ) ) {
                return vCur;
            } else {
                return vNew;
            }
        }
        private boolean disallowCombination ( String name ) {
            if ( name.equals ( "Kids" ) ) {
                return false;
            } else if ( name.equals ( "Count" ) ) {     // CSOK: SimplifyBooleanReturn
                return false;
            } else {
                return true;
            }
        }
    }

    private class Attachment extends PDFExtensionAttachment implements FilterPageExtensionAttachment {
        /** {@inheritDoc} */
        public PDFElement getElement() {
            return PDFPageElement.this;
        }
        /** {@inheritDoc} */
        public String getLocalName() {
            return PDFPageElement.this.getLocalName();
        }
        /** {@inheritDoc} */
        public void toSAX ( ContentHandler handler ) throws SAXException {
            PDFPageElement.this.toSAX ( handler );
        }
        /** {@inheritDoc} */
        public boolean filter ( SimplePageMaster spm, int pageNumber ) {
            if ( inPageNumbers ( pageNumber ) ) {       // CSOK: SimplifyBooleanReturn
                return false;
            } else {
                return true;
            }
        }
    }

    static void addMappings ( Map map ) {
        map.put ( ELEMENT, new Maker() );
    }

    static class Maker extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new PDFPageElement(parent);
        }
    }

}

