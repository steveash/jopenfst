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

package com.github.steveash.jopenfst;

import com.github.steveash.jopenfst.semiring.Semiring;
import com.github.steveash.jopenfst.utils.FstUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

/**
 * Immutable version of an FST; constructed from a mutableFst instance
 *
 * NOTE: All Immutable* classes are thread safe
 * @author Steve Ash
 */
public class ImmutableFst implements Fst {

  private final ImmutableList<ImmutableState> states;
  private final Semiring semiring;
  private final ImmutableState start;
  private final ImmutableSymbolTable itable;
  private final ImmutableSymbolTable otable;
  private final ImmutableSymbolTable stable;

  public ImmutableFst(MutableFst copyFrom) {
    this.semiring = copyFrom.getSemiring();
    this.itable = new ImmutableSymbolTable(copyFrom.getInputSymbols());
    this.otable = new ImmutableSymbolTable(copyFrom.getOutputSymbols());
    if (copyFrom.isUsingStateSymbols()) {
      this.stable = new ImmutableSymbolTable(copyFrom.getStateSymbols());
    } else {
      this.stable = null;
    }

    ImmutableList.Builder<ImmutableState> builder = ImmutableList.builder();
    for (int i = 0; i < copyFrom.getStateCount(); i++) {
      MutableState from = copyFrom.getState(i);
      builder.add(new ImmutableState(from));
    }
    this.states = builder.build();
    this.start = this.states.get(copyFrom.getStartState().getId());
    // always do this last after all invariants are setup
    for (ImmutableState state : this.states) {
      state.init(this);
    }
  }

  @Override
  public ImmutableState getStartState() {
    return start;
  }

  @Override
  public Semiring getSemiring() {
    return semiring;
  }

  @Override
  public int getStateCount() {
    return states.size();
  }

  @Override
  public ImmutableState getState(int index) {
    return states.get(index);
  }

  @Override
  public State getState(String name) {
    Preconditions.checkState(stable != null, "cant use get by name if not using state symols");
    return getState(stable.get(name));
  }

  @Override
  public ImmutableSymbolTable getInputSymbols() {
    return itable;
  }

  @Override
  public ImmutableSymbolTable getOutputSymbols() {
    return otable;
  }

  @Nullable
  @Override
  public ImmutableSymbolTable getStateSymbols() {
    return stable;
  }

  @Override
  public int getInputSymbolCount() {
    return itable.size();
  }

  @Override
  public int getOutputSymbolCount() {
    return otable.size();
  }

  @Override
  public int lookupInputSymbol(String symbol) {
    return itable.get(symbol);
  }

  @Override
  public int lookupOutputSymbol(String symbol) {
    return otable.get(symbol);
  }

  @Override
  public boolean isUsingStateSymbols() {
    return stable != null;
  }

  @Override
  public void throwIfInvalid() {
    // cant even construct an invalid immutable fst
  }

  @Override
  public boolean equals(Object obj) {
    return FstUtils.fstEquals(this, obj);
  }

  @Override
  public int hashCode() {
    int result = semiring != null ? semiring.hashCode() : 0;
    result = 31 * result + (states != null ? states.hashCode() : 0);
    result = 31 * result + (start != null ? start.hashCode() : 0);
    result = 31 * result + (itable != null ? itable.hashCode() : 0);
    result = 31 * result + (otable != null ? otable.hashCode() : 0);
    return result;
  }
}
