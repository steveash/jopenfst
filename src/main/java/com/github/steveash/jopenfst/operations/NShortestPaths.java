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

/**
 * N-shortest paths operation.
 *
 * See: M. Mohri, M. Riley, "An Efficient Algorithm for the n-best-strings problem", Proceedings of the International
 * Conference on Spoken Language Processing 2002 (ICSLP ’02).
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class NShortestPaths {

  private NShortestPaths() {
  }

  /**
   * Calculates the shortest distances from each state to the final.
   *
   * See: M. Mohri, "Semiring Framework and Algorithms for Shortest-Distance Problems", Journal of Automata, Languages
   * and Combinatorics, 7(3), pp. 321-350, 2002.
   *
   * @param fst the fst to calculate the shortest distances
   * @return the array containing the shortest distances
   */
  private static double[] shortestDistance(Fst fst) {

    Fst reversed = Reverse.reverse(fst);

    double[] d = new double[reversed.getStateCount()];
    double[] r = new double[reversed.getStateCount()];

    Semiring semiring = reversed.getSemiring();

    for (int i = 0; i < d.length; i++) {
      d[i] = semiring.zero();
      r[i] = semiring.zero();
    }

    State[] stateMap = new State[reversed.getStateCount()];
    for (int i = 0; i < reversed.getStateCount(); i++) {
      stateMap[i] = null;
    }
    ArrayList<Integer> queue = new ArrayList<>();

    queue.add(reversed.getStartState().getId());
    stateMap[reversed.getStartState().getId()] = reversed.getStartState();

    d[reversed.getStartState().getId()] = semiring.one();
    r[reversed.getStartState().getId()] = semiring.one();

    while (queue.size() > 0) {
      State q = stateMap[queue.remove(0)];
      double rnew = r[q.getId()];
      r[q.getId()] = semiring.zero();
      int numArcs = q.getNumArcs();
      for (int i = 0; i < numArcs; i++) {
        Arc a = q.getArc(i);
        State nextState = a.getNextState();
        double dnext = d[a.getNextState().getId()];
        double dnextnew = semiring.plus(dnext,
                                        semiring.times(rnew, a.getWeight()));
        if (dnext != dnextnew) {
          d[a.getNextState().getId()] = dnextnew;
          r[a.getNextState().getId()] = semiring.plus(r[a
              .getNextState().getId()], semiring.times(rnew,
                                                       a.getWeight()));
          if (!queue.contains(nextState.getId())) {
            queue.add(nextState.getId());
            stateMap[nextState.getId()] = nextState;
          }
        }
      }
    }
    return d;
  }

  /**
   * Calculates the n-best shortest path from the initial to the final state.
   *
   * @param fst         the fst to calculate the nbest shortest paths
   * @param n           number of best paths to return
   * @return an fst containing the n-best shortest paths
   */
  public static MutableFst apply(Fst fst, int n) {
    fst.throwIfInvalid();
    Semiring semiring = fst.getSemiring();
    double[] d = shortestDistance(fst);

    MutableFst res = MutableFst.emptyWithCopyOfSymbols(fst);
    MutableFst copy = ExtendFinal.apply(fst);

    int[] r = new int[copy.getStateCount()];
    for (int i = 0; i < r.length; i++) {
      r[i] = 0;
    }

    ArrayList<IndexWeight> queue = new ArrayList<>();
    HashMap<IndexWeight, IndexWeight> previous = new HashMap<>(copy.getStateCount());
    // source -> res id
    HashMap<IndexWeight, Integer> stateMap = new HashMap<>(copy.getStateCount());

    State start = copy.getStartState();
    IndexWeight first = new IndexWeight(start.getId(), semiring.one());
    queue.add(first);
    previous.put(first, null);

    while (queue.size() > 0) {
      IndexWeight pair = getLess(queue, d, semiring);
      State prevOld = copy.getState(pair.getIndex());
      double c = pair.getWeight();

      MutableState resNext = new MutableState(prevOld.getFinalWeight());
      res.addState(resNext);
      stateMap.put(pair, resNext.getId());
      IndexWeight prevEntry = previous.get(pair);
      if (prevEntry == null) {
        // this is the start state
        res.setStart(resNext);
      } else {
        // add the incoming arc from previous to current
        MutableState previousStateNew = res.getState(stateMap.get(prevEntry));
        State previousOldState = copy.getState(prevEntry.getIndex());
        int numArcs = previousOldState.getNumArcs();
        for (int j = 0; j < numArcs; j++) {
          Arc a = previousOldState.getArc(j);
          if (a.getNextState().getId() == prevOld.getId()) {
            previousStateNew.addArc(new MutableArc(a.getIlabel(), a.getOlabel(), a.getWeight(), resNext));
          }
        }
      }

      Integer stateIndex = prevOld.getId();
      r[stateIndex]++;

      if ((r[stateIndex] == n) && (res.getSemiring().isNotZero(prevOld.getFinalWeight()))) {
        break;
      }

      if (r[stateIndex] <= n) {
        int numArcs = prevOld.getNumArcs();
        for (int j = 0; j < numArcs; j++) {
          Arc a = prevOld.getArc(j);
          double cnew = semiring.times(c, a.getWeight());
          IndexWeight next = new IndexWeight(a.getNextState().getId(), cnew);
          previous.put(next, pair);
          queue.add(next);
        }
      }
    }

    return res;
  }

  /**
   * Removes from the queue the pair with the lower path cost
   */
  private static IndexWeight getLess(ArrayList<IndexWeight> queue, double[] d,
                                     Semiring semiring) {
    IndexWeight res = queue.get(0);

    for (IndexWeight p : queue) {
      int previousStateId = res.getIndex();
      int nextStateId = p.getIndex();
      double previous = res.getWeight();
      double next = p.getWeight();
      if (semiring.naturalLess(
          semiring.times(next, d[nextStateId]),
          semiring.times(previous, d[previousStateId]))) {
        res = p;
      }
    }
    queue.remove(res);
    return res;
  }
}
