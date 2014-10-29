/**
 *
 */
package com.github.steveash.jopenfst.operations;

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.ImmutableFst;
import com.github.steveash.jopenfst.State;

/**
 * Project operation.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class Project {

  /**
   * Default Constructor
   */
  private Project() {
  }

  /**
   * Projects an fst onto its domain or range by either copying each arc's input label to its output label or vice
   * versa.
   */
  public static void apply(Fst fst, ProjectType pType) {
    if (pType == ProjectType.INPUT) {
      fst.setOutputSymbolsFromThatInput(fst);
    } else if (pType == ProjectType.OUTPUT) {
      fst.setInputSymbolsFromThatOutput(fst);
    }

    int numStates = fst.getNumStates();
    for (int i = 0; i < numStates; i++) {
      State s = fst.getState(i);
      // Immutable fsts hold an additional (null) arc
      int numArcs = (fst instanceof ImmutableFst) ? s.getNumArcs() - 1 : s
          .getNumArcs();
      for (int j = 0; j < numArcs; j++) {
        Arc a = s.getArc(j);
        if (pType == ProjectType.INPUT) {
          a.setOlabel(a.getIlabel());
        } else if (pType == ProjectType.OUTPUT) {
          a.setIlabel(a.getOlabel());
        }
      }
    }
  }
}
