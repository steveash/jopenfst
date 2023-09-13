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
import com.github.steveash.jopenfst.semiring.UnionSemiring.UnionWeight;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import static com.github.steveash.jopenfst.semiring.GallicSemiring.GallicWeight.createFromGiven;
import static org.junit.Assert.assertEquals;

public class UnionSemiringTest {
  private WeightGenerator<UnionWeight<Double>> weightGen;
  private UnionSemiring<Double, PrimitiveSemiringAdapter> ring;
  private GallicSemiring gallicRing;
  private TropicalSemiring weightRing;
  private UnionSemiring<GallicWeight, GallicSemiring> gallicUnionRing;
  private UnionWeight<GallicWeight> zero;

  @Before
  public void setUp() throws Exception {
    weightGen = WeightGenerator.makeUnion(0xEEF123, false);
    weightRing = TropicalSemiring.INSTANCE;
    gallicRing = new GallicSemiring(weightRing, GallicSemiring.GallicMode.RESTRICT_GALLIC);
    ring = UnionSemiring.makeForNaturalOrdering(new PrimitiveSemiringAdapter(TropicalSemiring.INSTANCE));
    
    UnionSemiring.MergeStrategy<GallicWeight> merge = new UnionSemiring.MergeStrategy<GallicWeight>() {
		@Override
		public GallicWeight merge(GallicWeight a, GallicWeight b) {
		      Preconditions.checkArgument(a.getLabels().equals(b.getLabels()), "cant merge different labels");
		      return GallicWeight.create(a.getLabels(), weightRing.plus(a.getWeight(), b.getWeight()));
		}
	};
    
    gallicUnionRing = UnionSemiring.makeForOrdering(gallicRing, GallicSemiring.SHORTLEX_ORDERING, merge);
    zero = gallicUnionRing.zero();
  }

  @Test
  public void testUnionSemiring() {
    SemiringTester<UnionWeight<Double>> tester = new SemiringTester<>(weightGen);
    tester.setRandValuesToTest(200);
    tester.assertSemiring(ring);
  }

  @Test
  public void testUnionDivide() {
    // union divide is restricted to only single element unionweights
    SemiringTester<UnionWeight<Double>> tester = new SemiringTester<>(WeightGenerator.makeUnion(0x123BDD, true));
    tester.setRandValuesToTest(200);
    tester.assertDivide(ring);
  }

  @Test
  public void testWithAppend() {
    GallicWeight gw1 = GallicWeight.createFromGiven(42.0, 10, 11);
    GallicWeight gw2 = GallicWeight.createFromGiven(43.0, 10, 12);
    GallicWeight gw3 = GallicWeight.createFromGiven(20.0, 10, 12);
    // append to empty
    UnionWeight<GallicWeight> r1 = gallicUnionRing.withAppended(zero, gw1);
    assertEquals(ImmutableList.of(gw1), r1.getWeights());
    // append to one, no merge
    UnionWeight<GallicWeight> r2 = gallicUnionRing.withAppended(r1, gw2);
    assertEquals(ImmutableList.of(gw1, gw2), r2.getWeights());
    // append to two, with merge
    UnionWeight<GallicWeight> r3 = gallicUnionRing.withAppended(r1, gw3);
    assertEquals(ImmutableList.of(gw1, GallicWeight.createFromGiven(20.0, 10, 12)), r3.getWeights());
  }

  @Test(expected = IllegalStateException.class)
  public void testWithAppendFailsOutOfOrder() {
    GallicWeight gw1 = GallicWeight.createFromGiven(42.0, 10, 11);
    GallicWeight gw2 = GallicWeight.createFromGiven(43.0, 10);
    // ooo should throw
    UnionWeight<GallicWeight> result = gallicUnionRing.withAppended(UnionWeight.createSingle(gw1), gw2);
  }

  @Test
  public void testPlus1() {
    GallicWeight gw1 = GallicWeight.createFromGiven(43.0, 10);
    GallicWeight gw1b = GallicWeight.createFromGiven(13.0, 10);
    GallicWeight gw2 = GallicWeight.createFromGiven(42.0, 10, 11);
    GallicWeight gw3 = GallicWeight.createFromGiven(43.0, 10, 12);
    GallicWeight gw3b = GallicWeight.createFromGiven(22.0, 10, 12);
    GallicWeight gw4 = GallicWeight.createFromGiven(43.0, 13, 1);
    GallicWeight gw5 = GallicWeight.createFromGiven(43.0, 14, 1);
    assertPlus(zero, UnionWeight.createSingle(gw1), gw1);

    // both sides
    assertPlus(UnionWeight.createSingle(gw1), UnionWeight.createSingle(gw2), gw1, gw2);
    assertPlus(UnionWeight.createFromGiven(gw1, gw3), UnionWeight.createSingle(gw2), gw1, gw2, gw3);
    assertPlus(UnionWeight.createFromGiven(gw1, gw3), UnionWeight.createFromGiven(gw2, gw4), gw1, gw2, gw3, gw4);
    assertPlus(UnionWeight.createFromGiven(gw1, gw2), UnionWeight.createFromGiven(gw3, gw4), gw1, gw2, gw3, gw4);

    // with merge
    assertPlus(UnionWeight.createFromGiven(gw1, gw3, gw4), UnionWeight.createFromGiven(gw2, gw3b, gw5),
      gw1, gw2, gw3b, gw4, gw5);
    assertPlus(UnionWeight.createFromGiven(gw1b, gw3), UnionWeight.createFromGiven(gw1, gw3b), gw1b, gw3b);
  }

