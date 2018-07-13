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

import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import java.io.File;
import java.io.IOException;

/**
 * Provides a command line utility to convert an Fst in openfst's text format to java binary fst model
 *
 * @author John Salatas jsalatas@users.sourceforge.net
 */
public class Import {

  /**
   * Default Constructor
   */
  private Import() {
  }

  /**
   * Imports an openfst text format and serializes it as java binary model Several files are imported as follows: -
   * basename.input.syms - basename.output.syms - basename.fst.txt
   *
   * args[0] openfst's files basename
   * args[1] the java binary model filename
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Input and output files not provided");
      System.err
          .println("You need to provide both the input binary openfst model");
      System.err.println("and the output serialized java fst model.");
      System.exit(1);
    }

    MutableFst fst = Convert.importFst(args[0], TropicalSemiring.INSTANCE);

    // Serialize the java fst model to disk
    System.out.println("Saving as binary java fst model...");
    try {
      fst.saveModel(new File(args[1]));

    } catch (IOException e) {
      System.err.println("Cannot write to file " + args[1]);
      System.exit(1);
    }
  }
}
