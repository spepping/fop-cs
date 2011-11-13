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

package org.apache.fop.complexscripts.bidi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.area.LineArea;
import org.apache.fop.area.inline.Anchor;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineBlockParent;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.area.inline.InlineViewport;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.Space;
import org.apache.fop.area.inline.SpaceArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.UnresolvedPageNumber;
import org.apache.fop.area.inline.WordArea;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.flow.AbstractPageNumberCitation;
import org.apache.fop.fo.flow.AbstractGraphics;
import org.apache.fop.fo.flow.BidiOverride;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.flow.BlockContainer;
import org.apache.fop.fo.flow.Character;
import org.apache.fop.fo.flow.InlineContainer;
import org.apache.fop.fo.flow.InlineLevel;
import org.apache.fop.fo.flow.ListItem;
import org.apache.fop.fo.flow.PageNumber;
import org.apache.fop.fo.flow.Wrapper;
import org.apache.fop.fo.pagination.Flow;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.traits.Direction;
import org.apache.fop.util.CharUtilities;

// CSOFF: AvoidNestedBlocksCheck
// CSOFF: EmptyForIteratorPadCheck
// CSOFF: NoWhitespaceAfterCheck
// CSOFF: InnerAssignmentCheck
// CSOFF: SimplifyBooleanReturnCheck
// CSOFF: LineLengthCheck
// CSOFF: ParameterNumberCheck

/**
 * <p>A utility class for performing bidirectional resolution processing.</p>
 *
 * @author Glenn Adams
 */
public final class BidiResolver {

    /**
     * logging instance
     */
    private static final Log log = LogFactory.getLog(BidiResolver.class);                                                   // CSOK: ConstantNameCheck

    private BidiResolver() {
    }

    /**
     * Resolve inline directionality.
     * @param ps a page sequence FO instance
     */
    public static void resolveInlineDirectionality ( PageSequence ps ) {
        if (log.isDebugEnabled()) {
            log.debug ( "BD: RESOLVE: " + ps );
        }
        List ranges = pruneEmptyRanges ( collectRanges ( ps, new Stack() ) );
        resolveInlineDirectionality ( ranges );
    }

    /**
     * Reorder line area.
     * @param la a line area instance
     */
    public static void reorder ( LineArea la ) {

        // 1. collect inline levels
        List runs = collectRuns ( la.getInlineAreas(), new Vector() );
        if (log.isDebugEnabled()) {
            dumpRuns ( "BD: REORDER: INPUT:", runs );
        }

        // 2. split heterogeneous inlines
        runs = splitRuns ( runs );
        if (log.isDebugEnabled()) {
            dumpRuns ( "BD: REORDER: SPLIT INLINES:", runs );
        }

        // 3. determine minimum and maximum levels
        int[] mm = computeMinMaxLevel ( runs, null );
        if (log.isDebugEnabled()) {
            log.debug( "BD: REORDER: { min = " + mm[0] + ", max = " + mm[1] + "}" );
        }

        // 4. reorder from maximum level to minimum odd level
        int mn = mm[0];
        int mx = mm[1];
        for ( int l1 = mx, l2 = ( ( mn & 1 ) == 0 ) ? ( mn + 1 ) : mn; l1 >= l2; l1-- ) {
            runs = reorderRuns ( runs, l1 );
        }
        if (log.isDebugEnabled()) {
            dumpRuns ( "BD: REORDER: REORDERED RUNS:", runs );
        }

        // 5. reverse word consituents (characters and glyphs) while mirroring
        boolean mirror = true;
        reverseWords ( runs, mirror );
        if (log.isDebugEnabled()) {
            dumpRuns ( "BD: REORDER: REORDERED WORDS:", runs );
        }

        // 6. replace line area's inline areas with reordered runs' inline areas
        replaceInlines ( la, replicateSplitWords ( runs ) );
    }

    private static void resolveInlineDirectionality ( List ranges ) {
        for ( Iterator it = ranges.iterator(); it.hasNext(); ) {
            DelimitedTextRange r = (DelimitedTextRange) it.next();
            r.resolve();
            if (log.isDebugEnabled()) {
                log.debug ( r );
            }
        }
    }

