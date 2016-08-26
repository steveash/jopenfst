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
import com.github.steveash.jopenfst.io.Convert;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;
import com.github.steveash.jopenfst.utils.FstUtils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Compose Testing for
 *
 * Examples provided by M. Mohri, "Weighted Automata Algorithms", Handbook of Weighted Automata, Springer-Verlag, 2009,
 * pp. 213–254.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ComposeTest {

  @Test
  public void testCompose() {
    MutableFst fstA = Convert.importFst("data/tests/algorithms/compose/A",
                                        new TropicalSemiring());
    MutableFst fstB = Convert.importFst("data/tests/algorithms/compose/B",
                                        new TropicalSemiring());
    MutableFst composed = Convert.importFst("data/tests/algorithms/compose/expected",
                                            new TropicalSemiring());
//        .loadModel("data/tests/algorithms/compose/fstcompose.fst.ser");

    Fst fstComposed = Compose.compose(fstA, fstB, new TropicalSemiring());

    assertTrue(FstUtils.fstEquals(composed, fstComposed, 0.00001));
    // TODO fix this test -- i think now that its doing the right thing perhaps that composed
    // fst is not correct anymore (specifically take a look at the symbol tables with the unique'd epses
  }
}
