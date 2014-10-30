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

import java.util.Comparator;

/**
 * ArcSort operation.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ArcSort {

  /**
   * Default Constructor
   */
  private ArcSort() {
  }

  /**
   * Applies the ArcSort on the provided fst. Sorting can be applied either on input or output label based on the
   * provided comparator.
   *
   * ArcSort can be applied to both {@link com.github.steveash.jopenfst.Fst} and {@link com.github.steveash.jopenfst.ImmutableFst}
   *
   * @param fst the fst to sort it's arcs
   * @param cmp the provided Comparator
   */
  public static void apply(Fst fst, Comparator<Arc> cmp) {
    int numStates = fst.getNumStates();
    for (int i = 0; i < numStates; i++) {
      State s = fst.getState(i);
      s.arcSort(cmp);
    }
  }
}
