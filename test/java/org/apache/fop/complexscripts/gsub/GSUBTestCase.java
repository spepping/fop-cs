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

package org.apache.fop.complexscripts.gsub;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.fop.complexscripts.util.TTXFile;
import org.apache.fop.fonts.GlyphContextTester;
import org.apache.fop.fonts.GlyphSequence;
import org.apache.fop.fonts.GlyphSubtable;
import org.apache.fop.fonts.GlyphSubstitutionSubtable;
import org.apache.fop.fonts.GlyphSubstitutionTable;
import org.apache.fop.fonts.GlyphTable.LookupSpec;
import org.apache.fop.fonts.GlyphTable.LookupTable;
import org.apache.fop.fonts.ScriptContextTester;

import junit.framework.TestCase;

public class GSUBTestCase extends TestCase implements ScriptContextTester, GlyphContextTester {

    private static String ttxFilesRoot = "test/resources/complexscripts";

    private static String[][] ttxFonts = {
        { "f0", "arab/ttx/arab-001.ttx" },              // simplified arabic
        { "f1", "arab/ttx/arab-002.ttx" },              // traditional arabic
        { "f2", "arab/ttx/arab-003.ttx" },              // lateef
        { "f3", "arab/ttx/arab-004.ttx" },              // scheherazade
    };

