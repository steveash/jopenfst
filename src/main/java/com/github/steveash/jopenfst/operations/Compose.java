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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.ImmutableFst;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.github.steveash.jopenfst.operations.Compose.AugmentLabels.INPUT;
import static com.github.steveash.jopenfst.operations.Compose.AugmentLabels.OUTPUT;

/**
 * Compose operation.
 *
 * See: M. Mohri, "Weighted automata algorithms", Handbook of Weighted Automata. Springer, pp. 213-250, 2009.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */

public class Compose {

  public enum AugmentLabels { INPUT, OUTPUT }

  /**
   * Default Constructor
   */
  private Compose() {
  }

  /**
   * Computes the composition of two Fsts. Assuming no epsilon transitions.
   *
   * Input Fsts are not modified.
   *
   * @param fst1     the first Fst
   * @param fst2     the second Fst
   * @param semiring the semiring to use in the operation
   * @return the composed Fst
   */
  public static Fst compose(Fst fst1, Fst fst2, Semiring semiring,
                            boolean sorted) {
    fst1.throwIfThisOutputIsNotThatInput(fst2);

    Fst res = new Fst(semiring);

    HashMap<Pair<State, State>, State> stateMap = Maps.newHashMap();
    ArrayList<Pair<State, State>> queue = Lists.newArrayList();

    State s1 = fst1.getStart();
    State s2 = fst2.getStart();

    if ((s1 == null) || (s2 == null)) {
      throw new IllegalArgumentException("No initial state in one of " + fst1 + " or " + fst2);
    }

    Pair<State, State> p = Pair.of(s1, s2);
    State s = new State(semiring.times(s1.getFinalWeight(),
                                       s2.getFinalWeight()));

    res.addState(s);
    res.setStart(s);
    stateMap.put(p, s);
    queue.add(p);

    while (queue.size() > 0) {
      p = queue.remove(0);
      s1 = p.getLeft();
      s2 = p.getRight();
      s = stateMap.get(p);
      int numArcs1 = s1.getNumArcs();
      int numArcs2 = s2.getNumArcs();
      for (int i = 0; i < numArcs1; i++) {
        Arc a1 = s1.getArc(i);
        for (int j = 0; j < numArcs2; j++) {
          Arc a2 = s2.getArc(j);
          if (sorted && a1.getOlabel() < a2.getIlabel()) {
            break;
          }
          if (a1.getOlabel() == a2.getIlabel()) {
            State nextState1 = a1.getNextState();
            State nextState2 = a2.getNextState();
            Pair<State, State> nextPair = Pair.of(nextState1, nextState2);
            State nextState = stateMap.get(nextPair);
            if (nextState == null) {
              nextState = new State(semiring.times(
                  nextState1.getFinalWeight(),
                  nextState2.getFinalWeight()));
              res.addState(nextState);
              stateMap.put(nextPair, nextState);
              queue.add(nextPair);
            }
            Arc a = new Arc(a1.getIlabel(), a2.getOlabel(),
                            semiring.times(a1.getWeight(), a2.getWeight()),
                            nextState);
            s.addArc(a);
          }
        }
      }
    }

    res.setInputSymbolsFrom(fst1);
    res.setOutputSymbolsFrom(fst2);

    return res;
  }

  /**
   * Computes the composition of two Fsts. The two Fsts are augmented in order to avoid multiple epsilon paths in the
   * resulting Fst
   *
   * @param fst1     the first Fst
   * @param fst2     the second Fst
   * @param semiring the semiring to use in the operation
   * @return the composed Fst
   */
  public static Fst get(Fst fst1, Fst fst2, Semiring semiring) {
    if ((fst1 == null) || (fst2 == null)) {
      return null;
    }

    fst1.throwIfThisOutputIsNotThatInput(fst2);

    Fst filter = getFilter(fst1.getOsyms(), semiring);
    augment(1, fst1, semiring);
    augment(0, fst2, semiring);

    Fst tmp = Compose.compose(fst1, filter, semiring, false);
    Fst res = Compose.compose(tmp, fst2, semiring, false);

    // Connect.apply(res);

    return res;
  }

