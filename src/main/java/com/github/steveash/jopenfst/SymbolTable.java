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

import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.ObjectIntCursor;

import java.util.Iterator;

/**
 * @author Steve Ash
 */
public interface SymbolTable extends Iterable<ObjectIntCursor<String>> {

  int size();

  @Override
  Iterator<ObjectIntCursor<String>> iterator();

  Iterable<IntCursor> indexes();

  int get(String symbol);

  boolean contains(String symbol);

  InvertedSymbolTable invert();

  interface InvertedSymbolTable {

    /**
     * Looks up the key for this id and throws an exception if it cant find it
     */
    String keyForId(int id);

    boolean containsKey(int id);
  }


}
