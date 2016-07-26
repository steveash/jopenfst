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

package com.github.steveash.jopenfst;

import com.github.steveash.jopenfst.semiring.Semiring;

import javax.annotation.Nullable;

/**
 * Client interface for an FST
 * @author Steve Ash
 */
public interface Fst {

  String EPS = "<eps>";
  int EPS_INDEX = 0;

  State getStartState();

  Semiring getSemiring();

  int getStateCount();

  State getState(int index);

  State getState(String name);

  SymbolTable getInputSymbols();

  SymbolTable getOutputSymbols();

  @Nullable
  SymbolTable getStateSymbols();

  boolean isUsingStateSymbols();

  int getInputSymbolCount();

  int getOutputSymbolCount();

  int lookupInputSymbol(String symbol);

  int lookupOutputSymbol(String symbol);

  void throwIfThisOutputIsNotThatInput(Fst that);

  void throwIfInvalid();
}
