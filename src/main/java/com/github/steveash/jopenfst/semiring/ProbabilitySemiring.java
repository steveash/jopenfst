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

/**
 * Probability semiring implementation.
 *
 * @author "John Salatas jsalatas@users.sourceforge.net"
 */
public class ProbabilitySemiring extends Semiring {

  public static final ProbabilitySemiring INSTANCE = new ProbabilitySemiring();

  private static final long serialVersionUID = 5592668313009971909L;
  // zero value
  private static double zero = 0.f;

  // one value
  private static double one = 1.f;

  /*
   * (non-Javadoc)
   *
   * @see
   * edu.cmu.sphinx.fst.weight.AbstractSemiring#Plus(edu.cmu.sphinx.fst.weight
   * .float, edu.cmu.sphinx.fst.weight.float)
   */
  @Override
  public double plus(double w1, double w2) {
    if (!isMember(w1) || !isMember(w2)) {
      return Double.NEGATIVE_INFINITY;
    }

    return w1 + w2;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * edu.cmu.sphinx.fst.weight.AbstractSemiring#Times(edu.cmu.sphinx.fst.weight
   * .float, edu.cmu.sphinx.fst.weight.float)
   */
  @Override
  public double times(double w1, double w2) {
    if (!isMember(w1) || !isMember(w2)) {
      return Double.NEGATIVE_INFINITY;
    }

    return w1 * w2;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * edu.cmu.sphinx.fst.weight.AbstractSemiring#Divide(edu.cmu.sphinx.fst.
   * weight.float, edu.cmu.sphinx.fst.weight.float)
   */
  @Override
  public double divide(double w1, double w2) {
    return Double.NEGATIVE_INFINITY;
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.weight.AbstractSemiring#zero()
   */
  @Override
  public double zero() {
    return zero;
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.weight.AbstractSemiring#one()
   */
  @Override
  public double one() {
    return one;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * edu.cmu.sphinx.fst.weight.Semiring#isMember(edu.cmu.sphinx.fst.weight
   * .float)
   */
  @Override
  public boolean isMember(double w) {
    return !Double.isNaN(w) // not a NaN,
           && (w >= 0); // and positive
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.semiring.Semiring#reverse(float)
   */
  @Override
  public double reverse(double w1) {
    throw new UnsupportedOperationException("not implemented");
//    return Float.NEGATIVE_INFINITY;
  }

}
