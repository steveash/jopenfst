/**
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
public class NShortestPathsTest {

  @Test
  public void testNShortestPaths() {
    System.out.println("Testing NShortestPaths...");

    Fst fst = Convert.importFst("data/tests/algorithms/shortestpath/A",
                                new TropicalSemiring());
    Fst nsp = Convert.importFst("data/tests/algorithms/shortestpath/nsp",
                                new TropicalSemiring());

    Fst fstNsp = NShortestPaths.get(fst, 6, true);

    assertTrue(nsp.equals(fstNsp));

    System.out.println("Testing NShortestPaths Completed!\n");
  }
}
