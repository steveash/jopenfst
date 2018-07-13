/*
 * Copyright 2016 Steve Ash
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

package com.github.steveash.jopenfst.operations;

import com.github.steveash.jopenfst.FrozenSymbolTable;
import com.github.steveash.jopenfst.ImmutableFst;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.semiring.Semiring;

/**
 * If you have a large FST that you are frequently using in compose operations, you can precompute it to apply the
 * necessary mutating transformations that are used (for epsilon handling for example) to avoid expensive runtime
 * copies
 * @author Steve Ash
 */
public class PrecomputedComposeFst {

  private final Semiring semiring;
  private final String eps1;
  private final String eps2;
  private final ImmutableFst precomputed;
  private final FrozenSymbolTable inputSyms;
  private final ImmutableFst filterFst;

  PrecomputedComposeFst(String eps1, String eps2, ImmutableFst precomputed, Semiring semiring, ImmutableFst filterFst) {
    this.eps1 = eps1;
    this.eps2 = eps2;
    this.precomputed = precomputed;
    this.semiring = semiring;
    this.inputSyms = new FrozenSymbolTable(precomputed.getInputSymbols());
    this.filterFst = filterFst;
  }

  ImmutableFst getFst() {
    return precomputed;
  }

  String getEps1() {
    return eps1;
  }

  String getEps2() {
    return eps2;
  }

  Semiring getSemiring() {
    return semiring;
  }

  ImmutableFst getFilterFst() {
    return filterFst;
  }

  /**
   * Returns the precomputed FST's input symbol table as a frozen table; for composes A o B this should be
   * used as the output symbol table of A and you should be careful not to require any symbols that the B won't have
   * @return
   */
  public FrozenSymbolTable getFstInputSymbolsAsFrozen() {
    return inputSyms;
  }

  /**
   * Create a new outer (input) mutable FST that you will use to compute the FST that will be composed with this
   * precomputed one; note that the resulting mutable (empty) FST will have FROZEN input/output symbols and thus you
   * cannot add OOV symbols.
   *
   * @return
   */
  public MutableFst createNewOuterFst() {
    return new MutableFst(semiring, inputSyms, inputSyms);
  }
}
