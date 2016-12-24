notes on a functional-style implementation of perception and eating
====

Three state changes with one conditional fork must be accomplished when a
snipe encounters a mushroom.

* Snipe decides whether to eat

* Snipe's internal neural network state is altered as a result of 
  the decision process.

* If snipe eats, then the mushroom's nutritional value is added
  to Snipe's energy

* If Snipe eats, the mushroom disappears, and another mushroom
  appears elsewhere in the same subenv (which could be
  be the other mushroom at new coordinates, although I could make
  it random which kind it is that appears).

So the three things altered are:

* The mushroom field
* The Snipe's energy
* The Snipe's neural net state

However, the neural net state actually only needs to be preserved for
k-snipes.  And maybe the only thing that actually needs to be preserved
is the theta value.

What needs to be returned by `snipes-eat` is:

* a snipe field with updated snipes
* a mushroom field with "moved" mushrooms

To produce the latter, it's enough to have a collection of coordinates
of eaten mushrooms.  To produce the former, all snipes must be replaced
in the snipe field by their `assoc`-updated versions.

Since snipes contain their coordinates, one way to do this would be to
generate snipe/boolean pairs, where the boolean indicates whether eating
occurred.  Then use these to produce the new snipe and mushroom fields.
