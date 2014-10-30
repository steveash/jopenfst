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

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.HashMap;

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

  private static MutablePair<State, Float> getPair(
      ArrayList<MutablePair<State, Float>> queue, State state, Float zero) {
    MutablePair<State, Float> res = null;
    for (MutablePair<State, Float> tmp : queue) {
      if (state.getId() == tmp.getLeft().getId()) {
        res = tmp;
        break;
      }
    }

    if (res == null) {
      res = MutablePair.of(state, zero);
      queue.add(res);
    }

    return res;
  }

  private static ArrayList<Integer> getUniqueLabels(Fst fst,
                                                    ArrayList<MutablePair<State, Float>> pa) {
    ArrayList<Integer> res = new ArrayList<>();

    for (MutablePair<State, Float> p : pa) {
      State s = p.getLeft();

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

  private static State getStateLabel(ArrayList<MutablePair<State, Float>> pa,
                                     HashMap<String, State> stateMapper) {
    StringBuilder sb = new StringBuilder();

    for (MutablePair<State, Float> p : pa) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append("(").append(p.getLeft()).append(",").append(p.getRight()).append(")");
    }
    return stateMapper.get(sb.toString());
  }

  /**
   * Determinizes an fst. The result will be an equivalent fst that has the property that no state has two transitions
   * with the same input label. For this algorithm, epsilon transitions are treated as regular symbols.
   *
   * @param fst the fst to determinize
   * @return the determinized fst
   */
  public static Fst get(Fst fst) {

    if (fst.getSemiring() == null) {
      throw new IllegalArgumentException("FST " + fst + " has no semiring");
    }

    // initialize the queue and new fst
    Semiring semiring = fst.getSemiring();
    Fst res = new Fst(semiring);
    res.setInputSymbolsFrom(fst);
    res.setOutputSymbolsFrom(fst);

    // stores the queue (item in index 0 is next)
    ArrayList<ArrayList<MutablePair<State, Float>>> queue = new ArrayList<>();

    HashMap<String, State> stateMapper = new HashMap<>();

    State s = new State(semiring.zero());
    String stateString = "(" + fst.getStart() + "," + semiring.one() + ")";
    queue.add(new ArrayList<MutablePair<State, Float>>());
    queue.get(0).add(MutablePair.of(fst.getStart(), semiring.one()));
    res.addState(s);
    stateMapper.put(stateString, s);
    res.setStart(s);

    while (queue.size() > 0) {
      ArrayList<MutablePair<State, Float>> p = queue.get(0);
      State pnew = getStateLabel(p, stateMapper);
      queue.remove(0);
      ArrayList<Integer> labels = getUniqueLabels(fst, p);
      for (int label : labels) {
        Float wnew = semiring.zero();
        // calc w'
        for (MutablePair<State, Float> ps : p) {
          State old = ps.getLeft();
          Float u = ps.getRight();
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
        ArrayList<MutablePair<State, Float>> forQueue = new ArrayList<>();
        for (MutablePair<State, Float> ps : p) {
          State old = ps.getLeft();
          Float u = ps.getRight();
          Float wnewRevert = semiring.divide(semiring.one(), wnew);
          int numArcs = old.getNumArcs();
          for (int j = 0; j < numArcs; j++) {
            Arc arc = old.getArc(j);
            if (label == arc.getIlabel()) {
              State oldstate = arc.getNextState();
              MutablePair<State, Float> pair = getPair(forQueue,
                                                oldstate, semiring.zero());
              pair.setRight(semiring.plus(
                  pair.getRight(),
                  semiring.times(wnewRevert,
                                 semiring.times(u, arc.getWeight()))));
            }
          }
        }

        // build new state's id and new elements for queue
        String qnewid = "";
        for (MutablePair<State, Float> ps : forQueue) {
          State old = ps.getLeft();
          Float unew = ps.getRight();
          if (!qnewid.equals("")) {
            qnewid = qnewid + ",";
          }
          qnewid = qnewid + "(" + old + "," + unew + ")";
        }

        if (stateMapper.get(qnewid) == null) {
          State qnew = new State(semiring.zero());
          res.addState(qnew);
          stateMapper.put(qnewid, qnew);
          // update new state's weight
          Float fw = qnew.getFinalWeight();
          for (MutablePair<State, Float> ps : forQueue) {
            fw = semiring.plus(fw, semiring.times(ps.getLeft()
                                                      .getFinalWeight(), ps.getRight()));
          }
          qnew.setFinalWeight(fw);

          queue.add(forQueue);
        }
        pnew.addArc(new Arc(label, label, wnew, stateMapper.get(qnewid)));
      }
    }

    return res;
  }
}
