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
import com.github.steveash.jopenfst.IndexPair;
import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.MutableSymbolTable;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;

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
  private static MutableFst compose(Fst fst1, Fst fst2, Semiring semiring,
                                    boolean sorted) {
//    fst1.throwIfThisOutputIsNotThatInput(fst2);

    MutableFst
        res =
        new MutableFst(semiring, new MutableSymbolTable(fst1.getInputSymbols()), new MutableSymbolTable(fst2.getOutputSymbols()));

    HashMap<IndexPair, Integer> stateMap = Maps.newHashMap();
    ArrayList<IndexPair> queue = Lists.newArrayList();

    State s1 = fst1.getStartState();
    State s2 = fst2.getStartState();

    if ((s1 == null) || (s2 == null)) {
      throw new IllegalArgumentException("No initial state in one of " + fst1 + " or " + fst2);
    }

    IndexPair p = new IndexPair(s1.getId(), s2.getId());
    MutableState s = new MutableState(semiring.times(s1.getFinalWeight(),
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
            IndexPair nextPair = new IndexPair(nextState1.getId(), nextState2.getId());
            Integer nextState = stateMap.get(nextPair);
            MutableState realNextState;
            if (nextState == null) {
              realNextState = new MutableState(semiring.times(
                  nextState1.getFinalWeight(),
                  nextState2.getFinalWeight()));
              res.addState(realNextState);
              stateMap.put(nextPair, realNextState.getId());
              queue.add(nextPair);
            } else {
              realNextState = res.getState(nextState);
            }
            MutableArc a = new MutableArc(a1.getIlabel(), a2.getOlabel(),
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
  public static MutableFst compose(MutableFst fst1, MutableFst fst2, Semiring semiring) {
    fst1.throwIfInvalid();
    fst2.throwIfInvalid();
    fst1.throwIfThisOutputIsNotThatInput(fst2);

    Fst filter = makeFilter(fst1.getOutputSymbols(), semiring);
    augment(OUTPUT, fst1, semiring);
    augment(INPUT, fst2, semiring);

    MutableFst tmp = Compose.compose(fst1, filter, semiring, false);
    MutableFst res = Compose.compose(tmp, fst2, semiring, false);

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
  private static Fst makeFilter(MutableSymbolTable table, Semiring semiring) {
    table = new MutableSymbolTable(table);
    MutableFst filter = new MutableFst(semiring, table, table);

    int e1index = table.addNewUnique("eps1");
    int e2index = table.addNewUnique("eps2");

    // State 0
    MutableState s0 = new MutableState(table.size());
    s0.setFinalWeight(semiring.one());
    MutableState s1 = new MutableState(table.size());
    s1.setFinalWeight(semiring.one());
    MutableState s2 = new MutableState(table.size());
    s2.setFinalWeight(semiring.one());
    filter.addState(s0);
    s0.addArc(new MutableArc(e2index, e1index, semiring.one(), s0));
    s0.addArc(new MutableArc(e1index, e1index, semiring.one(), s1));
    s0.addArc(new MutableArc(e2index, e2index, semiring.one(), s2));
    for (ObjectIntCursor<String> cursor : table) {
      int i = cursor.value;
      if (cursor.key.equalsIgnoreCase(Fst.EPS) || i == e1index || i == e2index) {
        continue;
      }
      s0.addArc(new MutableArc(i, i, semiring.one(), s0));
    }
    filter.setStart(s0);

    // State 1
    filter.addState(s1);
    s1.addArc(new MutableArc(e1index, e1index, semiring.one(), s1));
    for (ObjectIntCursor<String> cursor : table) {
      int i = cursor.value;
      if (cursor.key.equalsIgnoreCase(Fst.EPS) || i == e1index || i == e2index) {
        continue;
      }
      s1.addArc(new MutableArc(i, i, semiring.one(), s0));
    }

    // State 2
    filter.addState(s2);
    s2.addArc(new MutableArc(e2index, e2index, semiring.one(), s2));
    for (ObjectIntCursor<String> cursor : table) {
      int i = cursor.value;
      if (cursor.key.equalsIgnoreCase(Fst.EPS) || i == e1index || i == e2index) {
        continue;
      }
      s2.addArc(new MutableArc(i, i, semiring.one(), s0));
    }

    return filter;
  }

  /**
   * Augments the labels of an Fst in order to use it for composition avoiding multiple epsilon paths in the resulting
   * Fst
   *
   * @param label constant denoting if the augment should take place on input or output labels For value equal to 0
   *              augment will take place for input labels For value equal to 1 augment will take place for output
   *              labels
   * @param fst   the fst to augment
   */
  public static void augment(AugmentLabels label, MutableFst fst, Semiring semiring) {
    // label: 0->augment on ilabel
    // 1->augment on olabel

    int e1inputIndex = fst.getInputSymbolCount();
    int e2inputIndex = e1inputIndex + 1;

    int e1outputIndex = fst.getOutputSymbolCount();
    int e2outputIndex = e1outputIndex + 1;

    int numStates = fst.getStateCount();
    for (int i = 0; i < numStates; i++) {
      MutableState s = fst.getState(i);
      int numArcs = s.getNumArcs();
      for (int j = 0; j < numArcs; j++) {
        MutableArc a = s.getArc(j);
        if ((label == OUTPUT) && (a.getOlabel() == 0)) {
          a.setOlabel(e2outputIndex);
        } else if ((label == INPUT) && (a.getIlabel() == 0)) {
          a.setIlabel(e1inputIndex);
        }
      }
      if (label == INPUT) {
        s.addArc(new MutableArc(e2inputIndex, 0, semiring.one(), s));
      } else if (label == OUTPUT) {
        s.addArc(new MutableArc(0, e1outputIndex, semiring.one(), s));
      }
    }
  }
}
