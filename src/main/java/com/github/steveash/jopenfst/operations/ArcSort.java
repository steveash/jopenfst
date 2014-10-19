/**
 *
 * Copyright 1999-2012 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package com.github.steveash.jopenfst.operations;

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.State;

import java.util.Comparator;

/**
 * ArcSort operation.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ArcSort {

  /**
   * Default Constructor
   */
  private ArcSort() {
  }

  /**
   * Applies the ArcSort on the provided fst. Sorting can be applied either on input or output label based on the
   * provided comparator.
   *
   * ArcSort can be applied to both {@link com.github.steveash.jopenfst.Fst} and {@link com.github.steveash.jopenfst.ImmutableFst}
   *
   * @param fst the fst to sort it's arcs
   * @param cmp the provided Comparator
   */
  public static void apply(Fst fst, Comparator<Arc> cmp) {
    int numStates = fst.getNumStates();
    for (int i = 0; i < numStates; i++) {
      State s = fst.getState(i);
      s.arcSort(cmp);
    }
  }
}
