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
package com.github.steveash.jopenfst.semiring;

import com.carrotsearch.hppc.IntArrayList;
import com.github.steveash.jopenfst.semiring.GallicSemiring.GallicWeight;
import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;
import com.google.common.math.DoubleMath;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

import static com.github.steveash.jopenfst.semiring.GallicSemiring.GallicMode.MIN_GALLIC;
import static com.github.steveash.jopenfst.semiring.GallicSemiring.GallicMode.RESTRICT_GALLIC;

/**
 * The gallic semiring is a generic semiring over gallic weights, which are defined as a product type of:
 * a string weight x a primitive weight (and in this case the primitive weight can belong to any primitive semiring)
 * NOTE: gallic times is not commutative (because its concatenation)
 *
 * Gallic weights are useful for representing FSTs _as_ FSAs; we convert the output labels + edge weights into a
 * single Gallic weight and then (within the constraints of the gallic semiring) it is a normal FSA
 */
public class GallicSemiring implements GenericSemiring<GallicWeight> {

  public enum GallicMode {

    // (default) plus only works when string portion is same (like determinize with functional option)
    RESTRICT_GALLIC,

    // plus doesn't actually plus, it just picks the min by weight (like determinize with disambiguate)
    MIN_GALLIC
  }

  /**
   * Shortlex/military ordering over the string portion of the gallic weights. This does NOT consider the
   * primitive weight portion of Gallic weights and thus is not a total, natural ordering (see NATURAL_ORDERING)
   */
  public static final Ordering<GallicWeight> SHORTLEX_ORDERING = new Ordering<GallicWeight>() {
    @Override
    public int compare(final GallicWeight left, final GallicWeight right) {
      IntArrayList s1 = left.labels;
      IntArrayList s2 = right.labels;
      int s1Size = s1.size();
      int s2Size = s2.size();
      if (s1Size < s2Size) return -1;
      if (s1Size > s2Size) return 1;
      for (int i = 0; i < s1Size; i++) {
        int l1 = s1.get(i);
        int l2 = s2.get(i);
        if (l1 < l2) return -1;
        if (l1 > l2) return 1;
      }
      return 0;
    }
  };

  /**
   * Natural ordering is a total ordering over unique Gallic Weights; the ordering is defined as shortlex then
   * primitive weight (to break shortlex ties)
   */
  public static final Ordering<GallicWeight> NATURAL_ORDERING = SHORTLEX_ORDERING.compound(
    Ordering.natural().onResultOf(GallicWeight::getWeight)
  );

  private final GallicWeight zero;
  private final GallicWeight one;
  private final Semiring weightSemiring;
  private final GallicMode mode;

  public GallicSemiring(Semiring weightSemiring) {
    this(weightSemiring, RESTRICT_GALLIC);
  }

  public GallicSemiring(Semiring weightSemiring, GallicMode mode) {
    this.weightSemiring = weightSemiring;
    this.zero = new GallicWeight(weightSemiring.zero()); // only place this constructor can be used
    this.one = GallicWeight.create(GallicWeight.EMPTY, weightSemiring.one());
    this.mode = mode;
  }

  /**
   * gallic plus just delegates to the string semiring plus + the primitive semiring plus
   * NOTE this isn't the Union ('general') Gallic Plus from openfst (i have split this out for sanity at the expense of
   * elegance).
   */
  @Override
  public GallicWeight plus(GallicWeight a, GallicWeight b) {
    if (isZero(a)) return b;
    if (isZero(b)) return a;
    if (mode == RESTRICT_GALLIC) {
      if (SHORTLEX_ORDERING.compare(a, b) != 0) {
        throw new IllegalArgumentException("Trying to plus two different gallic weights, which isn't allowed in " +
          "this context. Did you pass a non-functional FST where a functional one was required? a = " + a +
          "; b = " + b);
      }
      double newWeight = this.weightSemiring.plus(a.getWeight(), b.getWeight());
      return GallicWeight.create(new IntArrayList(a.getLabels()), newWeight);
    } else {
      Preconditions.checkState(mode == MIN_GALLIC);
      return this.weightSemiring.naturalLess(a.getWeight(), b.getWeight()) ? a : b;
    }
  }

