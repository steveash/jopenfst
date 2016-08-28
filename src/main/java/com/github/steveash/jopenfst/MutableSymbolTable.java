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

import com.google.common.collect.Lists;

import com.carrotsearch.hppc.cursors.IntObjectCursor;

import java.util.List;

/**
 * A mutable symbol table to record mappings between symbols and ids
 *
 * @author Steve Ash
 */
public class MutableSymbolTable extends AbstractSymbolTable implements WriteableSymbolTable {

  private int nextId;

  public MutableSymbolTable() {
    this.nextId = 0;
  }

  // protected constructor used by the union symbol table
  MutableSymbolTable(int nextId) {
    this.nextId = nextId;
  }

  public MutableSymbolTable(SymbolTable copyFrom) {
    super(copyFrom);

    if (copyFrom instanceof MutableSymbolTable) {
      this.nextId = ((MutableSymbolTable)copyFrom).nextId;
    } else {
      this.nextId = AbstractSymbolTable.maxIdIn(copyFrom) + 1;
    }
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

  public void remove(int id) {
    String symbol = invert().keyForId(id);
    idToSymbol.remove(id);
    symbolToId.remove(symbol);
  }

  /**
   * If there are ids to reclaim at the end, then this will do this (useful after a compaction/connect of the fst)
   */
  public void trimIds() {
    // typical case shortcut
    if (idToSymbol.containsKey(nextId - 1)) {
      return;
    }
    int max = -1;
    for (IntObjectCursor<String> cursor : idToSymbol) {
      max = Math.max(max, cursor.key);
    }
    nextId = max + 1;
  }

  /**
   * Remap a set of oldIndex -> newIndex such that this whole thing is done as one operation and you dont have
   * to worry about the ordering to consider to be sure you dont lose any symbols
   * Each indexPair is <oldId, newId>
   * @param listOfOldToNew
   */
  public void remapAll(List<IndexPair> listOfOldToNew) {
    List<String> symbols = Lists.newArrayListWithCapacity(listOfOldToNew.size());
    int max = -1;
    for (int i = 0; i < listOfOldToNew.size(); i++) {
      IndexPair indexPair = listOfOldToNew.get(i);
      symbols.add(invert().keyForId(indexPair.getLeft()));
      max = Math.max(max, indexPair.getRight());
    }
    if (max >= nextId) {
      nextId = max + 1;
    }
    // now actually remap them
    for (int i = 0; i < listOfOldToNew.size(); i++) {
      IndexPair pair = listOfOldToNew.get(i);
      String symbol = symbols.get(i);
      idToSymbol.remove(pair.getLeft());
      symbolToId.remove(symbol);

      idToSymbol.put(pair.getRight(), symbol);
      symbolToId.put(symbol, pair.getRight());
    }
  }

  /**
   * Returns the new id or the existing id if it already existed
   */
  @Override
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

  @Override
  public int addNewUnique(String prefix) {
    int thisId = nextId;
    String symbol = "<" + prefix + "$$_" + thisId + ">";
    putMappingOrThrow(symbol, thisId);
    nextId += 1;
    return thisId;
  }

  @Override
  public void put(String symbol, int id) {
    putMappingOrThrow(symbol, id);
    if (id >= nextId) {
      nextId = id + 1;
    }
  }
}
