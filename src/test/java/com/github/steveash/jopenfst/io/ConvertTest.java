/*
 * Copyright 2016 Steve Ash
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

package com.github.steveash.jopenfst.io;

import com.github.steveash.jopenfst.ImmutableFst;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ash
 */
public class ConvertTest {

  @Test
  public void shouldMakeImmutableEquals() throws Exception {
    MutableFst read1 = Convert.importFst("data/openfst/basic", new TropicalSemiring());
    ImmutableFst read2 = new ImmutableFst(read1);

    assertTrue(read1.equals(read2));

    read1.getState(0).getArc(1).setWeight(12332.32f);
    assertFalse(read1.equals(read2));
  }
}