  @Test
  public void testTimes1() {
    GallicWeight gw1 = GallicWeight.createFromGiven(43.0, 9);
    GallicWeight gw2 = GallicWeight.createFromGiven(13.0, 10);
    GallicWeight gw3 = GallicWeight.createFromGiven(42.0, 10, 11);
    // times concats left to right in the gallic... so its not commutative
    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(56.0, 9, 10)),
      gallicUnionRing.times(UnionWeight.createFromGiven(gw1), UnionWeight.createFromGiven(gw2)).getWeights());
    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(56.0, 10, 9)),
      gallicUnionRing.times(UnionWeight.createFromGiven(gw2), UnionWeight.createFromGiven(gw1)).getWeights());

    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(85.0, 9, 10, 11),
      GallicWeight.createFromGiven(55.0, 10, 10, 11)),
      gallicUnionRing.times(UnionWeight.createFromGiven(gw1, gw2), UnionWeight.createFromGiven(gw3)).getWeights());

    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(85.0, 10, 11, 9),
      GallicWeight.createFromGiven(55.0, 10, 11, 10)),
      gallicUnionRing.times(UnionWeight.createFromGiven(gw3), UnionWeight.createFromGiven(gw1, gw2)).getWeights());
  }

  @Test
  public void testDivideLeftSingular() {
    GallicWeight gw1 = GallicWeight.createFromGiven(43.0, 9);
    GallicWeight gw2 = GallicWeight.createFromGiven(13.0, 10);
    GallicWeight gw3 = GallicWeight.createFromGiven(42.0, 10, 11);
    GallicWeight gw4 = GallicWeight.createFromGiven(50.0, 10, 12, 13);
    // divide is only defined for union semiring when one side is a single valued union
    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(30.0)),
      gallicUnionRing.divide(UnionWeight.createFromGiven(gw1), UnionWeight.createFromGiven(gw2)).getWeights());

    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(29.0, 11)),
      gallicUnionRing.divide(UnionWeight.createFromGiven(gw3), UnionWeight.createFromGiven(gw2)).getWeights());

    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(8.0, 13), GallicWeight.createFromGiven(7.0, 12, 13)),
      gallicUnionRing.divide(UnionWeight.createFromGiven(gw4), UnionWeight.createFromGiven(gw1, gw2, gw3))
        .getWeights());

    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(-7.0)),
      gallicUnionRing.divide(UnionWeight.createFromGiven(gw1), UnionWeight.createFromGiven(gw2, gw3, gw4))
        .getWeights());
  }

  @Test
  public void testDivideRightSingular() {
    GallicWeight gw1 = GallicWeight.createFromGiven(43.0, 9);
    GallicWeight gw2 = GallicWeight.createFromGiven(13.0, 10);
    GallicWeight gw3 = GallicWeight.createFromGiven(42.0, 10, 11);
    GallicWeight gw4 = GallicWeight.createFromGiven(50.0, 10, 12, 13);
    // divide is only defined for union semiring when one side is a single valued union
    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(-30.0)),
      gallicUnionRing.divide(UnionWeight.createFromGiven(gw2), UnionWeight.createFromGiven(gw1)).getWeights());

    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(-29.0)),
      gallicUnionRing.divide(UnionWeight.createFromGiven(gw2), UnionWeight.createFromGiven(gw3)).getWeights());

    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(-37.0)),
      gallicUnionRing.divide(UnionWeight.createFromGiven(gw1, gw2, gw3), UnionWeight.createFromGiven(gw4))
        .getWeights());

    assertEquals(ImmutableList.of(GallicWeight.createFromGiven(-30.0),
      GallicWeight.createFromGiven(-1.0, 11),
      GallicWeight.createFromGiven(7.0, 12, 13)
    ), gallicUnionRing.divide(UnionWeight.createFromGiven(gw2, gw3, gw4), UnionWeight.createFromGiven(gw1))
      .getWeights());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDivideFailsMultiple() {
    GallicWeight gw1 = GallicWeight.createFromGiven(43.0, 9);
    GallicWeight gw2 = GallicWeight.createFromGiven(13.0, 10);
    GallicWeight gw3 = GallicWeight.createFromGiven(42.0, 10, 11);
    GallicWeight gw4 = GallicWeight.createFromGiven(50.0, 10, 12, 13);
    gallicUnionRing.divide(UnionWeight.createFromGiven(gw1, gw2), UnionWeight.createFromGiven(gw3, gw4));
  }

  @Test
  public void testDivideZero() {
    UnionWeight<GallicWeight> uw1 = UnionWeight.createSingle(GallicWeight.createFromGiven(43.0, 9));
    assertEquals(zero, gallicUnionRing.divide(uw1, zero));
    assertEquals(zero, gallicUnionRing.divide(zero, uw1));
  }

  @Test
  public void testCommonDivisor1() {
    UnionWeight<GallicWeight> result = gallicUnionRing.commonDivisor(UnionWeight.createSingle(createFromGiven(22.0)),
      UnionWeight.createSingle(createFromGiven(10.0, 10, 11)));
    assertEquals(ImmutableList.of(createFromGiven(10.0)), result.getWeights());

    UnionWeight<GallicWeight> result2 = gallicUnionRing.commonDivisor(UnionWeight.createSingle(createFromGiven(10.0, 10, 11)),
      UnionWeight.createSingle(createFromGiven(22.0)));
    assertEquals(ImmutableList.of(createFromGiven(10.0)), result2.getWeights());
  }

  @Test
  public void testCommonDivisor2() {
    UnionWeight<GallicWeight> result = gallicUnionRing.commonDivisor(
      UnionWeight.createSingle(createFromGiven(22.0, 10)),
      UnionWeight.createSingle(createFromGiven(10.0, 10, 11)));
    assertEquals(ImmutableList.of(createFromGiven(10.0, 10)), result.getWeights());

    UnionWeight<GallicWeight> result2 = gallicUnionRing.commonDivisor(
      UnionWeight.createSingle(createFromGiven(10.0, 10, 11)),
      UnionWeight.createSingle(createFromGiven(22.0, 10)));
    assertEquals(ImmutableList.of(createFromGiven(10.0, 10)), result2.getWeights());

    UnionWeight<GallicWeight> result3 = gallicUnionRing.commonDivisor(
      UnionWeight.createSingle(createFromGiven(10.0, 10, 11)),
      UnionWeight.createSingle(createFromGiven(22.0, 11)));
    assertEquals(ImmutableList.of(createFromGiven(10.0)), result3.getWeights());
  }

  @Test
  public void testCommonDivisor3() {
    UnionWeight<GallicWeight> result = gallicUnionRing.commonDivisor(
      UnionWeight.createSingle(createFromGiven(22.0, 10, 11, 12)),
      UnionWeight.createSingle(createFromGiven(10.0, 10, 11)));
    assertEquals(ImmutableList.of(createFromGiven(10.0, 10)), result.getWeights());

    UnionWeight<GallicWeight> result2 = gallicUnionRing.commonDivisor(
      UnionWeight.createSingle(createFromGiven(22.0, 10, 11, 12)),
      UnionWeight.createSingle(createFromGiven(10.0, 11, 12)));
    assertEquals(ImmutableList.of(createFromGiven(10.0)), result2.getWeights());
  }

  @Test
  public void testCommonDivisor4() {
    UnionWeight<GallicWeight> result = gallicUnionRing.commonDivisor(
      UnionWeight.createFromGiven(createFromGiven(22.0, 10, 11, 12), createFromGiven(9.0, 10)),
      UnionWeight.createFromGiven(createFromGiven(10.0, 10, 11), createFromGiven(11.0, 10, 11, 13)));
    assertEquals(ImmutableList.of(createFromGiven(9.0, 10)), result.getWeights());

    UnionWeight<GallicWeight> result2 = gallicUnionRing.commonDivisor(
      UnionWeight.createFromGiven(createFromGiven(22.0, 10, 11, 12), createFromGiven(9.0, 10)),
      UnionWeight.createFromGiven(createFromGiven(7.0, 13), createFromGiven(11.0, 10, 11, 13)));
    assertEquals(ImmutableList.of(createFromGiven(7.0)), result2.getWeights());
  }

  private void assertPlus(UnionWeight<GallicWeight> a, UnionWeight<GallicWeight> b, GallicWeight... expected) {
    ImmutableList<GallicWeight> expectedList = ImmutableList.copyOf(expected);
    assertEquals(expectedList, gallicUnionRing.plus(a, b).getWeights());
    assertEquals(expectedList, gallicUnionRing.plus(b, a).getWeights());
  }
}
