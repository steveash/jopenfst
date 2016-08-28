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

import com.carrotsearch.hppc.IntOpenHashSet;
import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;

import java.util.ArrayList;

/**
 * Connect operation which only preserves states/arcs that are on paths which
 * are accessible from the start and co-accessible to a final state; i.e. only
 * retain paths that can be hit on all paths between start -> final
 */
public class Connect {

  /**
   * Trims an Fst, removing states and arcs that are not on successful paths.
   *
   * @param fst the fst to trim
   */
  public static void apply(MutableFst fst) {
    fst.throwIfInvalid();

    IntOpenHashSet accessible = new IntOpenHashSet(fst.getStateCount());
    IntOpenHashSet coaccessible = new IntOpenHashSet(fst.getStateCount());
    dfsForward(fst.getStartState(), accessible);
    int numStates = fst.getStateCount();
    for (int i = 0; i < numStates; i++) {
      MutableState s = fst.getState(i);
      if (fst.getSemiring().isNotZero(s.getFinalWeight())) {
        dfsBackward(s, coaccessible);
      }
    }

    if (accessible.size() == fst.getStateCount() && coaccessible.size() == fst.getStateCount()) {
      // common case, optimization bail early
      return;
    }

    ArrayList<MutableState> toDelete = new ArrayList<>();
    for (int i = 0; i < numStates; i++) {
      MutableState s = fst.getState(i);
      if (!accessible.contains(s.getId()) || !coaccessible.contains(s.getId())) {
        toDelete.add(s);
      }
    }
    fst.deleteStates(toDelete);
  }

  private static void dfsBackward(MutableState state, IntOpenHashSet coaccessible) {
    coaccessible.add(state.getId());
    for (MutableState incoming : state.getIncomingStates()) {
      if (!coaccessible.contains(incoming.getId())) {
        dfsBackward(incoming, coaccessible);
      }
    }
  }

  private static void dfsForward(MutableState start, IntOpenHashSet accessible) {
    accessible.add(start.getId());
    for (MutableArc arc : start.getArcs()) {
      MutableState nextState = arc.getNextState();
      if (!accessible.contains(nextState.getId())) {
        dfsForward(nextState, accessible);
      }
    }
  }
}
