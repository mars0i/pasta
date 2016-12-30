Issues 12/2016 about clicking, double clicking,
inspectors, CircledPortrayal2D

Using the latest MASON distribution from the repo, with some
fixed that Sean Luke added recently:

With java 1.8.0_51 on the mba, I could get inspectors from
double-clicking on objects, but CircledPortrayal2D didn't respond to
clicks.

With Java 1.8.0_66 (?) on the mbp, I got the circles when clicking,
but instead of inspectors, I get a NPE.

I upgraded to java 1.8.0_112 on the mba, and now I get the circles but
also the NPE instead of inspectors.  This is true both on the master
branch and on branch non-fnl (but the circle will follow a snipe
only on non-fnl, not surprisingly).

My little java example masonborder gives both circles and inspectors
from double-clicking.

Intermittran gives inspectors when I double-cick (and it's not set up
with CircledPortrayal2D), and in fact they update as it runs.

In my Clojure Defrecord version of the Students example from the
manual, I get everything: The circle, which moves with the node, and
inspectors, which update as it runs.

So the inspector problem is specific to something I'm doing in
free-agent.

Adding a single bean-style accessor to the snipes (getEnergy) doesn't
help.  Hmm.  Surprising--because the NPE is on an arraylist called
`getMethods` in SimpleProperties.java.

I tried adding a line in project.clj that would compile to Java 1.5,
since that's what happens when compiling MASON.  This didn't help.

I tried adding getEnergy with a protocoal and an interface.  No help.


What's different about Intermittran?

1. The agent defs are in the same namespace as the main SimState
subclass.  And the gen-class is bigger.

2. It uses deftype rather than defrecord.  No, I have deftype
in branch non-fnl

Note that I am importing snipe and all that into free-agent.UI
already.  Adding snipe as a require doesn't help.

I tried adding "-" to getEnergy.  Had to do that in the interface,
too, and got the same error when double-clicking.

aot-compiling free-agent.snipe didn't help.
