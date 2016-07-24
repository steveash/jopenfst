/*
 * Copyright 2014 Steve Ash
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.steveash.jopenfst.operations;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.ImmutableFst;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.SymbolTable;
import com.github.steveash.jopenfst.semiring.Semiring;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
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

  public enum AugmentLabels {INPUT, OUTPUT}

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
//    fst1.throwIfThisOutputIsNotThatInput(fst2);

    Fst res = new Fst(semiring, new SymbolTable(fst1.getInputSymbols()), new SymbolTable(fst2.getOutputSymbols()));

    HashMap<Pair<Integer, Integer>, Integer> stateMap = Maps.newHashMap();
    ArrayList<Pair<Integer, Integer>> queue = Lists.newArrayList();

    State s1 = fst1.getStart();
    State s2 = fst2.getStart();

    if ((s1 == null) || (s2 == null)) {
      throw new IllegalArgumentException("No initial state in one of " + fst1 + " or " + fst2);
    }

    Pair<Integer, Integer> p = Pair.of(s1.getId(), s2.getId());
    State s = new State(semiring.times(s1.getFinalWeight(),
                                       s2.getFinalWeight()));

    res.addState(s);
    res.setStart(s);
    stateMap.put(p, s.getId());
    queue.add(p);

    while (queue.size() > 0) {
      p = queue.remove(0);
      s1 = fst1.getState(p.getLeft());
      s2 = fst2.getState(p.getRight());
      s = res.getState(stateMap.get(p));
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
            Pair<Integer, Integer> nextPair = Pair.of(nextState1.getId(), nextState2.getId());
            Integer nextState = stateMap.get(nextPair);
            State realNextState;
            if (nextState == null) {
              realNextState = new State(semiring.times(
                  nextState1.getFinalWeight(),
                  nextState2.getFinalWeight()));
              res.addState(realNextState);
              stateMap.put(nextPair, realNextState.getId());
              queue.add(nextPair);
            } else {
              realNextState = res.getState(nextState);
            }
            Arc a = new Arc(a1.getIlabel(), a2.getOlabel(),
                            semiring.times(a1.getWeight(), a2.getWeight()),
                            realNextState);
            s.addArc(a);
          }
        }
      }
    }

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

    Fst filter = getFilter(fst1.getOutputSymbols(), semiring);
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
   * @param table    the filter's input/output symbols
   * @param semiring the semiring to use in the operation
   * @return the filter
   */
  public static Fst getFilter(SymbolTable table, Semiring semiring) {
    table = new SymbolTable(table);
    Fst filter = new Fst(semiring, table, table);

    int e1index = table.addNewUnique("eps1");
    int e2index = table.addNewUnique("eps2");

    // State 0
    State s0 = new State(table.size());
    s0.setFinalWeight(semiring.one());
    State s1 = new State(table.size());
    s1.setFinalWeight(semiring.one());
    State s2 = new State(table.size());
    s2.setFinalWeight(semiring.one());
    filter.addState(s0);
    s0.addArc(new Arc(e2index, e1index, semiring.one(), s0));
    s0.addArc(new Arc(e1index, e1index, semiring.one(), s1));
    s0.addArc(new Arc(e2index, e2index, semiring.one(), s2));
    for (ObjectIntCursor<String> cursor : table) {
      int i = cursor.value;
      if (cursor.key.equalsIgnoreCase(Fst.EPS) || i == e1index || i == e2index) {
        continue;
      }
      s0.addArc(new Arc(i, i, semiring.one(), s0));
    }
    filter.setStart(s0);

    // State 1
    filter.addState(s1);
    s1.addArc(new Arc(e1index, e1index, semiring.one(), s1));
    for (ObjectIntCursor<String> cursor : table) {
      int i = cursor.value;
      if (cursor.key.equalsIgnoreCase(Fst.EPS) || i == e1index || i == e2index) {
        continue;
      }
      s1.addArc(new Arc(i, i, semiring.one(), s0));
    }

    // State 2
    filter.addState(s2);
    s2.addArc(new Arc(e2index, e2index, semiring.one(), s2));
    for (ObjectIntCursor<String> cursor : table) {
      int i = cursor.value;
      if (cursor.key.equalsIgnoreCase(Fst.EPS) || i == e1index || i == e2index) {
        continue;
      }
      s2.addArc(new Arc(i, i, semiring.one(), s0));
    }

    return filter;
  }

  /**
   * Augments the labels of an Fst in order to use it for composition avoiding multiple epsilon paths in the resulting
   * Fst
   *
   * Augment can be applied to both {@link com.github.steveash.jopenfst.Fst} and {@link
   * com.github.steveash.jopenfst.ImmutableFst}, as immutable fsts hold an additional null arc for that operation
   *
   * @param whichLabels constant denoting if the augment should take place on input or output labels For value equal to
   *                    0 augment will take place for input labels For value equal to 1 augment will take place for
   *                    output labels
   * @param fst         the fst to augment
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
