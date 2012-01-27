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

// Java
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/* image support modified from work of BoBoGi */
/* font support based on work by Takayuki Takeuchi */

/**
 * Class representing a PDF document.
 *
 * The document is built up by calling various methods and then finally
 * output to given filehandle using output method.
 *
 * A PDF document consists of a series of numbered objects preceded by a
 * header and followed by an xref table and trailer. The xref table
 * allows for quick access to objects by listing their character
 * positions within the document. For this reason the PDF document must
 * keep track of the character position of each object.  The document
 * also keeps direct track of the /Root, /Info and /Resources objects.
 *
 * Modified by Mark Lillywhite, mark-fop@inomial.com. The changes
 * involve: ability to output pages one-at-a-time in a streaming
 * fashion (rather than storing them all for output at the end);
 * ability to write the /Pages object after writing the rest
 * of the document; ability to write to a stream and flush
 * the object list; enhanced trailer output; cleanups.
 *
 */
public class PDFDocument {

    private static final Long LOCATION_PLACEHOLDER = new Long(0);

    /** Integer constant to represent PDF 1.3 */
    public static final int PDF_VERSION_1_3 = 3;

    /** Integer constant to represent PDF 1.4 */
    public static final int PDF_VERSION_1_4 = 4;

    /** the encoding to use when converting strings to PDF commands */
    public static final String ENCODING = "ISO-8859-1";

    /** the counter for object numbering */
    protected int objectcount = 0;

    /** the logger instance */
    private Log log = LogFactory.getLog("org.apache.fop.pdf");

    /** the current character position */
    private long position = 0;

    /** character position of xref table */
    private long xref;

    /** the character position of each object */
    private List<Long> location = new ArrayList<Long>();

    /** List of objects to write in the trailer */
    private List trailerObjects = new ArrayList();

    /** the objects themselves */
    private List objects = new LinkedList();

    /** Indicates what PDF version is active */
    private int pdfVersion = PDF_VERSION_1_4;

    /** Indicates which PDF profiles are active (PDF/A, PDF/X etc.) */
    private PDFProfile pdfProfile = new PDFProfile(this);

    /** the /Root object */
    private PDFRoot root;

    /** The root outline object */
    private PDFOutline outlineRoot = null;

    /** The /Pages object (mark-fop@inomial.com) */
    private PDFPages pages;

    /** the /Info object */
    private PDFInfo info;

    /** the /Resources object */
    private PDFResources resources;

    /** the document's encryption, if it exists */
    private PDFEncryption encryption;

    /** the colorspace (0=RGB, 1=CMYK) */
    private PDFDeviceColorSpace colorspace
        = new PDFDeviceColorSpace(PDFDeviceColorSpace.DEVICE_RGB);

    /** the counter for Pattern name numbering (e.g. 'Pattern1') */
    private int patternCount = 0;

    /** the counter for Shading name numbering */
    private int shadingCount = 0;

    /** the counter for XObject numbering */
    private int xObjectCount = 0;

    /** the {@link PDFXObject}s map */
    /* TODO: Should be modified (works only for image subtype) */
    private Map xObjectsMap = new HashMap();

    /** The {@link PDFFont} map */
    private Map fontMap = new HashMap();

    /** The {@link PDFFilter} map */
    private Map filterMap = new HashMap();

    /** List of {@link PDFGState}s. */
    private List gstates = new ArrayList();

    /** List of {@link PDFFunction}s. */
    private List functions = new ArrayList();

    /** List of {@link PDFShading}s. */
    private List shadings = new ArrayList();

    /** List of {@link PDFPattern}s. */
    private List patterns = new ArrayList();

    /** List of {@link PDFLink}s. */
    private List links = new ArrayList();

    /** List of {@link PDFDestination}s. */
    private List destinations;

    /** List of {@link PDFFileSpec}s. */
    private List filespecs = new ArrayList();

    /** List of {@link PDFGoToRemote}s. */
    private List gotoremotes = new ArrayList();

    /** List of {@link PDFGoTo}s. */
    private List gotos = new ArrayList();

