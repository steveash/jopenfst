package com.github.steveash.jopenfst.io.binary;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

/**
 * Representation of the header of an OpenFST binary file, with facilities for reading.
 * 
 * @author Josh Hansen
 *
 */
public class FstHeader {
	
	private static final int OPENFST_MAGIC_NUMBER = 2125659606;
	
	private static final int FLAG_INPUT_SYMBOLS = 0x1;
	private static final int FLAG_OUTPUT_SYMBOLS = 0x2;
	
	public final String type;// est 20 bytes
	public final String arcType;// est 20 bytes
	public final int version;// 4 bytes
	public final int flags;// 4 bytes
	public final BigInteger properties;// 8 bytes
	public final long start;// 8 bytes
	public final long numStates;// 8 bytes
	public final long numArcs;// 8 bytes
	
	
	public FstHeader(String type, String arcType, int version, int flags, BigInteger properties, long start,
			long numStates, long numArcs) {
		this.type = type;
		this.arcType = arcType;
		this.version = version;
		this.flags = flags;
		this.properties = properties;
		this.start = start;
		this.numStates = numStates;
		this.numArcs = numArcs;
	}

	public static FstHeader read(InputStream in) throws IOException {
		  
		  final int magic = FstBinaryInput.readI32(in);// 4 bytes
		  
		  if(magic != OPENFST_MAGIC_NUMBER) {
			  throw new IllegalArgumentException("Provided stream is not an OpenFST binary file");
		  }
		  
		  final String type = FstBinaryInput.readString(in);// est 20 bytes
		  final String arcType = FstBinaryInput.readString(in);// est 20 bytes
		  final int version = FstBinaryInput.readI32(in);// 4 bytes
		  final int flags = FstBinaryInput.readI32(in);// 4 bytes
		  final BigInteger properties = FstBinaryInput.readU64(in);// 8 bytes
		  final long start = FstBinaryInput.readI64(in);// 8 bytes
		  final long numStates = FstBinaryInput.readI64(in);// 8 bytes
		  final long numArcs = FstBinaryInput.readI64(in);// 8 bytes
		  
		  
		  return new FstHeader(
				  
				  type,
				  arcType,
				  version,
				  flags,
				  properties,
				  start,
				  numStates,
				  numArcs
				  
		  );
	}
	
	public boolean hasInputSymbols() {
		return (flags & FLAG_INPUT_SYMBOLS) > 0;
	}
	
	public boolean hasOutputSymbols() {
		return (flags & FLAG_OUTPUT_SYMBOLS) > 0;
	}
}
