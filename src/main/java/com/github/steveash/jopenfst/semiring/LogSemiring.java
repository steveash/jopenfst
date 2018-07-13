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
 * Log semiring implementation.
 *
 * @author John Salatas jsalatas@users.sourceforge.net
 */
public class LogSemiring extends Semiring {

  public static final LogSemiring INSTANCE = new LogSemiring();

  private static final long serialVersionUID = 5212106775584311083L;

  // zero value
  private static double zero = Double.POSITIVE_INFINITY;

  // one value
  private static double one = 0.0;

  /*
   * (non-Javadoc)
   *
   * @see
   * edu.cmu.sphinx.fst.weight.Semiring#plus(edu.cmu.sphinx.fst.weight.float,
   * edu.cmu.sphinx.fst.weight.float)
   */
  @Override
  public double plus(double w1, double w2) {
    if (!isMember(w1) || !isMember(w2)) {
      return Double.NEGATIVE_INFINITY;
    }
    if (w1 == Double.POSITIVE_INFINITY) {
      return w2;
    } else if (w2 == Double.POSITIVE_INFINITY) {
      return w1;
    }
    return (double) -Math.log(Math.exp(-w1) + Math.exp(-w2));
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * edu.cmu.sphinx.fst.weight.Semiring#times(edu.cmu.sphinx.fst.weight.float,
   * edu.cmu.sphinx.fst.weight.float)
   */
  @Override
  public double times(double w1, double w2) {
    if (!isMember(w1) || !isMember(w2)) {
      return Double.NEGATIVE_INFINITY;
    }

    return w1 + w2;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * edu.cmu.sphinx.fst.weight.Semiring#divide(edu.cmu.sphinx.fst.weight.float
   * , edu.cmu.sphinx.fst.weight.float)
   */
  @Override
  public double divide(double w1, double w2) {
    if (!isMember(w1) || !isMember(w2)) {
      return Double.NEGATIVE_INFINITY;
    }

    if (w2 == zero) {
      return Double.NEGATIVE_INFINITY;
    } else if (w1 == zero) {
      return zero;
    }

    return w1 - w2;
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.weight.Semiring#zero()
   */
  @Override
  public double zero() {
    return zero;
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.weight.Semiring#one()
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
    return (!Double.isNaN(w)) // not a NaN
           && (w != Double.NEGATIVE_INFINITY); // and different from -inf
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
