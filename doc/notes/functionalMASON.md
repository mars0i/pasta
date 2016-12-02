Notes toward functional-style MASON
====

## defrecord vs. deftype

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

## Is it necessary to run the scheduler?

Yes, it appears so.  Even if the model itself isn't controlled by the
scheduler--if say, you're just `take`int from a lazy sequence of model
states--it's the scheduler that makes the UI window repaint.  cf. page
24 of the v.19 manual.  (You make this happen by calling
Display2D.reset().)  


## But could I step the scheduler myself?

i.e. rather than the MASON scheduler driving Clojure code, let Clojure
drive the MASON UI.

Well there's a step() function in Schedule.  Note though it will only do
anything if there are things registered on the schedule.  So I would
have to add things to the schedule before calling step(), or maybe
just leave them on there but repeat.

But how do you pause it?  This is clearly possible, since you can do
this in the GUI.  But I think that step() is exposed mainly so people
can override it in a subclass.  

(Note though that step() in Steppable is something completely
different.)

In the MASON source, look at the second def of `pressPause() in
display/Console.java.  This calls setPlayState().  The comments there
suggest that we can call pressPause, but that we shouldn't mess with
setPlayState() or anything further.

Question: If I *can* do this repeated pausing and starting, is it a
good idea?  e.g. will it make the thing a lot slower?  Note that in
Console.java, pausing involves messing with the state of a thread.

## Is it necessary to extend SimState?

Summary: yes

Why?  

1. To get at a spun-up MersenneTwister RNG.
2. To support user parameter configurations from the GUI.
3. If you use MASON's scheduling facility, that comes from SimState, too.
4. You do need a GUIState if you want to have a UI, and the only
   constructor for GUIState listed in the docs requires that you pass 
   in a SimState.

#### discussion:

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

Or just write SimState in Java. :-(

(SimState provides step() and start(), but you can use GUIState's
intead, which will call the SimState's scheduler, for example.)
