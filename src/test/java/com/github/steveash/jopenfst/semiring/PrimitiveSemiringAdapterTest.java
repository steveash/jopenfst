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
import org.junit.Before;
import org.junit.Test;

/**
 * This is really testing the properties of the semirings and not so much the adapter. The adapter is useful since
 * the semiring requirements are tested as GenericSemiring since we can then use the same harness to test generic
 * weights as well as primitive semiring weights
 */
public class PrimitiveSemiringAdapterTest {

  private WeightGenerator<Double> weightGen;
  private SemiringTester<Double> semiTester;

  @Before
  public void setUp() throws Exception {
    weightGen = WeightGenerator.makeDouble(0xEEF123);
    semiTester = new SemiringTester<>(weightGen);
    semiTester.setRandValuesToTest(200);
  }

  @Test
  public void testTropicalSemiring() {
    semiTester.assertSemiringAndDivide(new PrimitiveSemiringAdapter(TropicalSemiring.INSTANCE));
  }

  @Test
  public void testLogSemiring() {
    semiTester.assertSemiringAndDivide(new PrimitiveSemiringAdapter(LogSemiring.INSTANCE));
  }

  @Test
  public void testProbabilitySemiring() {
    semiTester.assertSemiringAndDivide(new PrimitiveSemiringAdapter(ProbabilitySemiring.INSTANCE));
  }
}
