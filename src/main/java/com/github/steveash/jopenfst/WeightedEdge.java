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

/**
 * A simple immutable tuple representing a weighted edge in a graph
 * @author Steve Ash
 */
public class WeightedEdge {

  private final int from;
  private final int to;
  private final double weight;

  public WeightedEdge(int from, int to, double weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }

  public int getFrom() {
    return from;
  }

  public int getTo() {
    return to;
  }

  public double getWeight() {
    return weight;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WeightedEdge that = (WeightedEdge) o;

    if (from != that.from) {
      return false;
    }
    if (to != that.to) {
      return false;
    }
    return Double.compare(that.weight, weight) == 0;

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    result = from;
    result = 31 * result + to;
    temp = Double.doubleToLongBits(weight);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "WeightedEdge{" +
           "from=" + from +
           ", to=" + to +
           ", weight=" + weight +
           '}';
  }
}
