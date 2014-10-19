/**
 *
 */
package com.github.steveash.jopenfst;

import java.io.IOException;
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
  ImmutableFst(int numStates) {
    super(0);
    this.numStates = numStates;
    this.states = new ImmutableState[numStates];
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.Fst#getNumStates()
   */
  @Override
  public int getNumStates() {
    return this.numStates;
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.Fst#getState(int)
   */
  @Override
  public ImmutableState getState(int index) {
    return states[index];
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.Fst#addState(edu.cmu.sphinx.fst.State)
   */
  @Override
  public void addState(State state) {
    throw new IllegalArgumentException("You cannot modify an ImmutableFst.");
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.Fst#saveModel(java.lang.String)
   */
  @Override
  public void saveModel(String filename) throws IOException {
    throw new IllegalArgumentException("You cannot serialize an ImmutableFst.");
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

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.Fst#deleteState(edu.cmu.sphinx.fst.State)
   */
  @Override
  public void deleteState(State state) {
    throw new IllegalArgumentException("You cannot modify an ImmutableFst.");
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.Fst#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Fst(start=").append(start).append(", isyms=").append(Arrays.toString(isyms)).append(", osyms=").append(
        Arrays.toString(osyms)).append(", semiring=").append(semiring).append(")\n");
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

}
