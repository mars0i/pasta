(ns free-agent.popenv
  (:require [free-agent.snipe :as sn]
            [free-agent.mush :as mu]
            [utils.random :as ran])
  (:import [sim.field.grid Grid2D ObjectGrid2D]
           [sim.util IntBag]))

;(use '[clojure.pprint]) ; DEBUG

(defrecord PopEnv [snipe-field mush-field])

(defn make-popenv
  [rng cfg-data]
  (let [{:keys [env-width env-height]} cfg-data
        snipe-field    (ObjectGrid2D. env-width env-height)    ; eventually make two of each (for two sides of the mountain)
        mush-field (ObjectGrid2D. env-width env-height)]   ; also this one
    (PopEnv. snipe-field mush-field)))


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
  (let [{:keys [env-width env-height initial-energy k-snipe-prior num-k-snipes]} cfg-data]
    (dotimes [_ (:num-k-snipes cfg-data)] ; don't use lazy method--it may never be executed
      (add-snipe rng field env-width env-height 
                 (fn [x y] (sn/make-k-snipe initial-energy k-snipe-prior x y))))))

(defn add-r-snipes
  [rng cfg-data field]
  (let [{:keys [env-width env-height initial-energy r-snipe-low-prior r-snipe-high-prior num-r-snipes]} cfg-data]
    (dotimes [_ num-r-snipes]
      (add-snipe rng field env-width env-height 
                 (fn [x y] (sn/make-r-snipe initial-energy r-snipe-low-prior r-snipe-high-prior x y))))))

(defn maybe-add-mush
  [rng field x y cfg-data]
  (when (< (ran/next-double rng) (:mush-prob cfg-data)) ; flip biased coin to decide whether to grow a mushroom
    (let [{:keys [env-center mush-low-mean mush-high-mean 
                  mush-sd mush-pos-nutrition mush-neg-nutrition]} cfg-data
          [low-mean-nutrition high-mean-nutrition] (if (< x env-center) ; subenv determines whether low vs high reflectance
                                                     [mush-pos-nutrition mush-neg-nutrition]   ; paired with
                                                     [mush-neg-nutrition mush-pos-nutrition])] ; nutritious vs poison
      (.set field x y 
            (if (< (ran/next-double rng) 0.5) ; half the mushrooms are of each kind in each subenv, on average
              (mu/make-mush mush-low-mean  mush-sd low-mean-nutrition)
              (mu/make-mush mush-high-mean mush-sd high-mean-nutrition))))))

(defn add-mushs
  "For each patch in mush-field, optionally add a new mushroom, with 
  probability (:mush-prob cfg-data)."
  [rng cfg-data field]
  (let [{:keys [env-width env-height mush-prob
                mush-low-mean mush-high-mean mush-sd 
                mush-pos-nutrition mush-neg-nutrition]} cfg-data]
    (doseq [x (range env-width)
            y (range env-height)]
      (maybe-add-mush rng field x y cfg-data))))

(defn populate
  [rng cfg-data popenv]
  (let [{:keys [env-width env-height]} cfg-data
        mush-field (:mush-field popenv)
        snipe-field    (:snipe-field popenv)]
    (.clear mush-field)
    (add-mushs rng cfg-data mush-field)
    (.clear snipe-field)
    (add-k-snipes rng cfg-data snipe-field)
    (add-r-snipes rng cfg-data snipe-field)
    (PopEnv. snipe-field mush-field)))

;; reusable bags
(def x-coord-bag (IntBag. 6))
(def y-coord-bag (IntBag. 6))

(defn choose-next-loc
  "Return a pair of field coordinates randomly selected from the empty 
  hexagonally neighboring locations of snipe's location, or the current
  location if all neighboring locations are filled."
  [rng snipe-field snipe]
  (let [curr-x (:x snipe)
        curr-y (:y snipe)]
    (.getHexagonalLocations snipe-field              ; inserts coords of neighbors into x-pos and y-pos args
                            curr-x curr-y
                            1 Grid2D/TOROIDAL false  ; immediate neighbors, toroidally, don't include me
                            x-coord-bag y-coord-bag) ; will hold coords of neighbors
    (let [candidate-locs (for [[x y] (map vector (.toIntegerArray x-coord-bag)  ; x-pos, y-pos have to be IntBags
                                          (.toIntegerArray y-coord-bag)) ; but these are not ISeqs like Java arrays
                               :when (not (.get snipe-field x y))] ; when cell is empty
                           [x y])]
      (if (seq candidate-locs) ; when not empty
        (let [len (count candidate-locs)
              idx (ran/rand-idx rng len)
              [next-x next-y] (nth candidate-locs idx)]
          {[next-x next-y] [snipe]}) ; key is a pair of coords; val is a single-element vector containing a snipe
        {[curr-x curr-y] [snipe]}))))

(defn sample-one
  "Given a non-empty collection, returns a single randomly-chosen element."
  [rng xs]
  (let [len (count xs)]
    (if (= len 1)
      (nth xs 0)
      (nth xs 
           (ran/rand-idx rng len)))))

;; maybe there's a more efficient way


(defn move-snipe
  [snipe-field x y snipe]
  (.set snipe-field x y (assoc snipe :x x :y y)))


(defn next-popenv
  [popenv rng cfg-data] ; put popenv first so we can swap! it
  (let [{:keys [snipe-field mush-field]} popenv
        snipes (.elements snipe-field)
        loc-snipe-vec-maps (for [snipe snipes  ; make seq of snipes with intended next locations filled in
                                 :let [next-loc (choose-next-loc rng snipe-field snipe)]
                                 :when next-loc] ; nil if no place to move
                             next-loc)
        loc-snipe-vec-map (apply merge-with concat loc-snipe-vec-maps) ; convert sec of maps to a single map where snipe-vecs with same loc are concatenated
        loc-snipe-map (into {}                                       ; collect several maps into one
                            (for [[coords snipes] loc-snipe-vec-map] ; go through key-value pairs, where values are collections containing one or more snipes
                              (let [len (count snipes)]              ; convert to key-value pairs where value is a snipe
                                (if (= len 1)
                                  [coords (first snipes)]                          ; when more than one
                                  (let [mover (nth snipes (ran/rand-idx rng len))] ; randomly select one to move
                                    (into {coords mover} (map (fn [snipe] {[(:x snipe) (:y snipe)] snipe}) ; and make key-value pairs for others to "move" to where they are
                                                              (remove #(= mover %) snipes))))))))]         ; (could be more efficient to leave them alone)
    ;; TODO also need to update mush-field with new mushrooms, maybe destroy those eatent
    ;; TODO ? For now, update snipe-field Mason-style, i.e. modifying the same field every time:
    (.clear snipe-field)
    (doseq [[[x y] snipe] loc-snipe-map] ; will convert the map into a sequence of mapentries, which are seqs
      (move-snipe snipe-field x y snipe))
    ;; Since we destructively modified snipe-field, we don't have to assoc it in to a new popenv (oh...)
    popenv))
