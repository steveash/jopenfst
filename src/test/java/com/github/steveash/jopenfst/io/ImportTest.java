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

package com.github.steveash.jopenfst.io;

import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.FstInputOutput;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;


/**
 * @author "John Salatas <jsalatas@users.sourceforge.net>"
 */
public class ImportTest {

  private static final Logger log = LoggerFactory.getLogger(ImportTest.class);

  @Test
  public void testConvert() throws IOException {

    Fst fst1 = Convert.importFst("data/openfst/basic", new TropicalSemiring());

    File tempFile = File.createTempFile("fst", "deleteme");
    tempFile.deleteOnExit();
    fst1.saveModel(tempFile);

    Fst fst2 = FstInputOutput.loadModel(tempFile);
    Fst fst3 = FstInputOutput.loadModel("data/openfst/basic.fst.ser");

    assertTrue(fst1.equals(fst2));
    assertTrue(fst2.equals(fst3));
  }
}
