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

import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;

/**
 * Project operation.
 *
 * @author John Salatas jsalatas@users.sourceforge.net
 */
public class Project {

  /**
   * Projects an fst onto its domain or range by either copying each arc's input label to its output label or vice
   * versa.
   */
  public static void apply(MutableFst fst, ProjectType pType) {
    if (pType == ProjectType.INPUT) {
      fst.setOutputSymbolsAsCopyFromThatInput(fst);
    } else if (pType == ProjectType.OUTPUT) {
      fst.setInputSymbolsAsCopyFromThatOutput(fst);
    }

    for (int i = 0; i < fst.getStateCount(); i++) {
      MutableState state = fst.getState(i);
      // Immutable fsts hold an additional (null) arc
      for (int j = 0; j < state.getArcCount(); j++) {
        MutableArc a = state.getArc(j);
        if (pType == ProjectType.INPUT) {
          a.setOlabel(a.getIlabel());
        } else if (pType == ProjectType.OUTPUT) {
          a.setIlabel(a.getOlabel());
        }
      }
    }
  }
}
