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

/**
 *
 */
package com.github.steveash.jopenfst.operations;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.IndexWeight;
import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Determize operation.
 *
 * See: M. Mohri, "Finite-State Transducers in Language and Speech Processing", Computational Linguistics, 23:2, 1997.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class Determinize {

  /**
   * Default constructor
   */
  private Determinize() {

  }

  private static void incrementOrAppend(
      ArrayList<IndexWeight> queue, State state, Semiring semiring, float toAdd) {
    for (int i = 0; i < queue.size(); i++) {
      IndexWeight maybe = queue.get(i);
      if (state.getId() == maybe.getIndex()) {
        // already there so lets replace this with an incremented version
        float newVal = semiring.plus(maybe.getWeight(), toAdd);
        IndexWeight updated = new IndexWeight(maybe.getIndex(), newVal);
        queue.set(i, updated);
        return;
      }
    }
    // add it with this weight
    queue.add(new IndexWeight(state.getId(), semiring.plus(semiring.zero(), toAdd)));
  }

  private static ArrayList<Integer> getUniqueLabels(Fst fst,
                                                    ArrayList<IndexWeight> pa) {
    ArrayList<Integer> res = new ArrayList<>();

    for (IndexWeight p : pa) {
      State s = fst.getState(p.getIndex());

      int numArcs = s.getNumArcs();
      for (int j = 0; j < numArcs; j++) {
        Arc arc = s.getArc(j);
        if (!res.contains(arc.getIlabel())) {
          res.add(arc.getIlabel());
        }
      }
    }
    return res;
  }

  private static String getStateLabel(List<IndexWeight> pa) {
    StringBuilder sb = new StringBuilder();

    for (IndexWeight p : pa) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append("(").append(p.getIndex()).append(",").append(p.getWeight()).append(")");
    }
    return sb.toString();
  }

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
    MutableFst res = new MutableFst(semiring);
    res.setInputSymbolsFrom(fst);
    res.setOutputSymbolsFrom(fst);

    // stores the queue (item in index 0 is next)
    // indexes here always refer to the input fst
    ArrayList<ArrayList<IndexWeight>> queue = new ArrayList<>();

    HashMap<String, MutableState> stateMapper = new HashMap<>();

    MutableState s = new MutableState(semiring.zero());
    queue.add(new ArrayList<IndexWeight>());
    IndexWeight initialIw = new IndexWeight(fst.getStartState().getId(), semiring.one());
    String initialLabel = getStateLabel(ImmutableList.of(initialIw));
    queue.get(0).add(initialIw);
    res.addState(s);
    stateMapper.put(initialLabel, s);
    res.setStart(s);

    while (!queue.isEmpty()) {
      ArrayList<IndexWeight> p = queue.get(0);
      String thisLabel = getStateLabel(p);
      MutableState pnew = stateMapper.get(thisLabel);
      Preconditions.checkNotNull(pnew, "something is wrong with labels ", thisLabel);
      queue.remove(0);
      ArrayList<Integer> labels = getUniqueLabels(fst, p);
      for (int label : labels) {
        Float wnew = semiring.zero();
        // calc w'
        for (IndexWeight ps : p) {
          State old = fst.getState(ps.getIndex());
          Float u = ps.getWeight();
          int numArcs = old.getNumArcs();
          for (int j = 0; j < numArcs; j++) {
            Arc arc = old.getArc(j);
            if (label == arc.getIlabel()) {
              wnew = semiring.plus(wnew,
                                   semiring.times(u, arc.getWeight()));
            }
          }
        }

        // calc new states
        // keep residual weights to variable forQueue
        ArrayList<IndexWeight> forQueue = new ArrayList<>();
        for (IndexWeight ps : p) {
          State old = fst.getState(ps.getIndex());
          Float u = ps.getWeight();
          Float wnewRevert = semiring.divide(semiring.one(), wnew);
          int numArcs = old.getNumArcs();
          for (int j = 0; j < numArcs; j++) {
            Arc arc = old.getArc(j);
            if (label == arc.getIlabel()) {
              State oldstate = arc.getNextState();
              float toAdd = semiring.times(wnewRevert, semiring.times(u, arc.getWeight()));
              incrementOrAppend(forQueue, oldstate, semiring, toAdd);
            }
          }
        }

        // build new state's id and new elements for queue
        String qnewid = getStateLabel(forQueue);
        if (stateMapper.get(qnewid) == null) {
          MutableState qnew = new MutableState(semiring.zero());
          res.addState(qnew);
          stateMapper.put(qnewid, qnew);
          // update new state's weight
          Float fw = qnew.getFinalWeight();
          for (IndexWeight ps : forQueue) {
            float stateWeight = fst.getState(ps.getIndex()).getFinalWeight();
            fw = semiring.plus(fw, semiring.times(stateWeight, ps.getWeight()));
          }
          qnew.setFinalWeight(fw);

          queue.add(forQueue);
        }
        pnew.addArc(new MutableArc(label, label, wnew, stateMapper.get(qnewid)));
      }
    }

    return res;
  }
}
