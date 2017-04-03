Code documentation
====

This directory contains notes on pasta source code.  Some files, such as
perception.pdf, would be of interest to people who just want to
experiment with pasta without looking at the source code.  See the
README.md at the root of this repo for general notes about pasta, how to
run it, etc.

* perception.pdf: Notes on k-snipes' learning algorithm.

* defsim.md: Documentation on the `defsim` macro in `utils.defsim`.
This is used in Sim.clj and generates code whose effects are used
throughout pasta.

* functionalMASON.md: Notes on strategies for writing in a more
functional-programming style using MASON.

* ClojureMASONinteropTips.md: General notes on Clojure-Java interop
relevant to use of Clojure with MASON.  This document came from my
experiments implementing MASON's Students example in Clojure (see my
majure repo).  This document reflected my focus at the time on producing
code that was as fast as possible.  After the majure experiments, I
applied what I'd learned in my intermittran repo.  The resulting code is
not very idiomatic to Clojure, and unpleasant, imo.  My current approach is
to worry more about trying to write (relatively) idiomatic Clojure
rather than trying to eke out as much speed from MASON as possible, but
the interopTips document provides the background for my approach in
pasta, including the `defsim` macro.

* notes/ contains miscellaneous notes.