    /** List of {@link PDFLaunch}es. */
    private List launches = new ArrayList();

    /**
     * The PDFDests object for the name dictionary.
     * Note: This object is not a list.
     */
    private PDFDests dests;

    private PDFFactory factory;

    private boolean encodingOnTheFly = true;

    private FileIDGenerator fileIDGenerator;

    /**
     * Creates an empty PDF document.
     *
     * The constructor creates a /Root and /Pages object to
     * track the document but does not write these objects until
     * the trailer is written. Note that the object ID of the
     * pages object is determined now, and the xref table is
     * updated later. This allows Pages to refer to their
     * Parent before we write it out.
     *
     * @param prod the name of the producer of this pdf document
     */
    public PDFDocument(String prod) {

        this.factory = new PDFFactory(this);

        /* create the /Root, /Info and /Resources objects */
        this.pages = getFactory().makePages();

        // Create the Root object
        this.root = getFactory().makeRoot(this.pages);

        // Create the Resources object
        this.resources = getFactory().makeResources();

        // Make the /Info record
        this.info = getFactory().makeInfo(prod);
    }

    /**
     * @return the integer representing the active PDF version
     *          (one of PDFDocument.PDF_VERSION_*)
     */
    public int getPDFVersion() {
        return this.pdfVersion;
    }

    /** @return the String representing the active PDF version */
    public String getPDFVersionString() {
        switch (getPDFVersion()) {
        case PDF_VERSION_1_3:
            return "1.3";
        case PDF_VERSION_1_4:
            return "1.4";
        default:
            throw new IllegalStateException("Unsupported PDF version selected");
        }
    }

    /** @return the PDF profile currently active. */
    public PDFProfile getProfile() {
        return this.pdfProfile;
    }

    /**
     * Returns the factory for PDF objects.
     *
     * @return the {@link PDFFactory} object
     */
    public PDFFactory getFactory() {
        return this.factory;
    }

    /**
     * Indicates whether stream encoding on-the-fly is enabled. If enabled
     * stream can be serialized without the need for a buffer to merely
     * calculate the stream length.
     *
     * @return <code>true</code> if on-the-fly encoding is enabled
     */
    public boolean isEncodingOnTheFly() {
        return this.encodingOnTheFly;
    }

    /**
     * Converts text to a byte array for writing to a PDF file.
     *
     * @param text text to convert/encode
     * @return the resulting <code>byte</code> array
     */
    public static byte[] encode(String text) {
        try {
            return text.getBytes(ENCODING);
        } catch (UnsupportedEncodingException uee) {
            return text.getBytes();
        }
    }

    /**
     * Flushes the given text buffer to an output stream with the right encoding and resets
     * the text buffer. This is used to efficiently switch between outputting text and binary
     * content.
     * @param textBuffer the text buffer
     * @param out the output stream to flush the text content to
     * @throws IOException if an I/O error occurs while writing to the output stream
     */
    public static void flushTextBuffer(StringBuilder textBuffer, OutputStream out)
            throws IOException {
        out.write(encode(textBuffer.toString()));
        textBuffer.setLength(0);
    }

    /**
     * Sets the producer of the document.
     *
     * @param producer string indicating application producing the PDF
     */
    public void setProducer(String producer) {
        this.info.setProducer(producer);
    }

    /**
      * Sets the creation date of the document.
      *
      * @param date Date to be stored as creation date in the PDF.
      */
    public void setCreationDate(Date date) {
        this.info.setCreationDate(date);
    }

    /**
      * Sets the creator of the document.
      *
      * @param creator string indicating application creating the document
      */
    public void setCreator(String creator) {
        this.info.setCreator(creator);
    }

    /**
     * Sets the filter map to use for filters in this document.
     *
     * @param map the map of filter lists for each stream type
     */
    public void setFilterMap(Map map) {
        this.filterMap = map;
    }

    /**
     * Returns the {@link PDFFilter}s map used for filters in this document.
     *
     * @return the map of filters being used
     */
    public Map getFilterMap() {
        return this.filterMap;
    }

