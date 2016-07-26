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
import com.carrotsearch.hppc.cursors.ObjectIntCursor;

import java.util.Iterator;

/**
 * @author Steve Ash
 */
public abstract class AbstractSymbolTable implements SymbolTable {


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
  };

  protected AbstractSymbolTable() {
    this.symbolToId = new ObjectIntOpenHashMap<>();
    this.idToSymbol = new IntObjectOpenHashMap<>();
  }

  protected AbstractSymbolTable(ObjectIntOpenHashMap<String> copyFromToId,
                             IntObjectOpenHashMap<String> copyFromToString) {
    this.symbolToId = new ObjectIntOpenHashMap<>(copyFromToId);
    this.idToSymbol = new IntObjectOpenHashMap<>(copyFromToString);
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
  public InvertedSymbolTable invert() {
    return inverted;
  }

  @Override
  public String[] copyAsArray() {
    int maxId = -1;
    Iterator<IntCursor> iter1 = idToSymbol.keys().iterator();
    while (iter1.hasNext()) {
      maxId = Math.max(maxId, iter1.next().value);
    }

    String[] vals = new String[maxId + 1];
    Iterator<ObjectIntCursor<String>> iter = symbolToId.iterator();
    while (iter.hasNext()) {
      ObjectIntCursor<String> cursor = iter.next();
      vals[cursor.value] = cursor.key;
    }
    return vals;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !AbstractSymbolTable.class.isAssignableFrom(o.getClass())) {
      return false;
    }

    AbstractSymbolTable that = (AbstractSymbolTable) o;

    return symbolToId.equals(that.symbolToId);

  }

  @Override
  public int hashCode() {
    int result = 31 * symbolToId.hashCode();
    return result;
  }
}
