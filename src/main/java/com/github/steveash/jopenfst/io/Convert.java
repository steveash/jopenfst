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

package com.github.steveash.jopenfst.io;

import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.MutableSymbolTable;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.SymbolTable;
import com.github.steveash.jopenfst.semiring.Semiring;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;

import static com.google.common.io.Resources.asCharSource;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Provides the required functionality in order to convert from/to openfst's text format
 *
 * NOTE that the original CMU implementation of this assumed that the symbols themselves were in the fst text format
 * and NOT the symbol ids (as described in the AT&T spec). There is a static flag (yuck, I know) to control whether
 * you expect symbols or symbol ids in the input/output text files (defaulting to expecting the symbols themselves
 *
 * @author John Salatas jsalatas@users.sourceforge.net
 */
public class Convert {

  private static final String INPUT_SYMS = ".input.syms";
  private static final String OUTPUT_SYMS = ".output.syms";
  private static final String FST_TXT = ".fst.txt";
  private static final String STATES_SYMS = ".states.syms";

  // if true, then expect the tokens in the text format to be integer symbol ids and not the symbols themselves
  private static boolean useSymbolIdsInText = false;

  /**
   * if true, then expects that the tokens in the input and output symbols are the integer ids of the token and not
   * the token itself
   * @return
   */
  public static boolean isUseSymbolIdsInText() {
    return useSymbolIdsInText;
  }

  /**
   * If true then when importing an FST text file, it interprets the states as ids from the isymb/osymb tables
   * instead of the symbol values themselves (the strings)
   * @param useSymbolIdsInText
   */
  public static void setUseSymbolIdsInText(boolean useSymbolIdsInText) {
    Convert.useSymbolIdsInText = useSymbolIdsInText;
  }

  /**
   * Exports an fst to the openfst text format Several files are created as follows: - basename.input.syms -
   * basename.output.syms - basename.fst.txt See <a href="http://www.openfst.org/twiki/bin/view/FST/FstQuickTour">OpenFst
   * Quick Tour</a>
   *
   * @param fst      the fst to export
   * @param basename the files' base name
   */
  public static void export(Fst fst, String basename) {
    exportSymbols(fst.getInputSymbols(), basename + INPUT_SYMS);
    exportSymbols(fst.getOutputSymbols(), basename + OUTPUT_SYMS);
    if (fst.isUsingStateSymbols()) {
      exportSymbols(fst.getStateSymbols(), basename + STATES_SYMS);
    }
    exportFst(fst, basename + FST_TXT);
  }

  /**
   * Exports an fst to the openfst text format
   *
   * @param fst      the fst to export
   * @param filename the openfst's fst.txt filename
   */
  private static void exportFst(Fst fst, String filename) {
    FileWriter file;
    try {
      file = new FileWriter(filename);
      PrintWriter out = new PrintWriter(file);

      // print start first
      State start = fst.getStartState();
      out.println(start.getId() + "\t" + start.getFinalWeight());

      // print all states
      int numStates = fst.getStateCount();
      for (int i = 0; i < numStates; i++) {
        State s = fst.getState(i);
        if (s.getId() != fst.getStartState().getId()) {
          out.println(s.getId() + "\t" + s.getFinalWeight());
        }
      }

      MutableSymbolTable.InvertedSymbolTable inputIds = fst.getInputSymbols().invert();
      MutableSymbolTable.InvertedSymbolTable outputIds = fst.getOutputSymbols().invert();
      numStates = fst.getStateCount();
      for (int i = 0; i < numStates; i++) {
        State s = fst.getState(i);
        int numArcs = s.getArcCount();
        for (int j = 0; j < numArcs; j++) {
          Arc arc = s.getArc(j);
          String isym;
          String osym;
          if (useSymbolIdsInText) {
            isym = Integer.toString(arc.getIlabel());
            osym = Integer.toString(arc.getOlabel());
          } else {
            isym = inputIds.keyForId(arc.getIlabel());
            osym = outputIds.keyForId(arc.getOlabel());
          }

          out.println(s.getId() + "\t" + arc.getNextState().getId()
                      + "\t" + isym + "\t" + osym + "\t"
                      + arc.getWeight());
        }
      }

      out.close();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

  }

  /**
   * Exports a symbols' map to the openfst text format
   *
   * @param syms     the symbols' map
   * @param filename the the openfst's symbols filename
   */
  private static void exportSymbols(SymbolTable syms, String filename) {
    if (syms == null) {
      return;
    }

    try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
      for (ObjectIntCursor<String> sym : syms) {
        out.println(sym.key + "\t" + sym.value);
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Imports an openfst's symbols file
   *
   * @param filename the symbols' filename
   * @return HashMap containing the impprted string-to-id mapping
   */
  private static Optional<MutableSymbolTable> importSymbols(String filename) {

    URL resource;
    try {
      resource = Resources.getResource(filename);
    } catch (IllegalArgumentException e) {
      return Optional.absent();
    }
    return importSymbolsFrom(asCharSource(resource, Charsets.UTF_8));
  }

  private static Optional<MutableSymbolTable> importSymbols(File maybeFile) {
    if (maybeFile.exists()) {
      return importSymbolsFrom(Files.asCharSource(maybeFile, Charsets.UTF_8));
    }
    return Optional.absent();
  }

  private static Optional<MutableSymbolTable> importSymbolsFrom(CharSource cs) {
    try {
      ImmutableList<String> lines = cs.readLines();
      MutableSymbolTable newTable = new MutableSymbolTable();
      for (String line : lines) {
        if (isBlank(line)) {
          continue;
        }
        String[] tokens = line.split("\\s+");
        String sym = tokens[0];
        Integer index = Integer.parseInt(tokens[1]);
        newTable.put(sym, index);
      }
      return Optional.of(newTable);
    } catch (IOException e1) {
      throw Throwables.propagate(e1);
    }
  }

  public static MutableFst importFst(String basename) {
    return importFst(basename, TropicalSemiring.INSTANCE);
  }

  /**
   * Imports an openfst text format. You pass in the file pointing to the fst.txt file and
   * it assumes that the other files has the same prefix, but with input.syms and output.syms
   * as suffixes.  For exmaple if you pass in:
   * path/to/mymodel.fst.txt
   * It assumes that you also have:
   * path/to/mymodel.input.syms
   * path/to/mymodel.output.syms
   *
   * @param fileToFst the files' base name
   * @param semiring  the fst's semiring
   */
  public static MutableFst importFst(File fileToFst, Semiring semiring) {
    Preconditions.checkArgument(fileToFst.exists(), "File to the fst.txt openfst output doesnt exist", fileToFst);
    Preconditions.checkArgument(fileToFst.getName().endsWith(FST_TXT), "fst.txt path must end in .fst.txt", fileToFst);
    String basepath = fileToFst.getAbsolutePath();
    basepath = StringUtils.removeEnd(basepath, FST_TXT);
    CharSource cs = Files.asCharSource(fileToFst, Charsets.UTF_8);

    Optional<MutableSymbolTable> maybeInputs = importSymbols(new File(basepath + INPUT_SYMS));
    Optional<MutableSymbolTable> maybeOutputs = importSymbols(new File(basepath + OUTPUT_SYMS));
    Optional<MutableSymbolTable> maybeStates = importSymbols(new File(basepath + STATES_SYMS));

    return convertFrom(cs, maybeInputs, maybeOutputs, maybeStates, semiring);
  }

  /**
   * @see #importFst(File, Semiring)
   */
  public static MutableFst importFst(File fileToFst) {
    return importFst(fileToFst, TropicalSemiring.INSTANCE);
  }

  /**
   * Imports an openfst text format. You pass in the base path that can be loaded off of the classpath
   * For example if you had classpath location data with files data/mymodel.fst.txt, data/mymodel.input.syms,
   * and data/mymodel.output.syms then you would pass "data/mymodel" to this method
   *
   * @param basename the files' base name
   * @param semiring the fst's semiring
   */
  public static MutableFst importFst(String basename, Semiring semiring) {

    Optional<MutableSymbolTable> maybeInputs = importSymbols(basename + INPUT_SYMS);
    Optional<MutableSymbolTable> maybeOutputs = importSymbols(basename + OUTPUT_SYMS);
    Optional<MutableSymbolTable> maybeStates = importSymbols(basename + STATES_SYMS);
    CharSource cs = asCharSource(Resources.getResource(basename + FST_TXT), Charsets.UTF_8);

    return convertFrom(cs, maybeInputs, maybeOutputs, maybeStates, semiring);
  }

  private static MutableFst convertFrom(CharSource fstSource, Optional<MutableSymbolTable> maybeInputs,
                                        Optional<MutableSymbolTable> maybeOutputs,
                                        Optional<MutableSymbolTable> maybeStates, Semiring semiring) {
    MutableSymbolTable isyms;
    if (maybeInputs.isPresent()) {
      isyms = maybeInputs.get();
    } else {
      isyms = new MutableSymbolTable();
      isyms.put(MutableFst.EPS, 0);
    }

    MutableSymbolTable osyms;
    if (maybeOutputs.isPresent()) {
      osyms = maybeOutputs.get();
    } else {
      osyms = new MutableSymbolTable();
      osyms.put(MutableFst.EPS, 0);
    }

    MutableFst fst = new MutableFst(semiring, isyms, osyms);
    MutableSymbolTable ssyms = null;
    if (maybeStates.isPresent()) {
      ssyms = maybeStates.get();
      fst.useStateSymbols(ssyms);
    }

    try (BufferedReader br = fstSource.openBufferedStream()) {
      boolean firstLine = true;
      String line;
      HashMap<Integer, MutableState> stateMap = new HashMap<>();
      int lineNo = 0;
      while ((line = br.readLine()) != null) {
        lineNo += 1;
        if (isBlank(line)) {
          continue;
        }
        try {
          String[] tokens = line.split("\\t");
          Integer inputStateId;
          if (ssyms == null) {
            inputStateId = Integer.parseInt(tokens[0]);
          } else {
            inputStateId = ssyms.get(tokens[0]);
          }
          MutableState inputState = stateMap.get(inputStateId);
          if (inputState == null) {
            inputState = new MutableState(semiring.zero());
            fst.setState(inputStateId, inputState);
            stateMap.put(inputStateId, inputState);
          }

          if (firstLine) {
            firstLine = false;
            fst.setStart(inputState);
          }

          if (tokens.length > 2) {
            Integer nextStateId;
            if (ssyms == null) {
              nextStateId = Integer.parseInt(tokens[1]);
            } else {
              nextStateId = ssyms.get(tokens[1]);
            }

            MutableState nextState = stateMap.get(nextStateId);
            if (nextState == null) {
              nextState = new MutableState(semiring.zero());
              fst.setState(nextStateId, nextState);
              stateMap.put(nextStateId, nextState);
            }
            /// Adding arc
            int iLabel;
            int oLabel;
            if (useSymbolIdsInText) {
              iLabel = Integer.parseInt(tokens[2]);
              oLabel = Integer.parseInt(tokens[3]);
            } else {
              iLabel = isyms.getOrAdd(tokens[2]);
              oLabel = osyms.getOrAdd(tokens[3]);
            }
            double arcWeight;
            if (tokens.length >= 5) {
              arcWeight = Double.parseDouble(tokens[4]);
            } else {
              arcWeight = semiring.one();
            }
            fst.addArc(inputState, iLabel, oLabel, nextState, arcWeight);
          } else {
            // This is a final weight
            double finalWeight;
            if (tokens.length >= 2) {
              finalWeight = Double.parseDouble(tokens[1]);
            } else {
              finalWeight = semiring.one();
            }
            inputState.setFinalWeight(finalWeight);
          }
        } catch (RuntimeException e) {
          throw new RuntimeException("Problem converting and parsing line " + lineNo + " from FST input file. Line: " +
              line, e);
        }
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    fst.throwIfAnyNullStates();
    return fst;
  }
}
