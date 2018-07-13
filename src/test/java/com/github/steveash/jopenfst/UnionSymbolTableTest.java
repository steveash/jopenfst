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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ash
 */
public class UnionSymbolTableTest {

  @Test
  public void shouldDoBasicOperations() throws Exception {
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

    assertTrue(union.invert().containsKey(a));
    assertTrue(union.invert().containsKey(e));
    assertFalse(union.invert().containsKey(123));
    assertEquals("E", union.invert().keyForId(e));
    assertEquals("A", union.invert().keyForId(a));

    assertEquals(a, union.get("A"));
    assertEquals(d, union.get("D"));
    assertEquals(e, union.get("E"));
    assertEquals(f, union.get("F"));

    assertEquals(a, base.get("A"));
    assertEquals(d, base.get("D"));
    assertFalse(base.contains("E"));
    assertFalse(base.contains("F"));

    try {
      union.invert().keyForId(123);
    } catch (IllegalArgumentException iae) {
      assertEquals("No key exists for id 123", iae.getMessage());
    }
  }

  @Test
  public void shouldCreateFromDifferentBackingTables() throws Exception {
    MutableSymbolTable base = new MutableSymbolTable();
    int a = base.getOrAdd("A");
    UnionSymbolTable union1 = new UnionSymbolTable(base);
    assertEquals(union1, base);
    UnionSymbolTable union2 = new UnionSymbolTable(union1);
    assertEquals(union1, union2);

    // now mutate it, forcing a filter to be created and re-clone
    union1.getOrAdd("AAA");
    UnionSymbolTable union3 = new UnionSymbolTable(union1);
    assertEquals(union1, union3);
  }

  @Test
  public void shouldCreateCopyFromDifferentBackingTables() throws Exception {
    MutableSymbolTable base = new MutableSymbolTable();
    int a = base.getOrAdd("A");
    UnionSymbolTable union1 = new UnionSymbolTable(base);
    assertEquals(union1, base);
    UnionSymbolTable union2 = UnionSymbolTable.copyFrom(union1);
    assertEquals(union1, union2);

    // now mutate it, forcing a filter to be created and re-clone
    union1.getOrAdd("AAA");
    UnionSymbolTable union3 = UnionSymbolTable.copyFrom(union1);
    assertEquals(union1, union3);

    assertTrue(union1.contains("AAA"));
    assertFalse(union2.contains("AAA"));
    assertTrue(union3.contains("AAA"));
  }

  @Test
  public void shouldIterateOverIndexesAndSymbolsNoBacking() throws Exception {
    MutableSymbolTable base = new MutableSymbolTable();
    int a = base.getOrAdd("A");
    int b = base.getOrAdd("B");
    UnionSymbolTable union = new UnionSymbolTable(base);

    Set<String> valuesFromIterable = Sets.newHashSet();
    for (String symbol : union.symbols()) {
      assertTrue(valuesFromIterable.add(symbol));
    }
    assertEquals(ImmutableSet.of("A", "B"), valuesFromIterable);

    Set<Integer> indexesFromIterable = Sets.newHashSet();
    for (IntCursor cursor : union.indexes()) {
      assertTrue(indexesFromIterable.add(cursor.value));
    }
    assertEquals(ImmutableSet.of(a, b), indexesFromIterable);
  }

  @Test
  public void shouldIterateOverIndexesAndSymbolsWithBacking() throws Exception {
    MutableSymbolTable base = new MutableSymbolTable();
    int a = base.getOrAdd("A");
    int b = base.getOrAdd("B");
    UnionSymbolTable union = new UnionSymbolTable(base);
    int c = union.getOrAdd("C");

    Set<String> valuesFromIterable = Sets.newHashSet();
    for (String symbol : union.symbols()) {
      assertTrue(valuesFromIterable.add(symbol));
    }
    assertEquals(ImmutableSet.of("A", "B", "C"), valuesFromIterable);

    Set<Integer> indexesFromIterable = Sets.newHashSet();
    for (IntCursor cursor : union.indexes()) {
      assertTrue(indexesFromIterable.add(cursor.value));
    }
    assertEquals(ImmutableSet.of(a, b, c), indexesFromIterable);
  }

  @Test
  public void shouldSupportPutWithId() throws Exception {
    MutableSymbolTable base = new MutableSymbolTable();
    int a = base.getOrAdd("A");
    UnionSymbolTable union = new UnionSymbolTable(base);
    int b = union.getOrAdd("B");
    union.put("C", 42);
    int d = union.getOrAdd("D");

    Set<String> valuesFromIterable = Sets.newHashSet();
    for (String symbol : union.symbols()) {
      assertTrue(valuesFromIterable.add(symbol));
    }
    assertEquals(ImmutableSet.of("A", "B", "C", "D"), valuesFromIterable);

    Set<Integer> indexesFromIterable = Sets.newHashSet();
    for (IntCursor cursor : union.indexes()) {
      assertTrue(indexesFromIterable.add(cursor.value));
    }
    assertEquals(ImmutableSet.of(a, b, 42, d), indexesFromIterable);
  }
}