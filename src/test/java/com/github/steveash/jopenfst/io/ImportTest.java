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

package com.github.steveash.jopenfst.io;

import com.github.steveash.jopenfst.FstInputOutput;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;


/**
 * @author "John Salatas jsalatas@users.sourceforge.net"
 */
public class ImportTest {

  private static final Logger log = LoggerFactory.getLogger(ImportTest.class);

  @Test
  public void testConvert() throws IOException {

    MutableFst fst1 = Convert.importFst("data/openfst/basic", TropicalSemiring.INSTANCE);

    File tempFile = File.createTempFile("fst", "deleteme");
    tempFile.deleteOnExit();
    fst1.saveModel(tempFile);

    MutableFst fst2 = FstInputOutput.readFstFromBinaryFile(tempFile);
    assertTrue(fst1.equals(fst2));
  }
}
