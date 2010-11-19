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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.util.XMLUtil;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Extension element for pdf:embedded-file.
 */
public class PDFEmbeddedFileElement extends FONode {

    /** name of element */
    public static final String ELEMENT = "embedded-file";
    /** name of file to be embedded */
    private static final String ATT_FILENAME = "filename";
    /** source of file to be embedded (URI) */
    private static final String ATT_SRC = "src";
    /** a description of the file to be embedded */
    private static final String ATT_DESC = "desc";

    /** Extension attachment. */
    protected ExtensionAttachment attachment;

    /** description attribute (optional) */
    private String desc;

    /** source name attribute */
    private String src;

    /** filename attribute */
    private String filename;

    /**
     * Main constructor
     * @param parent parent FO node
     */
    protected PDFEmbeddedFileElement(FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    public void processNode(String elementName, Locator locator,
                            Attributes attlist, PropertyList propertyList)
                                throws FOPException {
        // process optional desc (description) attribute
        String desc = attlist.getValue(ATT_DESC);
        if ( desc != null ) {
            this.desc = desc;
        } else {
            // description is optional
        }
        // process mandatory src (source) attribute
        String src = attlist.getValue(ATT_SRC);
        if ( src != null ) {
            this.src = URISpecification.getURL(src);
        } else {
            missingAttributeError(ATT_SRC);
        }
        // process optional filename attribute
        String filename = attlist.getValue(ATT_FILENAME);
        if ( ( filename == null ) || ( filename.length() == 0 ) ) {
            try {
                URI uri = new URI(src);
                String path = uri.getPath();
                int idx = path.lastIndexOf('/');
                if (idx > 0) {
                    path = path.substring(idx + 1);
                }
                this.filename = path;
            } catch (URISyntaxException e) {
                throw new FOPException ( e.getMessage() );
            }
        } else {
            this.filename = filename;
        }
    }

    /** {@inheritDoc} */
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        if (parent.getNameId() != Constants.FO_DECLARATIONS) {
            invalidChildError(getLocator(), parent.getName(), getNamespaceURI(), getName(),
                "rule.childOfDeclarations");
        }
    }

    /** {@inheritDoc} */
    protected void endOfNode() throws FOPException {
        super.endOfNode();
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return ELEMENT;
    }

    /** {@inheritDoc} */
    public String getNormalNamespacePrefix() {
        return PDFElementMapping.NAMESPACE_PREFIX;
    }

    /** {@inheritDoc} */
    public String getNamespaceURI() {
        return PDFElementMapping.NAMESPACE;
    }

    /** {@inheritDoc} */
    public ExtensionAttachment getExtensionAttachment() {
        if ( attachment == null ) {
            attachment = instantiateExtensionAttachment();
        }
        return attachment;
    }

    /** {@inheritDoc} */
    protected ExtensionAttachment instantiateExtensionAttachment() {
        return new Attachment();
    }

    /** extension attachcment correponding to pdf:embedded-file */
    public class Attachment extends PDFExtensionAttachment {
        /** {@inheritDoc} */
        public String getLocalName() {
            return PDFEmbeddedFileElement.this.getLocalName();
        }
        /** {@inheritDoc} */
        protected Attributes getAttributes() {
            AttributesImpl atts = new AttributesImpl();
            if (desc != null && desc.length() > 0) {
                XMLUtil.addAttribute ( atts, ATT_DESC, desc );
            }
            if (src != null && src.length() > 0) {
                XMLUtil.addAttribute ( atts, ATT_SRC, src );
            }
            if (filename != null && filename.length() > 0) {
                XMLUtil.addAttribute ( atts, ATT_FILENAME, filename );
            }
            return atts;
        }
        /** @return the file description */
        public String getDescription() {
            return desc;
        }
        /** @return the source URI */
        public String getSource() {
            return src;
        }
        /** @return the file name */
        public String getFilename() {
            return filename;
        }
    }

    static void addMappings ( Map map ) {
        map.put ( ELEMENT, new Maker() );
    }

    static class Maker extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new PDFEmbeddedFileElement(parent);
        }
    }

}
