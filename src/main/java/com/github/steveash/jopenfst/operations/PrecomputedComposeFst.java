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

package com.github.steveash.jopenfst.operations;

import com.github.steveash.jopenfst.ImmutableFst;
import com.github.steveash.jopenfst.semiring.Semiring;

/**
 * If you have a large FST that you are frequently using in compose operations, you can precompute it to apply the
 * necessary mutating transformations that are used (for epsilon handling for example) to avoid expensive runtime
 * copies
 * @author Steve Ash
 */
public class PrecomputedComposeFst {

  private final Semiring semiring;
  private final String eps1;
  private final String eps2;
  private final ImmutableFst precomputed;

  PrecomputedComposeFst(String eps1, String eps2, ImmutableFst precomputed, Semiring semiring) {
    this.eps1 = eps1;
    this.eps2 = eps2;
    this.precomputed = precomputed;
    this.semiring = semiring;
  }

  ImmutableFst getFst() {
    return precomputed;
  }

  String getEps1() {
    return eps1;
  }

  String getEps2() {
    return eps2;
  }

  Semiring getSemiring() {
    return semiring;
  }
}
