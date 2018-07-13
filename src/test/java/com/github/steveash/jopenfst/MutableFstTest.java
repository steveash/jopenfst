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

import com.github.steveash.jopenfst.semiring.TropicalSemiring;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

  @Test
  public void shouldCreateWithStateSymbols() throws Exception {
    MutableFst fst = createStateSymbolFst();

    assertEquals(5, fst.getStateCount());
    assertEquals(1, fst.getState(0).getArcCount()); // start
    assertEquals(2, fst.getState(1).getArcCount()); // _B
    assertEquals(1, fst.getState(2).getArcCount()); // _A
    assertEquals(0, fst.getState(3).getArcCount()); // _C
    assertEquals(0, fst.getState(4).getArcCount()); // _D

    assertEquals(4, fst.getInputSymbolCount());
    assertEquals(4, fst.getOutputSymbolCount());

    MutableState stateA = fst.getState(2);
    assertEquals("_A", fst.getStateSymbols().invert().keyForId(stateA.getId()));
    assertEquals(1, stateA.getArcCount());
    MutableArc arc = stateA.getArc(0);
    assertEquals(fst.lookupInputSymbol("b"), arc.getIlabel());
    assertEquals(fst.lookupOutputSymbol("B"), arc.getOlabel());
    assertEquals(1.5, arc.getWeight(), 0.1);
    assertEquals(fst.getState("_B").getId(), arc.getNextState().getId());
    assertTrue(arc.hashCode() != 0);
    assertTrue(StringUtils.isNotBlank(arc.toString()));
  }

  @Test
  public void shouldReadAndWriteStateSymbolFst() throws Exception {
    MutableFst fst = createStateSymbolFst();
    File tempFile = File.createTempFile("fst-states", "deleteme");
    tempFile.deleteOnExit();
    fst.saveModel(tempFile);

    MutableFst fst2 = FstInputOutput.readFstFromBinaryFile(tempFile);
    assertEquals(fst, fst2);
    assertTrue(isNotBlank(fst2.toString()));
  }

  @Test
  public void shouldCopyWithTranslatedSymbols() throws Exception {
    MutableFst fst = new MutableFst(TropicalSemiring.INSTANCE);
    MutableState s0 = fst.newStartState();
    MutableState s1 = fst.newState();
    MutableState s2 = fst.newState();
    fst.getInputSymbols().getOrAdd("a");
    fst.getInputSymbols().getOrAdd("b");
    fst.getInputSymbols().getOrAdd("c");
    fst.getInputSymbols().getOrAdd("d");
    fst.getOutputSymbols().getOrAdd("A");
    fst.getOutputSymbols().getOrAdd("B");
    fst.getOutputSymbols().getOrAdd("C");
    fst.getOutputSymbols().getOrAdd("D");
    fst.addArc(s0, "a", "A", s1, 0.0);
    fst.addArc(s1, "b", "B", s2, 0.0);
    fst.addArc(s0, "c", "C", s2, 0.0);
    fst.addArc(s2, "d", "D", s2, 0.0);

    MutableSymbolTable newIn = new MutableSymbolTable();
    newIn.put("a", 101);
    newIn.put("b", 102);
    newIn.put("c", 103);
    newIn.put("d", 104);
    MutableSymbolTable newOut = new MutableSymbolTable();
    newOut.put("A", 201);
    newOut.put("B", 202);
    newOut.put("C", 203);
    newOut.put("D", 204);

    MutableFst result = MutableFst.copyAndTranslateSymbols(fst, FrozenSymbolTable.freeze(newIn),
        FrozenSymbolTable.freeze(newOut));

    MutableState rs0 = result.getState(0);
    MutableState rs1 = result.getState(1);
    MutableState rs2 = result.getState(2);
    assertEquals(101, rs0.getArc(0).getIlabel());
    assertEquals(201, rs0.getArc(0).getOlabel());
    assertEquals(103, rs0.getArc(1).getIlabel());
    assertEquals(203, rs0.getArc(1).getOlabel());

    assertEquals(102, rs1.getArc(0).getIlabel());
    assertEquals(202, rs1.getArc(0).getOlabel());

    assertEquals(104, rs2.getArc(0).getIlabel());
    assertEquals(204, rs2.getArc(0).getOlabel());
  }

  private MutableFst createStateSymbolFst() {
    MutableFst fst = new MutableFst(TropicalSemiring.INSTANCE);
    fst.useStateSymbols();
    MutableState startState = fst.newStartState("<start>");

    // creating a few symbols by hand, others will get created automatically
    fst.newState("_B");
    int inputA = fst.getInputSymbols().getOrAdd("a");

    fst.addArc("<start>", "a", "A", "_A", 1.0);
    fst.addArc("_A", "b", "B", "_B", 1.5);
    fst.addArc("_B", "c", "C", "_C", 1.0);
    fst.addArc("_B", "d", "D", "_D", 5.0);
    fst.getState("_C").setFinalWeight(9.0);
    fst.getState("_D").setFinalWeight(3.0); // going through D is actually cheaper
    return fst;
  }
}