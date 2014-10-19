/**
 *
 * Copyright 1999-2012 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package com.github.steveash.jopenfst;

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

  protected ArrayList<State> states = null;
  protected State start;
  protected String[] isyms;
  protected String[] osyms;
  protected Semiring semiring;

  /**
   * Default Constructor
   */
  public Fst() {
    states = new ArrayList<>();
  }

  /**
   * Constructor specifying the initial capacity of the states ArrayList (this is an optimization used in various
   * operations)
   *
   * @param numStates the initial capacity
   */
  public Fst(int numStates) {
    if (numStates > 0) {
      states = new ArrayList<>(numStates);
    }
  }

  /**
   * Constructor specifying the fst's semiring
   *
   * @param s the fst's semiring
   */
  public Fst(Semiring s) {
    this();
    this.semiring = s;
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
   * Set the Semiring
   *
   * @param semiring the semiring to set
   */
  public void setSemiring(Semiring semiring) {
    this.semiring = semiring;
  }

  /**
   * Set the initial state
   *
   * @param start the initial state
   */
  public void setStart(State start) {
    this.start = start;
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
  public void addState(State state) {
    this.states.add(state);
    state.id = states.size() - 1;
  }

  /**
   * Get the input symbols' array
   */
  public String[] getIsyms() {
    return isyms;
  }

  /**
   * Set the input symbols
   *
   * @param isyms the isyms to set
   */
  public void setIsyms(String[] isyms) {
    this.isyms = isyms;
  }

  /**
   * Get the output symbols' array
   */
  public String[] getOsyms() {
    return osyms;
  }

  /**
   * Set the output symbols
   *
   * @param osyms the osyms to set
   */
  public void setOsyms(String[] osyms) {
    this.osyms = osyms;
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
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Fst other = (Fst) obj;
    if (!Arrays.equals(isyms, other.isyms)) {
      return false;
    }
    if (!Arrays.equals(osyms, other.osyms)) {
      return false;
    }
    if (start == null) {
      if (other.start != null) {
        return false;
      }
    } else if (!start.equals(other.start)) {
      return false;
    }
    if (states == null) {
      if (other.states != null) {
        return false;
      }
    } else if (!states.equals(other.states)) {
      return false;
    }
    if (semiring == null) {
      if (other.semiring != null) {
        return false;
      }
    } else if (!semiring.equals(other.semiring)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Fst(start=").append(start).append(", isyms=").append(Arrays.toString(isyms)).append(", osyms=").append(
        Arrays.toString(osyms)).append(", semiring=").append(semiring).append(")\n");
    int numStates = states.size();
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
}
