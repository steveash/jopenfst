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

import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;

public class SemiringTester<W> {

  public static <W, S extends GenericSemiring<W>> void assertFuzzy(S ring, W a, W b) {
    if (!ring.isApproxEqual(a, b)) {
      throw new AssertionError("Weight " + a + " != " + b);
    }
  }

  private final WeightGenerator<W> weightGen;
  private int randValuesToTest = 50;

  public SemiringTester(WeightGenerator<W> weightGen) {
    this.weightGen = weightGen;
  }

  public void assertSemiringAndDivide(GenericSemiring<W> semiring) {
    assertSemiring(semiring);
    assertDivide(semiring);
  }

  public void assertSemiring(GenericSemiring<W> semiring) {
    for (int i = 0; i < randValuesToTest; i++) {
      weightGen.reset();
      W a = weightGen.generate();
      W b = weightGen.generate();
      W c = weightGen.generate();
      assertSemiringPropertiesOnValues(semiring, a, b, c);
    }
  }

  public void assertDivide(GenericSemiring<W> semiring) {
    for (int i = 0; i < randValuesToTest; i++) {
      weightGen.reset();
      W a = weightGen.generate();
      W b = weightGen.generate();
      assertDivideOnValues(semiring, a, b);
    }
  }

  public int getRandValuesToTest() {
    return randValuesToTest;
  }

  public void setRandValuesToTest(int randValuesToTest) {
    this.randValuesToTest = randValuesToTest;
  }

  private void assertSemiringPropertiesOnValues(GenericSemiring<W> s, W a, W b, W c) {

    // closure
    assertTwo(a, b, (aa, bb) -> s.isMember(s.plus(aa, bb)));
    assertTwo(a, b, (aa, bb) -> s.isMember(s.times(aa, bb)));
    // associativeity
    assertThree(a, b, c, (aa, bb, cc) ->
      s.isApproxEqual(s.plus(aa, s.plus(bb, cc)),
        s.plus(s.plus(aa, bb), cc))
    );
    assertThree(a, b, c, (aa, bb, cc) ->
      s.isApproxEqual(s.times(aa, s.times(bb, cc)),
        s.times(s.times(aa, bb), cc))
    );
    // identity operations
    assertTwo(a, b, (aa, bb) -> s.isApproxEqual(aa, s.plus(aa, s.zero())));
    assertTwo(a, b, (aa, bb) -> s.isApproxEqual(aa, s.plus(s.zero(), aa)));
    assertTwo(a, b, (aa, bb) -> s.isApproxEqual(aa, s.times(aa, s.one())));
    assertTwo(a, b, (aa, bb) -> s.isApproxEqual(aa, s.times(s.one(), aa)));
    // commutative
    assertTwo(a, b, (aa, bb) -> s.isApproxEqual(s.plus(aa, bb), s.plus(bb, aa)));
    // multiplication isn't always defined to be commutative but i think in the semirings i have so far it should be
    assertTwo(a, b, (aa, bb) -> s.isApproxEqual(s.times(aa, bb), s.times(bb, aa)));
    // zero is the annihilator
    assertTwo(a, b, (aa, bb) -> s.isApproxEqual(s.zero(), s.times(aa, s.zero())));
    assertTwo(a, b, (aa, bb) -> s.isApproxEqual(s.zero(), s.times(s.zero(), aa)));
    // left commutative (i think all of the rings that i have are this, but there are some rings that aren't
    // (e.g. right string ring), so this might need to be generalized in the future like openfst is
    assertThree(a, b, c, (aa, bb, cc) ->
      s.isApproxEqual(s.times(aa, s.plus(bb, cc)),
        s.plus(s.times(aa, bb), s.times(aa, cc)))
    );
  }

  private void assertDivideOnValues(GenericSemiring<W> s, W a, W b) {
    // test left division
    W ab = s.times(a, b);
    assertThree(a, b, ab, (aa, bb, aabb) -> {
        W dd = s.divide(aabb, aa);
        if (!s.isMember(dd)) { // division is defined to work on the entire range of values or all semirings
          return true;
        }
        return s.isApproxEqual(aabb, s.times(aa, dd));
      }
    );
  }

  private <T, U> void assertTwo(T first, U second, BiFunction<T, U, Boolean> func) {
    try {
      assertEquals(true, func.apply(first, second));
    } catch (AssertionError | RuntimeException e) {
      throw new AssertionError("Failed with (" + first + ", " + second + ")", e);
    }
  }

  private <T, U, V> void assertThree(T first, U second, V third, TriFunction<T, U, V, Boolean> func) {
    try {
      assertEquals(true, func.apply(first, second, third));
    } catch (AssertionError | RuntimeException e) {
      throw new AssertionError("Failed with (" + first + ", " + second + ", " + third + ")", e);
    }
  }

  @FunctionalInterface
  static interface TriFunction<T, U, V, R> {
    R apply(T t, U u, V v);
  }
}
