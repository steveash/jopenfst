/*
 * Copyright 2014 Steve Ash
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

import com.google.common.base.Throwables;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import com.github.steveash.jopenfst.semiring.Semiring;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.HashMap;

import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;

/**
 * Handles serialization and deserialization to get it out of the FST class (not its responsibility)
 *
 * @author Steve Ash
 */
public class FstInputOutput {

  /**
   * Deserializes a symbol map from an ObjectInputStream
   *
   * @param in the ObjectInputStream. It should be already be initialized by the caller.
   * @return the deserialized symbol map
   */
  public static MutableSymbolTable readStringMap(ObjectInputStream in)
      throws IOException, ClassNotFoundException {

    int mapSize = in.readInt();
    MutableSymbolTable syms = new MutableSymbolTable();
    for (int i = 0; i < mapSize; i++) {
      String sym = in.readUTF();
      int index = in.readInt();
      syms.put(sym, index);
    }
    return syms;
  }

  /**
   * Deserializes an Fst from an ObjectInputStream
   *
   * @param in the ObjectInputStream. It should be already be initialized by the caller.
   */
  private static MutableFst readFst(ObjectInputStream in) throws IOException,
                                                                 ClassNotFoundException {
    MutableSymbolTable is = readStringMap(in);
    MutableSymbolTable os = readStringMap(in);

    MutableSymbolTable ss = null;
    if (in.readBoolean()) {
      ss = readStringMap(in);
    }
    return readFstWithTables(in, is, os, ss);
  }

  private static MutableFst readFstWithTables(ObjectInputStream in, MutableSymbolTable is, MutableSymbolTable os, MutableSymbolTable ss)
      throws IOException, ClassNotFoundException {
    int startid = in.readInt();
    Semiring semiring = (Semiring) in.readObject();
    int numStates = in.readInt();
    MutableFst res = new MutableFst(new ArrayList<MutableState>(numStates), semiring, is, os);
    if (ss != null) {
      res.useStateSymbols(ss);
    }
    for (int i = 0; i < numStates; i++) {
      int numArcs = in.readInt();
      MutableState s = new MutableState(numArcs);
      float f = in.readFloat();
      if (f == res.getSemiring().zero()) {
        f = semiring.zero();
      } else if (f == semiring.one()) {
        f = semiring.one();
      }
      s.setFinalWeight(f);
      int thisStateId = in.readInt();
      res.setState(thisStateId, s);
    }
    res.setStart(res.getState(startid));

    numStates = res.getStateCount();
    for (int i = 0; i < numStates; i++) {
      MutableState s1 = res.getState(i);
      for (int j = 0; j < s1.initialNumArcs; j++) {
        MutableArc a = new MutableArc();
        a.setIlabel(in.readInt());
        a.setOlabel(in.readInt());
        a.setWeight(in.readFloat());
        a.setNextState(res.getState(in.readInt()));
        s1.addArc(a);
      }
    }

    return res;
  }

  /**
   * Deserializes an Fst from disk
   *
   * @param file the binary model filename
   */
  public static MutableFst loadModel(File file) {
    return loadModelFromSource(Files.asByteSource(file));
  }

  public static MutableFst loadModel(String resourceName) {
    ByteSource bs = asByteSource(getResource(resourceName));
    return loadModelFromSource(bs);
  }

  private static MutableFst loadModelFromSource(ByteSource bs) {
    try (ObjectInputStream ois = new ConvertingObjectInputStream(bs.openBufferedStream())) {
      return readFst(ois);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Serializes a symbol map to an ObjectOutputStream
   *
   * @param out the ObjectOutputStream. It should be already be initialized by the caller.
   * @param map the symbol map to serialize
   */
  private static void writeStringMap(ObjectOutputStream out, SymbolTable map)
      throws IOException {
    out.writeInt(map.size());
    for (ObjectIntCursor<String> cursor : map) {
      out.writeUTF(cursor.key);
      out.writeInt(cursor.value);
    }
  }

  /**
   * Serializes the current Fst instance to an ObjectOutputStream
   *
   * @param out the ObjectOutputStream. It should be already be initialized by the caller.
   */
  private static void writeFst(MutableFst fst, ObjectOutputStream out) throws IOException {
    writeStringMap(out, fst.getInputSymbols());
    writeStringMap(out, fst.getOutputSymbols());
    out.writeBoolean(fst.isUsingStateSymbols()); // whether or not we used a state symbol table
    if (fst.isUsingStateSymbols()) {
      writeStringMap(out, fst.getStateSymbols());
    }
    out.writeInt(fst.getStartState().getId());

    out.writeObject(fst.getSemiring());
    out.writeInt(fst.getStateCount());

    HashMap<State, Integer> stateMap = new HashMap<>(fst.getStateCount());
    for (int i = 0; i < fst.getStateCount(); i++) {
      State s = fst.getState(i);
      out.writeInt(s.getNumArcs());
      out.writeFloat(s.getFinalWeight());
      out.writeInt(s.getId());
      stateMap.put(s, i);
    }

    int numStates = fst.getStateCount();
    for (int i = 0; i < numStates; i++) {
      State s = fst.getState(i);
      int numArcs = s.getNumArcs();
      for (int j = 0; j < numArcs; j++) {
        Arc a = s.getArc(j);
        out.writeInt(a.getIlabel());
        out.writeInt(a.getOlabel());
        out.writeFloat(a.getWeight());
        out.writeInt(stateMap.get(a.getNextState()));
      }
    }
  }

  public static void saveModel(MutableFst fst, File file) throws IOException {
    ByteSink bs = Files.asByteSink(file);
    try (ObjectOutputStream oos = new ObjectOutputStream(bs.openBufferedStream())) {
      writeFst(fst, oos);
    }
  }

  // shim while i was still using John's serialized "expected" values to test the algorithms
  private static class ConvertingObjectInputStream extends ObjectInputStream {

    private static final String EDU_CMU_SPHINX_FST = "edu.cmu.sphinx.fst";

    public ConvertingObjectInputStream(InputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      if (desc.getName().startsWith(EDU_CMU_SPHINX_FST)) {
        String newClassName = "com.github.steveash.jopenfst" + desc.getName().substring(EDU_CMU_SPHINX_FST.length());
        return Class.forName(newClassName, false, Thread.currentThread().getContextClassLoader());
      }
      return super.resolveClass(desc);
    }
  }
}
