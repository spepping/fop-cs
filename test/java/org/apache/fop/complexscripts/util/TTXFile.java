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

package org.apache.fop.complexscripts.util;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.fonts.GlyphClassTable;
import org.apache.fop.fonts.GlyphCoverageTable;
import org.apache.fop.fonts.GlyphDefinitionSubtable;
import org.apache.fop.fonts.GlyphDefinitionTable;
import org.apache.fop.fonts.GlyphMappingTable;
import org.apache.fop.fonts.GlyphPositioningSubtable;
import org.apache.fop.fonts.GlyphPositioningTable;
import org.apache.fop.fonts.GlyphSubstitutionSubtable;
import org.apache.fop.fonts.GlyphSubstitutionTable;
import org.apache.fop.fonts.GlyphSubtable;
import org.apache.fop.fonts.GlyphTable;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


// CSOFF: InnerAssignmentCheck
// CSOFF: LineLengthCheck
// CSOFF: NoWhitespaceAfterCheck

/**
 * This class supports a subset of the <code>TTX</code> file as produced by the Adobe FLEX
 * SDK (AFDKO). In particular, it is used to parse a <code>TTX</code> file in order to
 * extract character to glyph code mapping data, glyph definition data, glyph substitution
 * data, and glyph positioning data.
 *
 * <code>TTX</code> files are used in FOP for testing and debugging purposes only. Such
 * files are used to represent font data employed by complex script processing, and
 * normally extracted directly from an opentype (or truetype) file. However, due to
 * copyright restrictions, it is not possible to include most opentype (or truetype) font
 * files directly in the FOP distribution. In such cases, <code>TTX</code> files are used
 * to distribute a subset of the complex script advanced table information contained in
 * certain font files to facilitate testing. This can be done because no glyph outline or
 * other proprietary information is included in the distributed <code>TTX</code> files.
 *
 * @author Glenn Adams
 */
public class TTXFile {

    /** logging instance */
    private static final Log log = LogFactory.getLog(TTXFile.class);                                                    // CSOK: ConstantNameCheck

    // transient parsing state
    private Locator locator;                            // current document locator
    private Stack<String[]> elements;                   // stack of ttx elements being parsed
    private Map<String,Integer> glyphIds;               // map of glyph names to glyph identifiers
    private Map<String,Integer> glyphClasses;           // map of glyph names to glyph classes
    private List<GlyphSubtable> subtables;              // list of constructed subtables
    private int ltSequence;                             // lookup sequence within table
    private int stSequence;                             // subtable sequence number within lookup
    private int stFormat;                               // format of current subtable being constructed
    private int stFlags;                                // flags of current subtable being constructed

    // resultant state
    private GlyphDefinitionTable gdef;                  // constructed glyph definition table
    private GlyphSubstitutionTable gsub;                // constructed glyph substitution table
    private GlyphPositioningTable gpos;                 // constructed glyph positioning table

    public TTXFile() {
        elements = new Stack<String[]>();
        glyphIds = new java.util.HashMap<String,Integer>();
        glyphClasses = new java.util.HashMap<String,Integer>();
        subtables = new java.util.ArrayList<GlyphSubtable>();
        ltSequence = 0;
        stSequence = 0;
        stFormat = 0;
        stFlags = 0;
    }

    public void parse ( String filename ) {
        parse ( new File ( filename ) );
    }

