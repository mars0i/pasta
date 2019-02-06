This branch adds type hints in popenv.clj to avoid reflection.

Note that the master that it's forked from already has recent type hints
in defsim on simData (but not newval--that's in another branc), and has
some new type hints on code in Sim.clj, and does not have type hints on
the parser functions in the defsim parameters.
