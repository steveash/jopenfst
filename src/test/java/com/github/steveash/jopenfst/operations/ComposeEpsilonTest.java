/**
 *
 * Copyright 1999-2012 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package com.github.steveash.jopenfst.operations;

import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.io.Convert;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ComposeEpsilonTest {

  @Test
  public void testCompose() {
    System.out.println("Testing Composition with Epsilons...");
    // Input label sort test

    Fst fstA = Convert.importFst("data/tests/algorithms/composeeps/A",
                                 new TropicalSemiring());
    Fst fstB = Convert.importFst("data/tests/algorithms/composeeps/B",
                                 new TropicalSemiring());
    Fst fstC = Convert.importFst(
        "data/tests/algorithms/composeeps/fstcomposeeps",
        new TropicalSemiring());

    Fst fstComposed = Compose.get(fstA, fstB, new TropicalSemiring());

    assertTrue(fstC.equals(fstComposed));

    System.out.println("Testing Composition with Epsilons Completed!\n");
  }

}
