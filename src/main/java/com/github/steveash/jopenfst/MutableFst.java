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

package com.github.steveash.jopenfst;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.github.steveash.jopenfst.semiring.LogSemiring;
import com.github.steveash.jopenfst.semiring.Semiring;
import com.github.steveash.jopenfst.utils.FstUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A mutable finite state transducer implementation.
 */
public class MutableFst implements Fst {

  public static MutableFst copyFrom(Fst fst) {
    MutableFst copy = new MutableFst(fst.getSemiring(), new MutableSymbolTable(fst.getInputSymbols()),
                       new MutableSymbolTable(fst.getOutputSymbols()));
    // build up states
    for (int i = 0; i < fst.getStateCount(); i++) {
      State source = fst.getState(i);
      MutableState target = new MutableState(source.getNumArcs());
      target.setFinalWeight(source.getFinalWeight());
      copy.setState(i, target);
    }
    // build arcs now that we have target state refs
    for (int i = 0; i < fst.getStateCount(); i++) {
      State source = fst.getState(i);
      MutableState target = copy.getState(i);
      for (int j = 0; j < source.getNumArcs(); j++) {
        Arc sarc = source.getArc(j);
        MutableState nextTargetState = copy.getState(sarc.getNextState().getId());
        MutableArc tarc = new MutableArc(sarc.getIlabel(), sarc.getOlabel(), sarc.getWeight(), nextTargetState);
        target.addArc(tarc);
      }
    }
    MutableState newStart = copy.getState(fst.getStartState().getId());
    copy.setStart(newStart);
    return copy;
  }

  private final Semiring semiring;
  private final ArrayList<MutableState> states;
  private MutableState start;
  private MutableSymbolTable inputSymbols;
  private MutableSymbolTable outputSymbols;

  public MutableFst() {
    this(makeDefaultRing(), new MutableSymbolTable(), new MutableSymbolTable());
  }

  private static Semiring makeDefaultRing() {
    return new LogSemiring();
  }

  /**
   * Constructor specifying the initial capacity of the states ArrayList (this is an optimization used in various
   * operations)
   *
   * @param numStates the initial capacity
   */
  public MutableFst(int numStates) {
    this(new ArrayList<MutableState>(numStates), makeDefaultRing(), new MutableSymbolTable(), new MutableSymbolTable());
  }

  public MutableFst(int numStates, Semiring semiring) {
    this(new ArrayList<MutableState>(numStates), semiring, new MutableSymbolTable(), new MutableSymbolTable());
  }

  /**
   * Constructor specifying the fst's semiring
   *
   * @param s the fst's semiring
   */
  public MutableFst(Semiring s) {
    this(s, new MutableSymbolTable(), new MutableSymbolTable());
  }

  public MutableFst(Semiring semiring, MutableSymbolTable inputSymbols, MutableSymbolTable outputSymbols) {
    this(Lists.<MutableState>newArrayList(), semiring, inputSymbols, outputSymbols);
  }

  protected MutableFst(ArrayList<MutableState> states, Semiring semiring, MutableSymbolTable inputSymbols,
                       MutableSymbolTable outputSymbols) {
    this.states = states;
    this.semiring = semiring;
    this.inputSymbols = inputSymbols;
    this.outputSymbols = outputSymbols;
  }

  /**
   * Get the initial states
   */
  @Override
  public MutableState getStartState() {
    return start;
  }

  /**
   * Get the semiring
   */
  @Override
  public Semiring getSemiring() {
    return semiring;
  }

  /**
   * Set the initial state
   *
   * @param start the initial state
   */
  public void setStart(MutableState start) {
    Preconditions.checkArgument(start.getId() >= 0, "must set id before setting start");
    this.start = start;
  }

  public MutableState newStartState() {
    Preconditions.checkArgument(start == null, "cant add more than one start state");
    MutableState newStart = newState();
    setStart(newStart);
    return newStart;
  }

  /**
   * Get the number of states in the fst
   */
  @Override
  public int getStateCount() {
    return this.states.size();
  }

  @Override
  public MutableState getState(int index) {
    return states.get(index);
  }

  /**
   * Adds a state to the fst
   *
   * @param state the state to be added
   */
  public MutableState addState(MutableState state) {
    Preconditions.checkArgument(state.getId() == - 1, "trying to add a state that already has id");
    this.states.add(state);
    state.id = states.size() - 1;
    return state;
  }

  public void setState(int id, MutableState state) {
    Preconditions.checkArgument(state.getId() == - 1, "trying to add a state that already has id");
    state.setId(id);
    // they provided the id so index properly
    if (id >= this.states.size()) {
      this.states.ensureCapacity(id + 1);
      for (int i = states.size(); i <= id; i++) {
        this.states.add(null);
      }
    }
    Preconditions.checkState(this.states.get(id) == null, "cant write two states with ", id);
    this.states.set(id, state);
  }

