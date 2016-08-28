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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.ImmutableFst;
import com.github.steveash.jopenfst.IndexPair;
import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.SymbolTable;
import com.github.steveash.jopenfst.WriteableSymbolTable;
import com.github.steveash.jopenfst.semiring.Semiring;

import java.util.ArrayList;
import java.util.HashMap;

import static com.github.steveash.jopenfst.operations.Compose.AugmentLabels.INPUT;
import static com.github.steveash.jopenfst.operations.Compose.AugmentLabels.OUTPUT;
import static com.github.steveash.jopenfst.utils.FstUtils.symbolTableEffectiveCopy;

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

    MutableFst res = new MutableFst(semiring,
                                    symbolTableEffectiveCopy(fst1.getInputSymbols()),
                                    symbolTableEffectiveCopy(fst2.getOutputSymbols()));

    // state map is q -> n where q is (q_i, q_j) a state pair made up of state from fst1 and a state from fst2 and
    // n is the new state index in the composed FST that represents this tuple
    HashMap<IndexPair, Integer> stateMap = Maps.newHashMap();
    ArrayList<IndexPair> queue = Lists.newArrayList();

    State s1 = fst1.getStartState();
    State s2 = fst2.getStartState();
    MutableState s;

    if ((s1 == null) || (s2 == null)) {
      throw new IllegalArgumentException("No initial state in one of " + fst1 + " or " + fst2);
    }

    IndexPair p = new IndexPair(s1.getId(), s2.getId());

    MutableState newStart = res.newStartState();
    newStart.setFinalWeight(semiring.times(s1.getFinalWeight(), s2.getFinalWeight()));
    stateMap.put(p, newStart.getId());
    queue.add(p);

    SymbolTable.InvertedSymbolTable fst1In = fst1.getInputSymbols().invert();
    SymbolTable.InvertedSymbolTable fst1Out = fst1.getOutputSymbols().invert();
    SymbolTable.InvertedSymbolTable fst2In = fst2.getInputSymbols().invert();
    SymbolTable.InvertedSymbolTable fst2Out = fst2.getOutputSymbols().invert();

    while (!queue.isEmpty()) {
      p = queue.remove(0);
      s1 = fst1.getState(p.getLeft());
      s2 = fst2.getState(p.getRight());
      s = res.getState(stateMap.get(p));
      int numArcs1 = s1.getArcCount();
      int numArcs2 = s2.getArcCount();
      for (int i = 0; i < numArcs1; i++) {
        Arc a1 = s1.getArc(i);
        for (int j = 0; j < numArcs2; j++) {
          Arc a2 = s2.getArc(j);
          if (sorted && a1.getOlabel() < a2.getIlabel()) {
            break;
          }
          String a1Isym = fst1In.keyForId(a1.getIlabel());
          String a1Osym = fst1Out.keyForId(a1.getOlabel());
          String a2Isym = fst2In.keyForId(a2.getIlabel());
          String a2Osym = fst2Out.keyForId(a2.getOlabel());
          if (a1Osym.equalsIgnoreCase(a2Isym)) {
            State nextState1 = a1.getNextState();
            State nextState2 = a2.getNextState();
            IndexPair nextPair = new IndexPair(nextState1.getId(), nextState2.getId());
            Integer nextState = stateMap.get(nextPair);
            MutableState realNextState;
            if (nextState == null) {
              realNextState = res.newState();
              realNextState.setFinalWeight(semiring.times(
                  nextState1.getFinalWeight(),
                  nextState2.getFinalWeight()));
              stateMap.put(nextPair, realNextState.getId());
              queue.add(nextPair);
            } else {
              realNextState = res.getState(nextState);
            }
            res.addArc(s, a1Isym, a2Osym, realNextState, semiring.times(a1.getWeight(), a2.getWeight()));
          }
        }
      }
    }

    return res;
  }

  public static PrecomputedComposeFst precomputeInner(Fst fst2, Semiring semiring) {
    fst2.throwIfInvalid();
    MutableFst mutableFst = MutableFst.copyFrom(fst2);
    WriteableSymbolTable table = mutableFst.getInputSymbols();
    int e1index = getOrAddEps(table, true);
    int e2index = getOrAddEps(table, false);
    String eps1 = table.invert().keyForId(e1index);
    String eps2 = table.invert().keyForId(e2index);
    augment(INPUT, mutableFst, semiring, eps1, eps2);
    return new PrecomputedComposeFst(eps1, eps2, new ImmutableFst(mutableFst), semiring);
  }

  public static MutableFst composeWithPrecomputed(MutableFst fst1, PrecomputedComposeFst fst2) {
    fst1.throwIfInvalid();
    Semiring semiring = fst2.getSemiring();

    WriteableSymbolTable table = symbolTableEffectiveCopy(fst1.getOutputSymbols());
    int e1index = getOrAddEps(table, true);
    int e2index = getOrAddEps(table, false);
    String eps1 = table.invert().keyForId(e1index);
    String eps2 = table.invert().keyForId(e2index);
    Preconditions.checkArgument(eps1.equals(fst2.getEps1()));
    Preconditions.checkArgument(eps2.equals(fst2.getEps2()));

    MutableFst filter = makeFilter(table, semiring, eps1, eps2);
    augment(OUTPUT, fst1, semiring, eps1, eps2);

    MutableFst tmp = Compose.compose(fst1, filter, semiring, false);
    MutableFst res = Compose.compose(tmp, fst2.getFst(), semiring, false);
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

    WriteableSymbolTable table = symbolTableEffectiveCopy(fst1.getOutputSymbols());

    int e1index = getOrAddEps(table, true);
    int e2index = getOrAddEps(table, false);
    String eps1 = table.invert().keyForId(e1index);
    String eps2 = table.invert().keyForId(e2index);
    MutableFst filter = makeFilter(table, semiring, eps1, eps2);
    augment(OUTPUT, fst1, semiring, eps1, eps2);
    augment(INPUT, fst2, semiring, eps1, eps2);

    MutableFst tmp = Compose.compose(fst1, filter, semiring, false);
    MutableFst res = Compose.compose(tmp, fst2, semiring, false);

    // Connect.apply(res);

    return res;
  }

  private static int getOrAddEps(WriteableSymbolTable table, boolean isFirst) {
    return table.getOrAdd("<$$compose$$eps" + (isFirst ? "1" : "2") + ">");
  }

  public static MutableFst composeSimple(Fst fst1, Fst fst2, Semiring semiring) {
    return Compose.compose(fst1, fst2, semiring, false);
  }

  /**
   * Get a filter to use for avoiding multiple epsilon paths in the resulting Fst
   *
   * See: M. Mohri, "Weighted automata algorithms", Handbook of Weighted Automata. Springer, pp. 213-250, 2009.
   *
   * @param table    the filter's input/output symbols
   * @param semiring the semiring to use in the operation
   */
  private static MutableFst makeFilter(WriteableSymbolTable table, Semiring semiring, String eps1, String eps2) {

    MutableFst filter = new MutableFst(semiring, table, table);

    // State 0
    MutableState s0 = filter.newStartState();
    s0.setFinalWeight(semiring.one());
    MutableState s1 = filter.newState();
    s1.setFinalWeight(semiring.one());
    MutableState s2 = filter.newState();
    s2.setFinalWeight(semiring.one());

    filter.addArc(s0, eps2, eps1, s0, semiring.one());
    filter.addArc(s0, eps1, eps1, s1, semiring.one());
    filter.addArc(s0, eps2, eps2, s2, semiring.one());
    // self loops
    filter.addArc(s1, eps1, eps1, s1, semiring.one());
    filter.addArc(s2, eps2, eps2, s2, semiring.one());
    for (ObjectIntCursor<String> cursor : table) {
      int i = cursor.value;
      String key = cursor.key;
      if (key.equals(Fst.EPS) || key.equals(eps1) || key.equals(eps2)) {
        continue;
      }
      filter.addArc(s0, i, i, s0, semiring.one());
      filter.addArc(s1, i, i, s0, semiring.one());
      filter.addArc(s2, i, i, s0, semiring.one());
    }

    // now we need to augment the input fsts to emit the eps1/eps2 in their I/O labels to compose this

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
  private static void augment(AugmentLabels label, MutableFst fst, Semiring semiring, String eps1, String eps2) {
    // label: 0->augment on ilabel
    // 1->augment on olabel

    int e1inputIndex = fst.getInputSymbols().getOrAdd(eps1);
    int e2inputIndex = fst.getInputSymbols().getOrAdd(eps2);

    int e1outputIndex = fst.getOutputSymbols().getOrAdd(eps1);
    int e2outputIndex = fst.getOutputSymbols().getOrAdd(eps2);

    int iEps = fst.getInputSymbols().get(Fst.EPS);
    int oEps = fst.getOutputSymbols().get(Fst.EPS);

    int numStates = fst.getStateCount();
    for (int i = 0; i < numStates; i++) {
      MutableState s = fst.getState(i);
      for (MutableArc arc : s.getArcs()) {
        if ((label == OUTPUT) && (arc.getOlabel() == oEps)) {
          arc.setOlabel(e2outputIndex);
        } else if ((label == INPUT) && (arc.getIlabel() == iEps)) {
          arc.setIlabel(e1inputIndex);
        }
      }
      if (label == INPUT) {
        fst.addArc(s, e2inputIndex, oEps, s, semiring.one());
      } else if (label == OUTPUT) {
        fst.addArc(s, iEps, e1outputIndex, s, semiring.one());
      }
    }
  }
}
