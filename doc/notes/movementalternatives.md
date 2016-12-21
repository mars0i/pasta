

  ;; snipes move and/or eat
    ; for each filled patch in snipe-field
    ; if unseen mushroom, decide whether to eat
    ; else get neighbor coords and randomly pick one
    ; add self to there, or if space is already filled, add self to a set or seq there
    ;    [would it be better to use a core.matrix or Clojure data structure?  A map or a vec of vecs?
    ;    Or either a core.matrix or Mason sparse 2D structure?  Maybe fill a non-sparse 2D, but then
    ;    something sparse like a map to hold the set?  Which could just be a sequence.  In any event,
    ;    what you need is to be able to (a) find these sets efficiently, and (b) find their indexes
    ;    efficiently.  So rather than using an index-to-object map such as a Mason 2D, so something else,
    ;    like a separate coordinated sequence of coordinates, or a Clojure map that pairs the sets with
    ;    pairs of coords maybe using the *former* as keys.]
    ; then
    ; for each set in next-snipe-field, randomly pick member and replace set with it (or something like that)
    ; then swap next-snipe-field and snipe-field
    ; and clear the new next-snipe-field.
    ; also replace in portrayal in gui
    ; hmm so maybe don't do the swap.  copy back to original instead. ?

