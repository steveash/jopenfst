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

import com.google.common.collect.Iterables;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ash
 */
public class UnionSymbolTableTest {

  @Test
  public void shouldEqual() throws Exception {
    MutableSymbolTable base = new MutableSymbolTable();
    int a = base.getOrAdd("A");
    base.getOrAdd("B");
    base.getOrAdd("C");
    int d = base.getOrAdd("D");
    MutableSymbolTable next = new MutableSymbolTable(base);
    UnionSymbolTable union = new UnionSymbolTable(base);
    assertEquals(union, next);
    assertEquals(union, base);

    next.getOrAdd("EEE");
    next.getOrAdd("FFF");
    assertEquals(union, base);
    assertFalse(union.equals(next));
    int e = union.getOrAdd("E");
    int f = union.getOrAdd("F");
    assertFalse(union.equals(base));
    assertEquals(6, union.size());
    assertEquals(6, next.size());
    assertEquals(6, Iterables.size(union));
    assertEquals(4, base.size());
    assertEquals(4, Iterables.size(base));

    assertEquals(a, union.get("A"));
    assertEquals(d, union.get("D"));
    assertEquals(e, union.get("E"));
    assertEquals(f, union.get("F"));

    assertEquals(a, base.get("A"));
    assertEquals(d, base.get("D"));
    assertFalse(base.contains("E"));
    assertFalse(base.contains("F"));
  }
}