(ns free-agent.popenv
  (:require [free-agent.snipe :as sn]
            [free-agent.mushroom :as mu]
            [utils.random :as ran])
  (:import [sim.field.grid Grid2D ObjectGrid2D]
           [sim.util IntBag]))


(defrecord PopEnv [snipe-field next-snipe-field mushroom-field])

(defn make-popenv
  [rng cfg-data]
  (let [{:keys [world-width world-height]} cfg-data
        snipe-field    (ObjectGrid2D. world-width world-height)    ; eventually make two of each (for two sides of the mountain)
        next-snipe-field  (ObjectGrid2D. world-width world-height) ; two of these, too
        mushroom-field (ObjectGrid2D. world-width world-height)]   ; also this one
    (PopEnv. snipe-field next-snipe-field mushroom-field)))


(defn add-snipe
  "Create and add a snipe to field using snipe-maker, which expects x and y
  coordinates as arguments.  May be inefficient if there are the number of
  snipes is a large proportion of the number of cells in the snipe field."
  [rng field width height snipe-maker]
  (loop []
    (let [x (ran/rand-idx rng width)
          y (ran/rand-idx rng height)]
      (if-not (.get field x y) ; don't clobber another snipe; empty slots contain Java nulls, i.e. Clojure nils
        (.set field x y (snipe-maker x y))
        (recur)))))

(defn add-k-snipes
  [rng cfg-data field]
  (let [{:keys [world-width world-height initial-energy k-snipe-prior num-k-snipes]} cfg-data]
    (dotimes [_ (:num-k-snipes cfg-data)] ; don't use lazy method--it may never be executed
      (add-snipe rng field world-width world-height 
                 (fn [x y] (sn/make-k-snipe initial-energy k-snipe-prior x y))))))

(defn add-r-snipes
  [rng cfg-data field]
  (let [{:keys [world-width world-height initial-energy r-snipe-prior-0 r-snipe-prior-1 num-r-snipes]} cfg-data]
    (dotimes [_ num-r-snipes]
      (add-snipe rng field world-width world-height 
                 (fn [x y] (sn/make-r-snipe initial-energy r-snipe-prior-0 r-snipe-prior-1 x y))))))

(defn maybe-add-mushroom
  [rng field x y mushroom-prob mean-0 mean-1 sd]
  (when (< (ran/next-double rng) mushroom-prob)
    (.set field x y (mu/make-mushroom rng 
                                      (if (< (ran/next-double rng) 0.5) mean-0 mean-1)
                                      sd))))

(defn add-mushrooms
  "For each patch in mushroom-field, optionally add a new mushroom, with 
  probability (:mushroom-prob cfg-data)."
  [rng cfg-data field]
  (let [{:keys [world-width world-height mushroom-prob mushroom-mean-0 mushroom-mean-1 mushroom-sd]} cfg-data]
    (doseq [x (range world-width)
            y (range world-height)]
      (maybe-add-mushroom rng field x y mushroom-prob
                          mushroom-mean-0 mushroom-mean-1 mushroom-sd))))

(defn populate
  [rng cfg-data popenv]
  (let [{:keys [world-width world-height]} cfg-data
        mushroom-field (:mushroom-field popenv)
        snipe-field    (:snipe-field popenv)
        next-snipe-field (:next-snipe-field popenv)] ; passed through unchanged for now
    (.clear mushroom-field)
    (add-mushrooms rng cfg-data mushroom-field)
    (.clear snipe-field)
    (add-k-snipes rng cfg-data snipe-field)
    (add-r-snipes rng cfg-data snipe-field)
    (PopEnv. snipe-field next-snipe-field mushroom-field)))

;; reusable bags
(def x-pos (IntBag. 6))
(def y-pos (IntBag. 6))

(defn choose-next-loc
  "Return a pair of field coordinates randomly selected from the empty 
  hexagonally neighboring locations of snipe's location, or nil all
  locations are filled."
  [rng snipe-field snipe]
  (.getHexagonalLocations (:x snipe) (:y snipe) Grid2D/TOROIDAL false x-pos y-pos) ; inserts coords of neighbors into x-pos and y-pos from above
  (let [candidate-locs (for [[x y] (map vector (.toIntegerArray x-pos)  ; x-pos, y-pos have to be IntBags
                                               (.toIntegerArray y-pos)) ; but these are not ISeqs like Java arrays
                             :when (not (.get snipe-field x y))] ; when cell is empty
                         [x y])]
    (when (seq candidate-locs) ; when not empty
      (let [len (count candidate-locs)
            idx (ran/rand-idx rng len)
            [next-x next-y] (nth candidate-locs idx)]
        {[next-x next-y] snipe}))))

(defn next-popenv
  [popenv rng cfg-data] ; put popenv first so we can swap! it
  (let [{:keys [snipe-field next-snipe-field mushroom-field]} popenv
        snipes (.elements snipe-field)
        next-loc-maps (for [snipe snipes  ; make seq of snipes with intended next locations filled in
                        :let [next-loc (choose-next-loc rng snipe-field snipe)]
                        :when next-loc] ; nil if no place to move
                    next-loc)
        next-locs-map (apply merge-width #() ; TODO ;; check whether any snipes are trying to move to the same spot
                        next-loc-maps)]
    ;; then move 'em


  ;; snipes move and/or eat
    ; for each filled patch in snipe-field
    ; if unseen mushroom, decide whether to eat
    ; else get neighbor coords and randomly pick one
    ; add self to next-snipe-field there, or if space is already filled, add self to a set or seq there
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
  ;; mushrooms spawn
  popenv))

