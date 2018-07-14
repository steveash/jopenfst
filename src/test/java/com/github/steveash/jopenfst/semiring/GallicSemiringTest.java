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

import com.github.steveash.jopenfst.WeightGenerator;
import com.github.steveash.jopenfst.semiring.GallicSemiring.GallicWeight;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.github.steveash.jopenfst.semiring.GallicSemiring.GallicMode.MIN_GALLIC;
import static com.github.steveash.jopenfst.semiring.GallicSemiring.GallicMode.RESTRICT_GALLIC;
import static com.github.steveash.jopenfst.semiring.GallicSemiring.GallicWeight.createFromGiven;
import static com.github.steveash.jopenfst.semiring.GallicSemiring.SHORTLEX_ORDERING;
import static com.github.steveash.jopenfst.semiring.SemiringTester.assertFuzzy;
import static org.junit.Assert.assertEquals;

public class GallicSemiringTest {

  private WeightGenerator<GallicWeight> weightGen;
  private SemiringTester<GallicWeight> semiTester;
  private GallicSemiring restrict;
  private GallicSemiring min;
  private double tropicalOne;

  @Before
  public void setUp() throws Exception {
    weightGen = WeightGenerator.makeGallic(0xEEF123);
    semiTester = new SemiringTester<>(weightGen);
    semiTester.setRandValuesToTest(200);
    restrict = new GallicSemiring(TropicalSemiring.INSTANCE, RESTRICT_GALLIC);
    min = new GallicSemiring(TropicalSemiring.INSTANCE, MIN_GALLIC);
    tropicalOne = TropicalSemiring.INSTANCE.one();
  }

  @Test
  public void testRestrictedSemiring() {
    semiTester.assertSemiringAndDivide(restrict);
  }

  @Test
  public void testMinSemiring() {
    semiTester.assertSemiringAndDivide(min);
  }

  @Test
  public void testNaturalOrdering() {
    GallicWeight a = createFromGiven(1.0, 3);
    GallicWeight b = createFromGiven(1.0, 1, 2);
    GallicWeight c = createFromGiven(1.0, 1, 3);
    GallicWeight d = createFromGiven(1.0, 3, 2);
    GallicWeight e = createFromGiven(1.0, 1, 2, 3);
    assertEquals(0, SHORTLEX_ORDERING.compare(a, a));
    assertEquals(0, SHORTLEX_ORDERING.compare(e, e));
    assertEquals(-1, SHORTLEX_ORDERING.compare(a, e));
    assertEquals(1, SHORTLEX_ORDERING.compare(e, a));
    assertEquals(ImmutableList.of(a, b, c, d, e), SHORTLEX_ORDERING.sortedCopy(Arrays.asList(e, d, a, b, c)));
  }

  @Test
  public void testRestrictPlusWithLabels() {
    // the zero paths are handled by the general semiring tester
    GallicWeight result = restrict.plus(createFromGiven(42.0, 10), createFromGiven(20.0, 10));
    assertFuzzy(restrict, result, createFromGiven(20.0, 10));
  }