  @Override
  public GallicWeight reverse(GallicWeight a) {
    throw new UnsupportedOperationException("gallic reverse isn't implemented");
  }

  /**
   * Gallic times is string subring times (concat) x primitive semiring times
   * @param a first
   * @param b second
   * @return first times second
   */
  @Override
  public GallicWeight times(GallicWeight a, GallicWeight b) {
    if (isZero(a) || isZero(b)) {
      return zero;
    }
    IntArrayList newOutputLabels = new IntArrayList();
    newOutputLabels.addAll(a.getLabels());
    newOutputLabels.addAll(b.getLabels());
    double newWeight = this.weightSemiring.times(a.getWeight(), b.getWeight());
    return GallicWeight.create(newOutputLabels, newWeight);
  }

  /**
   * Left divide for gallic weights (right divide is not implemented yet)
   *
   * @param a first
   * @param b second
   * @return left divide of first by second
   */
  @Override
  public GallicWeight divide(GallicWeight a, GallicWeight b) {
    if (isZero(a)) return zero;
    Preconditions.checkArgument(isNotZero(b), "cant divide by zero");
    IntArrayList newOutputLabels = new IntArrayList();
    for (int i = b.getLabels().size(); i < a.getLabels().size(); i++) {
      newOutputLabels.add(a.getLabels().get(i));
    }
    double newWeight = this.weightSemiring.divide(a.getWeight(), b.getWeight());
    return GallicWeight.create(newOutputLabels, newWeight);
  }

  @Override
  public GallicWeight zero() {
    return zero;
  }

  @Override
  public GallicWeight one() {
    return one;
  }

  @Override
  public boolean isMember(GallicWeight a) {
    return true; // gallic just throws whenever it encounters invalid; it doesn't construct invalid values
  }

  @Override
  public boolean isApproxEqual(GallicWeight a, GallicWeight b) {
    if (isZero(a) && isZero(b)) return true;
    if (isZero(a) || isZero(b)) return false;
    if (!DoubleMath.fuzzyEquals(a.getWeight(), b.getWeight(), Semiring.DEFAULT_APPROX_EQUAL)) {
      return false;
    }
    return a.getLabels().equals(b.getLabels());
  }

  @Override
  public boolean isZero(GallicWeight a) {
    // there is only one zero so reference equality check works
    return a == zero;
  }

  /**
   * In this gallic semiring we restrict the common divisor to only be the first character in the stringweight
   * In more general real-time FSTs that support substrings this could be longer, but openfst doesn't support this
   * and we don't support this
   *
   * @param a
   * @param b
   * @return
   */
  @Override
  public GallicWeight commonDivisor(GallicWeight a, GallicWeight b) {
    double newWeight = this.weightSemiring.plus(a.getWeight(), b.getWeight());
    if (isZero(a)) {
      if (isZero(b)) {
        return zero;
      }
      if (b.getLabels().isEmpty()) {
        return GallicWeight.create(GallicWeight.EMPTY, newWeight);
      }
      // just the first char of b
      return GallicWeight.createSingleLabel(b.getLabels().get(0), newWeight);
    } else if (isZero(b)) {
      if (a.getLabels().isEmpty()) {
        return GallicWeight.create(GallicWeight.EMPTY, newWeight);
      }
      // just the first char of a
      return GallicWeight.createSingleLabel(a.getLabels().get(0), newWeight);
    } else {
      // neither are zero, emit one char if they share it, otherwise empty
      if (a.getLabels().isEmpty() || b.getLabels().isEmpty()) {
        return GallicWeight.create(GallicWeight.EMPTY, newWeight);
      }
      if (a.getLabels().get(0) == b.getLabels().get(0)) {
        return GallicWeight.createSingleLabel(a.getLabels().get(0), newWeight);
      }
      return GallicWeight.create(GallicWeight.EMPTY, newWeight);
    }
  }

