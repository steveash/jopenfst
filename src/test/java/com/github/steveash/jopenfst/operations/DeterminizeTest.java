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
public class DeterminizeTest {

  @Test
  public void testDeterminize() {
    System.out.println("Testing Determinization...");
    Fst fstA = Convert.importFst("data/tests/algorithms/determinize/A",
                                 new TropicalSemiring());
    Fst determinized = FstInputOutput
        .loadModel("data/tests/algorithms/determinize/fstdeterminize.fst.ser");

    Fst fstDeterminized = Determinize.get(fstA);
    assertTrue(determinized.equals(fstDeterminized));

    System.out.println("Testing Determinization Completed!\n");
  }

  public static void main(String[] args) {
    DeterminizeTest test = new DeterminizeTest();
    test.testDeterminize();
  }
}