  @Test
  public void testRestrictPlusWithEmpty() {
    // the zero paths are handled by the general semiring tester
    GallicWeight result = restrict.plus(createFromGiven(42.0), createFromGiven(20.0));
    assertFuzzy(restrict, result, createFromGiven(20.0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRestrictPlusFails() {
    // the zero paths are handled by the general semiring tester
    GallicWeight result = restrict.plus(createFromGiven(42.0, 10), createFromGiven(20.0));
  }

  @Test
  public void testMinPlus1() {
    // the zero paths are handled by the general semiring tester
    GallicWeight result = min.plus(createFromGiven(42.0), createFromGiven(20.0, 10));
    assertFuzzy(min, result, createFromGiven(20.0, 10));
  }

  @Test
  public void testMinPlus2() {
    // the zero paths are handled by the general semiring tester
    GallicWeight result = min.plus(createFromGiven(42.0), createFromGiven(84.0, 10));
    assertFuzzy(min, result, createFromGiven(42.0));
  }

  @Test
  public void testTimes1() {
    GallicWeight result = min.times(createFromGiven(42.0), createFromGiven(12.0, 10));
    assertFuzzy(min, result, createFromGiven(54.0, 10));

    GallicWeight result2 = min.times(createFromGiven(12.0, 10), createFromGiven(42.0));
    assertFuzzy(min, result2, createFromGiven(54.0, 10));
  }

  @Test
  public void testTimes2() {
    GallicWeight result = min.times(createFromGiven(42.0, 11), createFromGiven(12.0, 10));
    assertFuzzy(min, result, createFromGiven(54.0, 11, 10));

    GallicWeight result2 = min.times(createFromGiven(12.0, 10), createFromGiven(42.0, 11));
    assertFuzzy(min, result2, createFromGiven(54.0, 10, 11));
  }

  @Test
  public void testTimes3() {
    GallicWeight result = min.times(createFromGiven(22.0, 10, 11), createFromGiven(12.0, 12));
    assertFuzzy(min, result, createFromGiven(34.0, 10, 11, 12));

    GallicWeight result2 = min.times(createFromGiven(12.0, 10), createFromGiven(22.0, 11, 12));
    assertFuzzy(min, result2, createFromGiven(34.0, 10, 11, 12));
  }

  @Test
  public void testDivide1() {
    GallicWeight result = min.divide(createFromGiven(22.0, 10, 11), createFromGiven(10.0, 10));
    assertFuzzy(min, result, createFromGiven(12.0, 11));

    GallicWeight result2 = min.divide(createFromGiven(10.0, 10), createFromGiven(22.0, 10, 11));
    assertFuzzy(min, result2, GallicWeight.createFromGiven(-12.0));
  }

  @Test
  public void testDivide2() {
    GallicWeight result = min.divide(GallicWeight.createFromGiven(22.0), createFromGiven(10.0, 10, 11));
    assertFuzzy(min, result, GallicWeight.createFromGiven(12.0));

    GallicWeight result2 = min.divide(createFromGiven(10.0, 10, 11), createFromGiven(22.0));
    assertFuzzy(min, result2, createFromGiven(-12.0, 10, 11));
  }

  @Test
  public void testCommonDivisor1() {
    GallicWeight result = min.commonDivisor(createFromGiven(22.0), createFromGiven(10.0, 10, 11));
    assertFuzzy(min, result, createFromGiven(10.0));

    GallicWeight result2 = min.commonDivisor(createFromGiven(10.0, 10, 11), createFromGiven(22.0));
    assertFuzzy(min, result2, createFromGiven(10.0));
  }

  @Test
  public void testCommonDivisor2() {
    GallicWeight result = min.commonDivisor(createFromGiven(22.0, 10), createFromGiven(10.0, 10, 11));
    assertFuzzy(min, result, createFromGiven(10.0, 10));

    GallicWeight result2 = min.commonDivisor(createFromGiven(10.0, 10, 11), createFromGiven(22.0, 10));
    assertFuzzy(min, result2, createFromGiven(10.0, 10));

    GallicWeight result3 = min.commonDivisor(createFromGiven(10.0, 10, 11), createFromGiven(22.0, 11));
    assertFuzzy(min, result3, createFromGiven(10.0));
  }

  @Test
  public void testCommonDivisor3() {
    GallicWeight result = min.commonDivisor(createFromGiven(22.0, 10, 11, 12), createFromGiven(10.0, 10, 11));
    assertFuzzy(min, result, createFromGiven(10.0, 10));

    GallicWeight result2 = min.commonDivisor(createFromGiven(10.0, 10, 11), createFromGiven(22.0, 10, 11, 12));
    assertFuzzy(min, result2, createFromGiven(10.0, 10));
  }

  @Test
  public void testCommonDivisorZero() {
    GallicWeight result = min.commonDivisor(min.zero(), createFromGiven(10.0, 10, 11));
    assertFuzzy(min, result, createFromGiven(10.0, 10));

    GallicWeight result2 = min.commonDivisor(createFromGiven(10.0, 10, 11), min.zero());
    assertFuzzy(min, result2, createFromGiven(10.0, 10));
  }

  @Test
  public void testFactorizeEmpty() {
    Pair<GallicWeight, GallicWeight> pair = min.factorize(GallicWeight.createEmptyLabels(10.0));
    assertEquals(GallicWeight.createEmptyLabels(10.0), pair.getLeft());
    assertEquals(GallicWeight.createEmptyLabels(TropicalSemiring.INSTANCE.one()), pair.getRight());
  }

  @Test
  public void testFactorizeOneLabel() {
    Pair<GallicWeight, GallicWeight> pair = min.factorize(GallicWeight.createFromGiven(10.0, 11));
    assertEquals(GallicWeight.createFromGiven(10.0, 11), pair.getLeft());
    assertEquals(GallicWeight.createEmptyLabels(tropicalOne), pair.getRight());
  }

  @Test
  public void testFactorizeTwoLabels() {
    Pair<GallicWeight, GallicWeight> pair = min.factorize(GallicWeight.createFromGiven(10.0, 11, 12));
    assertEquals(GallicWeight.createFromGiven(10.0, 11), pair.getLeft());
    assertEquals(GallicWeight.createFromGiven(tropicalOne, 12), pair.getRight());
  }

  @Test
  public void testFactorizeThreeLabels() {
    Pair<GallicWeight, GallicWeight> pair = min.factorize(GallicWeight.createFromGiven(10.0, 11, 12, 13));
    assertEquals(GallicWeight.createFromGiven(10.0, 11), pair.getLeft());
    assertEquals(GallicWeight.createFromGiven(tropicalOne, 12, 13), pair.getRight());
  }
}