    private static Object[][] ltSingle = {
        { GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_SINGLE },
        // arab-001.ttx
        { "f0", "lu2", "arab", "dflt", "isol",
          new String[][][] {
              { { "ainisolated" }, { "ain" } },
              { { "alefmaksuraisolated" }, { "alefmaksura" } },
              { { "behisolated" }, { "beh" } },
              { { "dadisolated" }, { "dad" } },
              { { "dalisolated" }, { "dal" } },
              { { "farsiyehisolated" }, { "farsiyeh" } },
              { { "fehisolated" }, { "feh" } },
              { { "gafisolated" }, { "gaf" } },
              { { "ghainisolated" }, { "ghain" } },
              { { "hahisolated" }, { "hah" } },
              { { "jeemisolated" }, { "jeem" } },
              { { "jehisolated" }, { "jeh" } },
              { { "kafisolated" }, { "arabickaf" } },
              { { "kehehisolated" }, { "keheh" } },
              { { "khahisolated" }, { "khah" } },
              { { "meemisolated" }, { "meem" } },
              { { "noonisolated" }, { "noon" } },
              { { "pehisolated" }, { "peh" } },
              { { "qafisolated" }, { "qaf" } },
              { { "rehisolated" }, { "reh" } },
              { { "sadisolated" }, { "sad" } },
              { { "seenisolated" }, { "seen" } },
              { { "sheenisolated" }, { "sheen" } },
              { { "tahisolated" }, { "tah" } },
              { { "tchehisolated" }, { "tcheh" } },
              { { "tehisolated" }, { "teh" } },
              { { "tehmarbutaisolated" }, { "tehmarbuta" } },
              { { "thalisolated" }, { "thal" } },
              { { "thehisolated" }, { "theh" } },
              { { "vehisolated" }, { "veh" } },
              { { "wawisolated" }, { "waw" } },
              { { "yehisolated" }, { "yeh" } },
              { { "yehwithhamzaaboveisolated" }, { "yehwithhamzaabove" } },
              { { "zahisolated" }, { "zah" } },
              { { "zainisolated" }, { "zain" } },
          },
        },
        { "f0", "lu4", "arab", "dflt", "fina",
          new String[][][] {
              { { "ain" }, { "ainfinal" } },
              { { "alefmaksura" }, { "alefmaksurafinal" } },
              { { "alefwasla" }, { "alefwaslafinal" } },
              { { "alefwithhamzaabove" }, { "alefwithhamzaabovefinal" } },
              { { "alefwithhamzabelow" }, { "alefwithhamzabelowfinal" } },
              { { "alefwithmaddaabove" }, { "alefwithmaddaabovefinal" } },
              { { "arabicae" }, { "hehfinal" } },
              { { "arabicalef" }, { "aleffinal" } },
              { { "arabickaf" }, { "arabickaf" } },
              { { "beh" }, { "beh" } },
              { { "dad" }, { "dad" } },
              { { "dal" }, { "dal" } },
              { { "farsiyeh" }, { "farsiyehfinal" } },
              { { "feh" }, { "feh" } },
              { { "gaf" }, { "gaffinal" } },
              { { "ghain" }, { "ghainfinal" } },
              { { "hah" }, { "hahfinal" } },
              { { "heh" }, { "hehfinal" } },
              { { "jeem" }, { "jeemfinal" } },
              { { "jeh" }, { "jeh" } },
              { { "keheh" }, { "kehehfinal" } },
              { { "khah" }, { "khahfinal" } },
              { { "lam" }, { "lam" } },
              { { "meem" }, { "meem" } },
              { { "noon" }, { "noon" } },
              { { "peh" }, { "peh" } },
              { { "qaf" }, { "qaf" } },
              { { "reh" }, { "reh" } },
              { { "sad" }, { "sad" } },
              { { "seen" }, { "seen" } },
              { { "sheen" }, { "sheen" } },
              { { "tah" }, { "tah" } },
              { { "tcheh" }, { "tchehfinal" } },
              { { "teh" }, { "teh" } },
              { { "tehmarbuta" }, { "tehmarbutafinal" } },
              { { "thal" }, { "thal" } },
              { { "theh" }, { "theh" } },
              { { "veh" }, { "veh" } },
              { { "waw" }, { "waw" } },
              { { "wawwithhamzaabove" }, { "wawwithhamzaabove" } },
              { { "yeh" }, { "yehfinal" } },
              { { "yehwithhamzaabove" }, { "yehwithhamzaabovefinal" } },
              { { "zah" }, { "zah" } },
              { { "zain" }, { "zain" } },
          }
        },
        { "f0", "lu5", "arab", "dflt", "init",
          new String[][][] {
              { { "ain" }, { "aininitial" } },
              { { "alefmaksura" }, { "uni0649.init" } },
              { { "arabickaf" }, { "kafmedial" } },
              { { "beh" }, { "behmedial" } },
              { { "dad" }, { "dadmedial" } },
              { { "farsiyeh" }, { "yehmedial" } },
              { { "feh" }, { "fehinitial" } },
              { { "gaf" }, { "gafinitial" } },
              { { "ghain" }, { "ghaininitial" } },
              { { "hah" }, { "hahmedial" } },
              { { "heh" }, { "hehinitial" } },
              { { "jeem" }, { "jeemmedial" } },
              { { "keheh" }, { "kehehinitial" } },
              { { "khah" }, { "khahmedial" } },
              { { "lam" }, { "lamisolated" } },
              { { "meem" }, { "meemmedial" } },
              { { "noon" }, { "noonmedial" } },
              { { "peh" }, { "pehmedial" } },
              { { "qaf" }, { "qafinitial" } },
              { { "sad" }, { "sadmedial" } },
              { { "seen" }, { "seenmedial" } },
              { { "sheen" }, { "sheenmedial" } },
              { { "tah" }, { "tah" } },
              { { "tcheh" }, { "tchehmedial" } },
              { { "teh" }, { "tehmedial" } },
              { { "theh" }, { "thehmedial" } },
              { { "veh" }, { "uni06A5.init" } },
              { { "yeh" }, { "yehmedial" } },
              { { "yehwithhamzaabove" }, { "yehwithhamzaabovemedial" } },
              { { "zah" }, { "zah" } },
          }
        },
        { "f0", "lu6", "arab", "dflt", "medi",
          new String[][][] {
              { { "ain" }, { "ainmedial" } },
              { { "alefmaksura" }, { "uni0649.init" } },
              { { "arabickaf" }, { "kafmedial" } },
              { { "beh" }, { "behmedial" } },
              { { "dad" }, { "dadmedial" } },
              { { "farsiyeh" }, { "yehmedial" } },
              { { "feh" }, { "fehmedial" } },
              { { "gaf" }, { "gafmedial" } },
              { { "ghain" }, { "ghainmedial" } },
              { { "hah" }, { "hahmedial" } },
              { { "heh" }, { "hehmedial" } },
              { { "jeem" }, { "jeemmedial" } },
              { { "keheh" }, { "kehehmedial" } },
              { { "khah" }, { "khahmedial" } },
              { { "lam" }, { "lammedial" } },
              { { "meem" }, { "meemmedial" } },
              { { "noon" }, { "noonmedial" } },
              { { "peh" }, { "pehmedial" } },
              { { "qaf" }, { "qafmedial" } },
              { { "sad" }, { "sadmedial" } },
              { { "seen" }, { "seenmedial" } },
              { { "sheen" }, { "sheenmedial" } },
              { { "tah" }, { "tah" } },
              { { "tcheh" }, { "tchehmedial" } },
              { { "teh" }, { "tehmedial" } },
              { { "theh" }, { "thehmedial" } },
              { { "veh" }, { "vehmedial" } },
              { { "yeh" }, { "yehmedial" } },
              { { "yehwithhamzaabove" }, { "yehwithhamzaabovemedial" } },
              { { "zah" }, { "zah" } },
          }
        },
        // arab-002.ttx
        { "f1", "lu1", },
        { "f1", "lu3", },
        { "f1", "lu4", },
        { "f1", "lu5", },
        { "f1", "lu13", },
        // arab-003.ttx
        { "f2", "lu1", },
        { "f2", "lu2", },
        { "f2", "lu3", },
        { "f2", "lu9", },
        { "f2", "lu10", },
        { "f2", "lu11", },
        { "f2", "lu12", },
        { "f2", "lu14", },
        { "f2", "lu15", },
        { "f2", "lu16", },
        { "f2", "lu17", },
        { "f2", "lu18", },
        { "f2", "lu19", },
        { "f2", "lu20", },
        { "f2", "lu21", },
        // arab-004.ttx
        { "f3", "lu1", },
        { "f3", "lu2", },
        { "f3", "lu3", },
        { "f3", "lu11", },
        { "f3", "lu12", },
        { "f3", "lu13", },
        { "f3", "lu15", },
        { "f3", "lu16", },
        { "f3", "lu17", },
        { "f3", "lu18", },
        { "f3", "lu19", },
        { "f3", "lu20", },
        { "f3", "lu21", },
        { "f3", "lu22", },
        { "f3", "lu23", },
    };

