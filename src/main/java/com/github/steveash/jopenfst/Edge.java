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
 * A tuple representing an edge in a graph (note that an Arc is a conceptual specialization of this
 * But since this is immutable (really just a simple tuple type) Arc doesnt inherit from this
 * @author Steve Ash
 */
public class Edge {

  private final int from;
  private final int to;

  public Edge(int from, int to) {
    this.from = from;
    this.to = to;
  }

  public int getFrom() {
    return from;
  }

  public int getTo() {
    return to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Edge edge = (Edge) o;

    if (from != edge.from) {
      return false;
    }
    return to == edge.to;

  }

  @Override
  public int hashCode() {
    int result = from;
    result = 31 * result + to;
    return result;
  }

  @Override
  public String toString() {
    return "Edge{" +
           "from=" + from +
           ", to=" + to +
           '}';
  }
}