  public MutableState newState() {
    MutableState s = new MutableState();
    return addState(s);
  }

  @Override
  public MutableSymbolTable getInputSymbols() {
    return inputSymbols;
  }

  @Override
  public MutableSymbolTable getOutputSymbols() {
    return outputSymbols;
  }

  @Override
  public int getInputSymbolCount() {
    return inputSymbols.size();
  }

  @Override
  public int getOutputSymbolCount() {
    return outputSymbols.size();
  }

  public void setInputSymbolsFrom(Fst sourceInputSymbols) {
    this.inputSymbols = new MutableSymbolTable(sourceInputSymbols.getInputSymbols());
  }

  public void setInputSymbolsFromThatOutput(Fst that) {
    this.inputSymbols = new MutableSymbolTable(that.getOutputSymbols());
  }

  public void setOutputSymbolsFrom(Fst sourceOutputSymbols) {
    this.outputSymbols = new MutableSymbolTable(sourceOutputSymbols.getOutputSymbols());
  }

  public void setOutputSymbolsFromThatInput(Fst that) {
    this.outputSymbols = new MutableSymbolTable(that.getInputSymbols());
  }

  @Override
  public int lookupInputSymbol(String symbol) {
    return inputSymbols.get(symbol);
  }

  @Override
  public int lookupOutputSymbol(String symbol) {
    return outputSymbols.get(symbol);
  }

  @Override
  public void throwIfThisOutputIsNotThatInput(Fst that) {
    if (!this.outputSymbols.equals(that.getInputSymbols())) {
      throw new IllegalArgumentException("Symbol tables don't match, cant compose " + this + " to " + that);
    }
  }

  @Override
  public void throwIfInvalid() {
    Preconditions.checkNotNull(semiring, "must have a semiring");
    Preconditions.checkNotNull(start, "must have a start state");
  }

  @Deprecated // just use the text version; it will be more forward compatible
  public void saveModel(File file) throws IOException {
    FstInputOutput.saveModel(this, file);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Fst(start=").append(start).append(", isyms=").append(inputSymbols).append(", osyms=").append(
        outputSymbols).append(", semiring=").append(semiring).append(")\n");
    for (State s : states) {
      sb.append("  ").append(s).append("\n");
      int numArcs = s.getNumArcs();
      for (int j = 0; j < numArcs; j++) {
        Arc a = s.getArc(j);
        sb.append("    ").append(a).append("\n");
      }
    }
    return sb.toString();
  }

  /**
   * Deletes the given states and remaps the existing state ids
   * @param statesToDelete
   */
  public void deleteStates(Collection<? extends State> statesToDelete) {
    for (State state : statesToDelete) {
      deleteState(state);
    }
    remapStateIds();
  }

  /**
   * Deletes a state;
   * @param state the state to delete
   */
  private void deleteState(State state) {
    if (state.getId() == this.start.getId()) {
      throw new IllegalArgumentException("Cannot delete start state.");
    }

    this.states.remove(state);

    // delete arc's with nextstate equal to stateid
    ArrayList<Integer> toDelete;
    int numStates = states.size();
    for (int i = 0; i < numStates; i++) {
      MutableState s1 = states.get(i);

      toDelete = new ArrayList<>();
      int numArcs = s1.getNumArcs();
      for (int j = 0; j < numArcs; j++) {
        Arc a = s1.getArc(j);
        if (a.getNextState().equals(state)) {
          toDelete.add(j);
        }
      }
      // indices not change when deleting in reverse ordering
      Object[] toDeleteArray = toDelete.toArray();
      Arrays.sort(toDeleteArray);
      for (int j = toDelete.size() - 1; j >= 0; j--) {
        Integer index = (Integer) toDeleteArray[j];
        s1.deleteArc(index);
      }
    }
  }

  private void remapStateIds() {
    int numStates = states.size();
    for (int i = 0; i < numStates; i++) {
      states.get(i).id = i;
    }
  }

  public void throwIfAnyNullStates() {
    for (int i = 0; i < states.size(); i++) {
      if (states.get(i) == null) {
        throw new IllegalStateException("Cannot have a null state in an FST. State " + i);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    return FstUtils.fstEquals(this, o);
  }

  @Override
  public int hashCode() {
    int result = semiring != null ? semiring.hashCode() : 0;
    result = 31 * result + (states != null ? states.hashCode() : 0);
    result = 31 * result + (start != null ? start.hashCode() : 0);
    result = 31 * result + (inputSymbols != null ? inputSymbols.hashCode() : 0);
    result = 31 * result + (outputSymbols != null ? outputSymbols.hashCode() : 0);
    return result;
  }
}
