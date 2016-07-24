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

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.State;
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
  private Fst createFst() {
    TropicalSemiring ts = new TropicalSemiring();
    Fst fst = new Fst(ts);

    State s1 = new State(ts.zero());
    State s2 = new State(ts.zero());
    State s3 = new State(ts.zero());
    State s4 = new State(2.f);

    // State 0
    fst.addState(s1);
    s1.addArc(new Arc(1, 5, 1.f, s2));
    s1.addArc(new Arc(2, 4, 3.f, s2));
    fst.setStart(s1);

    // State 1
    fst.addState(s2);
    s2.addArc(new Arc(3, 3, 7.f, s2));
    s2.addArc(new Arc(4, 2, 5.f, s3));

    // State 2
    fst.addState(s3);
    s3.addArc(new Arc(5, 1, 9.f, s4));

    // State 3
    fst.addState(s4);

    return fst;
  }

  /**
   * Create the Project on Input Labels as per the example at http://www.openfst.org/twiki/bin/view/FST/ProjectDoc
   *
   * @return the created fst
   */
  private Fst createPi() {
    TropicalSemiring ts = new TropicalSemiring();
    Fst fst = new Fst(ts);
    State s1 = new State(ts.zero());
    State s2 = new State(ts.zero());
    State s3 = new State(ts.zero());
    State s4 = new State(2.f);

    // State 0
    fst.addState(s1);
    s1.addArc(new Arc(1, 1, 1.f, s2));
    s1.addArc(new Arc(2, 2, 3.f, s2));
    fst.setStart(s1);

    // State 1
    fst.addState(s2);
    s2.addArc(new Arc(3, 3, 7.f, s2));
    s2.addArc(new Arc(4, 4, 5.f, s3));

    // State 2
    fst.addState(s3);
    s3.addArc(new Arc(5, 5, 9.f, s4));

    // State 3
    fst.addState(s4);

    return fst;
  }

  /**
   * Create the Project on Output Labels as per the example at http://www.openfst.org/twiki/bin/view/FST/ProjectDoc
   *
   * @return the created fst
   */
  private Fst createPo() {
    TropicalSemiring ts = new TropicalSemiring();
    Fst fst = new Fst(ts);

    State s1 = new State(ts.zero());
    State s2 = new State(ts.zero());
    State s3 = new State(ts.zero());
    State s4 = new State(2.f);

    // State 0
    fst.addState(s1);
    s1.addArc(new Arc(5, 5, 1.f, s2));
    s1.addArc(new Arc(4, 4, 3.f, s2));
    fst.setStart(s1);

    // State 1
    fst.addState(s2);
    s2.addArc(new Arc(3, 3, 7.f, s2));
    s2.addArc(new Arc(2, 2, 5.f, s3));

    // State 2
    fst.addState(s3);
    s3.addArc(new Arc(1, 1, 9.f, s4));

    // State 3
    fst.addState(s4);

    return fst;
  }

  @Test
  public void testProject() {
    // Project on Input label
    Fst fst = createFst();
    Fst p = createPi();
    Project.apply(fst, ProjectType.INPUT);
    assertTrue(fst.equals(p));

    // Project on Output label
    fst = createFst();
    p = createPo();
    Project.apply(fst, ProjectType.OUTPUT);
    assertTrue(fst.equals(p));

  }
}
