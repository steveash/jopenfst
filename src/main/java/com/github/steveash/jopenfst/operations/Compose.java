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
import com.github.steveash.jopenfst.utils.FstUtils;
import com.google.common.collect.Maps;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

import static com.github.steveash.jopenfst.utils.FstUtils.LOG_REPORTER;
import static com.github.steveash.jopenfst.utils.FstUtils.symbolTableEffectiveCopy;

/**
 * Compose operation.
 *
 * There are two ways to use compose: `compose(a, b, ring)` which does the compose on a b. However, if you are going
 * to be running with the same b over and over again, you can optimize some of the work by precomputing the b via
 * `precomputeInner(b)` and then subsequently calling `composeWithPrecomputed()
 *
 * See: M. Mohri, "Weighted automata algorithms", Handbook of Weighted Automata. Springer, pp. 213-250, 2009.
 *
 * @author John Salatas jsalatas@users.sourceforge.net
 */
public class Compose {

  private enum AugmentLabels {INPUT, OUTPUT}

  /**
   * Pre-processes a FST that is going to be used on the right hand side of a compose operator many times
   * @param fst2 the fst that will appear on the right hand side
   * @param semiring the semiring that will be used for the compose operation
   * @return a pre-processed form of the inner fst that can be passed to `composeWithPrecomputed`
   */
  public static PrecomputedComposeFst precomputeInner(Fst fst2, Semiring semiring) {
    fst2.throwIfInvalid();
    MutableFst mutableFst = MutableFst.copyFrom(fst2);
    WriteableSymbolTable table = mutableFst.getInputSymbols();
    int e1index = getOrAddEps(table, true);
    int e2index = getOrAddEps(table, false);
    String eps1 = table.invert().keyForId(e1index);
    String eps2 = table.invert().keyForId(e2index);
    augment(AugmentLabels.INPUT, mutableFst, semiring, eps1, eps2);
    ArcSort.sortByInput(mutableFst);

    MutableFst filter = makeFilter(table, semiring, eps1, eps2);
    ArcSort.sortByInput(filter);

    return new PrecomputedComposeFst(eps1, eps2, new ImmutableFst(mutableFst), semiring, new ImmutableFst(filter));
  }

  public static MutableFst composeWithPrecomputed(MutableFst fst1, PrecomputedComposeFst fst2) {
    return composeWithPrecomputed(fst1, fst2, false);
  }

  public static MutableFst composeWithPrecomputed(MutableFst fst1, PrecomputedComposeFst fst2, boolean useSorted) {
    return composeWithPrecomputed(fst1, fst2, useSorted, true);
  }

