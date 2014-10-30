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

import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.FstInputOutput;

/**
 * Provides a command line utility to convert a java binary fst model to openfst's text format
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class Export {

  /**
   * Default Constructor
   */
  private Export() {
  }

  /**
   * Exports a java binary model to openfst text format Several files are exported as follows: - basename.input.syms -
   * basename.output.syms - basename.fst.txt
   *
   * args[0] the java binary model filename
   * args[1] openfst's files basename
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Input and output files not provided");
      System.err
          .println("You need to provide both the input serialized java fst model");
      System.err.println("and the output binary openfst model.");
      System.exit(1);
    }

    Fst fst = FstInputOutput.loadModel(args[0]);

    // Serialize the java fst model to disk
    System.out.println("Saving as openfst text model...");
    Convert.export(fst, args[1]);
  }

}