    /**
     * Returns the {@link PDFPages} object associated with the root object.
     *
     * @return the {@link PDFPages} object
     */
    public PDFPages getPages() {
        return this.pages;
    }

    /**
     * Get the {@link PDFRoot} object for this document.
     *
     * @return the {@link PDFRoot} object
     */
    public PDFRoot getRoot() {
        return this.root;
    }

    /**
     * Makes sure a Lang entry has been set on the document catalog, setting it
     * to a default value if necessary. When accessibility is enabled the
     * language must be specified for any text element in the document.
     */
    public void enforceLanguageOnRoot() {
        if (root.getLanguage() == null) {
            String fallbackLanguage;
            if (getProfile().getPDFAMode().isPDFA1LevelA()) {
                //According to Annex B of ISO-19005-1:2005(E), section B.2
                fallbackLanguage = "x-unknown";
            } else {
                //No language has been set on the first page-sequence, so fall back to "en".
                fallbackLanguage = "en";
            }
            root.setLanguage(fallbackLanguage);
        }
    }

    /**
     * Get the {@link PDFInfo} object for this document.
     *
     * @return the {@link PDFInfo} object
     */
    public PDFInfo getInfo() {
        return this.info;
    }

    /**
     * Registers a {@link PDFObject} in this PDF document.
     * The object is assigned a new object number.
     *
     * @param obj {@link PDFObject} to add
     * @return the added {@link PDFObject} added (with its object number set)
     */
    public PDFObject registerObject(PDFObject obj) {
        assignObjectNumber(obj);
        addObject(obj);
        return obj;
    }

    /**
     * Assigns the {@link PDFObject} an object number,
     * and sets the parent of the {@link PDFObject} to this document.
     *
     * @param obj {@link PDFObject} to assign a number to
     */
    public void assignObjectNumber(PDFObject obj) {
        if (obj == null) {
            throw new NullPointerException("obj must not be null");
        }
        if (obj.hasObjectNumber()) {
            throw new IllegalStateException(
                "Error registering a PDFObject: "
                    + "PDFObject already has an object number");
        }
        PDFDocument currentParent = obj.getDocument();
        if (currentParent != null && currentParent != this) {
            throw new IllegalStateException(
                "Error registering a PDFObject: "
                    + "PDFObject already has a parent PDFDocument");
        }

        obj.setObjectNumber(++this.objectcount);

        if (currentParent == null) {
            obj.setDocument(this);
        }
    }

    /**
     * Adds a {@link PDFObject} to this document.
     * The object <em>MUST</em> have an object number assigned.
     *
     * @param obj {@link PDFObject} to add
     */
    public void addObject(PDFObject obj) {
        if (obj == null) {
            throw new NullPointerException("obj must not be null");
        }
        if (!obj.hasObjectNumber()) {
            throw new IllegalStateException(
                "Error adding a PDFObject: "
                    + "PDFObject doesn't have an object number");
        }

        //Add object to list
        this.objects.add(obj);

        //Add object to special lists where necessary
        if (obj instanceof PDFFunction) {
            this.functions.add(obj);
        }
        if (obj instanceof PDFShading) {
            final String shadingName = "Sh" + (++this.shadingCount);
            ((PDFShading)obj).setName(shadingName);
            this.shadings.add(obj);
        }
        if (obj instanceof PDFPattern) {
            final String patternName = "Pa" + (++this.patternCount);
            ((PDFPattern)obj).setName(patternName);
            this.patterns.add(obj);
        }
        if (obj instanceof PDFFont) {
            final PDFFont font = (PDFFont)obj;
            this.fontMap.put(font.getName(), font);
        }
        if (obj instanceof PDFGState) {
            this.gstates.add(obj);
        }
        if (obj instanceof PDFPage) {
            this.pages.notifyKidRegistered((PDFPage)obj);
        }
        if (obj instanceof PDFLaunch) {
            this.launches.add(obj);
        }
        if (obj instanceof PDFLink) {
            this.links.add(obj);
        }
        if (obj instanceof PDFFileSpec) {
            this.filespecs.add(obj);
        }
        if (obj instanceof PDFGoToRemote) {
            this.gotoremotes.add(obj);
        }
    }

