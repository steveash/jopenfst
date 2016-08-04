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
    String symbol = prefix + "$$_" + thisId;
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
