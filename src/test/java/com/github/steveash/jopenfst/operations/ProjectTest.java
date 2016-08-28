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
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ProjectTest {

  /**
   * Create an fst to Project as per the example at http://www.openfst.org/twiki/bin/view/FST/ProjectDoc
   *
   * @return the created fst
   */
  private MutableFst createFst() {
    TropicalSemiring ts = new TropicalSemiring();
    MutableFst fst = new MutableFst(ts);

    MutableState s1 = fst.newState(ts.zero());
    MutableState s2 = fst.newState(ts.zero());
    MutableState s3 = fst.newState(ts.zero());
    MutableState s4 = fst.newState(2.f);

    // State 0
    fst.addArc(s1, 1, 5, s2, 1.f);
    fst.addArc(s1, 2, 4, s2, 3.f);
    fst.setStart(s1);

    // State 1
    fst.addArc(s2, 3, 3, s2, 7.f);
    fst.addArc(s2, 4, 2, s3, 5.f);

    // State 2
    fst.addArc(s3, 5, 1, s4, 9.f);

    return fst;
  }

  /**
   * Create the Project on Input Labels as per the example at http://www.openfst.org/twiki/bin/view/FST/ProjectDoc
   *
   * @return the created fst
   */
  private MutableFst createPi() {
    TropicalSemiring ts = new TropicalSemiring();
    MutableFst fst = new MutableFst(ts);
    MutableState s1 = fst.newState(ts.zero());
    MutableState s2 = fst.newState(ts.zero());
    MutableState s3 = fst.newState(ts.zero());
    MutableState s4 = fst.newState(2.f);

    // State 0
    fst.addArc(s1, 1, 1, s2, 1.f);
    fst.addArc(s1, 2, 2, s2, 3.f);
    fst.setStart(s1);

    // State 1
    fst.addArc(s2, 3, 3, s2, 7.f);
    fst.addArc(s2, 4, 4, s3, 5.f);

    // State 2
    fst.addArc(s3, 5, 5, s4, 9.f);

    return fst;
  }

  /**
   * Create the Project on Output Labels as per the example at http://www.openfst.org/twiki/bin/view/FST/ProjectDoc
   *
   * @return the created fst
   */
  private MutableFst createPo() {
    TropicalSemiring ts = new TropicalSemiring();
    MutableFst fst = new MutableFst(ts);

    MutableState s1 = fst.newState(ts.zero());
    MutableState s2 = fst.newState(ts.zero());
    MutableState s3 = fst.newState(ts.zero());
    MutableState s4 = fst.newState(2.f);

    // State 0
    fst.addArc(s1, 5, 5, s2, 1.f);
    fst.addArc(s1, 4, 4, s2, 3.f);
    fst.setStart(s1);

    // State 1
    fst.addArc(s2, 3, 3, s2, 7.f);
    fst.addArc(s2, 2, 2, s3, 5.f);

    // State 2
    fst.addArc(s3, 1, 1, s4, 9.f);

    return fst;
  }

  @Test
  public void testProject() {
    // Project on Input label
    MutableFst fst = createFst();
    MutableFst p = createPi();
    Project.apply(fst, ProjectType.INPUT);
    assertTrue(fst.equals(p));

    // Project on Output label
    fst = createFst();
    p = createPo();
    Project.apply(fst, ProjectType.OUTPUT);
    assertTrue(fst.equals(p));

  }
}