  /**
   * Get a filter to use for avoiding multiple epsilon paths in the resulting Fst
   *
   * See: M. Mohri, "Weighted automata algorithms", Handbook of Weighted Automata. Springer, pp. 213-250, 2009.
   *
   * @param syms     the gilter's input/output symbols
   * @param semiring the semiring to use in the operation
   * @return the filter
   */
  public static Fst getFilter(String[] syms, Semiring semiring) {
    Fst filter = new Fst(semiring);

    int e1index = syms.length;
    int e2index = syms.length + 1;

    filter.setIsyms(syms);
    filter.setOsyms(syms);

    // State 0
    State s0 = new State(syms.length + 3);
    s0.setFinalWeight(semiring.one());
    State s1 = new State(syms.length);
    s1.setFinalWeight(semiring.one());
    State s2 = new State(syms.length);
    s2.setFinalWeight(semiring.one());
    filter.addState(s0);
    s0.addArc(new Arc(e2index, e1index, semiring.one(), s0));
    s0.addArc(new Arc(e1index, e1index, semiring.one(), s1));
    s0.addArc(new Arc(e2index, e2index, semiring.one(), s2));
    for (int i = 1; i < syms.length; i++) {
      s0.addArc(new Arc(i, i, semiring.one(), s0));
    }
    filter.setStart(s0);

    // State 1
    filter.addState(s1);
    s1.addArc(new Arc(e1index, e1index, semiring.one(), s1));
    for (int i = 1; i < syms.length; i++) {
      s1.addArc(new Arc(i, i, semiring.one(), s0));
    }

    // State 2
    filter.addState(s2);
    s2.addArc(new Arc(e2index, e2index, semiring.one(), s2));
    for (int i = 1; i < syms.length; i++) {
      s2.addArc(new Arc(i, i, semiring.one(), s0));
    }

    return filter;
  }

  /**
   * Augments the labels of an Fst in order to use it for composition avoiding multiple epsilon paths in the resulting
   * Fst
   *
   * Augment can be applied to both {@link com.github.steveash.jopenfst.Fst} and {@link com.github.steveash.jopenfst.ImmutableFst}, as
   * immutable fsts hold an additional null arc for that operation
   *
   * @param label    constant denoting if the augment should take place on input or output labels For value equal to 0
   *                 augment will take place for input labels For value equal to 1 augment will take place for output
   *                 labels
   * @param fst      the fst to augment
   */
  public static void augment(AugmentLabels whichLabels, Fst fst) {
    augment(whichLabels, fst, fst.getSemiring());
  }

  public static void augment(int label, Fst fst, Semiring semiring) {
    if (label == 0) {
      augment(INPUT, fst, semiring);
    } else if (label == 1) {
      augment(OUTPUT, fst, semiring);
    } else {
      throw new IllegalArgumentException("Only takes input 0 or output 1 labels");
    }
  }

  public static void augment(AugmentLabels label, Fst fst, Semiring semiring) {
    // label: 0->augment on ilabel
    // 1->augment on olabel

    int e1inputIndex = fst.getInputSymbolCount();
    int e2inputIndex = e1inputIndex + 1;

    int e1outputIndex = fst.getOutputSymbolCount();
    int e2outputIndex = e1outputIndex + 1;

    int numStates = fst.getNumStates();
    for (int i = 0; i < numStates; i++) {
      State s = fst.getState(i);
      // Immutable fsts hold an additional (null) arc for augmention
      int numArcs = (fst instanceof ImmutableFst) ? s.getNumArcs() - 1
                                                  : s.getNumArcs();
      for (int j = 0; j < numArcs; j++) {
        Arc a = s.getArc(j);
        if ((label == OUTPUT) && (a.getOlabel() == 0)) {
          a.setOlabel(e2outputIndex);
        } else if ((label == INPUT) && (a.getIlabel() == 0)) {
          a.setIlabel(e1inputIndex);
        }
      }
      if (label == INPUT) {
        if (fst instanceof ImmutableFst) {
          s.setArc(numArcs, new Arc(e2inputIndex, 0, semiring.one(),
                                    s));
        } else {
          s.addArc(new Arc(e2inputIndex, 0, semiring.one(), s));
        }
      } else if (label == OUTPUT) {
        if (fst instanceof ImmutableFst) {
          s.setArc(numArcs, new Arc(0, e1outputIndex, semiring.one(),
                                    s));
        } else {
          s.addArc(new Arc(0, e1outputIndex, semiring.one(), s));
        }
      }
    }
  }
}
