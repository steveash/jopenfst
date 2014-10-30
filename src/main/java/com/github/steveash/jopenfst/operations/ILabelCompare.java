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

package com.github.steveash.jopenfst.operations;

import com.github.steveash.jopenfst.Arc;

import java.util.Comparator;

/**
 * Comparator used in {@link ArcSort} for sorting based on input labels
 *
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ILabelCompare implements Comparator<Arc> {

  /*
   * (non-Javadoc)
   *
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(Arc o1, Arc o2) {
    if (o1 == null) {
      return 1;
    }
    if (o2 == null) {
      return -1;
    }

    return (o1.getIlabel() < o2.getIlabel()) ? -1 : ((o1.getIlabel() == o2
        .getIlabel()) ? 0 : 1);
  }

}
