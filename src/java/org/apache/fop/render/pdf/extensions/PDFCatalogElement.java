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

import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFRoot;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/** class representing pdf:catalog element */
public class PDFCatalogElement extends PDFElement {

    /** name of element */
    static final String ELEMENT = "catalog";

    /**
     * Explicit constructor.
     * @param parent parent of this node
     */
    public PDFCatalogElement ( FONode parent ) {
        super ( parent );
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
     * Augment root (catalog) object using this catalog element extension.
     * @param root (catalog) whose dictionary is to be augmented from this catalog element extension
     */
    public void augmentRoot ( PDFRoot root ) {
        if ( root != null ) {
            root.augment ( getDictionary(), new CatalogCombiner() );
        }
    }

    private static class CatalogCombiner implements PDFDictionary.Combiner {
        /** {@inheritDoc} */
        public Object combine ( String name, Object vCur, Object vNew ) {
            if ( name.equals ( "Type" ) ) {
                if ( ( vNew == null ) || !vNew.toString().equals ( vCur.toString() ) ) {
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
            if ( name.equals ( "Pages" ) ) {    // CSOK: SimplifyBooleanReturn
                return false;
            } else {
                return true;
            }
        }
    }

    private class Attachment extends PDFExtensionAttachment {
        /** {@inheritDoc} */
        public PDFElement getElement() {
            return PDFCatalogElement.this;
        }
        /** {@inheritDoc} */
        public String getLocalName() {
            return PDFCatalogElement.this.getLocalName();
        }
        /** {@inheritDoc} */
        public void toSAX ( ContentHandler handler ) throws SAXException {
            PDFCatalogElement.this.toSAX ( handler );
        }
    }

    static void addMappings ( Map map ) {
        map.put ( ELEMENT, new Maker() );
    }

    static class Maker extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new PDFCatalogElement(parent);
        }
    }

}