    public void parse ( File f ) {
        assert f != null;
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            sp.parse ( f, new Handler() );
        } catch ( FactoryConfigurationError e ) {
            throw new RuntimeException ( e.getMessage() );
        } catch ( ParserConfigurationException e ) {
            throw new RuntimeException ( e.getMessage() );
        } catch ( SAXException e ) {
            throw new RuntimeException ( e.getMessage() );
        } catch ( IOException e ) {
            throw new RuntimeException ( e.getMessage() );
        }
    }

    private class Handler extends DefaultHandler {
        private Handler() {
        }
        @Override
        public void startDocument() {
        }
        @Override
        public void endDocument() {
        }
        @Override
        public void setDocumentLocator ( Locator locator ) {
            TTXFile.this.locator = locator;
        }
        @Override
        public void startElement ( String uri, String localName, String qName, Attributes attrs ) throws SAXException {
            String[] en = makeExpandedName ( uri, localName, qName );
            if ( en[0] != null ) {
                unsupportedElement ( en );
            } else if ( en[1].equals ( "Alternate" ) ) {
                String[] pn = new String[] { null, "AlternateSet" };
                if ( isParent ( pn ) ) {
                    String glyph = attrs.getValue ( "glyph" );
                    if ( glyph == null ) {
                        missingRequiredAttribute ( en, "glyph" );
                    }
                    int gid = mapGlyphId ( glyph, en );
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "AlternateSet" ) ) {
                String[] pn = new String[] { null, "AlternateSubst" };
                if ( isParent ( pn ) ) {
                    String glyph = attrs.getValue ( "glyph" );
                    if ( glyph == null ) {
                        missingRequiredAttribute ( en, "glyph" );
                    }
                    int gid = mapGlyphId ( glyph, en );
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "AlternateSubst" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "BacktrackCoverage" ) ) {
                String[] pn1 = new String[] { null, "ChainContextSubst" };
                String[] pn2 = new String[] { null, "ChainContextPos" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "BaseAnchor" ) ) {
                String[] pn = new String[] { null, "BaseRecord" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "BaseArray" ) ) {
                String[] pn = new String[] { null, "MarkBasePos" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "BaseCoverage" ) ) {
                String[] pn = new String[] { null, "MarkBasePos" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "BaseRecord" ) ) {
                String[] pn = new String[] { null, "BaseArray" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "ChainContextPos" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "ChainContextSubst" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Class" ) ) {
                String[] pn = new String[] { null, "MarkRecord" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "ClassDef" ) ) {
                String[] pn1 = new String[] { null, "GlyphClassDef" };
                String[] pn2 = new String[] { null, "MarkAttachClassDef" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String glyph = attrs.getValue ( "glyph" );
                    if ( glyph == null ) {
                        missingRequiredAttribute ( en, "glyph" );
                    }
                    String glyphClass = attrs.getValue ( "class" );
                    if ( glyphClass == null ) {
                        missingRequiredAttribute ( en, "class" );
                    }
                    if ( ! glyphIds.containsKey ( glyph ) ) {
                        unsupportedGlyph ( en, glyph );
                    } else if ( isParent ( pn1 ) ) {
                        if ( glyphClasses.containsKey ( glyph ) ) {
                            duplicateGlyphClass ( en, glyph, glyphClass );
                        } else {
                            glyphClasses.put ( glyph, Integer.parseInt(glyphClass) );
                        }
                    } else if ( isParent ( pn2 ) ) {
                        if ( glyphClasses.containsKey ( glyph ) ) {
                            duplicateGlyphClass ( en, glyph, glyphClass );
                        } else {
                            glyphClasses.put ( glyph, Integer.parseInt(glyphClass) );
                        }
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "ComponentRecord" ) ) {
                String[] pn = new String[] { null, "LigatureAttach" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Coverage" ) ) {
                String[] pn1 = new String[] { null, "CursivePos" };
                String[] pn2 = new String[] { null, "LigCaretList" };
                String[] pn3 = new String[] { null, "MultipleSubst" };
                String[] pn4 = new String[] { null, "PairPos" };
                String[] pn5 = new String[] { null, "SinglePos" };
                String[][] pnx = new String[][] { pn1, pn2, pn3, pn4, pn5 };
                if ( isParent ( pnx ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "CursivePos" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "DefaultLangSys" ) ) {
                String[] pn = new String[] { null, "Script" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "EntryAnchor" ) ) {
                String[] pn = new String[] { null, "EntryExitRecord" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "EntryExitRecord" ) ) {
                String[] pn = new String[] { null, "CursivePos" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "ExitAnchor" ) ) {
                String[] pn = new String[] { null, "EntryExitRecord" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Feature" ) ) {
                String[] pn = new String[] { null, "FeatureRecord" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "FeatureIndex" ) ) {
                String[] pn1 = new String[] { null, "DefaultLangSys" };
                String[] pn2 = new String[] { null, "LangSys" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "FeatureList" ) ) {
                String[] pn1 = new String[] { null, "GSUB" };
                String[] pn2 = new String[] { null, "GPOS" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( ! isParent ( pnx ) ) {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "FeatureRecord" ) ) {
                String[] pn = new String[] { null, "FeatureList" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "FeatureTag" ) ) {
                String[] pn = new String[] { null, "FeatureRecord" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "GDEF" ) ) {
                String[] pn = new String[] { null, "ttFont" };
                if ( isParent ( pn ) ) {
                    assertSubtablesClear();
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "GPOS" ) ) {
                String[] pn = new String[] { null, "ttFont" };
                if ( isParent ( pn ) ) {
                    assertSubtablesClear();
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "GSUB" ) ) {
                String[] pn = new String[] { null, "ttFont" };
                if ( isParent ( pn ) ) {
                    assertSubtablesClear();
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Glyph" ) ) {
                String[] pn1 = new String[] { null, "Coverage" };
                String[] pn2 = new String[] { null, "InputCoverage" };
                String[] pn3 = new String[] { null, "LookAheadCoverage" };
                String[] pn4 = new String[] { null, "BacktrackCoverage" };
                String[] pn5 = new String[] { null, "MarkCoverage" };
                String[] pn6 = new String[] { null, "Mark1Coverage" };
                String[] pn7 = new String[] { null, "Mark2Coverage" };
                String[] pn8 = new String[] { null, "BaseCoverage" };
                String[] pn9 = new String[] { null, "LigatureCoverage" };
                String[][] pnx = new String[][] { pn1, pn2, pn3, pn4, pn5, pn6, pn7, pn8, pn9 };
                if ( isParent ( pnx ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    } else {
                        int gid = mapGlyphId ( value, en );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "GlyphClassDef" ) ) {
                String[] pn = new String[] { null, "GDEF" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    int sf = -1;
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    } else {
                        sf = Integer.parseInt ( format );
                        switch ( sf ) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat ( en, sf );
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    stFormat = sf;
                    assert glyphClasses.isEmpty();
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "GlyphID" ) ) {
                String[] pn = new String[] { null, "GlyphOrder" };
                if ( isParent ( pn ) ) {
                    String id = attrs.getValue ( "id" );
                    if ( id == null ) {
                        missingRequiredAttribute ( en, "id" );
                    }
                    String name = attrs.getValue ( "name" );
                    if ( name == null ) {
                        missingRequiredAttribute ( en, "name" );
                    }
                    if ( glyphIds.containsKey ( name ) ) {
                        duplicateGlyph ( en, name, id );
                    } else {
                        glyphIds.put ( name, Integer.parseInt(id) );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "GlyphOrder" ) ) {
                String[] pn = new String[] { null, "ttFont" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "InputCoverage" ) ) {
                String[] pn1 = new String[] { null, "ChainContextSubst" };
                String[] pn2 = new String[] { null, "ChainContextPos" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "LangSys" ) ) {
                String[] pn = new String[] { null, "LangSysRecord" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LangSysRecord" ) ) {
                String[] pn = new String[] { null, "Script" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LangSysTag" ) ) {
                String[] pn = new String[] { null, "LangSysRecord" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LigCaretList" ) ) {
                String[] pn = new String[] { null, "GDEF" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Ligature" ) ) {
                String[] pn = new String[] { null, "LigatureSet" };
                if ( isParent ( pn ) ) {
                    String components = attrs.getValue ( "components" );
                    if ( components == null ) {
                        missingRequiredAttribute ( en, "components" );
                    }
                    int[] cids = mapGlyphIds ( components, en );
                    String glyph = attrs.getValue ( "glyph" );
                    if ( glyph == null ) {
                        missingRequiredAttribute ( en, "glyph" );
                    }
                    int gid = mapGlyphId ( glyph, en );
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LigatureAnchor" ) ) {
                String[] pn = new String[] { null, "ComponentRecord" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LigatureArray" ) ) {
                String[] pn = new String[] { null, "MarkLigPos" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LigatureAttach" ) ) {
                String[] pn = new String[] { null, "LigatureArray" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LigatureCoverage" ) ) {
                String[] pn = new String[] { null, "MarkLigPos" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LigatureSet" ) ) {
                String[] pn = new String[] { null, "LigatureSubst" };
                if ( isParent ( pn ) ) {
                    String glyph = attrs.getValue ( "glyph" );
                    if ( glyph == null ) {
                        missingRequiredAttribute ( en, "glyph" );
                    }
                    int gid = mapGlyphId ( glyph, en );
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LigatureSubst" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LookAheadCoverage" ) ) {
                String[] pn1 = new String[] { null, "ChainContextSubst" };
                String[] pn2 = new String[] { null, "ChainContextPos" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "Lookup" ) ) {
                String[] pn = new String[] { null, "LookupList" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LookupFlag" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "LookupList" ) ) {
                String[] pn1 = new String[] { null, "GSUB" };
                String[] pn2 = new String[] { null, "GPOS" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( ! isParent ( pnx ) ) {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "LookupListIndex" ) ) {
                String[] pn1 = new String[] { null, "Feature" };
                String[] pn2 = new String[] { null, "SubstLookupRecord" };
                String[] pn3 = new String[] { null, "PosLookupRecord" };
                String[][] pnx = new String[][] { pn1, pn2, pn3 };
                if ( isParent ( pnx ) ) {
                    String index = attrs.getValue ( "index" );
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "LookupType" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Mark1Array" ) ) {
                String[] pn = new String[] { null, "MarkMarkPos" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Mark1Coverage" ) ) {
                String[] pn = new String[] { null, "MarkMarkPos" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Mark2Anchor" ) ) {
                String[] pn = new String[] { null, "Mark2Record" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Mark2Array" ) ) {
                String[] pn = new String[] { null, "MarkMarkPos" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Mark2Coverage" ) ) {
                String[] pn = new String[] { null, "MarkMarkPos" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Mark2Record" ) ) {
                String[] pn = new String[] { null, "Mark2Array" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "MarkAnchor" ) ) {
                String[] pn = new String[] { null, "MarkRecord" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "MarkArray" ) ) {
                String[] pn1 = new String[] { null, "MarkBasePos" };
                String[] pn2 = new String[] { null, "MarkLigPos" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( ! isParent ( pnx ) ) {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "MarkAttachClassDef" ) ) {
                String[] pn = new String[] { null, "GDEF" };
                if ( isParent ( pn ) ) {
                    String format = attrs.getValue ( "Format" );
                    int sf = -1;
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    } else {
                        sf = Integer.parseInt ( format );
                        switch ( sf ) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat ( en, sf );
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    stFormat = sf;
                    assert glyphClasses.isEmpty();
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "MarkBasePos" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "MarkCoverage" ) ) {
                String[] pn1 = new String[] { null, "MarkBasePos" };
                String[] pn2 = new String[] { null, "MarkLigPos" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "MarkLigPos" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "MarkMarkPos" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "MarkRecord" ) ) {
                String[] pn1 = new String[] { null, "MarkArray" };
                String[] pn2 = new String[] { null, "Mark1Array" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "MultipleSubst" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "PairPos" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "PairSet" ) ) {
                String[] pn = new String[] { null, "PairPos" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "PairValueRecord" ) ) {
                String[] pn = new String[] { null, "PairSet" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "PosLookupRecord" ) ) {
                String[] pn1 = new String[] { null, "ChainContextSubst" };
                String[] pn2 = new String[] { null, "ChainContextPos" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "ReqFeatureIndex" ) ) {
                String[] pn1 = new String[] { null, "DefaultLangSys" };
                String[] pn2 = new String[] { null, "LangSys" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "Script" ) ) {
                String[] pn = new String[] { null, "ScriptRecord" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "ScriptList" ) ) {
                String[] pn1 = new String[] { null, "GSUB" };
                String[] pn2 = new String[] { null, "GPOS" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( ! isParent ( pnx ) ) {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "ScriptRecord" ) ) {
                String[] pn = new String[] { null, "ScriptList" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "ScriptTag" ) ) {
                String[] pn = new String[] { null, "ScriptRecord" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "SecondGlyph" ) ) {
                String[] pn = new String[] { null, "PairValueRecord" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    } else {
                        int gid = mapGlyphId ( value, en );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Sequence" ) ) {
                String[] pn = new String[] { null, "MultipleSubst" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "SequenceIndex" ) ) {
                String[] pn1 = new String[] { null, "PosLookupRecord" };
                String[] pn2 = new String[] { null, "SubstLookupRecord" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "SinglePos" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "SingleSubst" ) ) {
                String[] pn = new String[] { null, "Lookup" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String format = attrs.getValue ( "Format" );
                    if ( format == null ) {
                        missingRequiredAttribute ( en, "Format" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "SubstLookupRecord" ) ) {
                String[] pn = new String[] { null, "ChainContextSubst" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Substitute" ) ) {
                String[] pn = new String[] { null, "Sequence" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    if ( index == null ) {
                        missingRequiredAttribute ( en, "index" );
                    }
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    } else {
                        int gid = mapGlyphId ( value, en );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Substitution" ) ) {
                String[] pn = new String[] { null, "SingleSubst" };
                if ( isParent ( pn ) ) {
                    String in = attrs.getValue ( "in" );
                    if ( in == null ) {
                        missingRequiredAttribute ( en, "in" );
                    } else {
                        int igid = mapGlyphId ( in, en );
                    }
                    String out = attrs.getValue ( "out" );
                    if ( out == null ) {
                        missingRequiredAttribute ( en, "out" );
                    } else {
                        int ogid = mapGlyphId ( out, en );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Value" ) ) {
                String[] pn = new String[] { null, "SinglePos" };
                if ( isParent ( pn ) ) {
                    String index = attrs.getValue ( "index" );
                    String xPlacement = attrs.getValue ( "XPlacement" );
                    String yPlacement = attrs.getValue ( "YPlacement" );
                    String xAdvance = attrs.getValue ( "XAdvance" );
                    String yAdvance = attrs.getValue ( "YAdvance" );
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Value1" ) ) {
                String[] pn = new String[] { null, "PairValueRecord" };
                if ( isParent ( pn ) ) {
                    String xPlacement = attrs.getValue ( "XPlacement" );
                    String yPlacement = attrs.getValue ( "YPlacement" );
                    String xAdvance = attrs.getValue ( "XAdvance" );
                    String yAdvance = attrs.getValue ( "YAdvance" );
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Value2" ) ) {
                String[] pn = new String[] { null, "PairValueRecord" };
                if ( isParent ( pn ) ) {
                    String xPlacement = attrs.getValue ( "XPlacement" );
                    String yPlacement = attrs.getValue ( "YPlacement" );
                    String xAdvance = attrs.getValue ( "XAdvance" );
                    String yAdvance = attrs.getValue ( "YAdvance" );
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "ValueFormat" ) ) {
                String[] pn = new String[] { null, "SinglePos" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "ValueFormat1" ) ) {
                String[] pn = new String[] { null, "PairPos" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "ValueFormat2" ) ) {
                String[] pn = new String[] { null, "PairPos" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "Version" ) ) {
                String[] pn1 = new String[] { null, "GDEF" };
                String[] pn2 = new String[] { null, "GPOS" };
                String[] pn3 = new String[] { null, "GSUB" };
                String[][] pnx = new String[][] { pn1, pn2, pn3 };
                if ( isParent ( pnx ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "XCoordinate" ) ) {
                String[] pn1 = new String[] { null, "BaseAnchor" };
                String[] pn2 = new String[] { null, "EntryAnchor" };
                String[] pn3 = new String[] { null, "ExitAnchor" };
                String[] pn4 = new String[] { null, "LigatureAnchor" };
                String[] pn5 = new String[] { null, "MarkAnchor" };
                String[] pn6 = new String[] { null, "Mark2Anchor" };
                String[][] pnx = new String[][] { pn1, pn2, pn3, pn4, pn5, pn6 };
                if ( isParent ( pnx ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "YCoordinate" ) ) {
                String[] pn1 = new String[] { null, "BaseAnchor" };
                String[] pn2 = new String[] { null, "EntryAnchor" };
                String[] pn3 = new String[] { null, "ExitAnchor" };
                String[] pn4 = new String[] { null, "LigatureAnchor" };
                String[] pn5 = new String[] { null, "MarkAnchor" };
                String[] pn6 = new String[] { null, "Mark2Anchor" };
                String[][] pnx = new String[][] { pn1, pn2, pn3, pn4, pn5, pn6 };
                if ( isParent ( pnx ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "checkSumAdjustment" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "cmap" ) ) {
                String[] pn = new String[] { null, "ttFont" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "cmap_format_0" ) ) {
                String[] pn = new String[] { null, "cmap" };
                if ( isParent ( pn ) ) {
                    String platformID = attrs.getValue ( "platformID" );
                    if ( platformID == null ) {
                        missingRequiredAttribute ( en, "platformID" );
                    }
                    String platEncID = attrs.getValue ( "platEncID" );
                    if ( platEncID == null ) {
                        missingRequiredAttribute ( en, "platEncID" );
                    }
                    String language = attrs.getValue ( "language" );
                    if ( language == null ) {
                        missingRequiredAttribute ( en, "language" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "cmap_format_4" ) ) {
                String[] pn = new String[] { null, "cmap" };
                if ( isParent ( pn ) ) {
                    String platformID = attrs.getValue ( "platformID" );
                    if ( platformID == null ) {
                        missingRequiredAttribute ( en, "platformID" );
                    }
                    String platEncID = attrs.getValue ( "platEncID" );
                    if ( platEncID == null ) {
                        missingRequiredAttribute ( en, "platEncID" );
                    }
                    String language = attrs.getValue ( "language" );
                    if ( language == null ) {
                        missingRequiredAttribute ( en, "language" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "created" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "flags" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "fontDirectionHint" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "fontRevision" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "glyphDataFormat" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "head" ) ) {
                String[] pn = new String[] { null, "ttFont" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "hmtx" ) ) {
                String[] pn = new String[] { null, "ttFont" };
                if ( ! isParent ( pn ) ) {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "indexToLocFormat" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "lowestRecPPEM" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "macStyle" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "magicNumber" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "map" ) ) {
                String[] pn1 = new String[] { null, "cmap_format_0" };
                String[] pn2 = new String[] { null, "cmap_format_4" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pnx ) ) {
                    String code = attrs.getValue ( "code" );
                    if ( code == null ) {
                        missingRequiredAttribute ( en, "code" );
                    }
                    String name = attrs.getValue ( "name" );
                    if ( name == null ) {
                        missingRequiredAttribute ( en, "name" );
                    } else {
                        int gid = mapGlyphId ( name, en );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "modified" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "mtx" ) ) {
                String[] pn = new String[] { null, "hmtx" };
                if ( isParent ( pn ) ) {
                    String name = attrs.getValue ( "name" );
                    if ( name == null ) {
                        missingRequiredAttribute ( en, "name" );
                    } else {
                        int gid = mapGlyphId ( name, en );
                    }
                    String width = attrs.getValue ( "width" );
                    if ( width == null ) {
                        missingRequiredAttribute ( en, "width" );
                    }
                    String lsb = attrs.getValue ( "lsb" );
                    if ( lsb == null ) {
                        missingRequiredAttribute ( en, "lsb" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "tableVersion" ) ) {
                String[] pn1 = new String[] { null, "cmap" };
                String[] pn2 = new String[] { null, "head" };
                String[][] pnx = new String[][] { pn1, pn2 };
                if ( isParent ( pn1 ) ) {               // child of cmap
                    String version = attrs.getValue ( "version" );
                    if ( version == null ) {
                        missingRequiredAttribute ( en, "version" );
                    }
                } else if ( isParent ( pn2 ) ) {        // child of head
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pnx );
                }
            } else if ( en[1].equals ( "ttFont" ) ) {
                String[] pn = new String[] { null, null };
                if ( isParent ( pn ) ) {
                    String sfntVersion = attrs.getValue ( "sfntVersion" );
                    if ( sfntVersion == null ) {
                        missingRequiredAttribute ( en, "sfntVersion" );
                    }
                    String ttLibVersion = attrs.getValue ( "ttLibVersion" );
                    if ( ttLibVersion == null ) {
                        missingRequiredAttribute ( en, "ttLibVersion" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), null );
                }
            } else if ( en[1].equals ( "unitsPerEm" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "xMax" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "xMin" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "yMax" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else if ( en[1].equals ( "yMin" ) ) {
                String[] pn = new String[] { null, "head" };
                if ( isParent ( pn ) ) {
                    String value = attrs.getValue ( "value" );
                    if ( value == null ) {
                        missingRequiredAttribute ( en, "value" );
                    }
                } else {
                    notPermittedInElementContext ( en, getParent(), pn );
                }
            } else {
                unsupportedElement ( en );
            }
            elements.push ( en );
        }
        @Override
        public void endElement ( String uri, String localName, String qName ) throws SAXException {
            if ( elements.empty() ) {
                throw new SAXException ( "element stack is unbalanced, no elements on stack!" );
            }
            String[] enParent = elements.peek();
            if ( enParent == null ) {
                throw new SAXException ( "element stack is empty, elements are not balanced" );
            }
            String[] en = makeExpandedName ( uri, localName, qName );
            if ( ! sameExpandedName ( enParent, en ) ) {
                throw new SAXException ( "element stack is unbalanced, expanded name mismatch" );
            }
            if ( en[0] != null ) {
                unsupportedElement ( en );
            } else if ( en[1].equals ( "BacktrackCoverage" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "BaseCoverage" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "Coverage" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "GDEF" ) ) {
                if ( subtables.size() > 0 ) {
                    gdef = new GlyphDefinitionTable ( subtables );
                }
                clearLookups();
            } else if ( en[1].equals ( "GPOS" ) ) {
                if ( subtables.size() > 0 ) {
                    gpos = new GlyphPositioningTable ( gdef, constructLookups(), subtables );
                }
                clearLookups();
            } else if ( en[1].equals ( "GSUB" ) ) {
                if ( subtables.size() > 0 ) {
                    gsub = new GlyphSubstitutionTable ( gdef, constructLookups(), subtables );
                }
                clearLookups();
            } else if ( en[1].equals ( "GlyphClassDef" ) ) {
                GlyphMappingTable mapping = extractClassDefMapping ( glyphClasses, stFormat, true );
                List entries = new java.util.ArrayList();
                subtables.add ( GlyphDefinitionTable.createSubtable ( GlyphDefinitionTable.GDEF_LOOKUP_TYPE_GLYPH_CLASS, getLookupId(), 0, 0, 1, mapping, entries ) );
                nextLookup();
            } else if ( en[1].equals ( "FeatureList" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "InputCoverage" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "LigCaretList" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "LigatureCoverage" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "LookAheadCoverage" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "Lookup" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "LookupList" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "MarkAttachClassDef" ) ) {
                GlyphMappingTable mapping = extractClassDefMapping ( glyphClasses, stFormat, true );
                List entries = new java.util.ArrayList();
                subtables.add ( GlyphDefinitionTable.createSubtable ( GlyphDefinitionTable.GDEF_LOOKUP_TYPE_MARK_ATTACHMENT, getLookupId(), 0, 0, 1, mapping, entries ) );
                nextLookup();
            } else if ( en[1].equals ( "MarkCoverage" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "Mark1Coverage" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "Mark2Coverage" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "ScriptList" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "cmap" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "head" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "hmtx" ) ) {
                // TODO - IMPLEMENT ME
            } else if ( en[1].equals ( "ttFont" ) ) {
                // TODO - IMPLEMENT ME
            }
            elements.pop();
        }
        @Override
        public void characters ( char[] chars, int start, int length ) {
        }
        private String[] getParent() {
            if ( ! elements.empty() ) {
                return elements.peek();
            } else {
                return new String[] { null, null };
            }
        }
        private boolean isParent ( Object enx ) {
            if ( enx instanceof String[][] ) {
                for ( String[] en : (String[][]) enx ) {
                    if ( isParent ( en ) ) {
                        return true;
                    }
                }
                return false;
            } else if ( enx instanceof String[] ) {
                String[] en = (String[]) enx;
                if ( ! elements.empty() ) {
                    String[] pn = elements.peek();
                    return ( pn != null ) && sameExpandedName ( en, pn );
                } else if ( ( en[0] == null ) && ( en[1] == null ) ) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        private Map<GlyphTable.LookupSpec,List<String>> constructLookups() {
            Map<GlyphTable.LookupSpec,List<String>> lookups = new java.util.LinkedHashMap();
            // TODO - IMPLEMENT ME
            return lookups;
        }
        private GlyphClassTable extractClassDefMapping ( Map<String,Integer> glyphClasses, int format, boolean clearSourceMap ) {
            GlyphClassTable ct;
            if ( format == 1 ) {
                ct = extractClassDefMapping1 ( extractClassMappings ( glyphClasses, clearSourceMap ) );
            } else if ( format == 2 ) {
                ct = extractClassDefMapping2 ( extractClassMappings ( glyphClasses, clearSourceMap ) );
            } else {
                ct = null;
            }
            return ct;
        }
        private GlyphClassTable extractClassDefMapping1 ( int[][] cma ) {
            List entries = new java.util.ArrayList<Integer>();
            int s = -1;
            int l = -1;
            Integer zero = Integer.valueOf(0);
            for ( int[] m : cma ) {
                int g = m[0];
                int c = m[1];
                if ( s < 0 ) {
                    s = g;
                    l = g - 1;
                    entries.add ( Integer.valueOf ( s ) );
                }
                if ( g == ( l + 1 ) ) {
                    entries.add ( Integer.valueOf ( c ) );
                } else {
                    for ( ; l < ( g - 1 ); l++ ) {
                        entries.add ( zero );
                    }
                }
                assert l == ( g - 1 );
                l = g;
            }
            return GlyphClassTable.createClassTable ( entries );
        }
        private GlyphClassTable extractClassDefMapping2 ( int[][] cma ) {
            List entries = new java.util.ArrayList<Integer>();
            int s = -1;
            int e =  s;
            int l = -1;
            for ( int[] m : cma ) {
                int g = m[0];
                int c = m[1];
                if ( c != l ) {
                    if ( s >= 0 ) {
                        entries.add ( new GlyphClassTable.MappingRange ( s, e, l ) );
                    }
                    s = e = g;
                } else {
                    e = g;
                }
                l = c;
            }
            return GlyphClassTable.createClassTable ( entries );
        }
        private int[][] extractClassMappings ( Map<String,Integer> glyphClasses, boolean clearSourceMap ) {
            int nc = glyphClasses.size();
            int i = 0;
            int[][] cma = new int [ nc ] [ 2 ];
            for ( Map.Entry<String,Integer> e : glyphClasses.entrySet() ) {
                Integer gid = glyphIds.get ( e.getKey() );
                assert gid != null;
                int[] m = cma [ i ];
                m [ 0 ] = (int) gid;
                m [ 1 ] = (int) e.getValue();
                i++;
            }
            if ( clearSourceMap ) {
                glyphClasses.clear();
            }
            return sortClassMappings ( cma );
        }
        private int[][] sortClassMappings ( int[][] cma ) {
            Arrays.sort ( cma, new Comparator<int[]>() {
                    public int compare ( int[] m1, int[] m2 ) {
                        assert m1.length > 0;
                        assert m2.length > 0;
                        if ( m1[0] < m2[0] ) {
                            return -1;
                        } else if ( m1[0] > m2[0] ) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }
            );
            return cma;
        }
        private String getLookupId() {
            return "lu" + ltSequence;
        }
        private void clearSubtableInLookup() {
            stFormat = 0;
            stFlags = 0;
        }
        private void clearSubtablesInLookup() {
            clearSubtableInLookup();
            stSequence = 0;
        }
        private void clearSubtablesInTable() {
            clearSubtablesInLookup();
            subtables.clear();
        }
        private void nextSubtableInLookup() {
            stSequence++;
            clearSubtableInLookup();
        }
        private void clearLookup() {
            clearSubtablesInLookup();
        }
        private void clearLookups() {
            clearLookup();
            ltSequence = 0;
            clearSubtablesInTable();
        }
        private void nextLookup() {
            ltSequence++;
            clearLookup();
        }
        private void assertSubtableClear() {
            assert stFormat == 0;
            assert stFlags == 0;
        }
        private void assertSubtablesClear() {
            assertSubtableClear();
            assert subtables.size() == 0;
        }
    }

    private int mapGlyphId ( String glyph, String[] currentElement ) throws SAXException {
        Integer gid = glyphIds.get ( glyph );
        if ( gid != null ) {
            return (int) gid;
        } else {
            unsupportedGlyph ( currentElement, glyph );
            return -1;
        }
    }
    private int[] mapGlyphIds ( String glyphs, String[] currentElement ) throws SAXException {
        String[] ga = glyphs.split(",");
        int[] gids = new int [ ga.length ];
        int i = 0;
        for ( String glyph : ga ) {
            gids[i++] = mapGlyphId ( glyph, currentElement );
        }
        return gids;
    }
    private String formatLocator() {
        if ( locator == null ) {
            return "{null}";
        } else {
            return "{" + locator.getSystemId() + ":" + locator.getLineNumber() + ":" + locator.getColumnNumber() + "}";
        }
    }
    private void unsupportedElement ( String[] en ) throws SAXException {
        throw new SAXException ( formatLocator() + ": unsupported element " + formatExpandedName ( en ) );
    }
    private void notPermittedInElementContext ( String[] en, String[] cn, Object xns ) throws SAXException {
        assert en != null;
        assert cn != null;
        String s = "element " + formatExpandedName(en) + " not permitted in current element context " + formatExpandedName(cn);
        if ( xns == null ) {
            s += ", expected root context";
        } else if ( xns instanceof String[][] ) {
            int nxn = 0;
            s += ", expected one of { ";
            for ( String[] xn : (String[][]) xns ) {
                if ( nxn++ > 0 ) {
                    s += ", ";
                }
                s += formatExpandedName ( xn );
            }
            s += " }";
        } else if ( xns instanceof String[] ) {
            s += ", expected " + formatExpandedName ( (String[]) xns );
        }
        throw new SAXException ( formatLocator() + ": " + s );
    }
    private void missingRequiredAttribute ( String[] en, String name ) throws SAXException {
        throw new SAXException ( formatLocator() + ": element " + formatExpandedName(en) + " missing required attribute " + name );
    }
    private void duplicateGlyph ( String[] en, String name, String id ) throws SAXException {
        throw new SAXException ( formatLocator() + ": element " + formatExpandedName(en) + " contains duplicate name \"" + name + "\", with identifier value " + id );
    }
    private void unsupportedGlyph ( String[] en, String name ) throws SAXException {
        throw new SAXException ( formatLocator() + ": element " + formatExpandedName(en) + " refers to unsupported glyph id \"" + name + "\"" );
    }
    private void duplicateGlyphClass ( String[] en, String name, String glyphClass ) throws SAXException {
        throw new SAXException ( formatLocator() + ": element " + formatExpandedName(en) + " contains duplicate glyph class for \"" + name + "\", with class value " + glyphClass );
    }
    private void unsupportedFormat ( String[] en, int format ) throws SAXException {
        throw new SAXException ( formatLocator() + ": element " + formatExpandedName(en) + " refers to unsupported table format \"" + format + "\"" );
    }
    private static String[] makeExpandedName ( String uri, String localName, String qName ) {
        if ( ( uri != null ) && ( uri.length() == 0 ) ) {
            uri = null;
        }
        if ( ( localName != null ) && ( localName.length() == 0 ) ) {
            localName = null;
        }
        if ( ( uri == null ) && ( localName == null ) ) {
            uri = extractPrefix ( qName );
            localName = extractLocalName ( qName );
        }
        return new String[] { uri, localName };
    }
    private static String extractPrefix ( String qName ) {
        String[] sa = qName.split(":");
        if ( sa.length == 2 ) {
            return sa[0];
        } else {
            return null;
        }
    }
    private static String extractLocalName ( String qName ) {
        String[] sa = qName.split(":");
        if ( sa.length == 2 ) {
            return sa[1];
        } else if ( sa.length == 1 ) {
            return sa[0];
        } else {
            return null;
        }
    }
    private static boolean sameExpandedName ( String[] n1, String[] n2 ) {
        String u1 = n1[0];
        String u2 = n2[0];
        if ( ( u1 == null ) ^ ( u2 == null ) ) {
            return false;
        }
        if ( ( u1 != null ) && ( u2 != null ) ) {
            if ( ! u1.equals ( u2 ) ) {
                return false;
            }
        }
        String l1 = n1[1];
        String l2 = n2[1];
        if ( ( l1 == null ) ^ ( l2 == null ) ) {
            return false;
        }
        if ( ( l1 != null ) && ( l2 != null ) ) {
            if ( ! l1.equals ( l2 ) ) {
                return false;
            }
        }
        return true;
    }
    private static String formatExpandedName ( String[] n ) {
        String u = ( n[0] != null ) ? n[0] : "null";
        String l = ( n[1] != null ) ? n[1] : "null";
        return "{" + u + "}" + l;
    }
}
