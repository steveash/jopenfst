/*
 * FrozenSymbolTableTest.java
 *
 * Copyright (c) 2017 Amazon.com, Inc. or its affiliates. All rights reserved.
 *
 * PROPRIETARY/CONFIDENTIAL
 *
 * Use is subject to license terms.
 */
package com.github.steveash.jopenfst;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FrozenSymbolTableTest {

  @Test
  public void shouldGetExisting() throws Exception {
    MutableSymbolTable table = new MutableSymbolTable();
    int a = table.getOrAdd("A");
    FrozenSymbolTable frozen = new FrozenSymbolTable(table);
    assertEquals(a, frozen.get("A"));
    assertEquals(a, frozen.getOrAdd("A"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnAdd() throws Exception {
    MutableSymbolTable table = new MutableSymbolTable();
    int a = table.getOrAdd("A");
    FrozenSymbolTable frozen = new FrozenSymbolTable(table);
    assertEquals(a, frozen.get("A"));
    frozen.getOrAdd("B");
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnPut() throws Exception {
    MutableSymbolTable table = new MutableSymbolTable();
    int a = table.getOrAdd("A");
    FrozenSymbolTable frozen = new FrozenSymbolTable(table);
    assertEquals(a, frozen.get("A"));
    frozen.put("B", 42);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFreezeViaFactoryMethod() throws Exception {
    MutableSymbolTable table = new MutableSymbolTable();
    int a = table.getOrAdd("A");
    FrozenSymbolTable frozen = FrozenSymbolTable.freeze(table);
    assertEquals(a, frozen.get("A"));
    assertEquals(a, frozen.getOrAdd("NOTTHERE"));
  }
}