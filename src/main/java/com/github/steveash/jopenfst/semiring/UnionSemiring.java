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

import com.github.steveash.jopenfst.semiring.UnionSemiring.UnionWeight;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import javax.annotation.CheckReturnValue;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The union semiring is a generic semiring over the union weights.
 * The union weight is the special weight that presents a set of weights as a single weights (from an underlying
 * generic semiring).
 * The union semiring generalizes the underlying generic ring's Times and One operations (but they are semantically
 * the same as the underlying one)
 * <p>
 * Union semiring Plus is replaced with set union
 * Union semiring Zero is replaced with the empty set; and thus it accumulate sequences instead of merge/plus-ing
 * them together.
 * <p>
 * There are also two strategies that must be specified for how to define order and equality in the set as well
 * as how to merge two elements that are deemed to be equivalent by the ordering. A natural ordering
 * is built in which uses a merge strategy of returning the 'left' (arbitrary) element being merged
 *
 * @param <W> the type of the weight contained in a unionweight
 * @param <S> the type of the semiring that governs the contained W weight type
 */
public class UnionSemiring<W, S extends GenericSemiring<W>> extends GenericSemiring<UnionWeight<W>> {

  public enum UnionMode {
    // (default) this is the typical semantics of union described in the class doc
    NORMAL,

    // this mode causes operations to fail if the union is actually used for more than 1 element; use this
    // when you need a union semiring but don't actually want any of the semantics (as is the case in
    // Determinize in FUNCTIONAL mode)
    RESTRICTED;
  }

  /**
   * Creates a union semiring for the given semiring (that has naturally comparable weights where the default
   * merge strategy will work just fine)
   *
   * @param weightSemiring underlying weight semiring, this will use the natural ordering and the default merge
   * @param <W>            the underlying weight type
   * @param <S>            the underlying semiring type for W
   * @return
   */
  public static <W extends Comparable<W>, S extends GenericSemiring<W>> UnionSemiring<W, S> makeForNaturalOrdering(
    S weightSemiring) {
	  
	final Ordering<W> ordering = Ordering.natural();
	final MergeStrategy<W> merge = defaultMerge();
	  
    return makeForOrdering(weightSemiring, ordering, merge);//FIXME
  }

  /**
   * Creates a new union semirng for the given underlying weight semiring and ordering
   *
   * @param weightSemiring underlying weight semiring
   * @param ordering       the ordering to govern elements in the union set; note that when ordering indicates that two
   *                       elements are equal, that is when the merge strategy will be called to merge them
   * @param merge          the way two elements should be combined when the ordering strategy indicates they are equal
   * @param <W>            the underlying weight type
   * @param <S>            the underlying semiring type for W
   * @return
   */
  public static <W, S extends GenericSemiring<W>> UnionSemiring<W, S> makeForOrdering(S weightSemiring,
                                                                                      Ordering<W> ordering,
                                                                                      MergeStrategy<W> merge) {
    return makeForOrdering(weightSemiring, ordering, merge, UnionMode.NORMAL);
  }

  /**
   * Creates a new union semiring for the given underlying weight semiring and ordering
   *
   * @param weightSemiring underlying weight semiring
   * @param ordering       the ordering to govern elements in the union set; note that when ordering indicates that two
   *                       elements are equal, that is when the merge strategy will be called to merge them
   * @param merge          the way two elements should be combined when the ordering strategy indicates they are equal
   * @param mode           the mode of this union semiring indicating how it should behave
   * @param <W>            the underlying weight type
   * @param <S>            the underlying semiring type for W
   * @return
   */
  public static <W, S extends GenericSemiring<W>> UnionSemiring<W, S> makeForOrdering(S weightSemiring,
                                                                                      Ordering<? super W> ordering,
                                                                                      MergeStrategy<W> merge,
                                                                                      UnionMode mode) {
    return new UnionSemiring<>(weightSemiring, ordering, merge, mode);
  }

  private static final UnionWeight<?> ZERO = new UnionWeight<>();

  private final S weightSemiring;
  private final UnionWeight<W> one;
  private final MergeStrategy<W> mergeStrategy;
  private final Ordering<? super W> ordering;
  private final UnionMode mode;

  private UnionSemiring(S weightSemiring,
                        Ordering<? super W> ordering,
                        MergeStrategy<W> mergeStrategy,
                        UnionMode mode) {
    this.weightSemiring = weightSemiring;
    this.ordering = ordering;
    this.one = UnionWeight.createSingle(weightSemiring.one());
    this.mergeStrategy = mergeStrategy;
    this.mode = mode;
  }

