/*
 * Copyright 2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.apache.fop.svg;

import java.awt.geom.AffineTransform;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.UserAgent;
import org.apache.fop.fonts.FontInfo;

/**
 * BridgeContext which registers the custom bridges for PDF output.
 */
public class PDFBridgeContext extends BridgeContext {
    
    /** The font list. */
    private final FontInfo fontInfo;

    private AffineTransform linkTransform;
    
    /**
     * Constructs a new bridge context.
     * @param userAgent the user agent
     * @param fontInfo the font list for the text painter, may be null in which case text is
     *                 painted as shapes
     * @param linkTransform AffineTransform to properly place links, may be null
     */
    public PDFBridgeContext(UserAgent userAgent, FontInfo fontInfo, 
                AffineTransform linkTransform) {
        super(userAgent);
        this.fontInfo = fontInfo;
        this.linkTransform = linkTransform;
    }

    /**
     * Constructs a new bridge context.
     * @param userAgent the user agent
     * @param fontInfo the font list for the text painter, may be null in which case text is
     *                 painted as shapes
     */
    public PDFBridgeContext(UserAgent userAgent, FontInfo fontInfo) {
        this(userAgent, fontInfo, null);
    }

    /** @see org.apache.batik.bridge.BridgeContext#registerSVGBridges() */
    public void registerSVGBridges() {
        super.registerSVGBridges();

        if (fontInfo != null) {
            putBridge(new PDFTextElementBridge(fontInfo));
        }

        PDFAElementBridge pdfAElementBridge = new PDFAElementBridge();
        if (linkTransform != null) {
            pdfAElementBridge.setCurrentTransform(linkTransform);
        } else {
            pdfAElementBridge.setCurrentTransform(new AffineTransform());
        }
        putBridge(pdfAElementBridge);

        putBridge(new PDFImageElementBridge());
    }
    
}