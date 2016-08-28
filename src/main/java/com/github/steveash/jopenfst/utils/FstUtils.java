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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steve Ash
 */
public class FstUtils {

  private static final Logger log = LoggerFactory.getLogger(FstUtils.class);

  public interface EqualsReporter {
    void report(String msg, Object a, Object b);
  }

  public static final EqualsReporter NULL_REPORTER = new EqualsReporter() {
    @Override
    public void report(String msg, Object a, Object b) {
      // nothing
    }
  };

  public static final EqualsReporter LOG_REPORTER = new EqualsReporter() {
    @Override
    public void report(String msg, Object a, Object b) {
      String aa = objToString(a);
      String bb = objToString(b);
      log.info("Equals difference: " + msg + " " + aa + " " + bb);
    }

    private String objToString(Object a) {
      String aa = "<null>";
      if (a != null) {
        aa = a.toString();
        if (aa.length() > 80) {
          aa = "\n" + aa;
        }
      }
      return aa;
    }
  };

  public static boolean fstEquals(Object thisFstObj, Object thatFstObj) {
    return fstEquals(thisFstObj, thatFstObj, Double.MIN_VALUE);
  }

  public static boolean fstEquals(Object thisFstObj, Object thatFstObj, EqualsReporter reporter) {
    return fstEquals(thisFstObj, thatFstObj, Double.MIN_VALUE, reporter);
  }

  public static boolean fstEquals(Object thisFstObj, Object thatFstObj,
                                  double epsilon) {
    return fstEquals(thisFstObj, thatFstObj, epsilon, NULL_REPORTER);
  }

  public static boolean fstEquals(Object thisFstObj,
                                  Object thatFstObj,
                                  double epsilon,
                                  EqualsReporter reporter) {
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
      reporter.report("fst.semiring", thisFst.getSemiring(), thatFst.getSemiring());
      return false;
    }

    if (thisFst.getStateCount() != thatFst.getStateCount()) {
      reporter.report("fst.statecount", thisFst.getStateCount(), thatFst.getStateCount());
      return false;
    }
    for (int i = 0; i < thisFst.getStateCount(); i++) {
      State thisState = thisFst.getState(i);
      State thatState = thatFst.getState(i);
      if (!FstUtils.stateEquals(thisState, thatState, epsilon, reporter)) {
        reporter.report("fst.state", thisState, thatState);
        return false;
      }
    }
    if (thisFst.getStartState() != null ? (thisFst.getStartState().getId() != thatFst.getStartState().getId()) : thatFst.getStartState() != null) {
      reporter.report("fst.startstate", thisFst.getStartState(), thatFst.getStartState());
      return false;
    }
    if (thisFst.getInputSymbols() != null ? !FstUtils.symbolTableEquals(thisFst.getInputSymbols(), thatFst.getInputSymbols(), reporter) : thatFst.getInputSymbols() != null) {
      reporter.report("fst.inputSymbols", thisFst.getInputSymbols(), thatFst.getInputSymbols());
      return false;
    }
    if (thisFst.getStateSymbols() != null ? !FstUtils.symbolTableEquals(thisFst.getStateSymbols(), thatFst.getStateSymbols(), reporter) : thatFst.getStateSymbols() != null) {
      reporter.report("fst.stateSymbols", thisFst.getStateSymbols(), thatFst.getStateSymbols());
      return false;
    }
    if (!(thisFst.getOutputSymbols() != null ? FstUtils.symbolTableEquals(thisFst.getOutputSymbols(), thatFst.getOutputSymbols(), reporter) :thatFst.getOutputSymbols() == null)) {
      reporter.report("fst.outSymbols", thisFst.getOutputSymbols(), thatFst.getOutputSymbols());
      return false;
    }
    return true;
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
    return stateEquals(thisStateObj, thatStateObj, epsilon, NULL_REPORTER);
  }

  public static boolean stateEquals(
      Object thisStateObj, Object thatStateObj, double epsilon, EqualsReporter reporter) {
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
      reporter.report("state.id", thisState.getId(), thatState.getId());
      return false;
    }
    if (!DoubleMath.fuzzyEquals(thatState.getFinalWeight(), thisState.getFinalWeight(), epsilon)) {
      reporter.report("state.finalWeight", thisState.getFinalWeight(), thatState.getFinalWeight());
      return false;
    }
    if (thisState.getArcs().size() != thatState.getArcs().size()) {
      reporter.report("state.arcCount", thisState.getArcCount(), thatState.getArcCount());
      return false;
    }
    for (int i = 0; i < thisState.getArcs().size(); i++) {
      Arc thisArc = thisState.getArc(i);
      Arc thatArc = thatState.getArc(i);
      if (!arcEquals(thisArc, thatArc, epsilon)) {
        reporter.report("state.arc", thisArc, thatArc);
        return false;
      }
    }
    return true;
  }

  public static boolean symbolTableEquals(Object thisSyms, Object thatSyms) {
    return symbolTableEquals(thisSyms, thatSyms, NULL_REPORTER);
  }

  public static boolean symbolTableEquals(Object thisSyms,
                                          Object thatSyms,
                                          EqualsReporter reporter) {
    if (thisSyms == thatSyms) {
      return true;
    }
    if (thisSyms == null || thatSyms == null) {
      return false;
    }
    if (!SymbolTable.class.isAssignableFrom(thisSyms.getClass()) || !SymbolTable.class.isAssignableFrom(thatSyms.getClass())) {
      reporter.report("symbolTable.isAssignable", thisSyms, thatSyms);
      return false;
    }

    SymbolTable thisS = (SymbolTable) thisSyms;
    SymbolTable thatS = (SymbolTable) thatSyms;

    if (thisS.size() != thatS.size()) {
      reporter.report("symbolTable.size", thisS.size(), thatS.size());
      return false;
    }
    for (ObjectIntCursor<String> cursor : thisS) {
      if (thatS.contains(cursor.key)) {
        int thatV = thatS.get(cursor.key);
        if (thatV == cursor.value) {
          continue;
        }
        reporter.report("symbolTable.key", cursor.value, thatV);
      } else {
        reporter.report("symbolTable.missingKey", cursor.key, cursor.value);
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
