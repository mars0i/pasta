Notes on how to set up the two subenvironments
====

If I use two separate (pairs of) Mason fields, one for each subenv, with
swapped mushroom roles, and then randomly put new snipes in them, each
snipe will always remain in that subenv, which is what I'd prefer.
However, I then have to figure out how to implement this in Mason.
Definitely doable, but it's more work and more complication.

The alternative is simply to treat the middle of a field as a dividing
line, with different mushroom roles on either side.  So the only trick--
a trivial one--is to set the mushroom types depending on which side of
the line they're on.

One drawback is that if I allow snipes to move freely, then some snipes
will move from one subenv to the other, either by crossing the border or
by going beyond the left or right edge of the world and crossing over to
the other subenv.  This is OK, but it seems a ittle costly to r-snipes
compared to k-snipes: k-snipes are always willing to relearn, so it's no
big deal for them.  With r-snipes, though, many will die when they end
up in the wrong environment.  A few might last long enough to go over to
the other environment, but only a few, and those will be balanced by
those who quickly go from a good environment to a bad one.  The others
in the wrong environment will die.  On the other hand, those r-snipes
suited to their environment will persist, but then there is always a
chance that they'll wander into the wrong environment.  r-snipes face
two risks: Quickly ending up in the wrong environment in the first
place, or crossing over after you've survived for a while.
This cost isn't incurred in the two-field solution.

However, think for the sake of simplicity, a single-world solution is
worth it.  It might mean that more extreme parameters are needed for
r-snipes to be selected over k-snipes, but it should still be possible.
