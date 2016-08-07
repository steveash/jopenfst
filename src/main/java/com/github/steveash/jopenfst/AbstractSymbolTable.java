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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import com.github.steveash.jopenfst.utils.FstUtils;

import java.util.Iterator;

/**
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

  // no hash code because these shouldn't ever be maps

  @Override
  public int hashCode() {
    throw new IllegalStateException();
  }
}
