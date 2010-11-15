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

import java.util.Stack;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FOElementMapping;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFName;
import org.apache.fop.render.intermediate.IFConstants;
import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.DelegatingContentHandler;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

// CSOFF: WhitespaceAfterCheck
// CSOFF: LineLengthCheck

/**
 * Content handler factory for building PDF extension DOM instances that take the
 * form of a PDF dictionary.
 */
public class PDFDictionaryContentHandlerFactory implements ContentHandlerFactory {

    /** {@inheritDoc} */
    public String[] getSupportedNamespaces() {
        return new String[] { PDFElementMapping.NAMESPACE };
    }

    /** {@inheritDoc} */
    public ContentHandler createContentHandler() throws SAXException {
        return new DocumentHandler();
    }

    /**
     * Create a content handler that constructs a PDF dictionary object (as opposed to
     * a DOM representation thereof).
     * @return conttent handler
     * @throws SAXException in case of uncaught SAX exception
     */
    public ContentHandler createDictionaryContentHandler() throws SAXException {
        return new DictionaryHandler();
    }

    private static class DocumentHandler extends DelegatingContentHandler implements ContentHandlerFactory.ObjectSource {

        /** transformer factory singleton */
        private static SAXTransformerFactory transformerFactory;

        /** listener to receive object (document) built notification */
        private ObjectBuiltListener listener;

        /** document being (or already) constructed */
        private Document doc;

        /** transformer handler */
        private TransformerHandler handler;

        private DOMImplementation getDOMImplementation() {
            return ElementMapping.getDefaultDOMImplementation();
        }

        private void initializeDocument() throws SAXException {
            assert doc == null;
            try { 
                doc = getDOMImplementation().createDocument ( null, null, null );
            } catch ( DOMException e ) {
                throw new SAXException ( "can't create document: " + e.getMessage() );
            }
        }

        private void initializeDelegate() throws SAXException {
            if ( transformerFactory == null ) {
                transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            }
            if ( handler == null ) {
                try {
                    handler = transformerFactory.newTransformerHandler();
                    handler.setResult ( new DOMResult ( doc ) );
                } catch ( TransformerConfigurationException e ) {
                    throw new SAXException ( "can't create transformer handler: " + e.getMessage() );
                }
            }
            setDelegateContentHandler ( handler );
        }

        /** {@inheritDoc} */
        public void startDocument() throws SAXException {
            if ( doc == null ) {
                initializeDocument();
                initializeDelegate();
            }
            super.startDocument();
        }

        /** {@inheritDoc} */
        public void startElement ( String uri, String localName, String qName, Attributes atts ) throws SAXException {
            super.startElement ( uri, localName, qName, atts );
        }

        /** {@inheritDoc} */
        public void endDocument() throws SAXException {
            super.endDocument();
            if ( listener != null ) {
                listener.notifyObjectBuilt ( getObject() );
            }
        }

        /** {@inheritDoc} */
        public Object getObject() {
            return doc;
        }

        /** {@inheritDoc} */
        public void setObjectBuiltListener ( ObjectBuiltListener listener ) {
            this.listener = listener;
        }

    }

    private static class DictionaryHandler extends DefaultHandler implements ContentHandlerFactory.ObjectSource {

        /** dictionary entry type */
        private static final String ELT_DICTIONARY = "dictionary";
        /** name entry type */
        private static final String ELT_NAME = "name";
        /** string entry type */
        private static final String ELT_STRING = "string";
        /** boolean entry type */
        private static final String ELT_BOOLEAN = "boolean";
        /** number entry type */
        private static final String ELT_NUMBER = "number";
        /** key attribute */
        private static final String ATT_KEY = "key";

        /** listener to receive object (document) built notification */
        private ObjectBuiltListener listener;

        /** key of entry element being constructed */
        private String key;

        /** stack of dictionaries being constructed */
        private StringBuffer chars;

        /** stack of keys of dictionaries being constructed */
        private Stack dictionaryKeys;

        /** stack of dictionaries being constructed */
        private Stack dictionaries;

        /** top level dictionary */
        private PDFDictionary dictionary;

        /** {@inheritDoc} */
        public void startDocument() throws SAXException {
            chars = new StringBuffer();
            dictionaries = new Stack();
            dictionaryKeys = new Stack();
        }

