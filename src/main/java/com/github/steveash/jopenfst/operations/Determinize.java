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


package com.github.steveash.jopenfst.operations;

import com.github.steveash.jopenfst.*;
import com.github.steveash.jopenfst.semiring.GallicSemiring;
import com.github.steveash.jopenfst.semiring.GallicSemiring.GallicMode;
import com.github.steveash.jopenfst.semiring.GallicSemiring.GallicWeight;
import com.github.steveash.jopenfst.semiring.GenericSemiring;
import com.github.steveash.jopenfst.semiring.Semiring;
import com.github.steveash.jopenfst.semiring.UnionSemiring;
import com.github.steveash.jopenfst.semiring.UnionSemiring.MergeStrategy;
import com.github.steveash.jopenfst.semiring.UnionSemiring.UnionMode;
import com.github.steveash.jopenfst.semiring.UnionSemiring.UnionWeight;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static com.github.steveash.jopenfst.semiring.GallicSemiring.SHORTLEX_ORDERING;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Determize operation for FSAs and FSTs. The result will be an equivalent fst that has the property that no state
 * has two transitions with the same input label (i.e. the output will be a deterministic FST)
 * <p>
 * Determinize can only emit a deterministic FST when the input is _functional_ meaning that there is only one
 * output path for each input path. If this isn't true in the input FST, then the `mode` parameter controls
 * what behavior Determinize does when it encounters an issue.
 * <ol>
 * <li>If FUNCTIONAL (default) then determinize will throw if it encounters nonfunctional input</li>
 * <li>If NON_FUNCTIONAL then determinize will keep both paths (but that means the output wont be deterministic</li>
 * <li>If DISAMBIGUATE then determinize will keep the min arc of the options and output will still be deterministic</li>
 * </ol>
 * </p>
 * <p>
 * Not all FSTs are determinizable: all acyclic FSA/FSTs are, all unweighted FSAs are.
 * </p>
 * This implementation differs from OpenFST in a number of ways:
 * <ul>
 * <li>computes the result eagerly (not delayed)</li>
 * <li>does not offer ability to increment ending epsilon input ids (see OpenFST subsequential_label settings)</li>
 * <li>does not offer a quantization delta for finding 'matching' (fuzzy-)weights</li>
 * </ul>
 * See: M. Mohri, "Finite-State Transducers in Language and Speech Processing", Computational Linguistics, 23:2, 1997.
 */
public class Determinize {

  public enum DeterminizeMode {

    // (default) this means that the input FST is functional (only one distinct output path for each
    // input path) and thus Determinize should throw if it realizes that the input is in fact non-functional
    FUNCTIONAL,

    // the input FST may not be functional and if it isn't then preserve all paths, even though that means that the
    // output of Determinize won't be deterministic for these paths (but it will for all other paths)
    NON_FUNCTIONAL,

    // if the input is non-functional just keep the min path (defined by the semiring); in this case the output
    // will be deterministic
    DISAMBIGUATE
  }

  private final int outputEps = 0; // output EPS is supposed to be EPS but could be configurable in the future
  private final DeterminizeMode mode;
  private final GallicMode gallicMode;

  // runtime state
  private Semiring semiring;
  private Fst inputFst;
  private UnionSemiring<GallicWeight, GallicSemiring> unionSemiring;
  private GallicSemiring gallicSemiring;

  // the output fst that is being built
  private MutableFst outputFst;
  // this maps the outputFst's states to the 'tuple' that represents the union of the set of union states (and
  // residuals that make up the output state)
  private BiMap<Integer, DetStateTuple> outputStateIdToTuple;

  /**
   * Determinizes an FSA or FST. For this algorithm, epsilon transitions are treated as regular symbols. This
   * defaults to FUNCTIONAL mode. If you want to run with a different mode then construct an instance of
   * Determinize and pass whatever configuration you like
   *
   * @param fst the fst to determinize; not modified (obviously since it is declared as FST)
   * @return a determinized FST
   */
  public static MutableFst apply(Fst fst) {
    return new Determinize().compute(fst);
  }

  public Determinize() {
    this(DeterminizeMode.FUNCTIONAL);
  }

  public Determinize(DeterminizeMode mode) {
    this.mode = mode;
    this.gallicMode = (mode == DeterminizeMode.DISAMBIGUATE ?
      GallicMode.MIN_GALLIC :
      GallicMode.RESTRICT_GALLIC);
  }

  /**
   * Determinizes an FSA or FST. For this algorithm, epsilon transitions are treated as regular symbols.
   *
   * @param fst the fst to determinize
   * @return the determinized fst
   */
  public MutableFst compute(final Fst fst) {
    fst.throwIfInvalid();

    // init for this run of compute
    this.semiring = fst.getSemiring();
    this.gallicSemiring = new GallicSemiring(this.semiring, this.gallicMode);
    this.unionSemiring = makeUnionRing(semiring, gallicSemiring, mode);
    this.inputFst = fst;
    this.outputFst = MutableFst.emptyWithCopyOfSymbols(fst);

    this.outputStateIdToTuple = HashBiMap.create();
    // workQueue holds the pending work of determinizing the input fst
    Deque<DetStateTuple> workQueue = new LinkedList<>();
    // finalQueue holds the pending work of expanding out the final paths (handled by the FactorFst in the
    // open fst implementation)
    Deque<DetElement> finalQueue = new LinkedList<>();

    // start the algorithm by starting with the input start state
    MutableState initialOutState = outputFst.newStartState();
    DetElement initialElement = new DetElement(fst.getStartState().getId(),
      GallicWeight.createEmptyLabels(semiring.one()));
    DetStateTuple initialTuple = new DetStateTuple(initialElement);
    workQueue.addLast(initialTuple);
    this.outputStateIdToTuple.put(initialOutState.getId(), initialTuple);

    // process all of the input states via the work queue
    while (!workQueue.isEmpty()) {
      DetStateTuple entry = workQueue.removeFirst();
      MutableState outStateForTuple = getOutputStateForStateTuple(entry);

      Collection<DetArcWork> arcWorks = groupByInputLabel(entry);
      for(DetArcWork work : arcWorks) {
    	  this.normalizeArcWork(work);
      }

      for (DetArcWork arcWork : arcWorks) {
        DetStateTuple targetTuple = new DetStateTuple(arcWork.pendingElements);
        if (!this.outputStateIdToTuple.inverse().containsKey(targetTuple)) {
          // we've never seen this tuple before so new state + enqueue the work
          MutableState newOutState = outputFst.newState();
          this.outputStateIdToTuple.put(newOutState.getId(), targetTuple);
          newOutState.setFinalWeight(computeFinalWeight(newOutState.getId(), targetTuple, finalQueue));
          workQueue.addLast(targetTuple);
        }
        MutableState targetOutState = getOutputStateForStateTuple(targetTuple);
        // the computed divisor is a 'legal' arc meaning that it only has zero or one substring; though there
        // might be multiple entries if we're in non_functional mode
        UnionWeight<GallicWeight> unionWeight = arcWork.computedDivisor;
        for (GallicWeight gallicWeight : unionWeight.getWeights()) {
          Preconditions.checkState(gallicSemiring.isNotZero(gallicWeight), "gallic weight zero computed from group by",
            gallicWeight);
          int oLabel = this.outputEps;
          if (!gallicWeight.getLabels().isEmpty()) {
            Preconditions.checkState(gallicWeight.getLabels().size() == 1,
              "cant gave gallic arc weight with more than a single symbol", gallicWeight);
            oLabel = gallicWeight.getLabels().get(0);
          }
          outputFst.addArc(outStateForTuple, arcWork.inputLabel, oLabel, targetOutState, gallicWeight.getWeight());
        }
      }
    }

    // we might've deferred some final state work that needs to be expanded
    expandDeferredFinalStates(finalQueue);
    return outputFst;
  }

  private static UnionSemiring<GallicWeight, GallicSemiring> makeUnionRing(final Semiring semiring,
                                                                           final GallicSemiring gallicSemiring,
                                                                           DeterminizeMode mode) {
    switch (mode) {
      case FUNCTIONAL:
        // here we expect functional input so we dont want union semantics; instead we want to merge everything
        // with gallic plus; in the functional case, gallic plus is gallic restricted so it will throw if
        // the input is non-functional

        // because we already configured the gallic semiring based on determinize mode, the union configuration
        // is the same for functinoal and disambiguate, so fall through
      case DISAMBIGUATE:
        // disambiguate also doesn't want union semantics and we still merge via gallic plus -- but in this case
        // it is min gallic plus so it will just pick the shorter path
		return UnionSemiring.makeForOrdering(gallicSemiring, Ordering.allEqual(), new MergeStrategy<GallicWeight>() {
			@Override
			public GallicWeight merge(GallicWeight a, GallicWeight b) {
				return gallicSemiring.plus(a, b);
			}
		}, UnionMode.RESTRICTED);
        

      case NON_FUNCTIONAL:
        // in this case we do use the real union semantics and want to merge the same label weights
        
        UnionSemiring.MergeStrategy<GallicWeight> doMerge = new UnionSemiring.MergeStrategy<GallicSemiring.GallicWeight>() {
			@Override
			public GallicWeight merge(GallicWeight a, GallicWeight b) {
				Preconditions.checkArgument(a.getLabels().equals(b.getLabels()), "cant merge different labels");
		          return GallicWeight.create(a.getLabels(), semiring.plus(a.getWeight(), b.getWeight()));
			}
		};
        
        return UnionSemiring.makeForOrdering(gallicSemiring, SHORTLEX_ORDERING, doMerge, UnionMode.NORMAL);
      default:
        throw new IllegalArgumentException("unknown mode " + mode);
    }
  }

  // for a particular state tuple (which is equivalent to an output state), group by input labels across all
  // input states in the residset, and just create pending (possibly duplicate) input residuals for the target states
  private Collection<DetArcWork> groupByInputLabel(final DetStateTuple detStateTuple) {
    Map<Integer, DetArcWork> inputLabelToWork = Maps.newHashMap();
    for (DetElement detElement : detStateTuple.getElements()) {
      State inputState = getInputStateForId(detElement.getInputStateId());
      for (Arc inputArc : inputState.getArcs()) {

        UnionWeight<GallicWeight> inputArcAsUnion = UnionWeight.createSingle(
          GallicWeight.createSingleLabel(inputArc.getOlabel(), inputArc.getWeight())
        );
        DetElement pendingElement = new DetElement(inputArc.getNextState().getId(),
          this.unionSemiring.times(detElement.getResidual(), inputArcAsUnion));

        DetArcWork work = inputLabelToWork.get(inputArc.getIlabel());
        if(work == null) {
        	work = new DetArcWork(inputArc.getIlabel(), this.unionSemiring.zero());
        	inputLabelToWork.put(inputArc.getIlabel(), work);
        }
        
        work.pendingElements.add(pendingElement);
      }
    }
    return inputLabelToWork.values();
  }

  // each arcWork may have duplicate states in the pending work (because groupBy doesn't dedup), normalize these
  // and compute new resulting arc weights
  private void normalizeArcWork(final DetArcWork arcWork) {
    Collections.sort(arcWork.pendingElements);
    ArrayList<DetElement> deduped = Lists.newArrayList();
    for (int i = 0; i < arcWork.pendingElements.size(); i++) {
      // first update the running common divisor that we'll use later
      DetElement currentElement = arcWork.pendingElements.get(i);
      arcWork.computedDivisor = unionSemiring.commonDivisor(arcWork.computedDivisor, currentElement.residual);
      // now we want to add and dedup at the same time (this is ok because we sorted)
      if (deduped.isEmpty() || deduped.get(deduped.size() - 1).inputStateId != currentElement.inputStateId) {
        deduped.add(currentElement);
      } else {
        // merge this next one into the existing one
        int lastIndex = deduped.size() - 1;
        DetElement lastElement = deduped.get(lastIndex);
        UnionWeight<GallicWeight> merged = unionSemiring.plus(lastElement.residual, currentElement.residual);
        deduped.set(lastIndex, lastElement.withResidual(merged));
      }
    }
    arcWork.pendingElements = deduped;

    // dividing out the weights with the divisor across all elements
    for (int i = 0; i < arcWork.pendingElements.size(); i++) {
      DetElement currentElement = arcWork.pendingElements.get(i);
      UnionWeight<GallicWeight> divided = unionSemiring.divide(currentElement.residual, arcWork.computedDivisor);
      arcWork.pendingElements.set(i, currentElement.withResidual(divided));
      // we aren't quantizing anything in jopenfst but we would want to quantize here if we add that in the future
    }
  }

  // computes the new final weight for an output state in the determinized FST; to do this
  // we take any relevant residual weights (in the tuple representing the new output weight) and multiply
  // against the original FSTs final weight; then add those up
  // we might end up with a final weight that still has output symbols in the residual, in which case we can't make
  // _this_ new outState a final state, and instead we queue it into a separate queue for later expansion
  private double computeFinalWeight(final int outputStateId,
                                    final DetStateTuple targetTuple,
                                    Deque<DetElement> finalQueue) {
    UnionWeight<GallicWeight> result = this.unionSemiring.zero();
    for (DetElement detElement : targetTuple.getElements()) {
      State inputState = this.getInputStateForId(detElement.inputStateId);
      if (this.semiring.isZero(inputState.getFinalWeight())) {
        continue; // not final so it wont contribute
      }
      UnionWeight<GallicWeight> origFinal = UnionWeight.createSingle(GallicWeight.createEmptyLabels(
        inputState.getFinalWeight()));
      result = this.unionSemiring.plus(result, this.unionSemiring.times(detElement.residual, origFinal));
    }
    if (this.unionSemiring.isZero(result)) {
      return this.semiring.zero();
    }
    if (result.size() == 1 && result.get(0).getLabels().isEmpty()) {
      // by good fortune the residual is just a weight, no path to expand so this new state can have a final weight
      // set now! with nothing to enqueue
      return result.get(0).getWeight();
    }
    // this state can't be a final state because we have more path to emit so defer until later; we know that we cant
    // have any duplicate elements in the finalQueue since we only call computeFinalWeight once for each outputStateId
    finalQueue.addLast(new DetElement(outputStateId, result));
    return this.semiring.zero(); // since we're deferring this weight can't be a final weight
  }

  // we have some paths to expand, which is the case when we've pushed some common divisor labels earlier in the
  // automata and now we just have residual output labels to emit. We emit <eps>:oLabel arcs for each (pushing any
  // residual primitive weight early in the path)
  private void expandDeferredFinalStates(Deque<DetElement> finalQueue) {
    HashBiMap<Integer, GallicWeight> outputStateIdToFinalSuffix = HashBiMap.create();
    while (!finalQueue.isEmpty()) {
      DetElement element = finalQueue.removeFirst();
      for (GallicWeight gallicWeight : element.residual.getWeights()) {
        // factorization is like a simple version of the divisor/divide calculation earlier
        Pair<GallicWeight, GallicWeight> factorized = gallicSemiring.factorize(gallicWeight);
        GallicWeight prefix = factorized.getLeft();
        GallicWeight suffix = factorized.getRight();
        if (!outputStateIdToFinalSuffix.inverse().containsKey(suffix)) {
          // we don't have a synthetic state for this suffix yet
          MutableState newOutputState = outputFst.newState();
          outputStateIdToFinalSuffix.put(newOutputState.getId(), suffix);
          if (suffix.getLabels().isEmpty()) {
            // this suffix is a real final state, and there's no more work to do
            newOutputState.setFinalWeight(suffix.getWeight());
          } else {
            // this suffix still has more labels to emit, so leave final weight as zero and enqueue for expansion
            finalQueue.addLast(new DetElement(newOutputState.getId(), suffix));
          }
        }
        Integer outputStateId = outputStateIdToFinalSuffix.inverse().get(suffix);
        MutableState nextState = checkNotNull(outputFst.getState(outputStateId), "state should exist", outputStateId);
        MutableState thisState = checkNotNull(outputFst.getState(element.inputStateId));
        Preconditions.checkArgument(prefix.getLabels().size() == 1, "prefix size should be 1", prefix);
        int oLabel = prefix.getLabels().get(0);
        // note that openfst has an 'increment subsequent epsilons' feature so that these paths can still be
        // guarenteed to be deterministic (with just multiple definitions of <EPS>; this feature would go here
        // if we decide to implement it in the future
        outputFst.addArc(thisState, this.outputEps, oLabel, nextState, prefix.getWeight());
      }
    }
  }

  private MutableState getOutputStateForStateTuple(final DetStateTuple stateTuple) {
    int outputStateId = checkNotNull(this.outputStateIdToTuple.inverse().get(stateTuple),
      "no output state exists for state tuple", stateTuple);
    return checkNotNull(this.outputFst.getState(outputStateId), "no result state for id", outputStateId);
  }

  private State getInputStateForId(final int inputStateId) {
    return checkNotNull(this.inputFst.getState(inputStateId), "no input state for id", inputStateId);
  }

  /**
   * Holder of work to compute a new determinized arc
   */
  private static class DetArcWork {

    // input label for this particular arc-in-progress
    final int inputLabel;

    // all of the pending elements that are residuals along this path; these will form the tuple that identifies
    // the next output state
    ArrayList<DetElement> pendingElements = Lists.newArrayList();

    // the computed arc weight (note that this is the union/gallic weight which can then be computed into
    // the real FST arc weight
    UnionWeight<GallicWeight> computedDivisor;

    DetArcWork(final int inputLabel, UnionWeight<GallicWeight> zero) {
      this.inputLabel = inputLabel;
      this.computedDivisor = zero;
    }
  }

  /**
   * A set of residuals, which together form the content of one output state in the determinized automata;
   * NOTE DeterminizeStateTuple is the name from openfst
   */
  @VisibleForTesting
  static class DetStateTuple {

    private final LinkedHashSet<DetElement> elements;

    DetStateTuple(DetElement singleElement) {
      this(Sets.newLinkedHashSet(Collections.singletonList(singleElement)));
    }

    DetStateTuple(List<DetElement> elementsAsList) {
      this(Sets.newLinkedHashSet(elementsAsList));
    }

    DetStateTuple(final LinkedHashSet<DetElement> elements) {
      this.elements = elements;
    }

    LinkedHashSet<DetElement> getElements() {
      return elements;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DetStateTuple that = (DetStateTuple) o;
      return Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
      return Objects.hash(elements);
    }

    @Override
    public String toString() {
      return "ResidualSet{" + elements + '}';
    }
  }

  /**
   * Encapsulates the input state + any residual left for that path; the natural ordering is only based on
   * state ids
   */
  @VisibleForTesting
  static class DetElement implements Comparable<DetElement> {

    private final int inputStateId;
    private final UnionWeight<GallicWeight> residual;

    DetElement(final int inputStateId, final GallicWeight singleGallic) {
      this(inputStateId, UnionWeight.createSingle(singleGallic));
    }

    DetElement(final int inputStateId, final UnionWeight<GallicWeight> residual) {
      this.inputStateId = inputStateId;
      this.residual = residual;
    }

    int getInputStateId() {
      return inputStateId;
    }

    UnionWeight<GallicWeight> getResidual() {
      return residual;
    }

    // returns a copy of this DetElement but with the given weight as the residual weight
    DetElement withResidual(UnionWeight<GallicWeight> weight) {
      return new DetElement(this.inputStateId, weight);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DetElement that = (DetElement) o;
      return inputStateId == that.inputStateId &&
        Objects.equals(residual, that.residual);
    }

    @Override
    public int hashCode() {
      return Objects.hash(inputStateId, residual);
    }

    @Override
    public String toString() {
      return "InputResidual{" +
        "inputStateId=" + inputStateId +
        ", residual=" + residual +
        '}';
    }

    @Override
    public int compareTo(final DetElement that) {
      return Integer.compare(this.inputStateId, that.inputStateId);
    }
  }
}
