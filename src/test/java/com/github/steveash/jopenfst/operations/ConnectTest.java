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
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ConnectTest {

  @Test
  public void testConnect() {
    System.out.println("Testing Connect...");
    Fst fst = Convert.importFst("data/tests/algorithms/connect/A",
                                new TropicalSemiring());
    Fst connectSaved = FstInputOutput
        .loadModel("data/tests/algorithms/connect/fstconnect.fst.ser");
    Connect.apply(fst);

    assertTrue(connectSaved.equals(fst));

    System.out.println("Testing Connect Completed!\n");
  }

  public static void main(String[] args) {
    ConnectTest test = new ConnectTest();
    test.testConnect();
  }

}
