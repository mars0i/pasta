(ns free-agent.popenv
  (:require [clojure.algo.generic.functor :as gf]
            [free-agent.snipe :as sn]
            [free-agent.mushroom :as mu]
            [utils.random :as ran])
  (:import [sim.field.grid Grid2D ObjectGrid2D]
           [sim.util IntBag]))


(defrecord PopEnv [snipe-field mushroom-field])

(defn make-popenv
  [rng cfg-data]
  (let [{:keys [world-width world-height]} cfg-data
        snipe-field    (ObjectGrid2D. world-width world-height)    ; eventually make two of each (for two sides of the mountain)
        mushroom-field (ObjectGrid2D. world-width world-height)]   ; also this one
    (PopEnv. snipe-field mushroom-field)))


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
        snipe-field    (:snipe-field popenv)]
    (.clear mushroom-field)
    (add-mushrooms rng cfg-data mushroom-field)
    (.clear snipe-field)
    (add-k-snipes rng cfg-data snipe-field)
    (add-r-snipes rng cfg-data snipe-field)
    (PopEnv. snipe-field mushroom-field)))

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
        {[next-x next-y] [snipe]})))) ; key is a pair of coords; val is a single-element vector containing a snipe

(defn sample-one
  "Given a non-empty collection, returns a single randomly-chosen element."
  [rng xs]
  (let [len (count xs)]
    (if (= len 1)
      (nth xs 0)
      (nth xs 
           (ran/rand-idx rng (count xs))))))

(defn next-popenv
  [popenv rng cfg-data] ; put popenv first so we can swap! it
  (let [{:keys [snipe-field mushroom-field]} popenv
        snipes (.elements snipe-field)
        loc-snipe-vec-maps (for [snipe snipes  ; make seq of snipes with intended next locations filled in
                        :let [next-loc (choose-next-loc rng snipe-field snipe)]
                        :when next-loc] ; nil if no place to move
                    next-loc)
        loc-snipe-vec-map (merge-with concat loc-snipe-vec-maps) ; convert sec of maps to a single map where snipe-vecs with same loc are concatenated
        loc-snipe-map (gf/fmap (partial sample-one rng) loc-snipe-vec-map)] ; create map from locs to snipes randomly chosen from snipe-vecs
    ;; TODO also need to update mushroom-field with ne mushrooms, maybe destroy those eatent
    ;; For now, update snipe-field Mason-style, i.e. modifying the same field every time:
    (.clear snipe-field)
    (doseq [[[x y] snipe] loc-snipe-map]
      (.set snipe-field x y snipe))
    ;; Since we destructively modified snipe-field, we don't have to assoc it in to a new popenv (oh...)
    popenv))
