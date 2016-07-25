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

import java.util.List;

/**
 * Immutable version of the state
 */
public class ImmutableState implements State {

  private final int id;
  private final float finalWeight;
  private final ImmutableList<ImmutableArc> arcs;

  public ImmutableState(State copyFrom) {
    this(copyFrom.getId(), copyFrom.getFinalWeight(), copyFrom.getArcs());
  }

  public ImmutableState(int id, float finalWeight, List<? extends Arc> copyFrom) {
    this.id = id;
    this.finalWeight = finalWeight;
    ImmutableList.Builder<ImmutableArc> builder = ImmutableList.builder();
    for (Arc arc : copyFrom) {
      builder.add(new ImmutableArc(arc.getIlabel(), arc.getOlabel(), arc.getWeight(), arc.getNextState().getId()));
    }
    this.arcs = builder.build();
  }


  @Override
  public float getFinalWeight() {
    return finalWeight;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public int getNumArcs() {
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
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ImmutableState that = (ImmutableState) o;

    if (id != that.id) {
      return false;
    }
    if (Float.compare(that.finalWeight, finalWeight) != 0) {
      return false;
    }
    return arcs != null ? arcs.equals(that.arcs) : that.arcs == null;

  }

  @Override
  public int hashCode() {
    int result = id;
    result = 31 * result + (finalWeight != +0.0f ? Float.floatToIntBits(finalWeight) : 0);
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
