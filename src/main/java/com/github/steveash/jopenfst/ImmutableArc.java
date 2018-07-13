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

package com.github.steveash.jopenfst;

import com.github.steveash.jopenfst.utils.FstUtils;

/**
 * Immutable version of an arc
 * NOTE: All Immutable* classes are thread safe
 * @author Steve Ash
 */
public class ImmutableArc implements Arc {

  private final int ilabel;
  private final int olabel;
  private final double weight;
  private final int nextStateId;
  private volatile ImmutableState nextStateRef; // given cycles we have to set this after construction

  public ImmutableArc(int ilabel, int olabel, double weight, int nextStateId) {
    this.ilabel = ilabel;
    this.olabel = olabel;
    this.weight = weight;
    this.nextStateId = nextStateId;
  }

  @Override
  public double getWeight() {
    return weight;
  }

  @Override
  public int getIlabel() {
    return ilabel;
  }

  @Override
  public int getOlabel() {
    return olabel;
  }

  @Override
  public ImmutableState getNextState() {
    return nextStateRef;
  }

  public int getNextStateId() {
    return nextStateId;
  }

  // given this is a graph we have to initialize this after the fact
  void init(ImmutableFst initFrom) {
    this.nextStateRef = initFrom.getState(nextStateId);
  }

  @Override
  public boolean equals(Object o) {
    return FstUtils.arcEquals(this, o);
  }

  @Override
    public int hashCode() {
      int result;
      long temp;
      temp = Double.doubleToLongBits(weight);
      result = (int) (temp ^ (temp >>> 32));
      result = 31 * result + ilabel;
      result = 31 * result + olabel;
      result = 31 * result + nextStateId;
      return result;
    }

  @Override
  public String toString() {
    return "ImmutableArc{" +
           "ilabel=" + ilabel +
           ", olabel=" + olabel +
           ", weight=" + weight +
           ", nextStateId=" + nextStateId +
           '}';
  }
}
