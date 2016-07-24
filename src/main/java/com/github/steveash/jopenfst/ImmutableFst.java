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

/**
 *
 */
package com.github.steveash.jopenfst;

import com.github.steveash.jopenfst.semiring.Semiring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An immutable finite state transducer implementation.
 *
 * Holds a fixed size array of {@link ImmutableState} objects not allowing additions/deletions
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ImmutableFst extends Fst {

  protected ImmutableState[] states = null;
  protected int numStates;

  /**
   * Default private constructor.
   *
   * An ImmutableFst cannot be created directly. It needs to be deserialized.
   *
   * @see FstInputOutput#loadImmutableModel(String)
   */
  ImmutableFst() {

  }

  /**
   * Private Constructor specifying the capacity of the states array
   *
   * An ImmutableFst cannot be created directly. It needs to be deserialized.
   *
   * @param numStates the number of fst's states
   * @see FstInputOutput#loadImmutableModel(String)
   */
  ImmutableFst(int numStates, Semiring semiring, SymbolTable isym, SymbolTable osym) {
    super(new ArrayList<State>(numStates), semiring, isym, osym);
    this.numStates = numStates;
    this.states = new ImmutableState[numStates];
  }

  @Override
  public int getNumStates() {
    return this.numStates;
  }

  @Override
  public ImmutableState getState(int index) {
    return states[index];
  }

  @Override
  public State addState(State state) {
    throw throwMutate();
  }

  @Override
  public State addState() {
    throw throwMutate();
  }

  @Override
  public State addStartState() {
    throw throwMutate();
  }

  @Override
  public void saveModel(String filename) throws IOException {
    throwMutate();
  }

  //
//  /**
//   * Deserializes an ImmutableFst from an InputStream
//   *
//   * @param inputStream the InputStream. It should be already be initialized by the caller.
//   */
//  public static ImmutableFst loadImmutableModel(InputStream inputStream) {
//    try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(inputStream))) {
//      return readImmutableFst(ois);
//    } catch (Exception e) {
//      throw Throwables.propagate(e);
//    }
//  }

  @Override
  public void deleteState(State state) {
    throwMutate();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Fst(start=").append(start).append(", isyms=").append(inputSymbols).append(", osyms=").append(
        outputSymbols).append(", semiring=").append(semiring).append(")\n");
    int numStates = states.length;
    for (ImmutableState s : states) {
      sb.append("  ").append(s).append("\n");
      int numArcs = s.getNumArcs();
      for (int j = 0; j < numArcs; j++) {
        Arc a = s.getArc(j);
        sb.append("    ").append(a).append("\n");
      }
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ImmutableFst other = (ImmutableFst) obj;
    if (!Arrays.equals(states, other.states)) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    return true;
  }

  private static IllegalArgumentException throwMutate() {
    throw new IllegalArgumentException("You cannot modify an ImmutableFst.");
  }
}
