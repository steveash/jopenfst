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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.github.steveash.jopenfst.semiring.LogSemiring;
import com.github.steveash.jopenfst.semiring.Semiring;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A mutable finite state transducer implementation.
 *
 * Holds an ArrayList of {@link State} objects allowing additions/deletions.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class Fst {

  public static final String EPS = "<eps>";

  protected final Semiring semiring;
  protected ArrayList<State> states;
  protected State start;
  protected SymbolTable inputSymbols;
  protected SymbolTable outputSymbols;

  public Fst() {
    this(makeDefaultRing(), new SymbolTable(), new SymbolTable());
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
  public Fst(int numStates) {
    this(new ArrayList<State>(numStates), makeDefaultRing(), new SymbolTable(), new SymbolTable());
  }

  public Fst(int numStates, Semiring semiring) {
    this(new ArrayList<State>(numStates), semiring, new SymbolTable(), new SymbolTable());
  }

  /**
   * Constructor specifying the fst's semiring
   *
   * @param s the fst's semiring
   */
  public Fst(Semiring s) {
    this(s, new SymbolTable(), new SymbolTable());
  }

  public Fst(Semiring semiring, SymbolTable inputSymbols, SymbolTable outputSymbols) {
    this(Lists.<State>newArrayList(), semiring, inputSymbols, outputSymbols);
  }

  protected Fst(ArrayList<State> states, Semiring semiring, SymbolTable inputSymbols,
             SymbolTable outputSymbols) {
    this.states = states;
    this.semiring = semiring;
    this.inputSymbols = inputSymbols;
    this.outputSymbols = outputSymbols;
  }

  /**
   * Get the initial states
   */
  public State getStart() {
    return start;
  }

  /**
   * Get the semiring
   */
  public Semiring getSemiring() {
    return semiring;
  }

  /**
   * Set the initial state
   *
   * @param start the initial state
   */
  public void setStart(State start) {
    this.start = start;
  }

  public State addStartState() {
    Preconditions.checkState(start == null, "cant add more than one start state");
    State newStart = addState();
    setStart(newStart);
    return newStart;
  }

  /**
   * Get the number of states in the fst
   */
  public int getNumStates() {
    return this.states.size();
  }

  public State getState(int index) {
    return states.get(index);
  }

  /**
   * Adds a state to the fst
   *
   * @param state the state to be added
   */
  public State addState(State state) {

    this.states.add(state);
    state.id = states.size() - 1;
    return state;
  }

  public void setState(int id, State state) {
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

  public State addState() {
    State s = new State();
    this.states.add(s);
    s.id = states.size() - 1; // not thread safe
    return s;
  }

  public SymbolTable getInputSymbols() {
    return inputSymbols;
  }

  public SymbolTable getOutputSymbols() {
    return outputSymbols;
  }

  public int getInputSymbolCount() {
    return inputSymbols.size();
  }

  public int getOutputSymbolCount() {
    return outputSymbols.size();
  }

  public void setInputSymbolsFrom(Fst sourceInputSymbols) {
    this.inputSymbols = new SymbolTable(sourceInputSymbols.inputSymbols);
  }

  public void setInputSymbolsFromThatOutput(Fst that) {
    this.inputSymbols = new SymbolTable(that.outputSymbols);
  }

  public void setOutputSymbolsFrom(Fst sourceOutputSymbols) {
    this.outputSymbols = new SymbolTable(sourceOutputSymbols.outputSymbols);
  }

  public void setOutputSymbolsFromThatInput(Fst that) {
    this.outputSymbols = new SymbolTable(that.inputSymbols);
  }

  public int lookupInputSymbol(String symbol) {
    return inputSymbols.get(symbol);
  }

  public int lookupOutputSymbol(String symbol) {
    return outputSymbols.get(symbol);
  }

  public void throwIfThisOutputIsNotThatInput(Fst that) {
    if (!this.outputSymbols.equals(that.inputSymbols)) {
      throw new IllegalArgumentException("Symbol tables don't match, cant compose " + this + " to " + that);
    }
  }

  /**
   * Saves binary model to disk
   *
   * @param filename the binary model filename
   */
  public void saveModel(String filename) throws IOException {
    saveModel(new File(filename));
  }

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
   * Deletes a state
   *
   * @param state the state to delete
   */
  public void deleteState(State state) {
    if (state.getId() == this.start.getId()) {
      throw new IllegalArgumentException("Cannot delete start state.");
    }

    this.states.remove(state);

    // delete arc's with nextstate equal to stateid
    ArrayList<Integer> toDelete;
    // for (State s1 : states) {
    int numStates = states.size();
    for (int i = 0; i < numStates; i++) {
      State s1 = states.get(i);

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

  /**
   * Remaps the states' ids.
   *
   * States' ids are renumbered starting from 0 up to @see {@link Fst#getNumStates()}
   */
  public void remapStateIds() {
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
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Fst fst = (Fst) o;

    if (semiring != null ? !semiring.equals(fst.semiring) : fst.semiring != null) {
      return false;
    }
    if (states != null ? !states.equals(fst.states) : fst.states != null) {
      return false;
    }
    if (start != null ? !start.equals(fst.start) : fst.start != null) {
      return false;
    }
    if (inputSymbols != null ? !inputSymbols.equals(fst.inputSymbols) : fst.inputSymbols != null) {
      return false;
    }
    return outputSymbols != null ? outputSymbols.equals(fst.outputSymbols) : fst.outputSymbols == null;

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
