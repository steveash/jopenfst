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

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.semiring.Semiring;
import com.github.steveash.jopenfst.utils.Utils;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;

/**
 * Provides the required functionality in order to convert from/to openfst's text format
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class Convert {

  /**
   * Default private Constructor.
   */
  private Convert() {
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
    exportSymbols(fst.getIsyms(), basename + ".input.syms");
    exportSymbols(fst.getOsyms(), basename + ".output.syms");
    exportFst(fst, basename + ".fst.txt");
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
      State start = fst.getStart();
      out.println(start.getId() + "\t" + start.getFinalWeight());

      // print all states
      int numStates = fst.getNumStates();
      for (int i = 0; i < numStates; i++) {
        State s = fst.getState(i);
        if (s.getId() != fst.getStart().getId()) {
          out.println(s.getId() + "\t" + s.getFinalWeight());
        }
      }

      String[] isyms = fst.getIsyms();
      String[] osyms = fst.getOsyms();
      numStates = fst.getNumStates();
      for (int i = 0; i < numStates; i++) {
        State s = fst.getState(i);
        int numArcs = s.getNumArcs();
        for (int j = 0; j < numArcs; j++) {
          Arc arc = s.getArc(j);
          String isym = (isyms != null) ? isyms[arc.getIlabel()]
                                        : Integer.toString(arc.getIlabel());
          String osym = (osyms != null) ? osyms[arc.getOlabel()]
                                        : Integer.toString(arc.getOlabel());

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
  private static void exportSymbols(String[] syms, String filename) {
    if (syms == null) {
      return;
    }

    try {
      FileWriter file = new FileWriter(filename);
      PrintWriter out = new PrintWriter(file);

      for (int i = 0; i < syms.length; i++) {
        String key = syms[i];
        out.println(key + "\t" + i);
      }

      out.close();
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
  private static HashMap<String, Integer> importSymbols(String filename) {

    URL resource;
    try {
      resource = getResource(filename);
    } catch (IllegalArgumentException e) {
      return null; // resource not found
    }
    CharSource cs = asCharSource(resource, Charsets.UTF_8);
    try {
      ImmutableList<String> lines = cs.readLines();
      HashMap<String, Integer> syms = Maps.newHashMap();
      for (String line : lines) {

        String[] tokens = line.split("\\s+");
        String sym = tokens[0];
        Integer index = Integer.parseInt(tokens[1]);
        syms.put(sym, index);
      }
      return syms;
    } catch (IOException e1) {
      throw Throwables.propagate(e1);
    }
  }

  /**
   * Imports an openfst text format Several files are imported as follows: - basename.input.syms - basename.output.syms
   * - basename.fst.txt
   *
   * @param basename the files' base name
   * @param semiring the fst's semiring
   */
  public static Fst importFst(String basename, Semiring semiring) {
    Fst fst = new Fst(semiring);

    HashMap<String, Integer> isyms = importSymbols(basename + ".input.syms");
    if (isyms == null) {
      isyms = Maps.newHashMap();
      isyms.put("<eps>", 0);
    }

    HashMap<String, Integer> osyms = importSymbols(basename + ".output.syms");
    if (osyms == null) {
      osyms = Maps.newHashMap();
      osyms.put("<eps>", 0);
    }

    HashMap<String, Integer> ssyms = importSymbols(basename + ".states.syms");

    CharSource cs = asCharSource(getResource(basename + ".fst.txt"), Charsets.UTF_8);
    try (BufferedReader br = cs.openBufferedStream()) {
      boolean firstLine = true;
      String strLine;
      HashMap<Integer, State> stateMap = new HashMap<>();

      while ((strLine = br.readLine()) != null) {
        String[] tokens = strLine.split("\\t");
        Integer inputStateId;
        if (ssyms == null) {
          inputStateId = Integer.parseInt(tokens[0]);
        } else {
          inputStateId = ssyms.get(tokens[0]);
        }
        State inputState = stateMap.get(inputStateId);
        if (inputState == null) {
          inputState = new State(semiring.zero());
          fst.addState(inputState);
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

          State nextState = stateMap.get(nextStateId);
          if (nextState == null) {
            nextState = new State(semiring.zero());
            fst.addState(nextState);
            stateMap.put(nextStateId, nextState);
          }
          // Adding arc
          if (isyms.get(tokens[2]) == null) {
            isyms.put(tokens[2], isyms.size());
          }
          int iLabel = isyms.get(tokens[2]);
          if (osyms.get(tokens[3]) == null) {
            osyms.put(tokens[3], osyms.size());
          }
          int oLabel = osyms.get(tokens[3]);
          float arcWeight = Float.parseFloat(tokens[4]);
          Arc arc = new Arc(iLabel, oLabel, arcWeight, nextState);
          inputState.addArc(arc);
        } else {
          // This is a final weight
          float finalWeight = Float.parseFloat(tokens[1]);
          inputState.setFinalWeight(finalWeight);
        }
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    fst.setIsyms(Utils.toStringArray(isyms));
    fst.setOsyms(Utils.toStringArray(osyms));

    return fst;
  }
}
