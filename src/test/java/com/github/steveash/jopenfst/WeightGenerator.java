/*
 * Copyright 2018 Steve Ash
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.github.steveash.jopenfst;

import com.carrotsearch.hppc.IntArrayList;
import com.github.steveash.jopenfst.semiring.GallicSemiring;
import com.github.steveash.jopenfst.semiring.UnionSemiring.UnionWeight;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import java.util.Random;
import java.util.Set;

/**
 * Utility for testing weights that can generate weights for testing
 * @param <W> weight type to generate; e.g. Double
 */
public abstract class WeightGenerator<W> {

  public static WeightGenerator<Double> makeDouble(long seed) {
    return new WeightGenerator<Double>(seed) {
      @Override
      public Double generate() {
        return rand.nextDouble();
      }
    };
  }

  public static WeightGenerator<GallicSemiring.GallicWeight> makeGallic(long seed) {
    return new WeightGenerator<GallicSemiring.GallicWeight>(seed) {
      IntArrayList elems;
      @Override
      public GallicSemiring.GallicWeight generate() {
        if (isReset) {
          // right now since we're only doing determinize, we only do a single or zero chars
          int elementCount = rand.nextBoolean() ? 1 : 0;
          IntArrayList elems = new IntArrayList(elementCount);
          for (int i = 0; i < elementCount; i++) {
            elems.add(rand.nextInt(10) + 1);
          }
          this.elems = elems;
          this.isReset = false;
        }
        return GallicSemiring.GallicWeight.create(elems, rand.nextDouble());
      }
    };
  }

  public static WeightGenerator<UnionWeight<Double>> makeUnion(int seed, final boolean onlySingles) {
    return new WeightGenerator<UnionWeight<Double>>(seed) {
      @Override
      public UnionWeight<Double> generate() {
        int elemCount = (onlySingles ? 1 : rand.nextInt(4) + 1);
        Set<Double> elems = Sets.newHashSet();
        for (int i = 0; i < elemCount; i++) {
          elems.add(rand.nextDouble());
        }
        return UnionWeight.createFromList(Ordering.natural().sortedCopy(elems));
      }
    };
  }

  protected final Random rand;
  protected boolean isReset = true;

  public WeightGenerator(long seed) {
    this.rand = new Random(seed);
  }

  abstract public W generate();

  public void reset() {
    isReset = true;
  }
}
