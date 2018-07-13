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

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import com.github.steveash.jopenfst.utils.FstUtils;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import java.util.Iterator;

/**
 * The base abstract implementation which uses carrotsearch primitive maps for optimized mappings of
 * int -> string and vice versa.
 * This implementation is effectively thread safe if no mutating operations are performed after
 * construction.
 *
 * @author Steve Ash
 */
public abstract class AbstractSymbolTable implements SymbolTable {

  private final Function<ObjectCursor<String>, String> keyFromContainer =
      new Function<ObjectCursor<String>, String>() {
        @Override
        public String apply(ObjectCursor<String> input) {
          return input.value;
        }
      };

  /**
   * Returns the current max id mapped in this symbol table or 0 if this has no mappings
   * @param table
   * @return
   */
  public static int maxIdIn(SymbolTable table) {
    int max = 0;
    for (ObjectIntCursor<String> cursor : table) {
      max = Math.max(max, cursor.value);
    }
    return max;
  }

  protected final ObjectIntOpenHashMap<String> symbolToId;
  protected final IntObjectOpenHashMap<String> idToSymbol;
  private final InvertedSymbolTable inverted = new InvertedSymbolTable() {
    @Override
    public String keyForId(int id) {
      String maybe = idToSymbol.getOrDefault(id, null);
      if (maybe == null) {
        throw new IllegalArgumentException("No key exists for id " + id);
      }
      return maybe;
    }

    @Override
    public boolean containsKey(int id) {
      return idToSymbol.containsKey(id);
    }
  };

  protected AbstractSymbolTable() {
    this.symbolToId = new ObjectIntOpenHashMap<>();
    this.idToSymbol = new IntObjectOpenHashMap<>();
  }

  protected AbstractSymbolTable(SymbolTable copyFrom) {

    this.symbolToId = new ObjectIntOpenHashMap<>(copyFrom.size());
    this.idToSymbol = new IntObjectOpenHashMap<>(copyFrom.size());
    for (ObjectIntCursor<String> cursor : copyFrom) {
      symbolToId.put(cursor.key, cursor.value);
      idToSymbol.put(cursor.value, cursor.key);
    }
  }

  @Override
  public int size() {
    return symbolToId.size();
  }

  @Override
  public Iterator<ObjectIntCursor<String>> iterator() {
    return symbolToId.iterator();
  }

  @Override
  public Iterable<IntCursor> indexes() {
    return idToSymbol.keys();
  }

  public Iterable<String> symbols() {
    return Iterables.transform(symbolToId.keys(), keyFromContainer);
  }

  /**
   * Returns the id of the symbol or throws an exception if this symbol isnt in the table
   */
  @Override
  public int get(String symbol) {
    int id = symbolToId.getOrDefault(symbol, -1);
    if (id < 0) {
      throw new IllegalArgumentException("No symbol exists for key " + symbol);
    }
    return id;
  }

  @Override
  public boolean contains(String symbol) {
    return symbolToId.containsKey(symbol);
  }

  @Override
  public InvertedSymbolTable invert() {
    return inverted;
  }

  @Override
  public boolean equals(Object o) {
    return FstUtils.symbolTableEquals(this, o);

  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Symbols;size=").append(size()).append(";[");
    boolean tooBig = this.size() > 25;
    int count = 0;
    for (int i = 0; i < Math.min(25, this.size()); i++) {
      String symbol = idToSymbol.get(i);
      if (i > 0) sb.append(',');
      sb.append(i).append("=").append(symbol == null ? "<null>" : symbol);
    }
    if (tooBig) {
      int omitted = this.size() - 25;
      sb.append(",...").append(omitted).append(" omitted]");
    }
    return sb.toString();
  }

  // no hash code because these shouldn't ever be maps

  @Override
  public int hashCode() {
    throw new IllegalStateException();
  }
}
