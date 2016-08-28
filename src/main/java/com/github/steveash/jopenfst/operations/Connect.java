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
import com.google.common.collect.Sets;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Connect operation.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class Connect {

  /**
   * Calculates the coaccessible states of an fst
   */
  private static void calcCoAccessible(MutableFst fst, MutableState state,
                                       List<IntArrayList> paths,
                                       IntOpenHashSet coaccessible) {
    // hold the coaccessible added in this loop
    ArrayList<MutableState> newCoAccessibles = new ArrayList<>();
    for (IntArrayList path : paths) {
      int index = path.lastIndexOf(state.getId());
      if (index != -1) {
        if (fst.getSemiring().isNotZero(state.getFinalWeight()) || coaccessible.contains(state.getId())) {
          for (int j = index; j > -1; j--) {
            if (!coaccessible.contains(path.get(j))) {
              newCoAccessibles.add(fst.getState(path.get(j)));
              coaccessible.add(path.get(j));
            }
          }
        }
      }
    }

    // run again for the new coaccessibles
    for (MutableState s : newCoAccessibles) {
      calcCoAccessible(fst, s, paths, coaccessible);
    }
  }

  /**
   * Copies a path
   */
  private static void duplicatePath(int lastPathIndex, MutableState fromState,
                                    MutableState toState, List<IntArrayList> paths) {
    IntArrayList lastPath = paths.get(lastPathIndex);
    // copy the last path to a new one, from start to current state
    int fromIndex = lastPath.indexOf(fromState.getId());
    int toIndex = lastPath.indexOf(toState.getId());
    if (toIndex == -1) {
      toIndex = lastPath.size() - 1;
    }
    // sublist
    IntArrayList newPath = new IntArrayList(1 + (toIndex - fromIndex));
    for (int i = fromIndex; i < toIndex; i++) {
      newPath.add(lastPath.get(i));
    }
    paths.add(newPath);
  }

  /**
   * The depth first search recursion
   */
  private static MutableState dfs(MutableFst fst, MutableState start,
                           List<IntArrayList> paths, List<Set<Arc>> exploredArcs,
                           IntOpenHashSet accessible) {
    int lastPathIndex = paths.size() - 1;

    Set<Arc> currentExploredArcs = exploredArcs.get(start.getId());
    paths.get(lastPathIndex).add(start.getId());
    if (start.getArcCount() != 0) {
      int arcCount = 0;
      int numArcs = start.getArcCount();
      for (int j = 0; j < numArcs; j++) {
        MutableArc arc = start.getArc(j);
        if ((currentExploredArcs == null) || !currentExploredArcs.contains(arc)) {
          lastPathIndex = paths.size() - 1;
          if (arcCount++ > 0) {
            duplicatePath(lastPathIndex, fst.getStartState(), start, paths);
            lastPathIndex = paths.size() - 1;
            paths.get(lastPathIndex).add(start.getId());
          }
          MutableState next = arc.getNextState();
          addExploredArc(start.getId(), arc, exploredArcs);
          // detect self loops
          if (next.getId() != start.getId()) {
            dfs(fst, next, paths, exploredArcs, accessible);
          }
        }
      }
    }
    accessible.add(start.getId());

    return start;
  }

  /**
   * Adds an arc top the explored arcs list
   */
  private static void addExploredArc(int stateId, Arc arc,
                                     List<Set<Arc>> exploredArcs) {
    Set<Arc> ea = exploredArcs.get(stateId);
    if (ea == null) {
      ea = Sets.newIdentityHashSet();
      exploredArcs.set(stateId, ea);
    }
    ea.add(arc);
  }

  /**
   * Initialization of a depth first search recursion
   */
  private static void depthFirstSearch(MutableFst fst, IntOpenHashSet accessible,
                                       List<IntArrayList> paths,
                                       List<Set<Arc>> exploredArcs,
                                       IntOpenHashSet coaccessible) {
    MutableState currentState = fst.getStartState();
    MutableState nextState = currentState;
    do {
      if (!accessible.contains(currentState.getId())) {
        nextState = dfs(fst, currentState, paths, exploredArcs,
                        accessible);
      }
    } while (currentState.getId() != nextState.getId());
    int numStates = fst.getStateCount();
    for (int i = 0; i < numStates; i++) {
      MutableState s = fst.getState(i);
      if (fst.getSemiring().isNotZero(s.getFinalWeight())) {
        calcCoAccessible(fst, s, paths, coaccessible);
      }
    }
  }

  /**
   * Trims an Fst, removing states and arcs that are not on successful paths.
   *
   * @param fst the fst to trim
   */
  public static void apply(MutableFst fst) {
    fst.throwIfInvalid();

    IntOpenHashSet accessible = new IntOpenHashSet(fst.getStateCount());
    IntOpenHashSet coaccessible = new IntOpenHashSet(fst.getStateCount());
    @SuppressWarnings("unchecked")
    List<Set<Arc>> exploredArcs = Lists.newArrayListWithCapacity(fst.getStateCount());
    for (int i = 0; i < fst.getStateCount(); i++) {
      exploredArcs.add(null);
    }
    ArrayList<IntArrayList> paths = new ArrayList<>();
    paths.add(new IntArrayList());

    depthFirstSearch(fst, accessible, paths, exploredArcs, coaccessible);
    if (accessible.size() == fst.getStateCount() || coaccessible.size() == fst.getStateCount()) {
      // all are accessible so nothing to delete
      return;
    }
    ArrayList<MutableState> toDelete = new ArrayList<>();

    int numStates = fst.getStateCount();
    for (int i = 0; i < numStates; i++) {
      MutableState s = fst.getState(i);
      if (!(accessible.contains(s.getId()) || coaccessible.contains(s.getId()))) {
        toDelete.add(s);
      }
    }

    fst.deleteStates(toDelete);
  }
}
