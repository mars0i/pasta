notes toward a more functional-style use of MASON
====

### defrecord vs deftype

(cf. document about this in the doc dir for intermittran)

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

### Is it necessary to extend SimState?

It's clear you need to extend MASON's GUIState to display stuff via
MASON.  Is it necessary to have a class that extends its SimState?
What does SimState provide?

Note that GUIState does *not* extend SimState.  You must pass a SimState
to GUIState's constructor.  So purely formally, you have to create a
SimState in order to use GUIState.

But what does the SimState give you?  Can you just let it sit there,
unused?

One of the main things SimState provides, if you use it, is the ability
to modify parameters in the simulation from the gui.  So, OK, this is
important enough to use it.  I can set those things from a repl, or from
a commandline, but having them modifiable from the GUI is definitely
useful.

Note that this means that you have to use gen-class's state field, and
use atoms or deftype or something else to allow modification of
parameters.  So you have to have a whole bunch of boilerplate for each
parameter you want accessible through the GUI.

SimState provides step() and start(), but you can use GUIState's
intead, which will call the SimState's scheduler, for example.


