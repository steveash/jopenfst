
JOpenFST
========

A _partial_ Java port of the C++ [OpenFST library](http://www.openfst.org/twiki/bin/view/FST/WebHome), which provides 
a library to build weighted finite state transducers (WFSTs) and perform various common FST/FSA tasks such as:

* Determinization (for both acceptors and transducers)
* Union / Intersection 
* Composition
* Shortest Path computation

OpenFST is a mature, elegant, and feature rich library in C++. JOpenFST aims to implement many of the features of 
OpenFST in a pure Java implementation, which can be useful if you are trying to use WFST operations within an existing
pure Java architecture or service. Some environments are not easily suited to using JNI or creating a separate C++ 
service endpoint to do all of the WFST operations. In these circumstances, JOpenFST provides an alternative.

OpenFST is designed quite elegantly and relies on sophisticated C++ template metaprogramming features to achieve 
top speed (and nice generality). Since Java offers no rich metaprogramming facility, JOpenFST differs significantly
in its API and implementation. JOpenFST is probably more fairly described as a re-imagining of OpenFST within the 
constraints and idioms of Java/JVM. Because of that, JOpenFST lacks some features that are present in OpenFST, but 
hopefully that gap will close over time (PRs welcome!).

Here are some of the most notable differences from OpenFST:
* All WFST operations are eagerly executed, there is no deferred/lazy evaluation. There are however some optimizations
  when doing operations on Immutable instances (see Compose) to avoid unnecessary copying.
* JOpenFST can only import/export using the OpenFST/AT&T text format (as produced by `fstprint` and consumed by 
  `fstcompile`); JOpenFST cannot currently import OpenFST binary models (as produced by `fstcompile`).
* There are mutable and immutable types that mirror each other (MutableFst, ImmutableFst, MutableState, ImmutableState, etc.)
* There is a Gallic weight and semiring but not a separate String semiring.
* The Gallic Weights are either Gallic Restricted or Gallic Min; if you want General Gallic weights, you have to use the
  Union Semiring directly.
* The following operations are implemented:
    * ArcSort
    * Compose
    * Connect
    * Determinize (for both acceptors and transducers; all modes: functional, non-functional, and disambiguate)
    * Shortest Paths
    * Project
    * Remove Epsilon
    * Reverse
* The following operations are currently NOT implemented (PRs welcome):
    * Minimization (coming soon)
    * TopSort (coming soon)
    * Closure
    * Concat/Union 
    * Encode/Decode
    * Difference/Intersect
    * Invert
    * Prune (as a separate operation)
    * Push
    * Synchronize
    
This project was originally work in the CMU Sphinx project by John Salatas as part of his GSOC 2012 project, but since
then it has been mostly rewritten to bring in new enhancements, improve the API, and improve performance. 
 
Current version:
```xml
<dependency>
    <groupId>com.github.steveash.jopenfst</groupId>
    <artifactId>jopenfst</artifactId>
    <version>0.3.0</version>
</dependency>
```

Quick Start
-----------
The API started out pretty close to OpenFST but has diverged over time. The basic abstractions of `Fst`, `State`, `Arc`,
and `SymbolTable` have conceptual analogs in OpenFST. In JOpenFST there are *Mutable* and *Immutable* implementations
of each. As you programmatically build up your WFSTs, you will use the Mutable API.  If you want to de/serialize larger
models (large WFSTs built from training data that are used to construct lattices) and these models don't need to 
change, then you can convert the mutable instance into an immutable instance after you are done building it 
(`new ImmutableFst(myMutableFst)`. ImmutableFsts are likely faster at some operations and also are smarter about 
reducing unnecessary copying of state.

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
jOpenFst supports reading/writing the OpenFst text format and our own JOpenFST binary serialization format 
(more compact than text). We cannot currently read/write OpenFSTs binary serialization format.

To read OpenFst text format, you need a `mymodel.fst.txt` file that describes all of the arcs and weights. If you 
are using labeled states, inputs, or outputs (e.g. for a transducer) then you also need files for those 
named `mymodel.input.syms`, `mymodel.output.syms`, and `mymodel.states.syms` respectively. An exmaple of these files 
is in the `src/test/resources/data/openfst` folder in the source.

To read/write the text format call methods `Convert.importFst(..)` and `Convert.export(..)`. Both of these return 
instances of `MutableFst` which can be converted into `ImmutableFst` via `new ImmutableFst(myMutableFst)`.   
There are importFst overloads for dealing with either Files or resources from the classpath.

To read/write the binary format call methods `FstInputOutput.readFstFromBinaryFile` and 
`FstInputOutput.writeFstToBinaryFile` (there are overloads for dealing with streams/resources.  
Resources are useful if you want to package your serialized model in your jar and just read it from the classpath.

Resources
---------

* [John Salatas' blog](http://jsalatas.ictpro.gr/tag/java-fst/) has some posts that describe some of his initial design 
decisions.  The library has diverged pretty significantly from this original version, but this is still a reference.
* [C++ OpenFST library](http://www.openfst.org/twiki/bin/view/FST/WebHome) describes some of the FST algorithms implemented.

Release History
------------
* 0.3.0 - Adding union semiring, gallic semiring, and implemented Determinization for transducers with multiple modes
(functional, nonfunctional, and disambiguate) to match the behavior and options available in OpenFST.
* 0.2.0 - code covering unit tests, improvements to interoperability of the text format between JOpenFST and OpenFST.