    /**
     * Collect the sequence of delimited text ranges of node FO, where each new
     * range is pushed onto RANGES.
     */
    private static Stack collectRanges ( FONode fn, Stack ranges ) {
        // return existing ranges if passed null node
        if ( fn == null ) {
            return ranges;
        }
        // if boundary before, then push new range
        if ( isRangeBoundaryBefore ( fn ) ) {
            maybeNewRange ( ranges, fn );
        }
        // get current range, if one exists
        DelimitedTextRange r;
        if ( ranges.size() > 0 ) {
            r = (DelimitedTextRange) ranges.peek();
        } else {
            r = null;
        }
        // proceses this node
        if ( fn instanceof FOText ) {
            if ( r != null ) {
                r.append ( ( (FOText) fn ) .charIterator(), fn );
            }
        } else if ( fn instanceof Character ) {
            if ( r != null ) {
                r.append ( ( (Character) fn ) .charIterator(), fn );
            }
        } else if ( ( fn instanceof AbstractPageNumberCitation ) || ( fn instanceof AbstractGraphics ) ) {
            if ( r != null ) {
                r.append ( CharUtilities.OBJECT_REPLACEMENT_CHARACTER, fn );
            }
        } else if ( fn instanceof BidiOverride ) {
            if ( r != null ) {
                ranges = collectBidiOverrideRanges ( (BidiOverride) fn, r, ranges );
            }
        } else if ( fn instanceof ListItem ) {
            ranges = collectRanges ( ( (ListItem) fn ) .getLabel(), ranges );
            ranges = collectRanges ( ( (ListItem) fn ) .getBody(), ranges );
        } else if ( fn instanceof PageSequence ) {
            ranges = collectFlowRanges ( (PageSequence) fn, ranges );
        } else {
            for ( Iterator it = fn.getChildNodes(); ( it != null ) && it.hasNext(); ) {
                ranges = collectRanges ( (FONode) it.next(), ranges );
            }
        }
        // if boundary after, then push new range
        if ( isRangeBoundaryAfter ( fn ) ) {
            maybeNewRange ( ranges, fn );
        }
        return ranges;
    }

    private static Stack collectBidiOverrideRanges ( BidiOverride bo, DelimitedTextRange r, Stack ranges ) {
        char pfx = 0;
        char sfx = 0;
        int unicodeBidi = bo.getUnicodeBidi();
        int direction = bo.getDirection();
        if ( unicodeBidi == Constants.EN_BIDI_OVERRIDE ) {
            pfx = ( direction == Constants.EN_RTL ) ? CharUtilities.RLO : CharUtilities.LRO;
            sfx = CharUtilities.PDF;
        } else if ( unicodeBidi == Constants.EN_EMBED ) {
            pfx = ( direction == Constants.EN_RTL ) ? CharUtilities.RLE : CharUtilities.LRE;
            sfx = CharUtilities.PDF;
        }
        if ( pfx != 0 ) {
            r.append ( pfx, bo );
        }
        for ( Iterator it = bo.getChildNodes(); ( it != null ) && it.hasNext(); ) {
            ranges = collectRanges ( (FONode) it.next(), ranges );
        }
        if ( sfx != 0 ) {
            r.append ( sfx, bo );
        }
        return ranges;
    }

    private static Stack collectFlowRanges ( PageSequence ps, Stack ranges ) {
        assert ps != null;
        // collect ranges in static content flows
        Map<String, Flow> flows = ps.getFlowMap();
        if ( flows != null ) {
            for ( Flow f : flows.values() ) {
                if ( f instanceof StaticContent ) {
                    ranges = collectRanges ( f, ranges );
                }
            }
        }
        // collect ranges in main flow
        ranges = collectRanges ( ps.getMainFlow(), ranges );
        return ranges;
    }

