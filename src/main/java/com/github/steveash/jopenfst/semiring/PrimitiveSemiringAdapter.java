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

import com.google.common.math.DoubleMath;

/**
 * Adapter between a primitive simiring and a generic semiring; useful for testing and non-performance sensitive
 * areas
 */
public class PrimitiveSemiringAdapter extends GenericSemiring<Double> {

  private final Semiring semiring;

  public PrimitiveSemiringAdapter(Semiring semiring) {
    this.semiring = semiring;
  }

  @Override
  public Double plus(Double a, Double b) {
    return semiring.plus(a, b);
  }

  @Override
  public Double reverse(Double a) {
    return semiring.reverse(a);
  }

  @Override
  public Double times(Double a, Double b) {
    return semiring.times(a, b);
  }

  @Override
  public Double divide(Double a, Double b) {
    return semiring.divide(a, b);
  }

  @Override
  public Double zero() {
    return semiring.zero();
  }

  @Override
  public Double one() {
    return semiring.one();
  }

  @Override
  public boolean isZero(Double a) {
    return semiring.isZero(a);
  }

  @Override
  public boolean isMember(Double a) {
    return semiring.isMember(a);
  }

  @Override
  public boolean isApproxEqual(Double a, Double b) {
    return DoubleMath.fuzzyEquals(a, b, Semiring.DEFAULT_APPROX_EQUAL);
  }

  @Override
  public boolean naturalLess(Double a, Double b) {
    return semiring.naturalLess(a, b);
  }
}
