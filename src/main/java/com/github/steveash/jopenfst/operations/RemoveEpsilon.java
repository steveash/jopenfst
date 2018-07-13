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
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * Remove epsilon operation.
 *
 * @author John Salatas jsalatas@users.sourceforge.net
 */
public class RemoveEpsilon {

  /**
   * Removes epsilon transitions from an fst. Returns a new epsilon-free fst and does not modify the original fst
   *
   * @param fst the fst to remove epsilon transitions from
   * @return the epsilon-free fst
   */
  public static MutableFst remove(Fst fst) {
    Preconditions.checkNotNull(fst);
    Preconditions.checkNotNull(fst.getSemiring());

    Semiring semiring = fst.getSemiring();
    MutableFst result = MutableFst.emptyWithCopyOfSymbols(fst);
    int iEps = fst.getInputSymbols().get(Fst.EPS);
    int oEps = fst.getOutputSymbols().get(Fst.EPS);

    @SuppressWarnings("unchecked")
    HashMap<Integer,Double>[] closure = new HashMap[fst.getStateCount()];
    MutableState[] oldToNewStateMap = new MutableState[fst.getStateCount()];
    State[] newToOldStateMap = new State[fst.getStateCount()];
    initResultStates(fst, result, oldToNewStateMap, newToOldStateMap);
    addNonEpsilonArcs(fst, result, iEps, oEps, closure, oldToNewStateMap);

    // augment fst with arcs generated from epsilon moves.
    for (int i = 0; i < result.getStateCount(); i++) {
      MutableState state = result.getState(i);
      State oldState = newToOldStateMap[state.getId()];
      if (closure[oldState.getId()] == null) {
        continue;
      }
      for (Integer pathFinalStateIndex : closure[oldState.getId()].keySet()) {
        State closureState = fst.getState(pathFinalStateIndex);
        if (semiring.isNotZero(closureState.getFinalWeight())) {
          Double prevWeight = getPathWeight(oldState, closureState, closure);
          Preconditions.checkNotNull(prevWeight, "problem with prev weight on closure from %s", oldState);
          state.setFinalWeight(semiring.plus(state.getFinalWeight(), semiring.times(prevWeight, closureState.getFinalWeight())));
        }
        for (int j = 0; j < closureState.getArcCount(); j++) {
          Arc arc = closureState.getArc(j);
          if ((arc.getIlabel() != iEps) || (arc.getOlabel() != oEps)) {
            Double pathWeight = getPathWeight(oldState, closureState, closure);
            Preconditions.checkNotNull(pathWeight, "problem with prev weight on closure from %s", oldState);
            double newWeight = semiring.times(arc.getWeight(), pathWeight);
            MutableState nextState = oldToNewStateMap[arc.getNextState().getId()];
            result.addArc(state, arc.getIlabel(), arc.getOlabel(), nextState, newWeight);
          }
        }
      }
    }

    Connect.apply(result);
    ArcSort.sortByInput(result);

    return result;
  }

  private static void addNonEpsilonArcs(Fst fst, MutableFst result, int iEps, int oEps, HashMap<Integer,Double>[] closure, MutableState[] oldToNewStateMap) {
    for (int i = 0; i < fst.getStateCount(); i++) {
      State state = fst.getState(i);
      // Add non-epsilon arcs
      MutableState newState = oldToNewStateMap[state.getId()];
      for (int j = 0; j < state.getArcCount(); j++) {
        Arc arc = state.getArc(j);
        if ((arc.getIlabel() != iEps) || (arc.getOlabel() != oEps)) {
          MutableState resNextState = oldToNewStateMap[arc.getNextState().getId()];
          result.addArc(newState, arc.getIlabel(), arc.getOlabel(), resNextState, arc.getWeight());
        }
      }

      // Compute epsilon closure
      if (closure[state.getId()] == null) {
        calculateClosure(fst, state, closure, fst.getSemiring(), iEps, oEps);
      }
    }
  }

  private static void initResultStates(Fst fst, MutableFst res, MutableState[] oldToNewStateMap, State[] newToOldStateMap) {
    for (int i = 0; i < fst.getStateCount(); i++) {
      State state = fst.getState(i);
      // Add non-epsilon arcs
      MutableState newState = res.newState();
      newState.setFinalWeight(state.getFinalWeight());
      oldToNewStateMap[state.getId()] = newState;
      newToOldStateMap[newState.getId()] = state;
      if (newState.getId() == fst.getStartState().getId()) {
        res.setStart(newState);
      }
    }
  }

  /**
   * Put a new state in the epsilon closure
   */
  private static void put(State fromState, State toState, double weight, HashMap<Integer,Double>[] closure) {
    HashMap<Integer,Double> maybe = closure[fromState.getId()];
    if (maybe == null) {
      maybe = new HashMap<Integer,Double>();
      closure[fromState.getId()] = maybe;
    }
    maybe.put(toState.getId(), weight);
  }

  /**
   * Add a state in the epsilon closure
   */
  private static void add(State fromState, State toState, double weight, HashMap<Integer,Double>[] closure, Semiring semiring) {
    Double old = getPathWeight(fromState, toState, closure);
    if (old == null) {
      put(fromState, toState, weight, closure);
    } else {
      put(fromState, toState, semiring.plus(weight, old), closure);
    }

  }

  /**
   * Calculate the epsilon closure
   */
  private static void calculateClosure(Fst fst, State state, HashMap<Integer,Double>[] closure, Semiring semiring, int iEps, int oEps) {

    for (int j = 0; j < state.getArcCount(); j++) {
      Arc arc = state.getArc(j);
      if ((arc.getIlabel() != iEps) || (arc.getOlabel() != oEps)) {
        continue;
      }
      int nextStateId = arc.getNextState().getId();
      if (closure[nextStateId] == null) {
        calculateClosure(fst, arc.getNextState(), closure, semiring, iEps, oEps);
      }
      HashMap<Integer,Double> closureEntry = closure[nextStateId];
      if (closureEntry != null) {
        for (Integer pathFinalStateIndex : closureEntry.keySet()) {
          State pathFinalState = fst.getState(pathFinalStateIndex);
          Double prevPathWeight = getPathWeight(arc.getNextState(), pathFinalState, closure);
          Preconditions.checkNotNull(prevPathWeight, "prev arc %s never set in closure", arc);
          double newPathWeight = semiring.times(prevPathWeight, arc.getWeight());
          add(state, pathFinalState, newPathWeight, closure, semiring);
        }
      }
      add(state, arc.getNextState(), arc.getWeight(), closure, semiring);
    }
  }

  /**
   * Get an epsilon path's cost in epsilon closure
   */
  @Nullable
  private static Double getPathWeight(State inState, State outState, HashMap<Integer,Double>[] closure) {
    if (closure[inState.getId()] != null) {
      return closure[inState.getId()].get(outState.getId());
    }
    return null;
  }
}
