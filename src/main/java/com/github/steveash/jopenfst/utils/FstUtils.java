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

import com.google.common.math.DoubleMath;

import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.ImmutableSymbolTable;
import com.github.steveash.jopenfst.MutableSymbolTable;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.SymbolTable;
import com.github.steveash.jopenfst.UnionSymbolTable;
import com.github.steveash.jopenfst.WriteableSymbolTable;

/**
 * @author Steve Ash
 */
public class FstUtils {

  public static boolean fstEquals(Object thisFstObj, Object thatFstObj) {
    return fstEquals(thisFstObj, thatFstObj, Double.MIN_VALUE);
  }

  public static boolean fstEquals(Object thisFstObj, Object thatFstObj,
                                  double epsilon) {
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
      if (!FstUtils.stateEquals(thisFst.getState(i), thatFst.getState(i), epsilon)) {
        return false;
      }
    }
    if (thisFst.getStartState() != null ? (thisFst.getStartState().getId() != thatFst.getStartState().getId()) : thatFst.getStartState() != null) {
      return false;
    }
    if (thisFst.getInputSymbols() != null ? !FstUtils.symbolTableEquals(thisFst.getInputSymbols(), thatFst.getInputSymbols()) : thatFst.getInputSymbols() != null) {
      return false;
    }
    if (thisFst.getStateSymbols() != null ? !FstUtils.symbolTableEquals(thisFst.getStateSymbols(), thatFst.getStateSymbols()) : thatFst.getStateSymbols() != null) {
      return false;
    }
    return thisFst.getOutputSymbols() != null ? FstUtils.symbolTableEquals(thisFst.getOutputSymbols(), thatFst.getOutputSymbols()) :thatFst.getOutputSymbols() == null;
  }

  public static boolean arcEquals(Object thisArcObj, Object thatArcObj) {
    return arcEquals(thisArcObj, thatArcObj, Double.MIN_VALUE);
  }

  public static boolean arcEquals(Object thisArcObj, Object thatArcObj,
                                  double epsilon) {
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
      if (!DoubleMath.fuzzyEquals(thisArc.getWeight(), thatArc.getWeight(), epsilon)) {
        return false;
      }
    }
    return true;
  }

  public static boolean stateEquals(Object thisStateObj, Object thatStateObj) {
    return stateEquals(thisStateObj, thatStateObj, Double.MIN_VALUE);
  }

  public static boolean stateEquals(Object thisStateObj, Object thatStateObj, double epsilon) {
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
    if (!DoubleMath.fuzzyEquals(thatState.getFinalWeight(), thisState.getFinalWeight(), epsilon)) {
      return false;
    }
    if (thisState.getArcs().size() != thatState.getArcs().size()) {
      return false;
    }
    for (int i = 0; i < thisState.getArcs().size(); i++) {
      if (!arcEquals(thisState.getArc(i), thatState.getArc(i), epsilon)) {
        return false;
      }
    }
    return true;
  }

  public static boolean symbolTableEquals(Object thisSyms, Object thatSyms) {
    if (thisSyms == thatSyms) {
      return true;
    }
    if (thisSyms == null || thatSyms == null) {
      return false;
    }
    if (!SymbolTable.class.isAssignableFrom(thisSyms.getClass()) || !SymbolTable.class.isAssignableFrom(thatSyms.getClass())) {
      return false;
    }

    SymbolTable thisS = (SymbolTable) thisSyms;
    SymbolTable thatS = (SymbolTable) thatSyms;

    if (thisS.size() != thatS.size()) {
      return false;
    }
    for (ObjectIntCursor<String> cursor : thisS) {
      if (thatS.contains(cursor.key)) {
        if (thatS.get(cursor.key) == cursor.value) {
          continue;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * Returns an "effective" copy of the given symbol table which might be a unioned symbol
   * table that is just a mutable filter on top of a backing table which is treated as
   * immutable
   * @param syms
   * @return
   */
  public static WriteableSymbolTable symbolTableEffectiveCopy(SymbolTable syms) {
    if (syms instanceof ImmutableSymbolTable) {
      return new UnionSymbolTable(syms);
    }
    if (syms instanceof UnionSymbolTable) {
      return UnionSymbolTable.copyFrom((UnionSymbolTable) syms);
    }
    // maybe consider the size and if its "big" return a union of the mutable version?
    return new MutableSymbolTable(syms);
  }
}
