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

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

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
import org.apache.fop.traits.Direction;
import org.apache.fop.util.CharUtilities;

// CSOFF: EmptyForIteratorPadCheck
// CSOFF: InnerAssignmentCheck
// CSOFF: SimplifyBooleanReturnCheck

/**
 * The <code>InlineRun</code> class is a utility class, the instances of which are used
 * to capture a sequence of reordering levels associated with an inline area.
 *
 * @author Glenn Adams
 */
class InlineRun {
    private InlineArea inline;
    private int[] levels;
    private int minLevel;
    private int maxLevel;
    private int reversals;
    InlineRun ( InlineArea inline, int[] levels ) {
        this.inline = inline;
        this.levels = levels;
        setMinMax ( levels );
    }
    private InlineRun ( InlineArea inline, int level, int count ) {
        this ( inline, makeLevels ( level, count ) );
    }
    InlineArea getInline() {
        return inline;
    }
    int getMinLevel() {
        return minLevel;
    }
    private void setMinMax ( int[] levels ) {
        int mn = Integer.MAX_VALUE;
        int mx = Integer.MIN_VALUE;
        if ( ( levels != null ) && ( levels.length > 0 ) ) {
            for ( int i = 0, n = levels.length; i < n; i++ ) {
                int l = levels [ i ];
                if ( l < mn ) {
                    mn = l;
                }
                if ( l > mx ) {
                    mx = l;
                }
            }
        } else {
            mn = mx = -1;
        }
        this.minLevel = mn;
        this.maxLevel = mx;
    }
    public boolean isHomogenous() {
        return minLevel == maxLevel;
    }
    public List split() {
        List runs = new Vector();
        for ( int i = 0, n = levels.length; i < n; ) {
            int l = levels [ i ];
            int s = i;
            int e = s;
            while ( e < n ) {
                if ( levels [ e ] != l ) {
                    break;
                } else {
                    e++;
                }
            }
            if ( s < e ) {
                runs.add ( new InlineRun ( inline, l, e - s ) );
            }
            i = e;
        }
        assert runs.size() < 2 : "heterogeneous inlines not yet supported!!";
        return runs;
    }
    public void updateMinMax ( int[] mm ) {
        if ( minLevel < mm[0] ) {
            mm[0] = minLevel;
        }
        if ( maxLevel > mm[1] ) {
            mm[1] = maxLevel;
        }
    }
    public boolean maybeNeedsMirroring() {
        return ( minLevel == maxLevel ) && ( ( minLevel & 1 ) != 0 );
    }
    public void reverse() {
        reversals++;
    }
    public void maybeReverseWord ( boolean mirror ) {
        if ( inline instanceof WordArea ) {
            WordArea w = (WordArea) inline;
            if ( ( reversals & 1 ) != 0 ) {
                w.reverse ( mirror );
            } else if ( mirror && maybeNeedsMirroring() ) {
                w.mirror();
            }
        }
    }
    public boolean equals ( Object o ) {
        if ( o instanceof InlineRun ) {
            InlineRun ir = (InlineRun) o;
            if ( ir.inline != inline ) {
                return false;
            } else if ( ir.minLevel != minLevel ) {
                return false;
            } else if ( ir.maxLevel != maxLevel ) {
                return false;
            } else if ( ( ir.levels != null ) && ( levels != null ) ) {
                if ( ir.levels.length != levels.length ) {
                    return false;
                } else {
                    for ( int i = 0, n = levels.length; i < n; i++ ) {
                        if ( ir.levels[i] != levels[i] ) {
                            return false;
                        }
                    }
                    return true;
                }
            } else if ( ( ir.levels == null ) && ( levels == null ) ) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    public int hashCode() {
        int l = ( inline != null ) ? inline.hashCode() : 0;
        l = ( l ^ minLevel ) + ( l << 19 );
        l = ( l ^ maxLevel )   + ( l << 11 );
        return l;
    }
    public String toString() {
        StringBuffer sb = new StringBuffer( "RR: { type = \'" );
        char c;
        String content = null;
        if ( inline instanceof WordArea ) {
            c = 'W';
            content = ( (WordArea) inline ) .getWord();
        } else if ( inline instanceof SpaceArea ) {
            c = 'S';
            content = ( (SpaceArea) inline ) .getSpace();
        } else if ( inline instanceof Anchor ) {
            c = 'A';
        } else if ( inline instanceof Leader ) {
            c = 'L';
        } else if ( inline instanceof Space ) {
            c = 'G'; // 'G' => glue
        } else if ( inline instanceof UnresolvedPageNumber ) {
            c = '#';
            content = ( (UnresolvedPageNumber) inline ) .getText();
        } else if ( inline instanceof InlineBlockParent ) {
            c = 'B';
        } else if ( inline instanceof InlineViewport ) {
            c = 'V';
        } else if ( inline instanceof InlineParent ) {
            c = 'I';
        } else {
            c = '?';
        }
        sb.append ( c );
        sb.append ( "\', levels = \'" );
        sb.append ( generateLevels ( levels ) );
        sb.append ( "\', min = " );
        sb.append ( minLevel );
        sb.append ( ", max = " );
        sb.append ( maxLevel );
        sb.append ( ", reversals = " );
        sb.append ( reversals );
        sb.append ( ", content = <" );
        sb.append ( CharUtilities.toNCRefs ( content ) );
        sb.append ( "> }" );
        return sb.toString();
    }
    private String generateLevels ( int[] levels ) {
        StringBuffer lb = new StringBuffer();
        int maxLevel = -1;
        int numLevels = levels.length;
        for ( int i = 0; i < numLevels; i++ ) {
            int l = levels [ i ];
            if ( l > maxLevel ) {
                maxLevel = l;
            }
        }
        if ( maxLevel < 0 ) {
            // leave level buffer empty
        } else if ( maxLevel < 10 ) {
            // use string of decimal digits
            for ( int i = 0; i < numLevels; i++ ) {
                lb.append ( (char) ( '0' + levels [ i ] ) );
            }
        } else {
            // use comma separated list
            boolean first = true;
            for ( int i = 0; i < numLevels; i++ ) {
                if ( first ) {
                    first = false;
                } else {
                    lb.append(',');
                }
                lb.append ( levels [ i ] );
            }
        }
        return lb.toString();
    }
    private static int[] makeLevels ( int level, int count ) {
        int[] levels = new int [ count ];
        Arrays.fill ( levels, level );
        return levels;
    }
}

