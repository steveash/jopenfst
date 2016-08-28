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

import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.io.Convert;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;
import com.github.steveash.jopenfst.utils.FstUtils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ConnectTest {

  @Test
  public void testConnect() {
    MutableFst fst = Convert.importFst("data/tests/algorithms/connect/A",
                                       new TropicalSemiring());
    MutableFst connectSaved = Convert.importFst("data/tests/algorithms/connect/expected",
                                                new TropicalSemiring());
    Connect.apply(fst);

    assertTrue(FstUtils.fstEquals(fst, connectSaved, FstUtils.LOG_REPORTER));

  }

  @Test
  public void testConnectNoOp() {
    MutableFst fst = Convert.importFst("data/tests/algorithms/connect/B");
    MutableFst connectSaved = Convert.importFst("data/tests/algorithms/connect/B");
    Connect.apply(fst);

    assertTrue(FstUtils.fstEquals(fst, connectSaved, FstUtils.LOG_REPORTER));
  }

  @Test
  public void testConnectWithStateSymbols() {
    MutableFst fst = Convert.importFst("data/tests/algorithms/connect2/A",
                                       new TropicalSemiring());
    assertTrue(fst.isUsingStateSymbols());
    MutableFst connectSaved = Convert.importFst("data/tests/algorithms/connect2/expected",
                                                new TropicalSemiring());
    Connect.apply(fst);
    assertTrue(fst.isUsingStateSymbols());

    assertTrue(FstUtils.fstEquals(fst, connectSaved, FstUtils.LOG_REPORTER));

  }
}