  /**
   * Executes a compose of fst1 o fst2, with fst2 being a precomputed/preprocessed fst (for performance reasons)
   * @param fst1 outer fst
   * @param fst2 inner fst
   * @param useSorted if true, then performance will be faster; NOTE fst1 must be sorted by OUTPUT labels
   * @param trimOutput if true, then output will be trimmed before returning
   * @return
   */
  public static MutableFst composeWithPrecomputed(MutableFst fst1, PrecomputedComposeFst fst2, boolean useSorted, boolean trimOutput) {
    fst1.throwIfInvalid();
    if (useSorted) {
      if (fst1.getOutputSymbols() != fst2.getFstInputSymbolsAsFrozen() &&
          fst1.getOutputSymbols() != fst2.getFst().getInputSymbols()) {
        throw new IllegalArgumentException("When using the precomputed and useSorted optimization, you must have " +
            "the outer's output symbol be the same symbol table as the inner's input");
      }
    }
    Semiring semiring = fst2.getSemiring();

    augment(AugmentLabels.OUTPUT, fst1, semiring, fst2.getEps1(), fst2.getEps2());
    if (useSorted) {
      ArcSort.sortByOutput(fst1);
    }
    ImmutableFst filter = fst2.getFilterFst();
    MutableFst tmp = Compose.doCompose(fst1, filter, semiring, useSorted);
    if (useSorted) {
      ArcSort.sortByOutput(tmp);
    }
    MutableFst res = Compose.doCompose(tmp, fst2.getFst(), semiring, useSorted);
    // definitionally the output of compose should be trimmed, but if you don't care, you can save some cpu
    if (trimOutput) {
      Connect.apply(res);
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
    return compose(fst1, fst2, semiring, false);
  }

  /**
   * Computes the composition of two Fsts. The two Fsts are augmented in order to avoid multiple epsilon paths in the
   * resulting Fst
   *
   * @param fst1     the first Fst
   * @param fst2     the second Fst
   * @param semiring the semiring to use in the operation
   * @param useSorted if true then the input fsts should be sorted
   * @return the composed Fst
   */
  public static MutableFst compose(MutableFst fst1, MutableFst fst2, Semiring semiring, boolean useSorted) {
    fst1.throwIfInvalid();
    fst2.throwIfInvalid();
    if (!FstUtils.symbolTableEquals(fst1.getOutputSymbols(), fst2.getInputSymbols())) {
      throw new IllegalArgumentException("Symbol tables don't match, cant compose " + fst1 + " to " + fst2);
    }

    WriteableSymbolTable table = symbolTableEffectiveCopy(fst1.getOutputSymbols());

    int e1index = getOrAddEps(table, true);
    int e2index = getOrAddEps(table, false);
    String eps1 = table.invert().keyForId(e1index);
    String eps2 = table.invert().keyForId(e2index);
    MutableFst filter = makeFilter(table, semiring, eps1, eps2);
    augment(AugmentLabels.OUTPUT, fst1, semiring, eps1, eps2);
    augment(AugmentLabels.INPUT, fst2, semiring, eps1, eps2);
    if (useSorted) {
      ArcSort.sortByOutput(fst1);
      ArcSort.sortByInput(filter);
    }
    assert(FstUtils.symbolTableEquals(fst1.getOutputSymbols(), filter.getInputSymbols(), LOG_REPORTER));
    MutableFst tmp = Compose.doCompose(fst1, filter, semiring, useSorted);
    if (useSorted) {
      ArcSort.sortByOutput(tmp);
    }
    assert(FstUtils.symbolTableEquals(tmp.getOutputSymbols(), fst2.getInputSymbols(), LOG_REPORTER));
    MutableFst res = Compose.doCompose(tmp, fst2, semiring, useSorted);
    Connect.apply(res);

    return res;
  }

  private static int getOrAddEps(WriteableSymbolTable table, boolean isFirst) {
    return table.getOrAdd("<$$compose$$eps" + (isFirst ? "1" : "2") + ">");
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
        if ((label == AugmentLabels.OUTPUT) && (arc.getOlabel() == oEps)) {
          arc.setOlabel(e2outputIndex);
        } else if ((label == AugmentLabels.INPUT) && (arc.getIlabel() == iEps)) {
          arc.setIlabel(e1inputIndex);
        }
      }
      if (label == AugmentLabels.INPUT) {
        fst.addArc(s, e2inputIndex, oEps, s, semiring.one());
      } else if (label == AugmentLabels.OUTPUT) {
        fst.addArc(s, iEps, e1outputIndex, s, semiring.one());
      }
    }
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
  private static MutableFst doCompose(Fst fst1, Fst fst2, Semiring semiring, boolean useSorted) {
    if (useSorted) {
      assert(FstUtils.symbolTableEquals(fst1.getOutputSymbols(), fst2.getInputSymbols()));
    }

    MutableFst res = new MutableFst(semiring, symbolTableEffectiveCopy(fst1.getInputSymbols()),
        symbolTableEffectiveCopy(fst2.getOutputSymbols()));

    // state map is q -> n where q is (q_i, q_j) a state pair made up of state from fst1 and a state from fst2 and
    // n is the new state index in the composed FST that represents this tuple
    HashMap<IndexPair, Integer> stateMap = Maps.newHashMap();
    Deque<IndexPair> queue = new LinkedList<>();

    IndexPair first = new IndexPair(fst1.getStartState().getId(), fst2.getStartState().getId());
    MutableState newStart = res.newStartState();
    newStart.setFinalWeight(semiring.times(fst1.getStartState().getFinalWeight(), fst2.getStartState().getFinalWeight()));
    stateMap.put(first, newStart.getId());
    queue.addLast(first);

    SymbolTable.InvertedSymbolTable fst1In = fst1.getInputSymbols().invert();
    SymbolTable.InvertedSymbolTable fst1Out = fst1.getOutputSymbols().invert();
    SymbolTable.InvertedSymbolTable fst2In = fst2.getInputSymbols().invert();
    SymbolTable.InvertedSymbolTable fst2Out = fst2.getOutputSymbols().invert();

    while (!queue.isEmpty()) {
      IndexPair p = queue.removeFirst();
      State s1 = fst1.getState(p.getLeft());
      State s2 = fst2.getState(p.getRight());
      MutableState resultState = res.getState(stateMap.get(p));
      if (useSorted) {
        assert (ArcSort.isSorted(s1, OLabelCompare.INSTANCE) && ArcSort.isSorted(s2, ILabelCompare.INSTANCE)) :
            "\ns1 " + s1.getArcs() + "\n s2 " + s2.getArcs();
      }

      int jstart = 0; // if not sorted jstart is never updated so always does full nested loop
      for (int i = 0; i < s1.getArcCount(); ++i) {
        Arc a1 = s1.getArc(i);
        for (int j = jstart; j < s2.getArcCount(); ++j) {
          Arc a2 = s2.getArc(j);
          if (useSorted && a1.getOlabel() < a2.getIlabel()) {
            break; // if we know the arcs are sorted then once we've gotten here we know there cant be more j's
          }
          String a1Isym = fst1In.keyForId(a1.getIlabel());
          String a1Osym = fst1Out.keyForId(a1.getOlabel());
          String a2Isym = fst2In.keyForId(a2.getIlabel());
          String a2Osym = fst2Out.keyForId(a2.getOlabel());
          if (a1Osym.equals(a2Isym)) {
            State nextState1 = a1.getNextState();
            State nextState2 = a2.getNextState();
            IndexPair nextPair = new IndexPair(nextState1.getId(), nextState2.getId());
            Integer nextState = stateMap.get(nextPair);
            MutableState realNextState;
            if (nextState == null) {
              realNextState = res.newState();
              realNextState.setFinalWeight(semiring.times(nextState1.getFinalWeight(), nextState2.getFinalWeight()));
              stateMap.put(nextPair, realNextState.getId());
              queue.addLast(nextPair);
            } else {
              realNextState = res.getState(nextState);
            }
            res.addArc(resultState, a1Isym, a2Osym, realNextState, semiring.times(a1.getWeight(), a2.getWeight()));
          } else if (useSorted && a1.getOlabel() > a2.getIlabel()) {
            // if we're sorted and outer is greater then we know we'll never need to eval this inner index again
            jstart = j + 1;
          }
        }
      }
    }
    return res;
  }
}
