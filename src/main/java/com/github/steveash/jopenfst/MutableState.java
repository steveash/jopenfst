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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.github.steveash.jopenfst.utils.FstUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The fst's mutable state implementation.
 *
 * Holds its outgoing {@link MutableArc} objects in an ArrayList allowing additions/deletions
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class MutableState implements State {

  // State's Id
  protected int id = -1;

  // Final weight
  private double fnlWeight = Double.NaN;

  // Outgoing arcs
  private final ArrayList<MutableArc> arcs;

  // Incoming arcs (at least states with arcs that are incoming to us)
  private final Set<MutableState> incomingStates = Sets.newIdentityHashSet();

  // initial number of arcs; this is only used during deserialization and should be ignored otherwise
  protected int initialNumArcs = -1;

  /**
   * Default Constructor
   */
  public MutableState() {
    arcs = Lists.newArrayList();
  }

  /**
   * Constructor specifying the state's final weight
   */
  public MutableState(double fnlWeight) {
    this.fnlWeight = fnlWeight;
    this.arcs = Lists.newArrayList();
  }

  /**
   * Constructor specifying the initial capacity of the arc's ArrayList (this is an optimization used in various
   * operations)
   */
  public MutableState(int initialNumArcs) {
    this.initialNumArcs = initialNumArcs;
    arcs = Lists.newArrayListWithCapacity(initialNumArcs);
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
  @Override
  public double getFinalWeight() {
    return fnlWeight;
  }

  /**
   * Set the state's final weight
   *
   * @param fnlfloat the final weight to set
   */
  public void setFinalWeight(double fnlfloat) {
    this.fnlWeight = fnlfloat;
  }

  /**
   * Get the state's id
   */
  @Override
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  /**
   * Get the number of outgoing arcs
   */
  @Override
  public int getArcCount() {
    return this.arcs.size();
  }

  /**
   * Get an arc based on it's index the arcs ArrayList
   *
   * @param index the arc's index
   * @return the arc
   */
  @Override
  public MutableArc getArc(int index) {
    return this.arcs.get(index);
  }


  @Override
  public List<MutableArc> getArcs() {
    return this.arcs;
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

  /* friend methods to let the fst maintain state's state */

  // deletes an arc, should only be called from FST itself so that the invariants can be
  // maintained
  MutableArc deleteArc(int index) {
    return this.arcs.remove(index);
  }

  // adds an arc but should only be used by MutableFst
  void addArc(MutableArc arc) {
      this.arcs.add(arc);
    }

  void addIncomingState(MutableState inState) {
    if (inState == this) return;
    this.incomingStates.add(inState);
  }

  void removeIncomingState(MutableState inState) {
    this.incomingStates.remove(inState);
  }

  public Iterable<MutableState> getIncomingStates() {
    return this.incomingStates;
  }

  @Override
  public boolean equals(Object o) {
    return FstUtils.stateEquals(this, o);

  }

  @Override
  public int hashCode() {
    int result = id;
    long temp = fnlWeight != +0.0 ? Double.doubleToLongBits(fnlWeight) : 0;
    result = 31 * result * ((int) (temp ^ (temp >>> 32)));
    result = 31 * result + (arcs != null ? arcs.hashCode() : 0);
    return result;
  }
}