    private static List collectRuns ( List inlines, List runs ) {
        for ( Iterator it = inlines.iterator(); it.hasNext(); ) {
            InlineArea ia = (InlineArea) it.next();
            if ( ia instanceof WordArea ) {
                runs = collectRuns ( (WordArea) ia, runs );
            } else if ( ia instanceof SpaceArea ) {
                runs = collectRuns ( (SpaceArea) ia, runs );
            } else if ( ia instanceof Anchor ) {
                runs = collectRuns ( (Anchor) ia, runs );
            } else if ( ia instanceof Leader ) {
                runs = collectRuns ( (Leader) ia, runs );
            } else if ( ia instanceof Space ) {
                runs = collectRuns ( (Space) ia, runs );
            } else if ( ia instanceof UnresolvedPageNumber ) {
                runs = collectRuns ( (UnresolvedPageNumber) ia, runs );
            } else if ( ia instanceof InlineBlockParent ) {
                runs = collectRuns ( (InlineBlockParent) ia, runs );
            } else if ( ia instanceof InlineViewport ) {
                runs = collectRuns ( (InlineViewport) ia, runs );
            } else if ( ia instanceof InlineParent ) {
                runs = collectRuns ( (InlineParent) ia, runs );
            }
        }
        return runs;
    }

    private static List collectRuns ( Anchor a, List runs ) {
        runs.add ( new InlineRun ( a, new int[] {a.getBidiLevel()}) );
        return runs;
    }

    private static List collectRuns ( InlineBlockParent a, List runs ) {
        runs.add ( new InlineRun ( a, new int[] {a.getBidiLevel()}) );
        return runs;
    }

    private static List collectRuns ( InlineParent a, List runs ) {
        return collectRuns ( a.getChildAreas(), runs );
    }

    private static List collectRuns ( InlineViewport a, List runs ) {
        runs.add ( new InlineRun ( a, new int[] {a.getBidiLevel()}) );
        return runs;
    }

    private static List collectRuns ( Leader a, List runs ) {
        runs.add ( new InlineRun ( a, new int[] {a.getBidiLevel()}) );
        return runs;
    }

    private static List collectRuns ( Space a, List runs ) {
        runs.add ( new InlineRun ( a, new int[] {a.getBidiLevel()}) );
        return runs;
    }

    private static List collectRuns ( SpaceArea a, List runs ) {
        runs.add ( new InlineRun ( a, new int[] {a.getBidiLevel()}) );
        return runs;
    }

    private static List collectRuns ( UnresolvedPageNumber a, List runs ) {
        runs.add ( new InlineRun ( a, new int[] {a.getBidiLevel()}) );
        return runs;
    }

    private static List collectRuns ( WordArea a, List runs ) {
        runs.add ( new InlineRun ( a, a.getBidiLevels() ) );
        return runs;
    }

    private static List splitRuns ( List runs ) {
        List runsNew = new Vector();
        for ( Iterator it = runs.iterator(); it.hasNext(); ) {
            InlineRun ir = (InlineRun) it.next();
            if ( ir.isHomogenous() ) {
                runsNew.add ( ir );
            } else {
                runsNew.addAll ( ir.split() );
            }
        }
        if ( ! runsNew.equals ( runs ) ) {
            runs = runsNew;
        }
        return runs;
    }

