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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * The fst's mutable state implementation.
 *
 * Holds its outgoing {@link Arc} objects in an ArrayList allowing additions/deletions
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class State {

  // State's Id
  protected int id = -1;

  // Final weight
  private float fnlWeight;

  // Outgoing arcs
  private ArrayList<Arc> arcs = null;

  // initial number of arcs
  protected int initialNumArcs = -1;

  /**
   * Default Constructor
   */
  protected State() {
    arcs = new ArrayList<>();
  }

  /**
   * Constructor specifying the state's final weight
   */
  public State(float fnlWeight) {
    this();
    this.fnlWeight = fnlWeight;
  }

  /**
   * Constructor specifying the initial capacity of the arc's ArrayList (this is an optimization used in various
   * operations)
   */
  public State(int initialNumArcs) {
    this.initialNumArcs = initialNumArcs;
    if (initialNumArcs > 0) {
      arcs = new ArrayList<>(initialNumArcs);
    }
  }

  /**
   * Shorts the arc's ArrayList based on the provided Comparator
   */
  public void arcSort(Comparator<Arc> cmp) {
    Collections.sort(arcs, cmp);
  }

  /**
   * Get the state's final Weight
   */
  public float getFinalWeight() {
    return fnlWeight;
  }

  /**
   * Set the state's arcs ArrayList
   *
   * @param arcs the arcs ArrayList to set
   */
  public void setArcs(ArrayList<Arc> arcs) {
    this.arcs = arcs;
  }

  /**
   * Set the state's final weight
   *
   * @param fnlfloat the final weight to set
   */
  public void setFinalWeight(float fnlfloat) {
    this.fnlWeight = fnlfloat;
  }

  /**
   * Get the state's id
   */
  public int getId() {
    return id;
  }

  /**
   * Get the number of outgoing arcs
   */
  public int getNumArcs() {
    return this.arcs.size();
  }

  /**
   * Add an outgoing arc to the state
   *
   * @param arc the arc to add
   */
  public void addArc(Arc arc) {
    this.arcs.add(arc);
  }

  /**
   * Get an arc based on it's index the arcs ArrayList
   *
   * @param index the arc's index
   * @return the arc
   */
  public Arc getArc(int index) {
    return this.arcs.get(index);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
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
    State other = (State) obj;
    if (id != other.id) {
      return false;
    }
    if (!(fnlWeight == other.fnlWeight)) {
      if (Float.floatToIntBits(fnlWeight) != Float
          .floatToIntBits(other.fnlWeight)) {
        return false;
      }
    }
    if (arcs == null) {
      if (other.arcs != null) {
        return false;
      }
    } else if (!arcs.equals(other.arcs)) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "(" + id + ", " + fnlWeight + ")";
  }

  /**
   * Delete an arc based on its index
   *
   * @param index the arc's index
   * @return the deleted arc
   */
  public Arc deleteArc(int index) {
    return this.arcs.remove(index);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id;
    // result = prime * result + Float.floatToIntBits(fnlWeight);
    // result = prime * result + ((arcs == null) ? 0 : arcs.hashCode());
    return result;
  }

  /**
   * Set an arc at the specified position in the arcs' ArrayList.
   *
   * @param index the position to the arcs' array
   * @param arc   the arc value to set
   */
  public void setArc(int index, Arc arc) {
    arcs.set(index, arc);
  }

}