        /** {@inheritDoc} */
        public void startElement ( String uri, String localName, String qName, Attributes atts ) throws SAXException {
            if ( ( uri != null ) && uri.equals ( PDFElementMapping.NAMESPACE ) ) {
                String k = atts.getValue ( ATT_KEY );
                if ( isEntryType ( localName ) && ( ( k == null ) || ( k.length() == 0 ) ) ) {
                    throw new SAXException ( "missing or empty '" + ATT_KEY + "' attribute" );
                }
                if ( isDictionaryType ( localName ) ) {
                    PDFDictionary d = new PDFDictionary();
                    dictionaries.push ( d );
                    dictionaryKeys.push ( k );
                } else if ( isPrimitiveType ( localName ) ) {
                    chars.setLength(0);
                    key = k;
                } else {
                    throw new SAXException ( "unknown or unsupported PDF extension element type" );
                }
            } else if ( uri.equals ( FOElementMapping.URI ) ) {
                throw new SAXException ( "unsupported FO namespace in this context" );
            } else if ( uri.equals ( IFConstants.NAMESPACE ) ) {
                throw new SAXException ( "unsupported IF namespace in this context" );
            } else {
                // ignore elements in other namespaces
            }
        }

        /** {@inheritDoc} */
        public void characters ( char[] ch, int start, int length ) {
            chars.append ( ch, start, length );
        }

        /** {@inheritDoc} */
        public void endElement ( String uri, String localName, String qName ) throws SAXException {
            // record top-level dictionary or add entry to current dictionary
            if ( ( uri != null ) && uri.equals ( PDFElementMapping.NAMESPACE ) ) {
                if ( isDictionaryType ( localName ) ) {
                    if ( ! dictionaries.empty() ) {
                        PDFDictionary d = (PDFDictionary) dictionaries.pop();
                        key = (String) dictionaryKeys.pop();
                        if ( dictionaries.empty() ) {
                            setType ( d, localName );
                            dictionary = d;
                        } else {
                            addEntry ( key, d );
                        }
                    }
                } else if ( isPrimitiveType ( localName ) ) {
                    addEntry ( key, createValue ( localName, chars.toString() ) );
                }
            }
            // reset element state
            key = null;
            chars.setLength(0);
        }

        /** {@inheritDoc} */
        public void endDocument() throws SAXException {
            if ( listener != null ) {
                listener.notifyObjectBuilt ( getObject() );
            }
        }

        /** {@inheritDoc} */
        public Object getObject() {
            return dictionary;
        }

        /** {@inheritDoc} */
        public void setObjectBuiltListener ( ObjectBuiltListener listener ) {
            this.listener = listener;
        }

        private boolean isDictionaryType ( String ln ) {
            return ln.equals ( PDFCatalogElement.ELEMENT )
                || ln.equals ( PDFPageElement.ELEMENT )
                || ln.equals ( ELT_DICTIONARY );
        }

        private boolean isEntryType ( String ln ) {
            return isPrimitiveType ( ln )
                || ln.equals ( ELT_DICTIONARY );
        }

        private boolean isPrimitiveType ( String ln ) {
            return ln.equals ( ELT_NAME )
                || ln.equals ( ELT_STRING )
                || ln.equals ( ELT_BOOLEAN )
                || ln.equals ( ELT_NUMBER );
        }

        private Object createValue ( String ln, String content ) {
            if ( ln.equals ( ELT_NAME ) ) {
                return new PDFName ( content );
            } else if ( ln.equals ( ELT_STRING ) ) {
                return content;
            } else if ( ln.equals ( ELT_BOOLEAN ) ) {
                return Boolean.valueOf ( content );
            } else if ( ln.equals ( ELT_NUMBER ) ) {
                Double v = Double.valueOf ( content );
                double d = v.doubleValue();
                double f = Math.floor ( d );
                if ( ( f == d ) && ( ( f >= (double) Integer.MIN_VALUE ) || ( f <= (double) Integer.MAX_VALUE ) ) ) {
                    return Integer.valueOf ( (int) f );
                } else {
                    return v;
                }
            }
            return null;
        }

        private void addEntry ( String key, Object value ) {
            PDFDictionary d = (PDFDictionary) dictionaries.peek();
            if ( d != null ) {
                if ( ( key != null ) && ( key.length() > 0 ) ) {
                    d.put ( key, value );
                }
            }
        }

        private void setType ( PDFDictionary d, String ln ) throws SAXException {
            String t;
            if ( ln.equals ( PDFCatalogElement.ELEMENT ) ) {
                t = "Catalog";
            } else if ( ln.equals ( PDFPageElement.ELEMENT ) ) {
                t = "Page";
            } else {
                t = null;
            }
            if ( t != null ) {
                PDFName n = (PDFName) d.get ( "Type" );
                if ( n != null ) {
                    if ( ! n.toString().equals ( t ) ) {
                        throw new SAXException ( "specified type '" + n.toString() + "' inconsistent with prescribed type '" + t + "'" );
                    }
                } else {
                    d.put ( "Type", new PDFName ( t ) );
                }
            }
        }

    }

}