    private static int[] computeMinMaxLevel ( List runs, int[] mm ) {
        if ( mm == null ) {
            mm = new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE};
        }
        for ( Iterator it = runs.iterator(); it.hasNext(); ) {
            InlineRun ir = (InlineRun) it.next();
            ir.updateMinMax ( mm );
        }
        return mm;
    }
    private static List reorderRuns ( List runs, int level ) {
        List runsNew = new Vector();
        for ( int i = 0, n = runs.size(); i < n; i++ ) {
            InlineRun iri = (InlineRun) runs.get(i);
            if ( iri.getMinLevel() < level ) {
                runsNew.add ( iri );
            } else {
                int s = i;
                int e = s;
                while ( e < n ) {
                    InlineRun ire = (InlineRun) runs.get(e);
                    if ( ire.getMinLevel() < level ) {
                        break;
                    } else {
                        e++;
                    }
                }
                if ( s < e ) {
                    runsNew.addAll ( reverseRuns ( runs, s, e ) );
                }
                i = e - 1;
            }
        }
        if ( ! runsNew.equals ( runs ) ) {
            runs = runsNew;
        }
        return runs;
    }
    private static List reverseRuns ( List runs, int s, int e ) {
        int n = e - s;
        Vector runsNew = new Vector ( n );
        if ( n > 0 ) {
            for ( int i = 0; i < n; i++ ) {
                int k = ( n - i - 1 );
                InlineRun ir = (InlineRun) runs.get(s + k);
                ir.reverse();
                runsNew.add ( ir );
            }
        }
        return runsNew;
    }
    private static void reverseWords ( List runs, boolean mirror ) {
        for ( Iterator it = runs.iterator(); it.hasNext(); ) {
            InlineRun ir = (InlineRun) it.next();
            ir.maybeReverseWord ( mirror );
        }
    }
    private static List replicateSplitWords ( List runs ) {
        // [TBD] for each run which inline word area appears multiple times in
        // runs, replicate that word
        return runs;
    }
    private static void replaceInlines ( LineArea la, List runs ) {
        List<InlineArea> inlines = new ArrayList<InlineArea>();
        for ( Iterator it = runs.iterator(); it.hasNext(); ) {
            InlineRun ir = (InlineRun) it.next();
            inlines.add ( ir.getInline() );
        }
        la.setInlineAreas ( unflattenInlines ( inlines ) );
    }
    private static List unflattenInlines ( List<InlineArea> inlines ) {
        return new UnflattenProcessor ( inlines ) .unflatten();
    }
    private static void dumpRuns ( String header, List runs ) {
        log.debug ( header );
        for ( Iterator it = runs.iterator(); it.hasNext(); ) {
            InlineRun ir = (InlineRun) it.next();
            log.debug ( ir );
        }
    }

    /**
     * <p>Conditionally add a new delimited text range to RANGES, where new range is
     * associated with node FN. A new text range is added unless all of the following are true:</p>
     * <ul>
     * <li>there exists a current range RCUR in RANGES</li>
     * <li>RCUR is empty</li>
     * <li>the node of the RCUR is the same node as FN or a descendent node of FN</li>
     * </ul>
     */
    private static DelimitedTextRange maybeNewRange ( Stack ranges, FONode fn ) {
        DelimitedTextRange rCur = null;
        DelimitedTextRange rNew = null;
        if ( ranges.empty() ) {
            if ( fn instanceof Block ) {
                rNew = new DelimitedTextRange(fn);
            }
        } else if ( ( rCur = (DelimitedTextRange) ranges.peek() ) != null ) {
            if ( ! rCur.isEmpty() || ! isSelfOrDescendent ( rCur.getNode(), fn ) ) {
                rNew = new DelimitedTextRange(fn);
            }
        }
        if ( rNew != null ) {
            ranges.push ( rNew );
        } else {
            rNew = rCur;
        }
        return rNew;
    }

    /**
     * Determine if node N2 is the same or a descendent of node N1.
     */
    private static boolean isSelfOrDescendent ( FONode n1, FONode n2 ) {
        for ( FONode n = n2; n != null; n = n.getParent() ) {
            if ( n == n1 ) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRangeBoundary ( FONode fn ) {
        if ( fn instanceof Block ) {                                    // fo:block
            return true;
        } else if ( fn instanceof Character ) {                         // fo:character
            return false;
        } else if ( fn instanceof InlineLevel ) {                       // fo:inline, fo:leader, fo:bidi-override, fo:title
            return false;
        } else if ( fn instanceof InlineContainer ) {                   // fo:inline-container
            return false;
        } else if ( fn instanceof BlockContainer ) {                    // fo:block-container
            return true;
        } else if ( fn instanceof AbstractPageNumberCitation ) {        // fo:page-number-citation, fo:page-number-citation-last
            return false;
        } else if ( fn instanceof PageNumber ) {                        // fo:page-number
            return false;
        } else if ( fn instanceof AbstractGraphics ) {                  // fo:external-graphic, fo:instream-foreign-object
            return false;
        } else if ( fn instanceof Wrapper ) {                           // fo:wrapper
            return false;
        } else if ( fn instanceof FOText ) {                            // #PCDATA
            return false;
        } else {
            return true;
        }
    }

    private static boolean isRangeBoundaryBefore ( FONode fn ) {
        return isRangeBoundary ( fn );
    }

    private static boolean isRangeBoundaryAfter ( FONode fn ) {
        return isRangeBoundary ( fn );
    }

    private static List pruneEmptyRanges ( Stack ranges ) {
        Vector rv = new Vector();
        for ( Iterator it = ranges.iterator(); it.hasNext(); ) {
            DelimitedTextRange r = (DelimitedTextRange) it.next();
            if ( ! r.isEmpty() ) {
                rv.add ( r );
            }
        }
        return rv;
    }

}
