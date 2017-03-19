jopenfst
========

A partial Java port of the C++ [OpenFST library](http://www.openfst.org/twiki/bin/view/FST/WebHome) which provides a library to
 build weighted finite state transducers and perform various common FST tasks such as:

* Determinization
* Union / Intersection 
* Composition
* Shortest Path computation
 
This project was originally forked from the CMU Sphinx project.  This was originally work by John 
Salatas as part of his GSOC 2012 project to port phonetisaurus over to java.  Since then the code appears to be 
abandoned and doesn't appear to have been integrated in to the final CMU Sphinx project trunk.  I needed a decent 
WFST library for some of my stuff, so I used his code as a starting point. I have cleaned up quite a bit
 of the code, really changed the APIs, and updated unit tests. My JG2P project uses this, and thus I have
 some confidence that the code is working accurately. However, I would still consider this *beta quality*. When I am comfortable with the stability, I will push a v1.0 to Maven Central Repo.

Quick Start
-----------
The API started out pretty close to OpenFST but is diverging over time. The basic abstractions of `Fst`, `State`, `Arc`,
and `SymbolTable` have conceptual analogs in OpenFST. In jopenfst there are *Mutable* and *Immutable* implementations
of each. As you programmatically build up your WFSTs, you will use the Mutable API.  If you want to de/serialize larger
models (large WFSTs built from training data that are used to construct lattices) and these models don't need to change, then you can convert the mutable instance into an immutable instance after you are done building it (`new ImmutableFst(myMutableFst)`.
ImmutableFsts are likely faster at some operations and also are smarter about reducing unnecessary copying of state.

The MutableFst API is probably the bast place to start. Here is a sample showing how to
construct a WFST which shows the basic operations of fsts, states, arcs, and symbols.

```java
MutableFst fst = new MutableFst(TropicalSemiring.INSTANCE);
// by default states are only identified by indexes assigned by the FST, if you want to instead
// identify your states with symbols (and read/write a state symbol table) then call this before
// adding any states to the FST
fst.useStateSymbols();
MutableState startState = fst.newStartState("<start>");
// setting a final weight makes this state an eligible final state
fst.newState("</s>").setFinalWeight(0.0);

// you can add symbols manually to the symbol table
int symbolId = fst.getInputSymbols().getOrAdd("<eps>");
fst.getOutputSymbols().getOrAdd("<eps>");

// add arcs on the MutableState instances directly or using convenience methods on the fst instance
// if using state labels you can pass the labels (if they dont exist, new states will be created)
// params are inputSatate, inputLabel, outputLabel, outputState, arcWeight
fst.addArc("state1", "inA", "outA", "state2", 1.0);

// alternatively (or if no state symbols) you can use the state instances
fst.addArc(startState, "inC", "outD", fst.getOrNewState("state3"), 123.0);

```
Input and Output
----------------
jOpenFst supports reading/writing the OpenFst text format and our own jopenfst binary serialization format (more compact than text). We cannot currently read/write openfsts binary serialization format, though pull requests with that functionality are very welcome.

To read OpenFst text format, you need a `mymodel.fst.txt` file that describes all of the arcs and weights. If you are using labeled states, inputs, or outputs (e.g. for a transducer) then you also need files for those named `mymodel.input.syms`, `mymodel.output.syms`, and `mymodel.states.syms` respectively. An exmaple of these files is in the `src/test/resources/data/openfst` folder in the source.

To read/write the text format call methods `Convert.importFst(..)` and `Convert.export(..)`. Both of these return instances of `MutableFst` which can be converted into `ImmutableFst` via `new ImmutableFst(myMutableFst)`.   There are importFst overloads for dealing with either Files or resources from the classpath.

To read/write the binary format call methods `FstInputOutput.readFstFromBinaryFile` and `FstInputOutput.writeFstToBinaryFile` (there are overloads for dealing with streams/resources.  Resources are useful if you want to package your serialized model in your jar and just read it from the classpath.

Resources
---------

* [John Salatas' blog](http://jsalatas.ictpro.gr/tag/java-fst/) has some posts that describe some of his initial design 
decisions.  I imagine that as I work on this these blog posts will become less representative of jopenFST but for the 
moment its pretty close
* [C++ OpenFST library](http://www.openfst.org/twiki/bin/view/FST/WebHome) describes some of the FST algorithms implemented.

Changes:
------------

* Adding back edges (kind of) to dramatically optimize a number of the original implementations that had poor algorithmic complexity
** In my jg2p project this reduced runtime for my datasets by a factor of 30x
* The original Connect/Trim implementation was wrong; fixed now.
* Separated out interfaces for read-only/writeable elements (Arcs, States, Fsts) which allows
convenient things like "union" symbol tables (to do mutating things without copying the entire source symbl table)
* Clearly separated out algorithms by ones that mutate input args vs ones that produce new fsts
* Updated some IO routines, used exceptions instead of System.err logging, some cleanup, fixed unit tests
* Changed packages (although it can still deserialize FST models from the original repo)