    /**
     * Add trailer object.
     * Adds an object to the list of trailer objects.
     *
     * @param obj the PDF object to add
     */
    public void addTrailerObject(PDFObject obj) {
        this.trailerObjects.add(obj);

        if (obj instanceof PDFGoTo) {
            this.gotos.add(obj);
        }
    }

    /**
     * Apply the encryption filter to a PDFStream if encryption is enabled.
     *
     * @param stream PDFStream to encrypt
     */
    public void applyEncryption(AbstractPDFStream stream) {
        if (isEncryptionActive()) {
            this.encryption.applyFilter(stream);
        }
    }

    /**
     * Enables PDF encryption.
     *
     * @param params The encryption parameters for the pdf file
     */
    public void setEncryption(PDFEncryptionParams params) {
        getProfile().verifyEncryptionAllowed();
        fileIDGenerator = FileIDGenerator.getRandomFileIDGenerator();
        this.encryption = PDFEncryptionManager.newInstance(++this.objectcount, params, this);
        if (this.encryption != null) {
            PDFObject pdfObject = (PDFObject)this.encryption;
            addTrailerObject(pdfObject);
        } else {
            log.warn(
                "PDF encryption is unavailable. PDF will be "
                    + "generated without encryption.");
        }
    }

    /**
     * Indicates whether encryption is active for this PDF or not.
     *
     * @return boolean True if encryption is active
     */
    public boolean isEncryptionActive() {
        return this.encryption != null;
    }

    /**
     * Returns the active Encryption object.
     *
     * @return the Encryption object
     */
    public PDFEncryption getEncryption() {
        return this.encryption;
    }

