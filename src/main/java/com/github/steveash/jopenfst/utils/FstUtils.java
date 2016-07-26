/*
 * Copyright 2016 Steve Ash
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

package com.github.steveash.jopenfst.utils;

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.State;

/**
 * @author Steve Ash
 */
public class FstUtils {

  public static boolean fstEquals(Object thisFstObj, Object thatFstObj) {
    if (thisFstObj == thatFstObj) {
      return true;
    }
    if (thisFstObj == null || thatFstObj == null) {
      return false;
    }
    if (!Fst.class.isAssignableFrom(thisFstObj.getClass()) || !Fst.class.isAssignableFrom(thatFstObj.getClass())) {
      return false;
    }

    Fst thisFst = (Fst) thisFstObj;
    Fst thatFst = (Fst) thatFstObj;

    if (thisFst.getSemiring() != null ? !thisFst.getSemiring().equals(thatFst.getSemiring()) : thatFst.getSemiring() != null) {
      return false;
    }

    if (thisFst.getStateCount() != thatFst.getStateCount()) {
      return false;
    }
    for (int i = 0; i < thisFst.getStateCount(); i++) {
      if (!thisFst.getState(i).equals(thatFst.getState(i))) {
        return false;
      }
    }
    if (thisFst.getStartState() != null ? !thisFst.getStartState().equals(thatFst.getStartState()) : thatFst.getStartState() != null) {
      return false;
    }
    if (thisFst.getInputSymbols() != null ? !thisFst.getInputSymbols().equals(thatFst.getInputSymbols()) : thatFst.getInputSymbols() != null) {
      return false;
    }
    return thisFst.getOutputSymbols() != null ? thisFst.getOutputSymbols().equals(thatFst.getOutputSymbols()) : thatFst.getOutputSymbols() == null;
  }

  public static boolean arcEquals(Object thisArcObj, Object thatArcObj) {
    if (thisArcObj == thatArcObj) {
      return true;
    }
    if (thisArcObj == null || thatArcObj == null) {
      return false;
    }
    if (!Arc.class.isAssignableFrom(thisArcObj.getClass()) || !Arc.class.isAssignableFrom(thatArcObj.getClass())) {
      return false;
    }
    Arc thisArc = (Arc) thisArcObj;
    Arc thatArc = (Arc) thatArcObj;
    if (thisArc.getIlabel() != thatArc.getIlabel()) {
      return false;
    }
    if (thisArc.getNextState().getId() != thatArc.getNextState().getId()) {
        return false;
    }
    if (thisArc.getOlabel() != thatArc.getOlabel()) {
      return false;
    }
    if (!(thisArc.getWeight() == thatArc.getWeight())) {
      if (Float.floatToIntBits(thisArc.getWeight()) != Float
          .floatToIntBits(thatArc.getWeight())) {
        return false;
      }
    }
    return true;
  }

  public static boolean stateEquals(Object thisStateObj, Object thatStateObj) {
    if (thisStateObj == thatStateObj) {
      return true;
    }
    if (thisStateObj == null || thatStateObj == null) {
      return false;
    }
    if (!State.class.isAssignableFrom(thisStateObj.getClass()) || !State.class.isAssignableFrom(thatStateObj.getClass())) {
      return false;
    }

    State thisState = (State) thisStateObj;
    State thatState = (State) thatStateObj;

    if (thisState.getId() != thatState.getId()) {
      return false;
    }
    if (Float.compare(thatState.getFinalWeight(), thisState.getFinalWeight()) != 0) {
      return false;
    }
    return thisState.getArcs().equals(thatState.getArcs());
  }
}
