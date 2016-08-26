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

package com.github.steveash.jopenfst.semiring;

import java.io.Serializable;

/**
 * Abstract semiring class.
 *
 * @author "John Salatas <jsalatas@users.sourceforge.net>"
 */
public abstract class Semiring implements Serializable {

  private static final long serialVersionUID = 1L;

  // significant decimal digits in floating point numbers
  protected static final int accuracy = 5;

  /**
   * Semiring's plus operation
   */
  public abstract double plus(double w1, double w2);

  public abstract double reverse(double w1);

  /**
   * Semiring's times operation
   */
  public abstract double times(double w1, double w2);

  /**
   * Semiring's divide operation
   */
  public abstract double divide(double w1, double w2);

  /**
   * Semiring's zero element
   */
  public abstract double zero();

  /**
   * Semiring's one element
   */
  public abstract double one();

  /**
   * Checks if a value is a valid one the semiring
   */
  public abstract boolean isMember(double w);

  public boolean isZero(double candidate) {
    return Double.isNaN(candidate) || candidate == zero();
  }

  public boolean isNotZero(double candidate) {
    return !isZero(candidate);
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
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.getClass().toString();
  }

  /**
   * NATURAL ORDER
   *
   * By definition: a <= b iff a + b = a
   *
   * The natural order is a negative partial order iff the semiring is idempotent. It is trivially monotonic for plus.
   * It is left (resp. right) monotonic for times iff the semiring is left (resp. right) distributive. It is a total
   * order iff the semiring has the path property.
   *
   * See Mohri, "Semiring Framework and Algorithms for Shortest-Distance Problems", Journal of Automata, Languages and
   * Combinatorics 7(3):321-350, 2002.
   *
   * We define the strict version of this order below.
   */
  public boolean naturalLess(double w1, double w2) {
    return (this.plus(w1, w2) == w1) && (w1 != w2);
  }

}