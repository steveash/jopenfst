package com.github.steveash.jopenfst.io.binary;

import java.io.IOException;
import java.io.InputStream;

import com.github.steveash.jopenfst.MutableFst;

/**
 * Interface to support reading various implementations Fst in OpenFST.
 * 
 * @author Josh Hansen
 *
 */
public interface FstBodyReader {
	
	/**
	 * Read an FST from an InputStream according to the OpenFST binary format, as per a particular implementation.
	 * 
	 * @param in The InputStream representing the .fst file. The stream should be advanced to the byte
	 *           immediately following the header.
	 *           
	 * @param header The header of the .fst file, previously read elsewhere.
	 * 
	 * @return A MutableFst instantiating the FST defined by the input
	 */
	MutableFst read(InputStream in, FstHeader header) throws IOException;
}
