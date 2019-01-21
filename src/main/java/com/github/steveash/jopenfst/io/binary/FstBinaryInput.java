package com.github.steveash.jopenfst.io.binary;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableSymbolTable;

/**
 * Support for reading binary files produced by OpenFST
 * 
 * @author Josh Hansen
 *
 */
public final class FstBinaryInput {

	private FstBinaryInput() {
		throw new UnsupportedOperationException("Utility class should not be instantiated");
	}

	private static final Map<String,Class<? extends FstBodyReader>> readers = new HashMap<>();
	static {
		readers.put(VectorFstReader.TYPE, VectorFstReader.class.asSubclass(FstBodyReader.class));
	}

//	public static long bytesRead = 0L;


	static int readI32(InputStream in) throws IOException {
//		bytesRead += 4L;

		final byte[] arr = new byte[4];
		in.read(arr);

		final ByteBuffer bb = ByteBuffer.wrap(arr);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		return bb.getInt();
	}


	private static final BigInteger TWO_TO_64 = BigInteger.valueOf(0L);
	static {
		TWO_TO_64.flipBit(64);
	}
	
	static BigInteger readU64(InputStream in) throws IOException {
		final long l = readI64(in);

		final BigInteger messedUpU64 = BigInteger.valueOf(l);

		BigInteger u64;
		if(messedUpU64.testBit(64)) {// a large u64 is erroneously treated as a negative number
			u64 = messedUpU64.add(TWO_TO_64);
		} else {
			u64 = messedUpU64;
		}

		return u64;

	}

	static long readI64(InputStream in) throws IOException {
//		bytesRead += 8L;

		final byte[] arr = new byte[8];
		in.read(arr);

		final ByteBuffer bb = ByteBuffer.wrap(arr);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		return bb.getLong();
	}

	static String readString(InputStream in) throws IOException {


		final int length = readI32(in);

//		bytesRead += length;

		final byte[] arr = new byte[length];
		in.read(arr);

		return new String(arr);
	}

	public static float readF32(InputStream in) throws IOException {
//		bytesRead += 4L;

		final byte[] arr = new byte[4];
		in.read(arr);

		final ByteBuffer bb = ByteBuffer.wrap(arr);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		return bb.getFloat();
	}

	public static double readF64(InputStream in) throws IOException {
//		bytesRead += 8L;

		final byte[] arr = new byte[8];
		in.read(arr);

		final ByteBuffer bb = ByteBuffer.wrap(arr);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		return bb.getDouble();
	}

	/**
	 * Read an OpenFST binary file and instantiate it as a MutableFst
	 * 
	 * @param in An InputStream representing the .fst binary file
	 * 
	 * @return The MutableFst representation of the file read.
	 * 
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static MutableFst readOpenfstBinaryFile(final InputStream in) throws IOException, InstantiationException, IllegalAccessException {
		// BufferedInputStream gives us mark/reset support, useful for pre-reading the header
		//		  final InputStream in = new BufferedInputStream(fstIn, FstHeader.HEADER_MAX_BYTES);

		final FstHeader header = FstHeader.read(in);

		final FstBodyReader reader = readers.get(header.type).newInstance();
		if(reader == null) {
			throw new IllegalArgumentException(".fst binary file of type '" + header.type + "' is not supported");
		}

		return reader.read(in, header);
	}


	private static final int SYMBOL_TABLE_MAGIC = 2125658996;
	public static MutableSymbolTable readSymbolTable(InputStream in) throws IOException {

		final int magic = readI32(in);

		if(magic != SYMBOL_TABLE_MAGIC) {
			throw new IllegalArgumentException("Attempted to read symbol table, but got magic of " + magic +" where " + SYMBOL_TABLE_MAGIC + " was expected");
		}

		final MutableSymbolTable syms = new MutableSymbolTable();

		@SuppressWarnings("unused")
		final String name = readString(in);

		@SuppressWarnings("unused")
		final long availableKey = readI64(in);

		final long size = readI64(in);


//		System.out.println("Reading symbol table " + name + " " + availableKey + " " + size);

		String symbol;
		long key;

		for(long i = 0; i < size; i++) {

			symbol = readString(in);
			key = readI64(in);

			syms.put(symbol, (int)key);//FIXME unchecked downcast

			System.out.println(symbol + " -> " + key);
		}

		return syms;
	}



}