  /**
   * Factorize a gallic weight into the (head x weight, rest x One); the contract of factorize is that:
   * val (factor1, factor2) = factorize(weight) implies weight = times(factor1, factor2)
   * (see openfst's GallicFactor)
   * @param weight gallic weight to factorize
   * @return
   */
  public Pair<GallicWeight, GallicWeight> factorize(GallicWeight weight) {
    Preconditions.checkArgument(isNotZero(weight), "cannot factorize a zero weight");
    IntArrayList labels = weight.getLabels();
    if (labels.isEmpty()) {
      return Pair.of(GallicWeight.createEmptyLabels(weight.getWeight()), one());
    }
    if (labels.size() == 1) {
      return Pair.of(GallicWeight.createSingleLabel(labels.get(0), weight.getWeight()), one());
    }
    IntArrayList prefix = new IntArrayList(1);
    IntArrayList suffix = new IntArrayList(labels.size() - 1);
    prefix.add(labels.get(0));
    for (int i = 1; i < labels.size(); i++) {
      suffix.add(labels.get(i));
    }
    return Pair.of(GallicWeight.create(prefix, weight.getWeight()), GallicWeight.create(suffix, weightSemiring.one()));
  }

  /**
   * Gallic weight is s String Weight + a (normal) weight (under the input FST's semiring). One place that we use
   * this is to encapsulate the residual weight + output symbols that we must propagate in the transducer
   * determinize operation
   */
  public static class GallicWeight {

    private static final IntArrayList EMPTY = new IntArrayList(); // treat as immutable

    /**
     * Construct a gallic weight for a specific list of labels
     *
     * @param weight     semiring weight
     * @param labels labels
     */
    public static GallicWeight createFromGiven(final double weight, int... labels) {
      IntArrayList list = new IntArrayList(labels.length);
      list.add(labels);
      return new GallicWeight(list, weight);
    }

    /**
     * Create a gallic weight with an empty label list
     *
     * @param weight
     * @return
     */
    public static GallicWeight createEmptyLabels(final double weight) {
      return new GallicWeight(EMPTY, weight);
    }

    /**
     * Create a gallic weight with only a single label
     *
     * @param singleLabel
     * @param weight
     * @return
     */
    public static GallicWeight createSingleLabel(final int singleLabel, final double weight) {
      IntArrayList labels = new IntArrayList(1);
      labels.add(singleLabel);
      return new GallicWeight(labels, weight);
    }

    /**
     * Create a gallic weight with the given list of output labels and weight
     *
     * @param labels
     * @param weight
     * @return
     */
    public static GallicWeight create(IntArrayList labels, double weight) {
      if (labels.isEmpty()) {
        labels = EMPTY; // help weak gen hypoth for common empty case (& maybe elide alloc after inlining)
      }
      return new GallicWeight(labels, weight);
    }

    private final double weight;
    private final IntArrayList labels; // treat as immutable

    // zero constructor; use semiring's zero method to get the singleton instance
    GallicWeight(double primitiveZero) {
      this.weight = primitiveZero;
      this.labels = null;
    }

    private GallicWeight(final IntArrayList labels, final double weight) {
      this.labels = labels;
      this.weight = weight;
    }

    public double getWeight() {
      return weight;
    }

    /**
     * returns the output label list in this gallic weight; DO NOT MODIFY the returned array. Treat it as
     * immutable
     *
     * @return output label list
     */
    public IntArrayList getLabels() {
      return labels;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      GallicWeight that = (GallicWeight) o;
      return Double.compare(that.weight, weight) == 0 &&
        Objects.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
      return Objects.hash(weight, labels);
    }

    @Override
    public String toString() {
      return "ResidualWeight{" +
        "weight=" + weight +
        ", labels=" + labels +
        '}';
    }
  }
}
