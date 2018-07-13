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

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.IndexWeight;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Ordering;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * N-shortest paths operation.
 *
 * See: M. Mohri, M. Riley, "An Efficient Algorithm for the n-best-strings problem", Proceedings of the International
 * Conference on Spoken Language Processing 2002 (ICSLP ’02).
 *
 * @author John Salatas jsalatas@users.sourceforge.net
 */
public class NShortestPaths {

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

    IntObjectOpenHashMap<State> stateMap = new IntObjectOpenHashMap<>();
    Deque<Integer> queue = new LinkedList<>();
    IntOpenHashSet enqueuedStateIds = new IntOpenHashSet();

    queue.addLast(reversed.getStartState().getId());
    stateMap.put(reversed.getStartState().getId(), reversed.getStartState());

    d[reversed.getStartState().getId()] = semiring.one();
    r[reversed.getStartState().getId()] = semiring.one();

    while (!queue.isEmpty()) {
      int thisStateId = queue.removeFirst();
      enqueuedStateIds.remove(thisStateId);
      State thisState = stateMap.get(thisStateId);
      double rnew = r[thisState.getId()];
      r[thisState.getId()] = semiring.zero();

      for (int i = 0; i < thisState.getArcCount(); i++) {
        Arc arc = thisState.getArc(i);
        State nextState = arc.getNextState();
        double dnext = d[arc.getNextState().getId()];
        double dnextnew = semiring.plus(dnext, semiring.times(rnew, arc.getWeight()));
        if (dnext != dnextnew) {
          d[arc.getNextState().getId()] = dnextnew;
          r[arc.getNextState().getId()] = semiring.plus(r[arc.getNextState().getId()], semiring.times(rnew, arc.getWeight()));
          int nextStateId = nextState.getId();
          if (!enqueuedStateIds.contains(nextStateId)) {
            queue.addLast(nextStateId);
            enqueuedStateIds.add(nextStateId);
            stateMap.put(nextStateId, nextState);
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
   * @param topk           number of best paths to return
   * @return an fst containing the n-best shortest paths
   */
  public static MutableFst apply(Fst fst, int topk) {
    fst.throwIfInvalid();
    final Semiring semiring = fst.getSemiring();
    final double[] d = shortestDistance(fst);

    MutableFst res = MutableFst.emptyWithCopyOfSymbols(fst);
    MutableFst copy = ExtendFinal.apply(fst);

    int[] r = new int[copy.getStateCount()];
    for (int i = 0; i < r.length; i++) {
      r[i] = 0;
    }

    MinMaxPriorityQueue<IndexWeight> qq = MinMaxPriorityQueue.orderedBy(new Ordering<IndexWeight>() {
      @Override
      public int compare(IndexWeight left, IndexWeight right) {
        double dx = d[left.getIndex()];
        double dy = d[right.getIndex()];
        double wx = semiring.times(dx, left.getWeight());
        double wy = semiring.times(dy, right.getWeight());
        if (semiring.naturalLess(wx, wy)) {
          return -1;
        }
        if (semiring.naturalLess(wy, wx)) {
          return +1;
        }
        return 0;
      }
    }).create();
    HashMap<IndexWeight, IndexWeight> previous = new HashMap<>(copy.getStateCount());
    // source -> res id
    HashMap<IndexWeight, Integer> stateMap = new HashMap<>(copy.getStateCount());

    State start = copy.getStartState();
    IndexWeight first = new IndexWeight(start.getId(), semiring.one());
    qq.add(first);
    previous.put(first, null);

    while (!qq.isEmpty()) {
      IndexWeight pair = qq.removeFirst();
      State prevOld = copy.getState(pair.getIndex());
      double pairWeight = pair.getWeight();

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
        int numArcs = previousOldState.getArcCount();
        for (int j = 0; j < numArcs; j++) {
          Arc a = previousOldState.getArc(j);
          if (a.getNextState().getId() == prevOld.getId()) {
            res.addArc(previousStateNew, a.getIlabel(), a.getOlabel(), resNext, a.getWeight());
          }
        }
      }

      int stateIndex = prevOld.getId();
      r[stateIndex]++;

      if ((r[stateIndex] == topk) && (res.getSemiring().isNotZero(prevOld.getFinalWeight()))) {
        break;
      }

      if (r[stateIndex] <= topk) {
        int numArcs = prevOld.getArcCount();
        for (int j = 0; j < numArcs; j++) {
          Arc a = prevOld.getArc(j);
          double cnew = semiring.times(pairWeight, a.getWeight());
          IndexWeight next = new IndexWeight(a.getNextState().getId(), cnew);
          previous.put(next, pair);
          qq.add(next);
        }
      }
    }
    return res;
  }
}
