/*
 * FstUtilsTest.java
 *
 * Copyright (c) 2017 Amazon.com, Inc. or its affiliates. All rights reserved.
 *
 * PROPRIETARY/CONFIDENTIAL
 *
 * Use is subject to license terms.
 */
package com.github.steveash.jopenfst.utils;

import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.io.Convert;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertTrue;

public class FstUtilsTest {

  @Test
  public void shouldExecuteLogReporter() throws Exception {
    // just a smoke test that the log reporting infrastructure doesn't explode
    MutableFst read = Convert.importFst("data/openfst/cyclic", TropicalSemiring.INSTANCE);
    MutableFst read2 = Convert.importFst("data/openfst/basic", TropicalSemiring.INSTANCE);
    FstUtils.fstEquals(read, read2, FstUtils.LOG_REPORTER);
  }

  @Test
  public void shouldGiveBlankReportWhenEqual() throws Exception {
    MutableFst read = Convert.importFst("data/openfst/cyclic", TropicalSemiring.INSTANCE);
    MutableFst read2 = Convert.importFst("data/openfst/cyclic", TropicalSemiring.INSTANCE);

    StringReporter reporter = new StringReporter();
    FstUtils.fstEquals(read, read2, reporter);
    String report = reporter.toString();
    assertTrue(StringUtils.isBlank(report));
  }

  @Test
  public void shouldGiveNonBlankReportWhenNotEqual() throws Exception {
    MutableFst read = Convert.importFst("data/openfst/cyclic", TropicalSemiring.INSTANCE);
    MutableFst read2 = Convert.importFst("data/openfst/basic", TropicalSemiring.INSTANCE);

    StringReporter reporter = new StringReporter();
    FstUtils.fstEquals(read, read2, reporter);
    String report = reporter.toString();
    assertTrue(StringUtils.isNotBlank(report));
  }

  private static class StringReporter implements FstUtils.EqualsReporter {

    private final StringBuilder buffer = new StringBuilder();

    @Override
    public String toString() {
      return buffer.toString();
    }

    @Override
    public void report(String msg, Object a, Object b) {
      String aa = Objects.toString(a, "<null>");
      String bb = Objects.toString(b, "<null>");
      buffer.append("Equals difference: ")
          .append(msg)
          .append(" ")
          .append(aa)
          .append(" ")
          .append(bb)
          .append("\n");
    }
  }
}