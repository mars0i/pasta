Issues 12/2016 about clicking, double clicking,
inspectors, CircledPortrayal2D

With java 1.8.0_51 on the mba, I could get inspectors from
double-clicking on objects, but CircledPortrayal2D didn't respond to
clicks.

With Java 1.8.0_66 (?) on the mbp, I got the circles when clicking,
but instead of inspectors, I get a NPE.

I upgraded to java 1.8.0_112 on the mba, and now I get the circles but
also the NPE instead of inspectors.  This is true both on the master
branch and on branch non-fnl.

My little java example masonborder gives both circles and inspectors
from double-clicking.

Intermittran gives inspectors when I double-cick (and it's not set up
with CircledPortrayal2D), and in fact they update as it runs.

In my Clojure Defrecord version of the Students example from the
manual, I get everything: The circle, which moves with the node, and
inspectors, which update as it runs.

So the inspector problem is specific to something I'm doing in
free-agent.
