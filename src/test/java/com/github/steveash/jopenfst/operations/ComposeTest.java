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

package com.github.steveash.jopenfst.operations;

import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.io.Convert;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;
import com.github.steveash.jopenfst.utils.FstUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Compose Testing for
 * <p>
 * Examples provided by M. Mohri, "Weighted Automata Algorithms", Handbook of Weighted Automata, Springer-Verlag, 2009,
 * pp. 213–254.
 *
 * @author John Salatas jsalatas@users.sourceforge.net
 */
public class ComposeTest {

  @Test
  public void testCompose() {
    // this is the exact input and output for openfst with no sorting
    MutableFst fstA = Convert.importFst("data/tests/algorithms/compose/A", TropicalSemiring.INSTANCE);
    MutableFst fstB = Convert.importFst("data/tests/algorithms/compose/B", TropicalSemiring.INSTANCE);
    MutableFst composed = Convert.importFst("data/tests/algorithms/compose/expected",
        TropicalSemiring.INSTANCE);
    Fst fstComposed = Compose.compose(fstA, fstB, TropicalSemiring.INSTANCE);

    assertTrue(FstUtils.fstEquals(composed, fstComposed, 0.1, FstUtils.LOG_REPORTER));
  }

  @Test
  public void testComposeSorted() {
    // the composeSorted and compose output's are equivalent but the state ids are different because the openfst output
    // wasn't done on sorted fsts so the compose algorithm proceeds in different order, resulting in different state ids
    // the expected-sorted is just the output built from sorted inputs
    MutableFst fstA = Convert.importFst("data/tests/algorithms/compose/A", TropicalSemiring.INSTANCE);
    MutableFst fstB = Convert.importFst("data/tests/algorithms/compose/B", TropicalSemiring.INSTANCE);
    MutableFst composed = Convert.importFst("data/tests/algorithms/compose/expected-sorted",
        TropicalSemiring.INSTANCE);
    Fst fstComposed = Compose.compose(fstA, fstB, TropicalSemiring.INSTANCE, true);

    assertTrue(FstUtils.fstEquals(composed, fstComposed, 0.1, FstUtils.LOG_REPORTER));
  }

  @Test
  public void testComposePrecomputed() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/compose/A", TropicalSemiring.INSTANCE);
    MutableFst fstB = Convert.importFst("data/tests/algorithms/compose/B", TropicalSemiring.INSTANCE);
    MutableFst composed = Convert.importFst("data/tests/algorithms/compose/expected-sorted",
        TropicalSemiring.INSTANCE);

    ArcSort.sortByOutput(fstA);
    ArcSort.sortByInput(fstB);
    PrecomputedComposeFst precomputed = Compose.precomputeInner(fstB, TropicalSemiring.INSTANCE);
    fstA = MutableFst.copyAndTranslateSymbols(fstA, fstA.getInputSymbols(), precomputed.getFstInputSymbolsAsFrozen());
    MutableFst fstComposed = Compose.composeWithPrecomputed(fstA, precomputed, true);

    assertTrue(FstUtils.fstEquals(composed, fstComposed, 0.1, FstUtils.LOG_REPORTER));
  }

  @Test
  public void testManualComposePrecomputed() {
    MutableFst fstB = Convert.importFst("data/tests/algorithms/compose3/B", TropicalSemiring.INSTANCE);
    MutableFst composed = Convert.importFst("data/tests/algorithms/compose3/expected",
        TropicalSemiring.INSTANCE);
    ArcSort.sortByInput(fstB);
    PrecomputedComposeFst precomputed = Compose.precomputeInner(fstB, TropicalSemiring.INSTANCE);

    MutableFst fstA = precomputed.createNewOuterFst();
    MutableState s0 = fstA.newStartState();
    MutableState s1 = fstA.newState();
    MutableState s2 = fstA.newState();
    MutableState s3 = fstA.newState();
    s3.setFinalWeight(0.7);
    fstA.addArc(s0, "a", "bb", s1, 0.1);
    fstA.addArc(s1, "a", "bb", s0, 0.2);
    fstA.addArc(s1, "b", "bb", s2, 0.3);
    fstA.addArc(s1, "b", "bb", s3, 0.4);
    fstA.addArc(s2, "a", "bb", s3, 0.5);
    fstA.addArc(s3, "a", "aa", s3, 0.6);
    ArcSort.sortByOutput(fstA);

    MutableFst fstComposed = Compose.composeWithPrecomputed(fstA, precomputed, true);
    assertTrue(FstUtils.fstEquals(composed, fstComposed, 0.1, FstUtils.LOG_REPORTER));
  }

  @Test
  public void testCompose2() {
    // this is the example of the compose from the openfst wiki page
    MutableFst fstA = Convert.importFst("data/tests/algorithms/compose2/A", TropicalSemiring.INSTANCE);
    MutableFst fstB = Convert.importFst("data/tests/algorithms/compose2/B", TropicalSemiring.INSTANCE);
    MutableFst composed = Convert.importFst("data/tests/algorithms/compose2/expected",
        TropicalSemiring.INSTANCE);

    Fst fstComposed = Compose.compose(fstA, fstB, TropicalSemiring.INSTANCE);

    assertTrue(FstUtils.fstEquals(composed, fstComposed, 0.1, FstUtils.LOG_REPORTER));
  }

  @Test
  public void testCompose2Precomputed() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/compose2/A", TropicalSemiring.INSTANCE);
    MutableFst fstB = Convert.importFst("data/tests/algorithms/compose2/B", TropicalSemiring.INSTANCE);
    MutableFst composed = Convert.importFst("data/tests/algorithms/compose2/expected",
        TropicalSemiring.INSTANCE);

    ArcSort.sortByOutput(fstA);
    ArcSort.sortByInput(fstB);
    PrecomputedComposeFst precomputed = Compose.precomputeInner(fstB, TropicalSemiring.INSTANCE);
    fstA = MutableFst.copyAndTranslateSymbols(fstA, fstA.getInputSymbols(), precomputed.getFstInputSymbolsAsFrozen());
    Fst fstComposed = Compose.composeWithPrecomputed(fstA, precomputed, true);

    assertTrue(FstUtils.fstEquals(composed, fstComposed, 0.1, FstUtils.LOG_REPORTER));
  }
}
