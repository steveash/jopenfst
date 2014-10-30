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
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class LogSemiring extends Semiring {

  private static final long serialVersionUID = 5212106775584311083L;

  // zero value
  private static float zero = Float.POSITIVE_INFINITY;

  // one value
  private static float one = 0.f;

  /*
   * (non-Javadoc)
   *
   * @see
   * edu.cmu.sphinx.fst.weight.Semiring#plus(edu.cmu.sphinx.fst.weight.float,
   * edu.cmu.sphinx.fst.weight.float)
   */
  @Override
  public float plus(float w1, float w2) {
    if (!isMember(w1) || !isMember(w2)) {
      return Float.NEGATIVE_INFINITY;
    }
    if (w1 == Float.POSITIVE_INFINITY) {
      return w2;
    } else if (w2 == Float.POSITIVE_INFINITY) {
      return w1;
    }
    return (float) -Math.log(Math.exp(-w1) + Math.exp(-w2));
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * edu.cmu.sphinx.fst.weight.Semiring#times(edu.cmu.sphinx.fst.weight.float,
   * edu.cmu.sphinx.fst.weight.float)
   */
  @Override
  public float times(float w1, float w2) {
    if (!isMember(w1) || !isMember(w2)) {
      return Float.NEGATIVE_INFINITY;
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
  public float divide(float w1, float w2) {
    if (!isMember(w1) || !isMember(w2)) {
      return Float.NEGATIVE_INFINITY;
    }

    if (w2 == zero) {
      return Float.NEGATIVE_INFINITY;
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
  public float zero() {
    return zero;
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.weight.Semiring#one()
   */
  @Override
  public float one() {
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
  public boolean isMember(float w) {
    return (!Float.isNaN(w)) // not a NaN
           && (w != Float.NEGATIVE_INFINITY); // and different from -inf
  }

  /*
   * (non-Javadoc)
   *
   * @see edu.cmu.sphinx.fst.semiring.Semiring#reverse(float)
   */
  @Override
  public float reverse(float w1) {
    throw new UnsupportedOperationException("not implemented");
//    return Float.NEGATIVE_INFINITY;
  }

}