    private static Object[][] ltMultiple = {
        { GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_MULTIPLE },
        // arab-001.ttx
        { "f0", "lu9", },
        // arab-002.ttx
        { "f1", "lu14", },
        { "f1", "lu15", },
        // arab-003.ttx
        { "f2", "lu0", },
        // arab-004.ttx
        { "f3", "lu0", },
    };

    private static Object[][] ltAlternate = {
        { GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_ALTERNATE },
        // arab-001.ttx
        // arab-002.ttx
        // arab-003.ttx
        // arab-004.ttx
        { "f3", "lu14", },
    };

    private static Object[][] ltLigature = {
        { GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_LIGATURE },
        // arab-001.ttx
        { "f0", "lu0", },
        { "f0", "lu7", },
        { "f0", "lu8", },
        // arab-002.ttx
        { "f1", "lu0", },
        { "f1", "lu6", },
        { "f1", "lu7", },
        { "f1", "lu8", },
        { "f1", "lu9", },
        { "f1", "lu10", },
        // arab-003.ttx
        { "f2", "lu5", },
        { "f2", "lu6", },
        { "f2", "lu7", },
        // arab-004.ttx
        { "f3", "lu5", },
        { "f3", "lu6", },
        { "f3", "lu7", },
        { "f3", "lu8", },
    };

    private static Object[][] ltContextual = {
        { GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_CONTEXTUAL },
        // arab-001.ttx
        // arab-002.ttx
        // arab-003.ttx
        // arab-004.ttx
    };

    private static Object[][] ltChainedContextual = {
        { GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_CHAINED_CONTEXTUAL },
        // arab-001.ttx
        { "f0", "lu1", },
        { "f0", "lu3", },
        // arab-002.ttx
        { "f1", "lu2", },
        { "f1", "lu11", },
        { "f1", "lu12", },
        // arab-003.ttx
        { "f2", "lu4", },
        { "f2", "lu8", },
        { "f2", "lu13", },
        // arab-004.ttx
        { "f3", "lu4", },
        { "f3", "lu9", },
        { "f3", "lu10", },
    };

    public void testGSUBSingle() throws Exception {
        performSubstitutions ( ltSingle );
    }

    public void testGSUBMultiple() throws Exception {
        performSubstitutions ( ltMultiple );
    }

