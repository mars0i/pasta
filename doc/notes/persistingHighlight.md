## Notes on making some method of highlighting a snipe in the UI despite the fact that it's a new snipe every tick

### Current strategy:

Snipes have an atom field `inspected$` which is set when `properties`
is first called, and which my subclass of `CircledPortrayal2D` checks,
displaying a circle iff it's `true`.

(Or there could be a record somewhere else--not in the snipe.)

### Question: How to turn off when no longer inspected?

* Is there something an inspector does when it dies?

* Can I make an Inspector that will turn off the flag?

	If I subclass an inspector, I could have it tell its current
	snipe before it dies, or something like that.

	Look for the string "Stop" in inspector code and
	documentation.

* Can I have snipes poll for whether they have an inspector?

	But how can the snipe, or anything, know that the inspector is
	now gone?  I think maybe `isStopped()` in `SimpleInspector`
	would do that.  Also see `isValidInspector()` in property
	inspectors.  But how to find the inspector to turn off your
	flag?  Maybe `Controller.getAllInspectors()`.

* Or make the flag turn off by default but have inspectors refresh it?


### Other notes:

(An odd thing: Sometimes if the "same" snipe returns to the same cell
very soon after CircledPortrayal2D was turned on, the circle displays
again.  Maybe this has to do with Clojure's updating, which, after all,
is not guaranteed to not use the same object repeatedly.  Otherwise,
the circle disappears as soon as the snipe moves.  Note that in
experiments with Java, circles move if the same object moves, but not
if it's replaced.)
