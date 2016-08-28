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

import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
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
   * Creates a new FST that is a copy of the existing with a new signle final state
   *
   * It adds a new final state with a 0.0 (Semiring's 1) final wight and connects the current final states to it using
   * epsilon transitions with weight equal to the original final state's weight.
   *
   * @param fst the input fst -- it will not be modified
   * @return a mutable fst that is a copy of the input + extended with a single final
   */
  public static MutableFst apply(Fst fst) {
    fst.throwIfInvalid();
    MutableFst copy = MutableFst.copyFrom(fst);
    Semiring semiring = copy.getSemiring();
    ArrayList<MutableState> fStates = new ArrayList<>();

    int numStates = copy.getStateCount();
    for (int i = 0; i < numStates; i++) {
      MutableState s = copy.getState(i);
      if (semiring.isNotZero(s.getFinalWeight())) {
        fStates.add(s);
      }
    }

    // Add a new single final
    MutableState newFinal = new MutableState(semiring.one());
    copy.addState(newFinal);
    int epsILabel = copy.getInputSymbols().get(Fst.EPS);
    int epsOLabel = copy.getOutputSymbols().get(Fst.EPS);
    for (MutableState s : fStates) {
      // add epsilon transition from the old final to the new one
      copy.addArc(s, epsILabel, epsOLabel, newFinal, s.getFinalWeight());
      // set old state's weight to zero
      s.setFinalWeight(semiring.zero());
    }
    return copy;
  }

}
