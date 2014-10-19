/**
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
public class ReverseTest {

  @Test
  public void testReverse() {
    System.out.println("Testing Reverse...");
    // Input label sort test

    Fst fst = Convert.importFst("data/tests/algorithms/reverse/A",
                                new TropicalSemiring());
    Fst fstB = FstInputOutput
        .loadModel("data/tests/algorithms/reverse/fstreverse.fst.ser");

    Fst fstReversed = Reverse.get(fst);

    assertTrue(fstB.equals(fstReversed));

    System.out.println("Testing Reverse Completed!\n");
  }

}
