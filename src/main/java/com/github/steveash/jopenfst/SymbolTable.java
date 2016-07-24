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
 * A mutable symbol table to record mappings between symbols and ids
 *
 * @author Steve Ash
 */
public class SymbolTable implements Iterable<ObjectIntCursor<String>> {

  public interface InvertedSymbolTable {

    /**
     * Looks up the key for this id and throws an exception if it cant find it
     */
    String keyForId(int id);
  }

  private int nextId;
  private final ObjectIntOpenHashMap<String> symbolToId;
  private final IntObjectOpenHashMap<String> idToSymbol;
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

  public SymbolTable() {
    this.nextId = 0;
    this.symbolToId = new ObjectIntOpenHashMap<>();
    this.idToSymbol = new IntObjectOpenHashMap<>();
  }

  public SymbolTable(SymbolTable copyFrom) {
    this.nextId = copyFrom.nextId;
    this.symbolToId = new ObjectIntOpenHashMap<>(copyFrom.symbolToId);
    this.idToSymbol = new IntObjectOpenHashMap<>(copyFrom.idToSymbol);
  }

  public SymbolTable(String[] existing) {
    symbolToId = new ObjectIntOpenHashMap<>(existing.length);
    idToSymbol = new IntObjectOpenHashMap<>(existing.length);
    for (int i = 0; i < existing.length; i++) {
      if (!symbolToId.putIfAbsent(existing[i], i)) {
        throw new IllegalArgumentException("There are two entries in the symbol table for " + existing[i]);
      }
      idToSymbol.put(i, existing[i]);
    }
    this.nextId = existing.length;
  }

  private void putMappingOrThrow(String symbol, int id) {
    if (!symbolToId.putIfAbsent(symbol, id)) {
      if (symbolToId.get(symbol) != id) {
        throw new IllegalArgumentException("Putting a contradictory mapping of " + symbol + " to " + id + " when its"
                                           + " already " + symbolToId.get(symbol));
      }
      return; // already have the mapping and we silently drop dups
    }
    if (!idToSymbol.putIfAbsent(id, symbol)) {
      throw new IllegalStateException("Somehow the id->symbol table is wrong for " + symbol + " to " + id + " got " +
                                      idToSymbol.get(id));
    }
  }

  public int size() {
    return symbolToId.size();
  }

  @Override
  public Iterator<ObjectIntCursor<String>> iterator() {
    return symbolToId.iterator();
  }

  public Iterable<IntCursor> indexes() {
    return idToSymbol.keys();
  }

  /**
   * Returns the new id or the existing id if it already existed
   */
  public int getOrAdd(String symbol) {
    int thisId = nextId;
    if (symbolToId.putIfAbsent(symbol, thisId)) {
      nextId += 1;
      if (!idToSymbol.putIfAbsent(thisId, symbol)) {
        throw new IllegalStateException("idToSymbol is inconsistent for " + symbol);
      }
      return thisId;
    } else {
      return symbolToId.get(symbol);
    }
  }

  public int addNew(String symbol) {
    int thisId = nextId;
    putMappingOrThrow(symbol, thisId);
    nextId += 1;
    return thisId;
  }

  public int addNewUnique(String prefix) {
    int thisId = nextId;
    String symbol = prefix + "$$_" + thisId;
    putMappingOrThrow(symbol, thisId);
    nextId += 1;
    return thisId;
  }

  public void put(String symbol, int id) {
    putMappingOrThrow(symbol, id);
    if (id >= nextId) {
      nextId = id + 1;
    }
  }

  /**
   * Returns the id of the symbol or throws an exception if this symbol isnt in the table
   */
  public int get(String symbol) {
    int id = symbolToId.getOrDefault(symbol, -1);
    if (id < 0) {
      throw new IllegalArgumentException("No symbol exists for key " + symbol);
    }
    return id;
  }

  public InvertedSymbolTable invert() {
    return inverted;
  }

  public String[] copyAsArray() {
    String[] vals = new String[nextId];
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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SymbolTable that = (SymbolTable) o;

    if (nextId != that.nextId) {
      return false;
    }
    return symbolToId.equals(that.symbolToId);

  }

  @Override
  public int hashCode() {
    int result = nextId;
    result = 31 * result + symbolToId.hashCode();
    return result;
  }
}