  private UnionWeight<W> makeCheckedFrom(List<W> list) {
    if (mode == UnionMode.RESTRICTED) {
      if (list.size() > 1) {
        throw new IllegalStateException("cannot create union weight with more than one element in RESTRICTED " +
          "mode; did you pass in a non-functional FST where a functional one is required?" + list);
      }
    }
    return UnionWeight.createFromList(list);
  }

  /**
   * A shortcut version of Plus that can be used when you know that you are inserting things in sorted order and you
   * just want to add a (maybe new, maybe dup) element into the union set
   * (This is analogous to openfst's union weight PushBack)
   *
   * @param weight   existing weight
   * @param toAppend you know that this is either the new end or should be merged into the existing end
   * @return appended weight
   */
  @CheckReturnValue
  public UnionWeight<W> withAppended(final UnionWeight<W> weight, final W toAppend) {
    if (isZero(weight) || weight.getWeights().isEmpty()) {
      return UnionWeight.createSingle(toAppend);
    }
    ImmutableList<W> weights = weight.getWeights();
    ImmutableList.Builder<W> builder = ImmutableList.builder();
    builder.addAll(weights.subList(0, weights.size() - 1));
    W last = weights.get(weights.size() - 1);
    int compare = ordering.compare(last, toAppend);
    Preconditions.checkState(compare <= 0, "cant call withAppend with a weight out of order", weight, toAppend);
    if (compare == 0) {
      builder.add(mergeStrategy.merge(last, toAppend));
    } else {
      // compare == -1, meaning that toAppend is greater than current end
      builder.add(last); // append existing last to builder
      builder.add(toAppend);
    }
    return makeCheckedFrom(builder.build());
  }

  /**
   * Union plus which adds the path to the current set of weights (this is the 'union' part of Union semiring)
   *
   * @param a weight W
   * @param b weight W
   * @return
   */
  @Override
  public UnionWeight<W> plus(UnionWeight<W> a, UnionWeight<W> b) {
    if (isZero(a)) return b;
    if (isZero(b)) return a;

    ImmutableList.Builder<W> builder = ImmutableList.builder();
    int cursorA = 0;
    int cursorB = 0;

    while (cursorA < a.size() && cursorB < b.size()) {
      W nextA = a.get(cursorA);
      W nextB = b.get(cursorB);

      int compare = ordering.compare(nextA, nextB);
      if (compare < 0) {
        builder.add(nextA);
        cursorA += 1;
      } else if (compare > 0) {
        builder.add(nextB);
        cursorB += 1;
      } else {
        // compare == 0, they are equal, then merge; because a and b are legal UnionWeights already we know they
        // don't have any duplicates so we can increment both
        builder.add(mergeStrategy.merge(nextA, nextB));
        cursorA += 1;
        cursorB += 1;
      }
    }
    for (; cursorA < a.size(); cursorA++) {
      builder.add(a.get(cursorA));
    }
    for (; cursorB < b.size(); cursorB++) {
      builder.add(b.get(cursorB));
    }
    return makeCheckedFrom(builder.build());
  }

  @Override
  public UnionWeight<W> reverse(UnionWeight<W> a) {
    throw new UnsupportedOperationException("reverse isnt implemented on union semiring");
  }

  /**
   * Times multiplies all of A by all of B; it is commutative if the underlying semiring Times is commutative
   * (which is not the case if the underlying is Gallic)
   * @param a
   * @param b
   * @return
   */
  @Override
  public UnionWeight<W> times(UnionWeight<W> a, UnionWeight<W> b) {
    if (isZero(a) || isZero(b)) {
      return zero();
    }
    UnionWeight<W> result = zero();
    for (int i = 0; i < a.size(); i++) {
      UnionWeight<W> row = zero(); // empty set
      for (int j = 0; j < b.size(); j++) {
        row = withAppended(row, this.weightSemiring.times(a.get(i), b.get(j)));
      }
      result = this.plus(result, row);
    }
    return result;
  }

