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

import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ArcSortTest {

  /**
   * Create an output label sorted fst as per the example at http://www.openfst.org/twiki/bin/view/FST/ArcSortDoc
   *
   * @return the created fst
   */
  private MutableFst createOsorted() {
    MutableFst fst = new MutableFst(new TropicalSemiring());

    MutableState s1 = new MutableState(0.f);
    MutableState s2 = new MutableState(0.f);
    MutableState s3 = new MutableState(0.f);

    // State 0
    fst.addState(s1);
    s1.addArc(new MutableArc(4, 1, 0.f, s3));
    s1.addArc(new MutableArc(5, 2, 0.f, s3));
    s1.addArc(new MutableArc(2, 3, 0.f, s2));
    s1.addArc(new MutableArc(1, 4, 0.f, s2));
    s1.addArc(new MutableArc(3, 5, 0.f, s2));

    // State 1
    fst.addState(s2);
    s2.addArc(new MutableArc(3, 1, 0.f, s3));
    s2.addArc(new MutableArc(1, 2, 0.f, s3));
    s2.addArc(new MutableArc(2, 3, 0.f, s2));

    // State 2 (final)
    fst.addState(s3);

    return fst;
  }

  /**
   * Create an input label sorted fst as per the example at http://www.openfst.org/twiki/bin/view/FST/ArcSortDoc
   *
   * @return the created fst
   */
  private MutableFst createIsorted() {
    MutableFst fst = new MutableFst(new TropicalSemiring());

    MutableState s1 = new MutableState(0.f);
    MutableState s2 = new MutableState(0.f);
    MutableState s3 = new MutableState(0.f);

    // State 0
    fst.addState(s1);
    s1.addArc(new MutableArc(1, 4, 0.f, s2));
    s1.addArc(new MutableArc(2, 3, 0.f, s2));
    s1.addArc(new MutableArc(3, 5, 0.f, s2));
    s1.addArc(new MutableArc(4, 1, 0.f, s3));
    s1.addArc(new MutableArc(5, 2, 0.f, s3));

    // State 1
    fst.addState(s2);
    s2.addArc(new MutableArc(1, 2, 0.f, s3));
    s2.addArc(new MutableArc(2, 3, 0.f, s2));
    s2.addArc(new MutableArc(3, 1, 0.f, s3));

    // State 2 (final)
    fst.addState(s3);

    return fst;
  }

  /**
   * Create an unsorted fst as per the example at http://www.openfst.org/twiki/bin/view/FST/ArcSortDoc
   *
   * @return the created fst
   */
  private MutableFst createUnsorted() {
    MutableFst fst = new MutableFst(new TropicalSemiring());

    MutableState s1 = new MutableState(0.f);
    MutableState s2 = new MutableState(0.f);
    MutableState s3 = new MutableState(0.f);

    // State 0
    fst.addState(s1);
    s1.addArc(new MutableArc(1, 4, 0.f, s2));
    s1.addArc(new MutableArc(3, 5, 0.f, s2));
    s1.addArc(new MutableArc(2, 3, 0.f, s2));
    s1.addArc(new MutableArc(5, 2, 0.f, s3));
    s1.addArc(new MutableArc(4, 1, 0.f, s3));

    // State 1
    fst.addState(s2);
    s2.addArc(new MutableArc(2, 3, 0.f, s2));
    s2.addArc(new MutableArc(3, 1, 0.f, s3));
    s2.addArc(new MutableArc(1, 2, 0.f, s3));

    // State 2 (final)
    fst.addState(s3);

    return fst;
  }

  @Test
  public void testArcSort() {
    // Input label sort test
    MutableFst fst1 = createUnsorted();
    MutableFst fst2 = createIsorted();
    assertTrue(!fst1.equals(fst2));
    ArcSort.sortBy(fst1, new ILabelCompare());
    assertTrue(fst1.equals(fst2));

    // Output label sort test
    fst1 = createUnsorted();
    fst2 = createOsorted();
    assertTrue(!fst1.equals(fst2));
    ArcSort.sortBy(fst1, new OLabelCompare());
    assertTrue(fst1.equals(fst2));

  }

}
