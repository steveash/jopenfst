/*
 * IndexPairTest.java
 *
 * Copyright (c) 2017 Amazon.com, Inc. or its affiliates. All rights reserved.
 *
 * PROPRIETARY/CONFIDENTIAL
 *
 * Use is subject to license terms.
 */
package com.github.steveash.jopenfst;

import org.junit.Test;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IndexPairTest {

  @Test
  public void shouldTupleEquals() throws Exception {
    IndexPair w1 = new IndexPair(42, 5);
    IndexPair w2 = new IndexPair(42, 5);
    IndexPair w3 = new IndexPair(42, 355);
    assertEquals(w1, w2);
    assertEquals(w1.hashCode(), w2.hashCode());
    assertFalse(w1.equals(w3));
    assertFalse(w1.hashCode() == w3.hashCode());

    assertTrue(w2.equals(w1));
    assertTrue(isNotBlank(w1.toString()));
  }
}