  @Override
  public UnionWeight<W> divide(UnionWeight<W> a, UnionWeight<W> b) {
    if (isZero(a) || isZero(b)) return zero();
    UnionWeight<W> row = zero();
    if (a.size() == 1) {
      // divide uses the reverse iterator in union-weight.h, which i think is becasue divide (definitionally) will
      // flip the order (by the same logic that multiple always increases monotonically, divide decreases montonically),
      // so we need to insert in reverse order to ensure that the withAppended keeps things sorted properly
      for (int i = b.size() - 1; i >= 0; i--) {
        row = withAppended(row, this.weightSemiring.divide(a.get(0), b.get(i)));
      }
    } else if (b.size() == 1) {
      for (int i = 0; i < a.size(); i++) {
        row = withAppended(row, this.weightSemiring.divide(a.get(i), b.get(0)));
      }
    } else {
      throw new IllegalArgumentException("cannot divide in the union semiring without a one element divisor; a = " +
        a + "; b = " + b);
    }
    return row;
  }

  @Override
  public UnionWeight<W> zero() {
    @SuppressWarnings("unchecked") // there's no W's to cast in a zero instance
      UnionWeight<W> zero = (UnionWeight<W>) ZERO;
    return zero;
  }

  @Override
  public UnionWeight<W> one() {
    return one;
  }

  @Override
  public boolean isApproxEqual(UnionWeight<W> a, UnionWeight<W> b) {
    if (isZero(a) && isZero(b)) return true;
    if (isZero(a) || isZero(b)) return false;
    if (a.size() != b.size()) return false;
    for (int i = 0; i < a.size(); i++) {
      if (!this.weightSemiring.isApproxEqual(a.get(i), b.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isZero(UnionWeight<W> a) {
    return a == ZERO;
  }

  @Override
  public UnionWeight<W> commonDivisor(UnionWeight<W> a, UnionWeight<W> b) {
    W result = this.weightSemiring.zero();
    if (isNotZero(a)) {
      for (W weight : a.getWeights()) {
        result = this.weightSemiring.commonDivisor(result, weight);
      }
    }
    if (isNotZero(b)) {
      for (W weight : b.getWeights()) {
        result = this.weightSemiring.commonDivisor(result, weight);
      }
    }
    if (this.weightSemiring.isZero(result)) {
      return zero();
    }
    return UnionWeight.createSingle(result);
  }

  @Override
  public boolean isMember(UnionWeight a) {
    return true; // union just throws; it doesn't propagate invalid values
  }

  @FunctionalInterface
  public interface MergeStrategy<W> {

    /**
     * Merge the two elements `a` and `b` into a single element; this is applied when the two elements have
     * been deemed to be the same element (by the ordering)
     *
     * @param a
     * @param b
     * @return
     */
    W merge(W a, W b);
  }

  /**
   * Default merge just picks one arbitrarily (but it always picks the left one)
   */
  private static final MergeStrategy<?> DEFAULT_MERGE = new MergeStrategy<Object>() {
		@Override
		public Object merge(Object a, Object b) {
			return a;
		}
  };

  public static <W> MergeStrategy<W> defaultMerge() {
    @SuppressWarnings("unchecked")
    MergeStrategy<W> strategy = (MergeStrategy<W>) DEFAULT_MERGE;
    return strategy;
  }

  /**
   * The union weight is the special weight that presents a set of weights as a 'set' of weights
   * You can define these as governed by a union semiring where the Times and One are (mostly) the direct Times and
   * One element from the underlying weight and the Plus is changed to be the union operation and the Zero element
   * is made the empty set.
   */
  public static class UnionWeight<W> {

    public static <W> UnionWeight<W> createFromList(final List<W> weights) {
      return new UnionWeight<>(weights);
    }

    public static <W> UnionWeight<W> createSingle(W singleWeight) {
      return new UnionWeight<W>(ImmutableList.of(singleWeight));
    }

    public static <W> UnionWeight<W> createFromGiven(W... elements) {
      ImmutableList.Builder<W> builder = ImmutableList.builder();
      builder.addAll(Arrays.asList(elements));
      return new UnionWeight<W>(builder.build());
    }

    private final ImmutableList<W> weights;

    private UnionWeight(final List<W> weights) {
      this.weights = ImmutableList.copyOf(weights);
    }

    private UnionWeight() {
      // special constructor for the zero instance
      this.weights = null;
    }

    public ImmutableList<W> getWeights() {
      return weights;
    }

    public int size() {
      return weights.size();
    }

    public W get(int index) {
      return weights.get(index);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UnionWeight that = (UnionWeight) o;
      return Objects.equals(weights, that.weights);
    }

    @Override
    public int hashCode() {
      return Objects.hash(weights);
    }

    @Override
    public String toString() {
      return "UnionWeight{" +
        "weights=" + weights +
        '}';
    }
  }
}
