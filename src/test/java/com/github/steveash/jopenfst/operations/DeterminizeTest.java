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


package com.github.steveash.jopenfst.operations;

import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.io.Convert;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class DeterminizeTest {

  private static final Logger log = LoggerFactory.getLogger(DeterminizeTest.class);

  @Before
  public void setUp() throws Exception {
    Convert.setRegexToSplitOn("\\s+");
  }

  @Test
  public void testDeterminizeFsa() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/determinize/A", TropicalSemiring.INSTANCE);
    MutableFst determinized = Convert.importFst("data/tests/algorithms/determinize/expected", TropicalSemiring.INSTANCE);

    MutableFst fstDeterminized = new Determinize().compute(fstA);
    assertTrue(determinized.equals(fstDeterminized));
  }

  @Test
  public void testDeterminizeFstNonFunctional() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/determinizeFstNonFunctional/A", TropicalSemiring.INSTANCE);
    MutableFst determinized = Convert.importFst("data/tests/algorithms/determinizeFstNonFunctional/expected", TropicalSemiring.INSTANCE);

    MutableFst fstDeterminized = new Determinize(Determinize.DeterminizeMode.NON_FUNCTIONAL).compute(fstA);
    assertTrue(determinized.equals(fstDeterminized));
  }

  @Test
  public void testDeterminizeFstDisambiguate() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/determinizeFstDisambiguate/A", TropicalSemiring.INSTANCE);
    MutableFst determinized = Convert.importFst("data/tests/algorithms/determinizeFstDisambiguate/expected", TropicalSemiring.INSTANCE);

    MutableFst fstDeterminized = new Determinize(Determinize.DeterminizeMode.DISAMBIGUATE).compute(fstA);
    assertTrue(determinized.equals(fstDeterminized));
  }

  @Test
  public void testDeterminizeFstFunctionalThrows() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/determinizeFstDisambiguate/A", TropicalSemiring.INSTANCE);
    try {
      new Determinize(Determinize.DeterminizeMode.FUNCTIONAL).compute(fstA);
      fail("shouldve thrown");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Trying to plus two different gallic weights"));
    }
  }

  @Test
  public void testDeterminizeWikiExample() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/determinizeWikiExample/A", TropicalSemiring.INSTANCE);
    MutableFst determinized = Convert.importFst("data/tests/algorithms/determinizeWikiExample/expected", TropicalSemiring.INSTANCE);

    MutableFst fstDeterminized = new Determinize(Determinize.DeterminizeMode.FUNCTIONAL).compute(fstA);
    assertTrue(determinized.equals(fstDeterminized));
    MutableFst fstDeterminized2 = new Determinize(Determinize.DeterminizeMode.NON_FUNCTIONAL).compute(fstA);
    assertTrue(determinized.equals(fstDeterminized2));
    MutableFst fstDeterminized3 = new Determinize(Determinize.DeterminizeMode.DISAMBIGUATE).compute(fstA);
    assertTrue(determinized.equals(fstDeterminized3));
  }

  @Test
  public void testAlreadyDeterminized() {
    MutableFst determinized = Convert.importFst("data/tests/algorithms/determinize/expected", TropicalSemiring.INSTANCE);
    MutableFst fstDeterminized = new Determinize().compute(determinized);
    assertTrue(determinized.equals(fstDeterminized));
  }

  @Test
  public void testDeterminizeFstNonFunctional2() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/determinizeFstNonFunctional2/A", TropicalSemiring.INSTANCE);
    MutableFst determinized = Convert.importFst("data/tests/algorithms/determinizeFstNonFunctional2/expected", TropicalSemiring.INSTANCE);
    MutableFst determinizedMin = Convert.importFst("data/tests/algorithms/determinizeFstNonFunctional2/expectedMin", TropicalSemiring.INSTANCE);

    MutableFst fstDeterminized = new Determinize(Determinize.DeterminizeMode.NON_FUNCTIONAL).compute(fstA);
    assertTrue(determinized.equals(fstDeterminized));

    MutableFst fstDeterminizedMin = new Determinize(Determinize.DeterminizeMode.DISAMBIGUATE).compute(fstA);
    assertTrue(determinizedMin.equals(fstDeterminizedMin));
  }

  @Test
  public void testDeterminizeFstNonFunctional3() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/determinizeFstNonFunctional3/A", TropicalSemiring.INSTANCE);
    MutableFst determinized = Convert.importFst("data/tests/algorithms/determinizeFstNonFunctional3/expected", TropicalSemiring.INSTANCE);

    MutableFst fstDeterminized = new Determinize(Determinize.DeterminizeMode.NON_FUNCTIONAL).compute(fstA);
    assertTrue(determinized.equals(fstDeterminized));
  }
}
