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

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.IndexWeight;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Determize operation.
 *
 * See: M. Mohri, "Finite-State Transducers in Language and Speech Processing", Computational Linguistics, 23:2, 1997.
 *
 * @author John Salatas jsalatas@users.sourceforge.net
 */
public class Determinize {

  /**
   * Determinizes an fst. The result will be an equivalent fst that has the property that no state has two transitions
   * with the same input label. For this algorithm, epsilon transitions are treated as regular symbols.
   *
   * @param fst the fst to determinize
   * @return the determinized fst
   */
  public static MutableFst apply(Fst fst) {
    fst.throwIfInvalid();

    // initialize the queue and new fst
    Semiring semiring = fst.getSemiring();
    MutableFst res = MutableFst.emptyWithCopyOfSymbols(fst);

    // stores the queue (item in index 0 is next) indexes here always refer to the input fst
    Deque<ArrayList<IndexWeight>> queue = new LinkedList<>();
    HashMap<String, MutableState> stateMapper = new HashMap<>();

    MutableState s = new MutableState(semiring.zero());
    queue.add(new ArrayList<IndexWeight>());
    IndexWeight initial = new IndexWeight(fst.getStartState().getId(), semiring.one());
    String initialLabel = makeStateLabel(ImmutableList.of(initial));
    queue.peekFirst().add(initial);
    res.addState(s);
    stateMapper.put(initialLabel, s);
    res.setStart(s);

    while (!queue.isEmpty()) {
      ArrayList<IndexWeight> entry = queue.removeFirst();
      String thisLabel = makeStateLabel(entry);
      MutableState thisState = stateMapper.get(thisLabel);
      Preconditions.checkNotNull(thisState, "something is wrong with labels ", thisLabel);
      IntArrayList labels = getUniqueInputs(fst, entry);
      for (IntCursor cursor : labels) {
        int label = cursor.value;
        double newWeight = calculateNewEntryWeight(fst, semiring, entry, label);
        ArrayList<IndexWeight> newPartials = calcNewPartials(fst, semiring, entry, label, newWeight);

        // build new state's id and new elements for queue
        String newStateLabel = makeStateLabel(newPartials);
        if (stateMapper.get(newStateLabel) == null) {
          MutableState newState = new MutableState(semiring.zero());
          res.addState(newState);
          stateMapper.put(newStateLabel, newState);
          double newStateWeight = newState.getFinalWeight();
          for (IndexWeight iw : newPartials) {
            double connectingWeight = fst.getState(iw.getIndex()).getFinalWeight();
            newStateWeight = semiring.plus(newStateWeight, semiring.times(connectingWeight, iw.getWeight()));
          }
          newState.setFinalWeight(newStateWeight);
          queue.addLast(newPartials);
        }
        res.addArc(thisState, label, label, stateMapper.get(newStateLabel), newWeight);
      }
    }

    return res;
  }

  private static ArrayList<IndexWeight> calcNewPartials(Fst fst, Semiring semiring, ArrayList<IndexWeight> entry, int label, double newWeight) {
    // calc new states; keep accumulated residual weights in forQueue
    ArrayList<IndexWeight> forQueue = new ArrayList<>();
    for (IndexWeight iw : entry) {
      State oldState = fst.getState(iw.getIndex());
      double partial = iw.getWeight();
      double revertWeight = semiring.divide(semiring.one(), newWeight);
      for (int j = 0; j < oldState.getArcCount(); j++) {
        Arc arc = oldState.getArc(j);
        if (label == arc.getIlabel()) {
          State oldNextState = arc.getNextState();
          double toAdd = semiring.times(revertWeight, semiring.times(partial, arc.getWeight()));
          incrementOrAppend(forQueue, oldNextState, semiring, toAdd);
        }
      }
    }
    return forQueue;
  }

  private static double calculateNewEntryWeight(Fst fst, Semiring semiring, ArrayList<IndexWeight> entry, int label) {
    double newWeight = semiring.zero();
    for (IndexWeight iw : entry) {
      State oldState = fst.getState(iw.getIndex());
      double partialWeight = iw.getWeight();
      for (int j = 0; j < oldState.getArcCount(); j++) {
        Arc arc = oldState.getArc(j);
        if (label == arc.getIlabel()) {
          newWeight = semiring.plus(newWeight, semiring.times(partialWeight, arc.getWeight()));
        }
      }
    }
    return newWeight;
  }

  private static void incrementOrAppend(ArrayList<IndexWeight> queue, State state, Semiring semiring, double toAdd) {
    for (int i = 0; i < queue.size(); i++) {
      IndexWeight maybe = queue.get(i);
      if (state.getId() == maybe.getIndex()) {
        // already there so lets replace this with an incremented version
        double updatedVal = semiring.plus(maybe.getWeight(), toAdd);
        IndexWeight updated = new IndexWeight(maybe.getIndex(), updatedVal);
        queue.set(i, updated);
        return;
      }
    }
    // didn't find existing so add it with this weight
    queue.add(new IndexWeight(state.getId(), semiring.plus(semiring.zero(), toAdd)));
  }

  private static IntArrayList getUniqueInputs(Fst fst, ArrayList<IndexWeight> entry) {
    IntOpenHashSet inputIds = new IntOpenHashSet();
    IntArrayList result = new IntArrayList();
    for (IndexWeight iw : entry) {
      State s = fst.getState(iw.getIndex());
      for (int j = 0; j < s.getArcCount(); j++) {
        Arc arc = s.getArc(j);
        if (inputIds.add(arc.getIlabel())) {
          result.add(arc.getIlabel());
        }
      }
    }
    return result;
  }

  private static String makeStateLabel(List<IndexWeight> pa) {
    StringBuilder sb = new StringBuilder();

    for (IndexWeight p : pa) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append("(").append(p.getIndex()).append(",").append(p.getWeight()).append(")");
    }
    return sb.toString();
  }
}
