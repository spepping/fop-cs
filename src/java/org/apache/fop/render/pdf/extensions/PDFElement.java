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

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.util.ContentHandlerFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.w3c.dom.Document;

/**
 * Abstract base class for PDF extension element wherein extension attachment is a DOM instance
 * representing a PDF dictionary, i.e., pdf:catalog, pdf:page, and where each such object is bound
 * to an extension attachment object represented as a DOM document whose root element is constructed
 * from this element.
 */
public abstract class PDFElement extends PDFObj {

    /** transformer factory singleton */
    private static volatile TransformerFactory transformerFactory;

    /** dictionary */
    private PDFDictionary dictionary;

    /** extension attachment */
    private ExtensionAttachment attachment;

    /**
     * Explicit constructor.
     * @param parent parent of this node
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public PDFElement ( FONode parent ) {
        super ( parent );
    }

    /** {@inheritDoc} */
    public ContentHandlerFactory getContentHandlerFactory() {
        return new PDFDictionaryContentHandlerFactory();
    }

    /** {@inheritDoc} */
    public void notifyObjectBuilt ( Object obj ) throws SAXException {
        if ( obj != null ) {
            if ( obj instanceof Document ) {
                super.notifyObjectBuilt ( obj );
                toDictionary ( (Document) obj );
            } else if ( obj instanceof PDFDictionary ) {
                this.dictionary = (PDFDictionary) obj;
            }
        }
    }

    /**
     * Convert XML representation of dictionary to PDF dictionary itself,
     * notifying this object listener with result.
     */
    private void toDictionary ( Document doc ) throws SAXException {
        ContentHandler ch = ((PDFDictionaryContentHandlerFactory)
                             getContentHandlerFactory()).createDictionaryContentHandler();
        if ( ch instanceof ContentHandlerFactory.ObjectSource ) {
            ((ContentHandlerFactory.ObjectSource) ch ).setObjectBuiltListener ( this );
            toSAX ( ch );
        }
    }

    /** {@inheritDoc} */
    public ExtensionAttachment getExtensionAttachment() {
        if ( attachment == null ) {
            attachment = instantiateExtensionAttachment();
        }
        return attachment;
    }

    /**
     * Instantiates extension attachment object.
     * @return extension attachment
     */
    protected abstract ExtensionAttachment instantiateExtensionAttachment();

    /** {@inheritDoc} */
    public void toSAX ( ContentHandler handler ) throws SAXException {
        Source ts = new DOMSource ( getDOMDocument().getDocumentElement() );
        Result tr = new SAXResult ( handler );
        try {
            TransformerFactory f = getTransformerFactory();
            Transformer t = f.newTransformer();
            t.transform ( ts, tr );
        } catch ( TransformerException e ) {
            throw new SAXException ( "transformation failed: " + e.getMessage() );
        }
    }

    private synchronized TransformerFactory getTransformerFactory() {
        if ( transformerFactory == null ) {
            transformerFactory = SAXTransformerFactory.newInstance();
        }
        return transformerFactory;
    }

    /**
     * Obtain dictionary instance associated with this element.
     * @return dictionary object
     */
    public PDFDictionary getDictionary() {
        return dictionary;
    }

    static void addMappings ( Map map ) {
        PDFCatalogElement.addMappings ( map );
        PDFPageElement.addMappings ( map );
    }

}

