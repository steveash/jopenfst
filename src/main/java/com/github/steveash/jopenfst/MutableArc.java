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

import com.github.steveash.jopenfst.utils.FstUtils;

/**
 * The fst's arc implementation.
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class MutableArc implements Arc {

  // Arc's weight
  private float weight;

  // input label
  private int iLabel;

  // output label
  private int oLabel;

  // next state's id
  private MutableState nextState;

  /**
   * Default Constructor
   */
  public MutableArc() {
  }

  /**
   * Arc Constructor
   *
   * @param iLabel    the input label's id
   * @param oLabel    the output label's id
   * @param weight    the arc's weight
   * @param nextState the arc's next state
   */
  public MutableArc(int iLabel, int oLabel, float weight, MutableState nextState) {
    this.weight = weight;
    this.iLabel = iLabel;
    this.oLabel = oLabel;
    this.nextState = nextState;
  }

  /**
   * Get the arc's weight
   */
  @Override
  public float getWeight() {
    return weight;
  }

  /**
   * Set the arc's weight
   */
  public void setWeight(float weight) {
    this.weight = weight;
  }

  /**
   * Get the input label's id
   */
  @Override
  public int getIlabel() {
    return iLabel;
  }

  /**
   * Set the input label's id
   *
   * @param iLabel the input label's id to set
   */
  public void setIlabel(int iLabel) {
    this.iLabel = iLabel;
  }

  /**
   * Get the output label's id
   */
  @Override
  public int getOlabel() {
    return oLabel;
  }

  /**
   * Set the output label's id
   *
   * @param oLabel the output label's id to set
   */
  public void setOlabel(int oLabel) {
    this.oLabel = oLabel;
  }

  /**
   * Get the next state
   */
  @Override
  public MutableState getNextState() {
    return nextState;
  }

  /**
   * Set the next state
   *
   * @param nextState the next state to set
   */
  public void setNextState(MutableState nextState) {
    this.nextState = nextState;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return FstUtils.arcEquals(this, obj);
  }

  @Override
  public int hashCode() {
    int result = iLabel;
    result = 31 * result + oLabel;
    result = 31 * result + (weight != +0.0f ? Float.floatToIntBits(weight) : 0);
    result = 31 * result + nextState.getId();
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "(" + iLabel + ", " + oLabel + ", " + weight + ", " + nextState
           + ")";
  }
}
