jopenfst
========

Java port of the C++ [OpenFST library](http://www.openfst.org/twiki/bin/view/FST/WebHome); originally forked from the CMU Sphinx project.  This was originally work by John 
Salatas as part of his GSOC 2012 project to port phonetisaurus over to java.  Since then the code appears to be 
abandoned and doesn't appeared to have been integrated in to the final CMU Sphinx project trunk.  I need a decent 
WFST library for some of my stuff, so I'm going to use his code as a starting point.  There are some rough edges 
in the original code that I intend to clean up -- so consider this *alpha quality* right now.  When I am comfortable
with the code I will push it to Maven Central Repo.

Resources:
------------

* [John Salatas' blog](http://jsalatas.ictpro.gr/tag/java-fst/) has some posts that describe some of his initial design 
decisions.  I imagine that as I work on this these blog posts will become less representative of jopenFST but for the 
moment its pretty close
* [C++ OpenFST library](http://www.openfst.org/twiki/bin/view/FST/WebHome) describes some of the FST algorithms implemented.

Changes:
------------

* Updated some IO routines, used exceptions instead of System.err logging, some cleanup, fixed unit tests
* Changed packages (although it can still deserialize FST models from the original repo)
* Ported over code from the GSOC branch which apparently never made it in to the CMU Sphinx trunk

