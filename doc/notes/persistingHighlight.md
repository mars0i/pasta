Notes on making some method of highlighting a snipe in the ui
despite the fact that it's a new snipe every tick:

(An odd thing: Sometimes if the "same" snipe returns to the same cell
very soon after CircledPortrayal2D was turned on, the circle displays
again.  Maybe this has to do with Clojure's updating, which, after all,
is not guaranteed to not use the same object repeatedly.  Otherwise,
the circle disappears as soon as the snipe moves.  Note that in
experiments with Java, circles move if the same object moves, but not
if it's replaced.)

## communication

If I have a flag in the snipe indicating that it's being inspected,
then maybe a subclass of CircledInspector2D or a portrayal set to
change color or shape could check that to see if its snipe is supposed
to get the special visual treatment, and then do that.

Or maybe the snipe could tell something else that will get checked.
Or there could be a record somewhere else--not in the snipe.

But how can the snipe, or anything, know that the inspector is now
gone?  I think maybe `isStopped()` in `SimpleInspector` would do that.
Also see `isValidInspector()` in property inspectors.

But how to find the inspector to turn off your flag?  Maybe
`Controller.getAllInspectors()`.

Or if I subclass an inspector, I could have it tell its current snipe
before it dies, or something like that.
