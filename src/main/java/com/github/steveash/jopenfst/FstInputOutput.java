package com.github.steveash.jopenfst;

import com.google.common.base.Throwables;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import com.github.steveash.jopenfst.semiring.Semiring;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
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
  public static String[] readStringMap(ObjectInputStream in)
      throws IOException, ClassNotFoundException {

    int mapSize = in.readInt();
    String[] map = new String[mapSize];
    for (int i = 0; i < mapSize; i++) {
      String sym = (String) in.readObject();
      map[i] = sym;
    }

    return map;
  }

  /**
   * Deserializes an Fst from an ObjectInputStream
   *
   * @param in the ObjectInputStream. It should be already be initialized by the caller.
   */
  private static Fst readFst(ObjectInputStream in) throws IOException,
                                                          ClassNotFoundException {
    String[] is = readStringMap(in);
    String[] os = readStringMap(in);
    int startid = in.readInt();
    Semiring semiring = (Semiring) in.readObject();
    int numStates = in.readInt();
    Fst res = new Fst(numStates);
    res.isyms = is;
    res.osyms = os;
    res.semiring = semiring;
    for (int i = 0; i < numStates; i++) {
      int numArcs = in.readInt();
      State s = new State(numArcs + 1);
      float f = in.readFloat();
      if (f == res.semiring.zero()) {
        f = res.semiring.zero();
      } else if (f == res.semiring.one()) {
        f = res.semiring.one();
      }
      s.setFinalWeight(f);
      s.id = in.readInt();
      res.states.add(s);
    }
    res.setStart(res.states.get(startid));

    numStates = res.getNumStates();
    for (int i = 0; i < numStates; i++) {
      State s1 = res.getState(i);
      for (int j = 0; j < s1.initialNumArcs - 1; j++) {
        Arc a = new Arc();
        a.setIlabel(in.readInt());
        a.setOlabel(in.readInt());
        a.setWeight(in.readFloat());
        a.setNextState(res.states.get(in.readInt()));
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
  public static Fst loadModel(File file) {
    return loadModelFromSource(Files.asByteSource(file));
  }

  public static Fst loadModel(String resourceName) {
    ByteSource bs = asByteSource(getResource(resourceName));
    return loadModelFromSource(bs);
  }

  private static Fst loadModelFromSource(ByteSource bs) {
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
  private static void writeStringMap(ObjectOutputStream out, String[] map)
      throws IOException {
    out.writeInt(map.length);
    for (int i = 0; i < map.length; i++) {
      out.writeObject(map[i]);
    }
  }

  /**
   * Serializes the current Fst instance to an ObjectOutputStream
   *
   * @param out the ObjectOutputStream. It should be already be initialized by the caller.
   */
  private static void writeFst(Fst fst, ObjectOutputStream out) throws IOException {
    writeStringMap(out, fst.isyms);
    writeStringMap(out, fst.osyms);
    out.writeInt(fst.states.indexOf(fst.start));

    out.writeObject(fst.semiring);
    out.writeInt(fst.states.size());

    HashMap<State, Integer> stateMap = new HashMap<>(
        fst.states.size(), 1.f);
    for (int i = 0; i < fst.states.size(); i++) {
      State s = fst.states.get(i);
      out.writeInt(s.getNumArcs());
      out.writeFloat(s.getFinalWeight());
      out.writeInt(s.getId());
      stateMap.put(s, i);
    }

    int numStates = fst.states.size();
    for (int i = 0; i < numStates; i++) {
      State s = fst.states.get(i);
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

  public static void saveModel(Fst fst, File file) throws IOException {
    ByteSink bs = Files.asByteSink(file);
    try (ObjectOutputStream oos = new ObjectOutputStream(bs.openBufferedStream())) {
      writeFst(fst, oos);
    }
  }

  /**
   * Deserializes an ImmutableFst from an ObjectInputStream
   *
   * @param in the ObjectInputStream. It should be already be initialized by the caller.
   */
  private static ImmutableFst readImmutableFst(ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    String[] is = readStringMap(in);
    String[] os = readStringMap(in);
    int startid = in.readInt();
    Semiring semiring = (Semiring) in.readObject();
    int numStates = in.readInt();
    ImmutableFst res = new ImmutableFst(numStates);
    res.isyms = is;
    res.osyms = os;
    res.semiring = semiring;
    for (int i = 0; i < numStates; i++) {
      int numArcs = in.readInt();
      ImmutableState s = new ImmutableState(numArcs + 1);
      float f = in.readFloat();
      if (f == res.semiring.zero()) {
        f = res.semiring.zero();
      } else if (f == res.semiring.one()) {
        f = res.semiring.one();
      }
      s.setFinalWeight(f);
      s.id = in.readInt();
      res.states[s.getId()] = s;
    }
    res.setStart(res.states[startid]);

    numStates = res.states.length;
    for (int i = 0; i < numStates; i++) {
      ImmutableState s1 = res.states[i];
      for (int j = 0; j < s1.initialNumArcs - 1; j++) {
        Arc a = new Arc();
        a.setIlabel(in.readInt());
        a.setOlabel(in.readInt());
        a.setWeight(in.readFloat());
        a.setNextState(res.states[in.readInt()]);
        s1.setArc(j, a);
      }
    }

    return res;
  }

  /**
   * Deserializes an ImmutableFst from disk
   *
   * @param filename the binary model filename
   */
  public static ImmutableFst loadImmutableModel(String filename) {
    ByteSource bs = asByteSource(getResource(filename));
    try (ObjectInputStream ois = new ConvertingObjectInputStream(bs.openBufferedStream())) {
      return readImmutableFst(ois);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

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
