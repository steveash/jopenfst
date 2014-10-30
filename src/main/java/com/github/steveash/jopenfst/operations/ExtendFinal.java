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
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;

import java.util.ArrayList;

/**
 * Extend an Fst to a single final state and undo operations.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ExtendFinal {

  /**
   * Default Contructor
   */
  private ExtendFinal() {
  }

  /**
   * Extends an Fst to a single final state.
   *
   * It adds a new final state with a 0.0 (Semiring's 1) final wight and connects the current final states to it using
   * epsilon transitions with weight equal to the original final state's weight.
   *
   * @param fst the Fst to extend
   */
  public static void apply(Fst fst) {
    Semiring semiring = fst.getSemiring();
    ArrayList<State> fStates = new ArrayList<>();

    int numStates = fst.getNumStates();
    for (int i = 0; i < numStates; i++) {
      State s = fst.getState(i);
      if (s.getFinalWeight() != semiring.zero()) {
        fStates.add(s);
      }
    }

    // Add a new single final
    State newFinal = new State(semiring.one());
    fst.addState(newFinal);
    for (State s : fStates) {
      // add epsilon transition from the old final to the new one
      s.addArc(new Arc(0, 0, s.getFinalWeight(), newFinal));
      // set old state's weight to zero
      s.setFinalWeight(semiring.zero());
    }
  }

  /**
   * Undo of the extend operation
   */
  public static void undo(Fst fst) {
    State f = null;
    int numStates = fst.getNumStates();
    for (int i = 0; i < numStates; i++) {
      State s = fst.getState(i);
      if (s.getFinalWeight() != fst.getSemiring().zero()) {
        f = s;
        break;
      }
    }

    if (f == null) {
      throw new IllegalStateException("Final state not found in " + fst);
    }
    for (int i = 0; i < numStates; i++) {
      State s = fst.getState(i);
      for (int j = 0; j < s.getNumArcs(); j++) {
        Arc a = s.getArc(j);
        if (a.getIlabel() == 0 && a.getOlabel() == 0
            && a.getNextState().getId() == f.getId()) {
          s.setFinalWeight(a.getWeight());
        }
      }
    }
    fst.deleteState(f);
  }

}