    private Object findPDFObject(List list, PDFObject compare) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            PDFObject obj = (PDFObject) iter.next();
            if (compare.contentEquals(obj)) {
                return obj;
            }
        }
        return null;
    }

    /**
     * Looks through the registered functions to see if one that is equal to
     * a reference object exists
     *
     * @param compare reference object
     * @return the function if it was found, null otherwise
     */
    protected PDFFunction findFunction(PDFFunction compare) {
        return (PDFFunction)findPDFObject(this.functions, compare);
    }

    /**
     * Looks through the registered shadings to see if one that is equal to
     * a reference object exists
     *
     * @param compare reference object
     * @return the shading if it was found, null otherwise
     */
    protected PDFShading findShading(PDFShading compare) {
        return (PDFShading)findPDFObject(this.shadings, compare);
    }

    /**
     * Find a previous pattern.
     * The problem with this is for tiling patterns the pattern
     * data stream is stored and may use up memory, usually this
     * would only be a small amount of data.
     *
     * @param compare reference object
     * @return the shading if it was found, null otherwise
     */
    protected PDFPattern findPattern(PDFPattern compare) {
        return (PDFPattern)findPDFObject(this.patterns, compare);
    }

    /**
     * Finds a font.
     *
     * @param fontname name of the font
     * @return PDFFont the requested font, null if it wasn't found
     */
    protected PDFFont findFont(String fontname) {
        return (PDFFont)this.fontMap.get(fontname);
    }

    /**
     * Finds a named destination.
     *
     * @param compare reference object to use as search template
     * @return the link if found, null otherwise
     */
    protected PDFDestination findDestination(PDFDestination compare) {
        int index = getDestinationList().indexOf(compare);
        if (index >= 0) {
            return (PDFDestination)getDestinationList().get(index);
        } else {
            return null;
        }
    }

    /**
     * Finds a link.
     *
     * @param compare reference object to use as search template
     * @return the link if found, null otherwise
     */
    protected PDFLink findLink(PDFLink compare) {
        return (PDFLink)findPDFObject(this.links, compare);
    }

    /**
     * Finds a file spec.
     *
     * @param compare reference object to use as search template
     * @return the file spec if found, null otherwise
     */
    protected PDFFileSpec findFileSpec(PDFFileSpec compare) {
        return (PDFFileSpec)findPDFObject(this.filespecs, compare);
    }

    /**
     * Finds a goto remote.
     *
     * @param compare reference object to use as search template
     * @return the goto remote if found, null otherwise
     */
    protected PDFGoToRemote findGoToRemote(PDFGoToRemote compare) {
        return (PDFGoToRemote)findPDFObject(this.gotoremotes, compare);
    }

    /**
     * Finds a goto.
     *
     * @param compare reference object to use as search template
     * @return the goto if found, null otherwise
     */
    protected PDFGoTo findGoTo(PDFGoTo compare) {
        return (PDFGoTo)findPDFObject(this.gotos, compare);
    }

    /**
     * Finds a launch.
     *
     * @param compare reference object to use as search template
     * @return the launch if found, null otherwise
     */
    protected PDFLaunch findLaunch(PDFLaunch compare) {
        return (PDFLaunch) findPDFObject(this.launches, compare);
    }

    /**
     * Looks for an existing GState to use
     *
     * @param wanted requested features
     * @param current currently active features
     * @return the GState if found, null otherwise
     */
    protected PDFGState findGState(PDFGState wanted, PDFGState current) {
        PDFGState poss;
        Iterator iter = this.gstates.iterator();
        while (iter.hasNext()) {
            PDFGState avail = (PDFGState)iter.next();
            poss = new PDFGState();
            poss.addValues(current);
            poss.addValues(avail);
            if (poss.equals(wanted)) {
                return avail;
            }
        }
        return null;
    }

    /**
     * Returns the PDF color space object.
     *
     * @return the color space
     */
    public PDFDeviceColorSpace getPDFColorSpace() {
        return this.colorspace;
    }

    /**
     * Returns the color space.
     *
     * @return the color space
     */
    public int getColorSpace() {
        return getPDFColorSpace().getColorSpace();
    }

    /**
     * Set the color space.
     * This is used when creating gradients.
     *
     * @param theColorspace the new color space
     */
    public void setColorSpace(int theColorspace) {
        this.colorspace.setColorSpace(theColorspace);
    }

    /**
     * Returns the font map for this document.
     *
     * @return the map of fonts used in this document
     */
    public Map getFontMap() {
        return this.fontMap;
    }

    /**
     * Resolve a URI.
     *
     * @param uri the uri to resolve
     * @throws java.io.FileNotFoundException if the URI could not be resolved
     * @return the InputStream from the URI.
     */
    protected InputStream resolveURI(String uri)
        throws java.io.FileNotFoundException {
        try {
            /* TODO: Temporary hack to compile, improve later */
            return new java.net.URL(uri).openStream();
        } catch (Exception e) {
            throw new java.io.FileNotFoundException(
                "URI could not be resolved (" + e.getMessage() + "): " + uri);
        }
    }

    /**
     * Get an image from the image map.
     *
     * @param key the image key to look for
     * @return the image or PDFXObject for the key if found
     * @deprecated Use getXObject instead (so forms are treated in the same way)
     */
    @Deprecated
    public PDFImageXObject getImage(String key) {
        return (PDFImageXObject)this.xObjectsMap.get(key);
    }

    /**
     * Get an XObject from the image map.
     *
     * @param key the XObject key to look for
     * @return the PDFXObject for the key if found
     */
    public PDFXObject getXObject(String key) {
        return (PDFXObject)this.xObjectsMap.get(key);
    }

    /**
     * Gets the PDFDests object (which represents the /Dests entry).
     *
     * @return the PDFDests object (which represents the /Dests entry).
     */
    public PDFDests getDests() {
        return this.dests;
    }

    /**
     * Adds a destination to the document.
     * @param destination the destination object
     */
    public void addDestination(PDFDestination destination) {
        if (this.destinations == null) {
            this.destinations = new ArrayList();
        }
        this.destinations.add(destination);
    }

    /**
     * Gets the list of named destinations.
     *
     * @return the list of named destinations.
     */
    public List getDestinationList() {
        if (hasDestinations()) {
            return this.destinations;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Gets whether the document has named destinations.
     *
     * @return whether the document has named destinations.
     */
    public boolean hasDestinations() {
        return this.destinations != null && !this.destinations.isEmpty();
    }

    /**
     * Add an image to the PDF document.
     * This adds an image to the PDF objects.
     * If an image with the same key already exists it will return the
     * old {@link PDFXObject}.
     *
     * @param res the PDF resource context to add to, may be null
     * @param img the PDF image to add
     * @return the PDF XObject that references the PDF image data
     */
    public PDFImageXObject addImage(PDFResourceContext res, PDFImage img) {
        // check if already created
        String key = img.getKey();
        PDFImageXObject xObject = (PDFImageXObject)this.xObjectsMap.get(key);
        if (xObject != null) {
            if (res != null) {
                res.getPDFResources().addXObject(xObject);
            }
            return xObject;
        }

        // setup image
        img.setup(this);
        // create a new XObject
        xObject = new PDFImageXObject(++this.xObjectCount, img);
        registerObject(xObject);
        this.resources.addXObject(xObject);
        if (res != null) {
            res.getPDFResources().addXObject(xObject);
        }
        this.xObjectsMap.put(key, xObject);
        return xObject;
    }

    /**
     * Add a form XObject to the PDF document.
     * This adds a Form XObject to the PDF objects.
     * If a Form XObject with the same key already exists it will return the
     * old {@link PDFFormXObject}.
     *
     * @param res the PDF resource context to add to, may be null
     * @param cont the PDF Stream contents of the Form XObject
     * @param formres a reference to the PDF Resources for the Form XObject data
     * @param key the key for the object
     * @return the PDF Form XObject that references the PDF data
     */
    public PDFFormXObject addFormXObject(
        PDFResourceContext res,
        PDFStream cont,
        PDFReference formres,
        String key) {

        // check if already created
        PDFFormXObject xObject = (PDFFormXObject)xObjectsMap.get(key);
        if (xObject != null) {
            if (res != null) {
                res.getPDFResources().addXObject(xObject);
            }
            return xObject;
        }

        xObject = new PDFFormXObject(
                ++this.xObjectCount,
                cont,
                formres);
        registerObject(xObject);
        this.resources.addXObject(xObject);
        if (res != null) {
            res.getPDFResources().addXObject(xObject);
        }
        this.xObjectsMap.put(key, xObject);
        return xObject;
    }

    /**
     * Get the root Outlines object. This method does not write
     * the outline to the PDF document, it simply creates a
     * reference for later.
     *
     * @return the PDF Outline root object
     */
    public PDFOutline getOutlineRoot() {
        if (this.outlineRoot != null) {
            return this.outlineRoot;
        }

        this.outlineRoot = new PDFOutline(null, null, true);
        assignObjectNumber(this.outlineRoot);
        addTrailerObject(this.outlineRoot);
        this.root.setRootOutline(this.outlineRoot);
        return this.outlineRoot;
    }

    /**
     * Get the /Resources object for the document
     *
     * @return the /Resources object
     */
    public PDFResources getResources() {
        return this.resources;
    }

    /**
     * Ensure there is room in the locations xref for the number of
     * objects that have been created.
     * @param objidx    the object's index
     * @param position  the position
     */
    private void setLocation(int objidx, long position) {
        while (this.location.size() <= objidx) {
            this.location.add(LOCATION_PLACEHOLDER);
        }
        this.location.set(objidx, position);
    }

    /**
     * Writes out the entire document
     *
     * @param stream the OutputStream to output the document to
     * @throws IOException if there is an exception writing to the output stream
     */
    public void output(OutputStream stream) throws IOException {
        //Write out objects until the list is empty. This approach (used with a
        //LinkedList) allows for output() methods to create and register objects
        //on the fly even during serialization.
        while (this.objects.size() > 0) {
            /* Retrieve first */
            PDFObject object = (PDFObject)this.objects.remove(0);
            /*
             * add the position of this object to the list of object
             * locations
             */
            setLocation(object.getObjectNumber() - 1, this.position);

            /*
             * output the object and increment the character position
             * by the object's length
             */
            this.position += object.output(stream);
        }

        //Clear all objects written to the file
        //this.objects.clear();
    }

    /**
     * Write the PDF header.
     *
     * This method must be called prior to formatting
     * and outputting AreaTrees.
     *
     * @param stream the OutputStream to write the header to
     * @throws IOException if there is an exception writing to the output stream
     */
    public void outputHeader(OutputStream stream) throws IOException {
        this.position = 0;

        getProfile().verifyPDFVersion();

        byte[] pdf = encode("%PDF-" + getPDFVersionString() + "\n");
        stream.write(pdf);
        this.position += pdf.length;

        // output a binary comment as recommended by the PDF spec (3.4.1)
        byte[] bin = {
                (byte)'%',
                (byte)0xAA,
                (byte)0xAB,
                (byte)0xAC,
                (byte)0xAD,
                (byte)'\n' };
        stream.write(bin);
        this.position += bin.length;
    }

    /**
     * Write the trailer
     *
     * @param stream the OutputStream to write the trailer to
     * @throws IOException if there is an exception writing to the output stream
     */
    public void outputTrailer(OutputStream stream) throws IOException {
        if (hasDestinations()) {
            Collections.sort(this.destinations, new DestinationComparator());
            this.dests = getFactory().makeDests(this.destinations);
            if (this.root.getNames() == null) {
                this.root.setNames(getFactory().makeNames());
            }
            this.root.getNames().setDests(dests);
        }
        output(stream);
        for (int count = 0; count < this.trailerObjects.size(); count++) {
            PDFObject o = (PDFObject)this.trailerObjects.get(count);
            setLocation(o.getObjectNumber() - 1, this.position);
            this.position += o.output(stream);
        }
        /* output the xref table and increment the character position
          by the table's length */
        this.position += outputXref(stream);

        /* construct the trailer */
        StringBuffer pdf = new StringBuffer(128);
        pdf.append("trailer\n<<\n/Size ")
                .append(this.objectcount + 1)
                .append("\n/Root ")
                .append(this.root.referencePDF())
                .append("\n/Info ")
                .append(this.info.referencePDF())
                .append('\n');

        if (this.isEncryptionActive()) {
            pdf.append(this.encryption.getTrailerEntry());
        } else {
            byte[] fileID = getFileIDGenerator().getOriginalFileID();
            String fileIDAsString = PDFText.toHex(fileID);
            pdf.append("/ID [" + fileIDAsString + " " + fileIDAsString + "]");
        }

        pdf.append("\n>>\nstartxref\n")
                .append(this.xref)
                .append("\n%%EOF\n");

        /* write the trailer */
        stream.write(encode(pdf.toString()));
    }

    /**
     * Write the xref table
     *
     * @param stream the OutputStream to write the xref table to
     * @return the number of characters written
     * @throws IOException in case of an error writing the result to
     *          the parameter stream
     */
    private int outputXref(OutputStream stream) throws IOException {

        /* remember position of xref table */
        this.xref = this.position;

        /* construct initial part of xref */
        StringBuffer pdf = new StringBuffer(128);
        pdf.append("xref\n0 ");
        pdf.append(this.objectcount + 1);
        pdf.append("\n0000000000 65535 f \n");

        String s, loc;
        for (int count = 0; count < this.location.size(); count++) {
            final String padding = "0000000000";
            s = this.location.get(count).toString();
            if (s.length() > 10) {
                throw new IOException("PDF file too large. PDF cannot grow beyond approx. 9.3GB.");
            }

            /* contruct xref entry for object */
            loc = padding.substring(s.length()) + s;

            /* append to xref table */
            pdf = pdf.append(loc).append(" 00000 n \n");
        }

        /* write the xref table and return the character length */
        byte[] pdfBytes = encode(pdf.toString());
        stream.write(pdfBytes);
        return pdfBytes.length;
    }

    long getCurrentFileSize() {
        return position;
    }

    FileIDGenerator getFileIDGenerator() {
        if (fileIDGenerator == null) {
            try {
                fileIDGenerator = FileIDGenerator.getDigestFileIDGenerator(this);
            } catch (NoSuchAlgorithmException e) {
                fileIDGenerator = FileIDGenerator.getRandomFileIDGenerator();
            }
        }
        return fileIDGenerator;
    }
}
