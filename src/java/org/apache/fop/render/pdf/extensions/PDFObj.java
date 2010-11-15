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
import org.apache.fop.fo.XMLObj;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Instantiable class for PDF extension elements which are modeled as XML objects, namely, all pdf:*
 * except for pdf:embedded-file, and which each such object may express a common 'key' attribute.
 */
public class PDFObj extends XMLObj {

    /**
     * Explicit constructor.
     * @param parent parent of this node
     */
    public PDFObj ( FONode parent ) {
        super ( parent );
    }

    /** {@inheritDoc} */
    public String getNamespaceURI() {
        return PDFElementMapping.NAMESPACE;
    }

    /** {@inheritDoc} */
    public String getNormalNamespacePrefix() {
        return PDFElementMapping.NAMESPACE_PREFIX;
    }

    static void addMappings ( Map map ) {
        map.put ( ElementMapping.DEFAULT, new Maker() );
    }

    static class Maker extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new PDFObj(parent);
        }
    }

}

