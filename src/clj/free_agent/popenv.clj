(ns free-agent.popenv
  (:require [free-agent.snipe :as sn]
            [free-agent.mush :as mu]
            [utils.random :as ran])
  (:import [sim.field.grid Grid2D ObjectGrid2D]
           [sim.util IntBag]))

;(use '[clojure.pprint]) ; DEBUG

(declare setup-popenv! new-popenv make-popenv populate-env! organism-setter 
         add-organism-to-rand-loc!  add-k-snipes!  add-r-snipes! add-mush! 
         maybe-add-mush! add-mushs!  perceive-mushroom eat-if-appetizing snipes-eat 
         choose-next-loc move-snipe! move-snipes next-popenv)

(defn setup-popenv-config!
  [cfg-data$]
  (let [{:keys [env-width]} @cfg-data$]
    (swap! cfg-data$ assoc :env-center (/ env-width 2.0))))

(defrecord PopEnv [snipe-field mush-field])

(defn make-popenv
  [rng cfg-data]
  (let [{:keys [env-width env-height]} cfg-data
        snipe-field (ObjectGrid2D. env-width env-height)
        mush-field  (ObjectGrid2D. env-width env-height)]
    (.clear mush-field)
    (add-mushs! rng cfg-data mush-field)
    (.clear snipe-field)
    (add-k-snipes! rng cfg-data snipe-field)
    (add-r-snipes! rng cfg-data snipe-field)
    (PopEnv. snipe-field mush-field)))

(defn next-popenv
  [popenv rng cfg-data] ; put popenv first so we can swap! it
  (let [{:keys [snipe-field mush-field]} popenv
        snipe-field (move-snipes rng cfg-data snipe-field)                         ; replaces with snipes with new snipes with new positions
        [snipe-field mush-field] (snipes-eat rng cfg-data snipe-field mush-field)] ; replaces snipes with new snipes with same positions
    (PopEnv. snipe-field mush-field)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn organism-setter
  ([organism-maker]
  (fn [field x y]
    (.set field x y (organism-maker x y)))))

(defn add-organism-to-rand-loc!
  "Create and add an organism to field using organism-maker, which expects 
  x and y coordinates as arguments.  Looks for an empty field, so could
  be inefficient if a large proportion of cells in the field are filled."
  [rng field width height organism-setter!]
  (loop []
    (let [x (ran/rand-idx rng width)
          y (ran/rand-idx rng height)]
      (if-not (.get field x y) ; don't clobber another snipe; empty slots contain Java nulls, i.e. Clojure nils
        (organism-setter! field x y)
        (recur)))))

(defn add-k-snipes!
  [rng cfg-data field]
  (let [{:keys [env-width env-height initial-energy k-snipe-prior num-k-snipes]} cfg-data]
    (dotimes [_ (:num-k-snipes cfg-data)] ; don't use lazy method--it may never be executed
      (add-organism-to-rand-loc! rng field env-width env-height 
                                 (organism-setter (partial sn/make-k-snipe initial-energy k-snipe-prior))))))

(defn add-r-snipes!
  [rng cfg-data field]
  (let [{:keys [env-width env-height initial-energy r-snipe-low-prior r-snipe-high-prior num-r-snipes]} cfg-data]
    (dotimes [_ num-r-snipes]
      (add-organism-to-rand-loc! rng field env-width env-height 
                                 (organism-setter (partial sn/make-r-snipe initial-energy 
                                                           r-snipe-low-prior 
                                                           r-snipe-high-prior))))))

(defn add-mush!
  [rng cfg-data field x y]
  (let [{:keys [env-center mush-low-mean mush-high-mean 
                mush-sd mush-pos-nutrition mush-neg-nutrition]} cfg-data
        [low-mean-nutrition high-mean-nutrition] (if (< x env-center) ; subenv determines whether low vs high reflectance
                                                   [mush-pos-nutrition mush-neg-nutrition]   ; paired with
                                                   [mush-neg-nutrition mush-pos-nutrition])] ; nutritious vs poison
    (.set field x y 
          (if (< (ran/next-double rng) 0.5) ; half the mushrooms are of each kind in each subenv, on average
            (mu/make-mush mush-low-mean  mush-sd low-mean-nutrition)
            (mu/make-mush mush-high-mean mush-sd high-mean-nutrition)))))

(defn maybe-add-mush!
  [rng cfg-data field x y]
  (when (< (ran/next-double rng) (:mush-prob cfg-data)) ; flip biased coin to decide whether to grow a mushroom
    (add-mush! rng cfg-data field x y)))

;; Do I really need so many mushrooms?  They don't change.  Couldn't I just define four mushrooms,
;; and reuse them?  (If so, be careful about their deaths.)
(defn add-mushs!
  "For each patch in mush-field, optionally add a new mushroom, with 
  probability (:mush-prob cfg-data)."
  [rng cfg-data field]
  (let [{:keys [env-width env-height mush-prob
                mush-low-mean mush-high-mean mush-sd 
                mush-pos-nutrition mush-neg-nutrition]} cfg-data]
    (doseq [x (range env-width)
            y (range env-height)]
      (maybe-add-mush! rng cfg-data field x y))))

;    ;; DEBUG:
;    (doseq [x (range (:env-width cfg-data))
;            y (range (:env-height cfg-data))]
;      (when-let [snipe (.get snipe-field x y)]
;        (println (and (= x (:x snipe)) (= y (:y snipe))))))

(defn perceive-mushroom [snipe mush]
  [snipe true]) ; FIXME

(defn add-to-energy
  [snipe-energy max-energy mush-nutrition]
  (max 0 
       (min max-energy 
            (+ snipe-energy mush-nutrition))))


(defn eat-if-appetizing 
  "Returns a pair containing (a) a new version of the snipe, with 
  an updated cognitive state, if appropriate, and an updated 
  energy level if eating occured; and (b) a boolean indicating
  whether eating occurred."
  [max-energy snipe mush] 
  (let [[experienced-snipe appetizing?] (perceive-mushroom snipe mush)]
    (if appetizing?
      [(update experienced-snipe :energy add-to-energy max-energy (:nutrition mush)) true]
      [experienced-snipe false])))

;(when-not (.get snipe-field (:x snipe) (:y snipe)) (println "Whoaa! No snipe at" (:x snipe) (:y snipe))) ; DEBUG

(defn snipes-eat
  [rng cfg-data snipe-field mush-field]
  (let [{:keys [env-width env-height max-energy]} cfg-data
        snipes (.elements snipe-field)
        snipes-plus-eaten? (for [snipe snipes    ; returns only snipes on mushrooms
                                 :let [{:keys [x y]} snipe
                                       mush (.get mush-field x y)]
                                 :when mush]
                             (eat-if-appetizing  max-energy snipe mush))
        new-snipe-field (ObjectGrid2D. snipe-field) ; new field that's a copy of old one
        new-mush-field  (ObjectGrid2D. mush-field)] ; TODO does this full copy (slower) rather than pointer-copy?
    ;; FIXME NOT RIGHT since unchanged mushrooms and snipes are not copied over to new fields
    (doseq [[snipe ate?] snipes-plus-eaten?]
      (when ate?
        (let [{:keys [x y]} snipe]
          (.set new-snipe-field x y snipe) ; replace old snipe with new, more experienced snipe, or maybe the same one
          (.set new-mush-field x y nil)    ; mushroom has been eaten
          ;; a new mushroom grows elsewhere:
          (add-organism-to-rand-loc! rng new-mush-field env-width env-height 
                                     (partial add-mush! rng cfg-data))))) ; can't use same procedure for mushrooms as for snipes
    [new-snipe-field new-mush-field]))

;; reusable bags for choose-next-loc
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

;; doesn't delete old snipe ref--designed to be used only after clearing snipe-field
(defn move-snipe!
  [snipe-field x y snipe]
  (.set snipe-field x y (assoc snipe :x x :y y)))

(defn move-snipes
  [rng cfg-data snipe-field]
  (let [{:keys [env-width env-height]} cfg-data
        snipes (.elements snipe-field)
        loc-snipe-vec-maps (for [snipe snipes  ; make seq of maps with a coord pair as key and singleton seq containing snipe as val
                                 :let [next-loc (choose-next-loc rng snipe-field snipe)]] ; can be current loc
                             next-loc)
        loc-snipe-vec-map (apply merge-with concat loc-snipe-vec-maps) ; convert sec of maps to a single map where snipe-vecs with same loc are concatenated
        loc-snipe-map (into {}                                       ; collect several maps into one
                            (for [[coords snipes] loc-snipe-vec-map] ; go through key-value pairs, where values are collections containing one or more snipes
                              (let [len (count snipes)]              ; convert to key-value pairs where value is a snipe
                                (if (= len 1)
                                  [coords (first snipes)]                          ; when more than one
                                  (let [mover (nth snipes (ran/rand-idx rng len))] ; randomly select one to move
                                    (into {coords mover} (map (fn [snipe] {[(:x snipe) (:y snipe)] snipe}) ; and make others "move" to current loc
                                                              (remove #(= mover %) snipes))))))))         ; (could be more efficient to leave them alone)
        new-snipe-field (ObjectGrid2D. env-width env-height)]
    ;; non-functional version:
        ;new-snipe-field snipe-field]
    ;(.clear snipe-field)
    ;; Since we have a collection of all new snipe positions, including those
    ;; who remained in place, we can just place them on a fresh snipe-field:
    (doseq [[[x y] snipe] loc-snipe-map] ; will convert the map into a sequence of mapentries, which are seqs
      (move-snipe! new-snipe-field x y snipe))
    ;; FIXME Since we destructively modified snipe-field, we don't have to assoc it in to a new popenv (oh...)
    new-snipe-field))
