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

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;

import java.util.ArrayList;
import java.util.List;

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
                                       List<? extends List<MutableState>> paths,
                                       List<MutableState> coaccessible) {
    // hold the coaccessible added in this loop
    ArrayList<MutableState> newCoAccessibles = new ArrayList<>();
    for (List<MutableState> path : paths) {
      int index = path.lastIndexOf(state);
      if (index != -1) {
        if (fst.getSemiring().isNotZero(state.getFinalWeight()) || coaccessible.contains(state)) {
          for (int j = index; j > -1; j--) {
            if (!coaccessible.contains(path.get(j))) {
              newCoAccessibles.add(path.get(j));
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
                                    MutableState toState, ArrayList<ArrayList<MutableState>> paths) {
    ArrayList<MutableState> lastPath = paths.get(lastPathIndex);
    // copy the last path to a new one, from start to current state
    int fromIndex = lastPath.indexOf(fromState);
    int toIndex = lastPath.indexOf(toState);
    if (toIndex == -1) {
      toIndex = lastPath.size() - 1;
    }
    ArrayList<MutableState> newPath = Lists.newArrayList(lastPath.subList(fromIndex, toIndex));
    paths.add(newPath);
  }

  /**
   * The depth first search recursion
   */
  private static MutableState dfs(MutableFst fst, MutableState start,
                           ArrayList<ArrayList<MutableState>> paths, ArrayList<Arc>[] exploredArcs,
                           ArrayList<MutableState> accessible) {
    int lastPathIndex = paths.size() - 1;

    ArrayList<Arc> currentExploredArcs = exploredArcs[start.getId()];
    paths.get(lastPathIndex).add(start);
    if (start.getArcCount() != 0) {
      int arcCount = 0;
      int numArcs = start.getArcCount();
      for (int j = 0; j < numArcs; j++) {
        MutableArc arc = start.getArc(j);
        if ((currentExploredArcs == null)
            || !currentExploredArcs.contains(arc)) {
          lastPathIndex = paths.size() - 1;
          if (arcCount++ > 0) {
            duplicatePath(lastPathIndex, fst.getStartState(), start,
                          paths);
            lastPathIndex = paths.size() - 1;
            paths.get(lastPathIndex).add(start);
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
    lastPathIndex = paths.size() - 1;
    accessible.add(start);

    return start;
  }

  /**
   * Adds an arc top the explored arcs list
   */
  private static void addExploredArc(int stateId, Arc arc,
                                     ArrayList<Arc>[] exploredArcs) {
    if (exploredArcs[stateId] == null) {
      exploredArcs[stateId] = Lists.newArrayList();
    }
    exploredArcs[stateId].add(arc);

  }

  /**
   * Initialization of a depth first search recursion
   */
  private static void depthFirstSearch(MutableFst fst, ArrayList<MutableState> accessible,
                                       ArrayList<ArrayList<MutableState>> paths,
                                       ArrayList<Arc>[] exploredArcs,
                                       ArrayList<MutableState> coaccessible) {
    MutableState currentState = fst.getStartState();
    MutableState nextState = currentState;
    do {
      if (!accessible.contains(currentState)) {
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
    Semiring semiring = fst.getSemiring();
    Preconditions.checkNotNull(semiring);

    ArrayList<MutableState> accessible = new ArrayList<>();
    ArrayList<MutableState> coaccessible = new ArrayList<>();
    @SuppressWarnings("unchecked")
    ArrayList<Arc>[] exploredArcs = new ArrayList[fst.getStateCount()];
    for (int i = 0; i < fst.getStateCount(); i++) {
      exploredArcs[i] = null;
    }
    ArrayList<ArrayList<MutableState>> paths = new ArrayList<>();
    paths.add(new ArrayList<MutableState>());

    depthFirstSearch(fst, accessible, paths, exploredArcs, coaccessible);

    ArrayList<State> toDelete = new ArrayList<>();

    int numStates = fst.getStateCount();
    for (int i = 0; i < numStates; i++) {
      State s = fst.getState(i);
      if (!(accessible.contains(s) || coaccessible.contains(s))) {
        toDelete.add(s);
      }
    }

    fst.deleteStates(toDelete);
  }
}
