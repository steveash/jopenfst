package com.github.steveash.jopenfst.io.binary;

import java.io.IOException;
import java.io.InputStream;

import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;

/**
 * An FstBodyReader for OpenFST binary files with type "vector".
 * 
 * @author Josh Hansen
 *
 */
public class VectorFstReader implements FstBodyReader {
	
	public static final String TYPE = "vector";
	private static final String STANDARD_ARC_TYPE = "standard";

	private static final int NO_STATE = -1;
	private static final int MIN_VERSION = 2;
	
	@Override
	public MutableFst read(InputStream in, FstHeader header) throws IOException {
		
		if(!header.type.equals(TYPE)) {
			throw new IllegalArgumentException("Attempted to read an FST of type " + TYPE + " but was passed a header with type " + header.type);
		}
		
		if(!header.arcType.equals(STANDARD_ARC_TYPE)) {
			throw new IllegalArgumentException("Attempted to read an FST with arc type " + header.arcType + " but only " + STANDARD_ARC_TYPE + " is supported");
		}
		
		if(header.version < MIN_VERSION) {
			throw new IllegalArgumentException("Reading an FST of type '" + TYPE + "' requires minimum version of " + MIN_VERSION + " but version " + header.version + " was found");
		}
		
		/*
		 * FIXME downcast necessitated by MutableFst(int) constructor
		 *       and ultimately Java array addressing via ArrayList
		 *       
		 *       This can apparently be done in a checked fashion in Java 8, fwiw
		 *       https://stackoverflow.com/questions/1590831/safely-casting-long-to-int-in-java
		 */
		MutableFst fst;
		if(header.numStates == NO_STATE) {
			fst = new MutableFst();
		} else {
			fst = new MutableFst((int)header.numStates);
		}
		
		
		
		if(header.hasInputSymbols()) {
			fst.setInputSymbolsAsCopy(FstBinaryInput.readSymbolTable(in));
		}
		
		if(header.hasOutputSymbols()) {
			fst.setOutputSymbolsAsCopy(FstBinaryInput.readSymbolTable(in));
		}
		
		final MutableState startState = new MutableState();
		fst.addState(startState);
		fst.setStart(startState);
		
		int stateIdx;
		
		// Pre-instantiate states
		for(stateIdx = 0; header.numStates == NO_STATE || stateIdx < header.numStates; stateIdx++) {
			fst.addState(new MutableState());
		}
		
		
		for(stateIdx = 0; header.numStates == NO_STATE || stateIdx < header.numStates; stateIdx++) {
			final MutableState state = fst.getState(stateIdx);
			
			float weight = FstBinaryInput.readF32(in);//???
			
			state.setFinalWeight(weight);
			
			final long arcCount = FstBinaryInput.readI64(in);
			
//			System.out.println("State " + stateIdx + " weight " + weight + " arc count " + arcCount);
			
			for(long arcIdx = 0; arcIdx < arcCount; arcIdx++) {
				
				//NOTE These types correspond to the StdArc in OpenFST. Could be generalized later.
				
				final int inputLabelIdx = FstBinaryInput.readI32(in);
				final int outputLabelIdx = FstBinaryInput.readI32(in);
				final float arcWeight = FstBinaryInput.readF32(in);
				final int nextStateIdx = FstBinaryInput.readI32(in);
				
				final MutableState nextState = fst.getState(nextStateIdx);
				fst.addArc(state, inputLabelIdx, outputLabelIdx, nextState, arcWeight);
				
			}
			
		}
		
		if(header.numStates != NO_STATE && stateIdx != header.numStates) {
			throw new IllegalArgumentException("Expected " + header.numStates +" states in input; found " + stateIdx);
		}
		
		
		return fst;
		
	}

}
