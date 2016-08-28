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

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ash
 */
public class MutableFstTest {

  @Test
  public void shouldCompactNulls1() throws Exception {
    ArrayList<Integer> listGood = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9);
    ArrayList<Integer> listBad = Lists.newArrayList(null, 1, 2, null, 3, 4, null, 5, 6, null, 7, 8, 9, null);
    MutableFst.compactNulls(listBad);
    assertEquals(listGood, listBad);
  }

  @Test
  public void shouldCompactNulls2() throws Exception {
    ArrayList<Integer> listBad = Lists.newArrayList(1);
    MutableFst.compactNulls(listBad);
    assertEquals(Lists.newArrayList(1), listBad);
  }

  @Test
  public void shouldCompactNulls3() throws Exception {
    ArrayList<Integer> listBad = Lists.newArrayList(null, 1);
    MutableFst.compactNulls(listBad);
    assertEquals(Lists.newArrayList(1), listBad);
  }

  @Test
  public void shouldCompactNulls4() throws Exception {
    ArrayList<Integer> listBad = Lists.newArrayList(1, null);
    MutableFst.compactNulls(listBad);
    assertEquals(Lists.newArrayList(1), listBad);
  }

  @Test
  public void shouldCompactNulls5() throws Exception {
    ArrayList<Integer> listBad = Lists.newArrayList(1, 2, 3, 4, null, 5);
    MutableFst.compactNulls(listBad);
    assertEquals(Lists.newArrayList(1, 2, 3, 4, 5), listBad);
  }
}