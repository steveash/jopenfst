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

import com.google.common.collect.ImmutableList;

import com.github.steveash.jopenfst.utils.FstUtils;

import java.util.List;

/**
 * Immutable version of the state
 */
public class ImmutableState implements State {

  private final int id;
  private final double finalWeight;
  private final ImmutableList<ImmutableArc> arcs;

  public ImmutableState(State copyFrom) {
    this(copyFrom.getId(), copyFrom.getFinalWeight(), copyFrom.getArcs());
  }

  public ImmutableState(int id, double finalWeight, List<? extends Arc> copyFrom) {
    this.id = id;
    this.finalWeight = finalWeight;
    ImmutableList.Builder<ImmutableArc> builder = ImmutableList.builder();
    for (Arc arc : copyFrom) {
      builder.add(new ImmutableArc(arc.getIlabel(), arc.getOlabel(), arc.getWeight(), arc.getNextState().getId()));
    }
    this.arcs = builder.build();
  }


  @Override
  public double getFinalWeight() {
    return finalWeight;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public int getArcCount() {
    return arcs.size();
  }

  @Override
  public ImmutableArc getArc(int index) {
    return arcs.get(index);
  }

  @Override
  public List<? extends Arc> getArcs() {
    return arcs;
  }

  void init(ImmutableFst from) {
    for (ImmutableArc arc : arcs) {
      arc.init(from);
    }
  }

  @Override
  public boolean equals(Object o) {
    return FstUtils.stateEquals(this, o);
  }

  @Override
  public int hashCode() {
    int result = id;
    long temp = finalWeight != +0.0 ? Double.doubleToLongBits(finalWeight) : 0;
    result = 31 * result * ((int) (temp ^ (temp >>> 32)));
    result = 31 * result + (arcs != null ? arcs.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ImmutableState{" +
           "id=" + id +
           ", finalWeight=" + finalWeight +
           ", arcs=" + arcs +
           '}';
  }
}