    public void testGSUBAlternate() throws Exception {
        performSubstitutions ( ltAlternate );
    }

    public void testGSUBLigature() throws Exception {
        performSubstitutions ( ltLigature );
    }

    public void testGSUBContextual() throws Exception {
        performSubstitutions ( ltContextual );
    }

    public void testGSUBChainedContextual() throws Exception {
        performSubstitutions ( ltChainedContextual );
    }

    /**
     * Perform substitutions on all test data in test specification TS.
     * @param ts test specification
     */
    private void performSubstitutions ( Object[][] ts ) {
        assert ts.length > 0;
        Object[] tp = ts[0];
        for ( int i = 1; i < ts.length; i++ ) {
            performSubstitutions ( tp, ts[i] );
        }
    }

    /**
     * Perform substitutions on all test data TD using test parameters TP.
     * @param tp test parameters
     * @param td test data
     */
    private void performSubstitutions ( Object[] tp, Object[] td ) {
        assert tp.length > 0;
        if ( td.length > 5 ) {
            String fid = (String) td[0];
            String lid = (String) td[1];
            String script = (String) td[2];
            String language = (String) td[3];
            String feature = (String) td[4];
            TTXFile tf = findTTX ( fid );
            assertTrue ( tf != null );
            GlyphSubstitutionTable gsub = tf.getGSUB();
            assertTrue ( gsub != null );
            GlyphSubstitutionSubtable[] sta = findGSUBSubtables ( gsub, script, language, feature, lid );
            assertTrue ( sta != null );
            assertTrue ( sta.length > 0 );
            ScriptContextTester sct = findScriptContextTester ( script, language, feature );
            String[][][] tia = (String[][][]) td[5];            // test instance array
            for ( String[][] ti : tia ) {                       // test instance
                if ( ti != null ) {
                    if ( ti.length > 1 ) {                      // must have at least input and output glyph id arrays
                        String[] igia = ti[0];                  // input glyph id array
                        String[] ogia = ti[1];                  // output glyph id array
                        GlyphSequence igs = tf.getGlyphSequence ( igia );
                        GlyphSequence ogs = tf.getGlyphSequence ( ogia );
                        GlyphSequence tgs = GlyphSubstitutionSubtable.substitute ( igs, script, language, feature, sta, sct );
                        assertTrue ( ogs.compareGlyphs ( tgs.getGlyphs() ) == 0 );
                    }
                }
            }
        }
    }

    private String findTTXPath ( String fid ) {
        for ( String[] fs : ttxFonts ) {
            if ( ( fs != null ) && ( fs.length > 1 ) ) {
                if ( fs[0].equals ( fid ) ) {
                    return ttxFilesRoot + File.separator + fs[1];
                }
            }
        }
        return null;
    }

    private TTXFile findTTX ( String fid ) {
        String pn = findTTXPath ( fid );
        assertTrue ( pn != null );
        try { 
            TTXFile tf = TTXFile.getFromCache ( pn );
            return tf;
        } catch ( Exception e ) {
            fail ( e.getMessage() );
            return null;
        }
    }

    private GlyphSubstitutionSubtable[] findGSUBSubtables ( GlyphSubstitutionTable gsub, String script, String language, String feature, String lid ) {
        Map<LookupSpec,List<LookupTable>> lookups = (Map<LookupSpec,List<LookupTable>>) gsub.matchLookups ( script, language, feature );
        for ( List<LookupTable> ltl : lookups.values() ) {
            for ( LookupTable lt : ltl ) {
                if ( lt.getId().equals ( lid ) ) {
                    return (GlyphSubstitutionSubtable[]) lt.getSubtables();
                }
            }
        }
        return null;
    }

    private ScriptContextTester findScriptContextTester ( String script, String language, String feature ) {
        return this;
    }

    private GlyphSequence getInputGlyphs ( Object[] td ) {
        // TODO - IMPLEMENT ME
        return null;
    }

    private GlyphSequence getOutputGlyphs ( Object[] td ) {
        // TODO - IMPLEMENT ME
        return null;
    }

    @Override
    public GlyphContextTester getTester ( String feature ) {
        return this;
    }

    @Override
    public boolean test ( String script, String language, String feature, GlyphSequence gs, int index ) {
        return true;
    }
        
}
