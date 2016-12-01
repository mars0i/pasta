notes toward a more functional-style use of MASON
====

Note it's possible to use defrecord with protocols if needed.  This
allows a defrecord to implement a MASON interface.  cf.  SIM.clj in
intermittran, in which the deftype Indiv implements MASON's Oriented2D
interface.  You can do something similar with defrecord.  
I'd rather keep the agents free of explicit MASON stuff, but it doesn't
actually hurt anything (other than code modularity and library inclusion)
to do this.

The real display stuff is all Protrayal stuff.  In intermittran,
I was able to put all of that in SimUI.clj (but made a decision
to put that one Oriented2D interface into Sim.clj).
