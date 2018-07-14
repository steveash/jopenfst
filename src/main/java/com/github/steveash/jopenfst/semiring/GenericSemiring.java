/*
 * Copyright 2018 Steve Ash
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.github.steveash.jopenfst.semiring;

/**
 * A GenericSemiring is the contract for a semiring on a generic weight type `W`
 * The Semiring class can be viewed as a specialization of GenericSemiring where the weight type is primitive double
 * <p>
 * Given the performance sensitive nature of most of the Semiring operations you wouldn't want to use
 * GenericSemiring<Double> (and get lots of boxing) where you could use Semiring instead
 */
public interface GenericSemiring<W> {

  /**
   * returns a plus b for this semiring; plus is the generalization of combining sequences
   *
   * @param a weight W
   * @param b weight W
   * @return resulting weight W
   */
  W plus(W a, W b);

  /**
   * returns the reverse of the weight for this semiring
   *
   * @param a
   * @return
   */
  W reverse(W a);

  /**
   * returns a times b for this semiring; times is the generalization of extending a sequence (like a path)
   *
   * @param a
   * @param b
   * @return
   */
  W times(W a, W b);

  /**
   * returns a dividedBy b; divide is only defined on some semirings and means: c = a times b then c dividedB b = a
   *
   * @param a
   * @param b
   * @return
   */
  W divide(W a, W b);

  /**
   * The ZERO element for this semiring; requirement that x = plus(x, zero)
   *
   * @return
   */
  W zero();

  /**
   * The ONE element for this semiring; requirement that x = times(x, one)
   *
   * @return
   */
  W one();

  /**
   * Returns true if a is a member of this semiring
   *
   * @param a
   * @return true if member, false if not
   */
  boolean isMember(W a);

  /**
   * Returns true if these two weights, members of this semiring, are approx equal to each other
   * @param a
   * @param b
   * @return
   */
  boolean isApproxEqual(W a, W b);

  /**
   * Returns true is the given weight is the zero element in this semiring
   * @param a
   * @return
   */
  boolean isZero(W a);

  /**
   * Returns true if the given weight is NOT the zero element in this semiring
   * @param a
   * @return
   */
  default boolean isNotZero(W a) {
    return !isZero(a);
  }

  /**
   * Common divisor over weights in a semiring that supports division (one use for this is determinization)
   * NOTE that this is not defined on all semirings
   *
   * @param a
   * @param b
   * @return
   */
  default W commonDivisor(W a, W b) {
    throw new UnsupportedOperationException("semiring " + this + " doesnt support common divisor");
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
  default boolean naturalLess(W a, W b) {
    return (!isApproxEqual(a, b) && isApproxEqual(plus(a, b), a));
  }
}
