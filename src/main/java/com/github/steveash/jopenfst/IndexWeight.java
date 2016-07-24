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
 * A tuple of an index (a state id, an arc id, whatever) + a weight (as a float)
 * This is immutable and is useful when you are storing these in collections
 * @author Steve Ash
 */
public class IndexWeight {

  private final int index;
  private final float weight;

  public IndexWeight(int index, float weight) {
    this.index = index;
    this.weight = weight;
  }

  public int getIndex() {
    return index;
  }

  public float getWeight() {
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

    IndexWeight that = (IndexWeight) o;

    if (index != that.index) {
      return false;
    }
    return Float.compare(that.weight, weight) == 0;

  }

  @Override
  public int hashCode() {
    int result = index;
    result = 31 * result + (weight != +0.0f ? Float.floatToIntBits(weight) : 0);
    return result;
  }

  @Override
  public String toString() {
    return "IndexWeight{" +
           "index=" + index +
           ", weight=" + weight +
           '}';
  }
}
