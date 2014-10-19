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
import com.github.steveash.jopenfst.FstInputOutput;
import com.github.steveash.jopenfst.semiring.ProbabilitySemiring;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class RmEpsilonTest {

  @Test
  public void testRmEpsilon() {
    System.out.println("Testing RmEpsilon...");

    Fst fst = Convert.importFst("data/tests/algorithms/rmepsilon/A",
                                new ProbabilitySemiring());
    Fst fstRmEps = FstInputOutput.loadModel("data/tests/algorithms/rmepsilon/fstrmepsilon.fst.ser");
    Fst rmEpsilon = RmEpsilon.get(fst);

    assertTrue(fstRmEps.equals(rmEpsilon));

    System.out.println("Testing RmEpsilon Completed!\n");
  }
}
