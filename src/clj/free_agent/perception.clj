;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free-agent.perception
  (:require [clojure.algo.generic.math-functions :as amath]
            [free-agent.mush :as mu]
            [utils.random :as ran]
            [utils.random-utils :as ranu])
  (:import [sim.field.grid Grid2D] ; ObjectGrid2D
           ;[sim.util IntBag]
           ))

;; Simple algorithm for k-snipes that's supposed to:
;; a. learn what size the nutritious mushrooms are around here, and
;; b. then tend to eat only those, and not the poisonous ones

;; For r-snipes, keeps only a minor random effect from preceding
;; algorithm, so r-snipes eat randomly (at present) while
;; k-snipes learn.

;; See free-fn.nt9 for motivation, analysis, derivations, etc.
;; (mush-pref is called 'eat' there.)


;; Put these somewhere else?
(def pref-dt 0.00001)
(def pref-noise-sd (double 1/16))

(defn pref-noise [rng]
  (ran/next-gaussian rng 0 pref-noise-sd))

;; See free-fn.nt9 for discussion
(defn calc-k-pref
  "Calculate a new mush-pref for a k-snipe.  Calculates an incremental change
  in mush-pref, and then adds the increment to mush-pref.  The core idea of the 
  increment calculation is that if the (somewhat random) appearance of a
  mushroom has a larger (smaller) value than the midpoint between the two 
  actual mushroom sizes, and the mushroom's nutrition turns out to be positive,
  then that's a reason to think that larger (smaller) mushrooms are nutritious,
  and the opposite for negative nutritional values.  Thus it makes sense to
  calculate the increment as a scaled value of the product of the mushroom's 
  nutrition and the difference between the appearance and the midpoint.  Thus
  positive values of mush-pref mean that in the past large mushrooms have often
  be nutritious, while negative values mean that small mushrooms have more
  often been nutritious, on average."
  [rng snipe mush appearance]
  (let [appearance (mu/appearance mush) ; get (noisy) sensory stimulation from mushroom
        {:keys [nutrition]} mush
        {:keys [mush-pref cfg-data$]} snipe
        {:keys [mush-mid-size mush-size-scale]} @cfg-data$
        pref-inc (* pref-dt
                   nutrition
                   (- appearance mush-mid-size)
                   mush-size-scale)]
    (+ mush-pref pref-inc)))

(defn k-snipe-pref
  "Decides whether snipe eats mush, and updates the snipe's mush-pref in 
  response to the experience if so, returning a possibly updated snipe 
  along with a boolean indicating whether snipe is eating.  (Note that 
  the energy transfer resulting from eating will occur elsewhere, in 
  response to the boolean returned here.)"
  [rng snipe mush]
  (let [{:keys [mush-pref cfg-data$]} snipe
        {:keys [mush-mid-size]} @cfg-data$
        scaled-appearance (- (mu/appearance mush) mush-mid-size)
        eat? (pos? (* (+ mush-pref (pref-noise rng)) ; my effective mushroom preference is noisy. (even if starts at zero, I might eat.)
                      scaled-appearance))]           ; eat if scaled appearance has same sign as mush-pref with noise
    [(if eat?
       (assoc snipe :mush-pref (calc-k-pref rng snipe mush scaled-appearance)) ; if we're eating, this affects future preferences
       snipe)
     eat?]))

(defn r-snipe-pref
 "Always prefers size initially specified in its mush-pref field."
  [rng snipe mush]
  (let [{:keys [mush-pref cfg-data$]} snipe
        {:keys [mush-mid-size]} @cfg-data$
        scaled-appearance (- (mu/appearance mush) mush-mid-size)
        eat? (pos? (* mush-pref scaled-appearance))]  ; eat if scaled appearance has same sign as mush-pref
    [snipe eat?]))

;; OBSOLETE?
(defn s-snipe-pref-freq-bias
  "Adopts mushroom preference with a sign like that of its neighbors."
  [rng snipe mush]
  (let [{:keys [x y cfg-data$]} snipe
        {:keys [popenv mush-mid-size neighbor-radius extreme-pref]} @cfg-data$
        {:keys [snipe-field]} popenv
        neighbors (.getHexagonalNeighbors snipe-field x y neighbor-radius Grid2D/TOROIDAL false)
        neighbors-sign (amath/sgn (reduce (fn [sum neighbor]  ; determine whether neighbors with positive or negative prefs
                                            (+ sum (amath/sgn (:mush-pref neighbor)))) ; predominate (or are equal); store sign of result.
                                          0 neighbors)) ; returns zero if no neighbors
        mush-pref (+ (* neighbors-sign extreme-pref) ; -1, 0, or 1 * extreme-pref
                     (pref-noise rng)) ; allows s-snipes to explore preference space even when all snipes are s-snipes
        scaled-appearance (- (mu/appearance mush) mush-mid-size)
        eat? (pos? (* mush-pref scaled-appearance))]  ; eat if scaled appearance has same sign as mush-pref
    [(assoc snipe :mush-pref mush-pref) eat?])) ; mush-pref will just be replaced next time, but this allows inspection

(defn OLD-this-env-neighbors
  [snipe]
  (let [{:keys [x y cfg-data$]} snipe
        {:keys [popenv neighbor-radius]} @cfg-data$
        {:keys [snipe-field]} popenv]
    (.getHexagonalNeighbors snipe-field x y neighbor-radius Grid2D/TOROIDAL false)))

(defn OLD-cross-env-neighbors
  [snipe]
  (let [{:keys [x y cfg-data$]} snipe
        {:keys [popenv neighbor-radius env-width env-center]} @cfg-data$
        {:keys [snipe-field]} popenv
        shifted-x (+ x env-center)
        cross-x (if (< shifted-x env-width)
                  shifted-x
                  (- shifted-x env-width))]
    (concat 
      (.getHexagonalNeighbors snipe-field  ; env for my kind of mushrooms
                              x y
                              neighbor-radius Grid2D/TOROIDAL false)
      (.getHexagonalNeighbors snipe-field  ; env for the other kind of mushrooms
                              cross-x y 
                              neighbor-radius Grid2D/TOROIDAL false))))

;; FIXME BUG: During init, this is called when creating each subenv, but
;; at that point, there is no popenv--not until after the subenvs
;; are created.  So we can't get subenvs from the popenv yet in
;; the second line of the let.
(defn subenv-loc-neighbors
  "Returns a MASON sim.util.Bag containing all snipes in the hexagonal region 
  around location <x,y> in the subenv corresponding to subenv-key, to a 
  distance of neighbor-radius.  This may include the snipe at <x,y>."
  [cfg-data subenv-key x y]
  (let[{:keys [popenv neighbor-radius]} cfg-data
       snipe-field (:snipe-field (subenv-key popenv))]
    ;(println subenv-key snipe-field) ; DEBUG
    (.getHexagonalNeighbors snipe-field x y neighbor-radius Grid2D/TOROIDAL true)))

(defn this-subenv-loc-neighbors
  "Returns a MASON sim.util.Bag containing all snipes in the hexagonal region 
  around location <x,y> in its subenv, to a distance of neighbor-radius.  
  This may include the snipe at <x,y>."
  [cfg-data subenv-key x y]
  (subenv-loc-neighbors cfg-data subenv-key x y))

(defn both-subenvs-loc-neighbors
  [cfg-data x y]
  "Returns a MASON sim.util.Bag containing all snipes in the hexagonal region 
  around location <x,y> in both of the subenvs, to a distance of neighbor-radius.  
  This may include the snipe at <x,y>."
  (.addAll (subenv-loc-neighbors cfg-data :west-subenv x y)
           (subenv-loc-neighbors cfg-data :east-subenv x y)))

;; Would it be faster to avoid doing all of the setup in the let twice for the both-subenvs version?
(defn subenv-snipe-neighbors
  "Returns a MASON sim.util.Bag containing all snipes in the hexagonal region 
  around snipe's location in the subenv corresponding to subenv-key, to a 
  distance of neighbor-radius.  This may include the original snipe."
  [subenv-key snipe]
  (let [{:keys [x y cfg-data$]} snipe]
    (subenv-loc-neighbors @cfg-data$ subenv-key x y)))

(defn this-subenv-snipe-neighbors
  "Returns a MASON sim.util.Bag containing all snipes in the hexagonal region 
  around snipe's location in its subenv, to a distance of neighbor-radius.  
  This will include the original snipe."
  [snipe]
  (subenv-snipe-neighbors (:subenv-key snipe) snipe))

(defn both-subenvs-snipe-neighbors
  [snipe]
  "Returns a MASON sim.util.Bag containing all snipes in the hexagonal region 
  around snipe's location in both of the subenvs, to a distance of neighbor-radius.  
  This will include the original snipe."
  (let [neighbors (subenv-snipe-neighbors :west-subenv snipe)]
    (.addAll neighbors (subenv-snipe-neighbors :east-subenv snipe))
    neighbors))

(defn best-neighbor
  "Return the neighbor (or self) with the most energy.  If there's a tie, return
  a randomly chosen one of the best.  Assumes that there is at least one \"neighbor\":
  oneself."
  [rng neighbors]
  ;(println (map #(vector (:energy %) (:mush-pref %)) neighbors) "\n")(flush) ; DEBUG
  (ranu/sample-one rng 
                   (reduce (fn [best-neighbors neighbor]
                             (let [best-energy (:energy (first best-neighbors))
                                   neighbor-energy (:energy neighbor)]
                               (cond (< neighbor-energy best-energy) best-neighbors
                                     (> neighbor-energy best-energy) [neighbor]
                                     :else (conj best-neighbors neighbor))))
                           [(first neighbors)] (next neighbors)))) ; neighbors should always at least include the "student" snipe

(defn get-best-neighbor-pref
  "Get the preference of the best neighbor in both subenvs."
  [rng snipe]
  (:mush-pref 
    (best-neighbor rng (both-subenvs-snipe-neighbors snipe))))

(defn s-snipe-pref
  "ADD HERE."
  [rng snipe mush]
  (if (= 0.0 (:mush-pref snipe))
    (r-snipe-pref rng 
                  (assoc snipe :mush-pref (get-best-neighbor-pref rng snipe))
                  mush)
    (r-snipe-pref rng snipe mush)))


(defn s-snipe-pref-success-bias
  "Adopts mushroom preference with a sign like that of its most successful 
  neighbor, where success is measured by current energy.  Ties are broken
  randomly.  (Note this simple measure means that a snipe that's recently 
  given birth and lost the birth cost may appear less successful than one 
  who's e.g. never given birth but is approaching the birth threshold.)"
  [rng snipe mush neighbors]
  (let [{:keys [cfg-data$]} snipe
        {:keys [mush-mid-size]} @cfg-data$
        mush-pref (+ (:mush-pref (best-neighbor rng neighbors))
                     (pref-noise rng)) ; allows s-snipes to explore preference space even when all snipes are s-snipes
        scaled-appearance (- (mu/appearance mush) mush-mid-size)
        eat? (pos? (* mush-pref scaled-appearance))]  ; eat if scaled appearance has same sign as mush-pref
    [(assoc snipe :mush-pref mush-pref) eat?])) ; mush-pref will just be replaced next time, but this allows inspection

(defn s-snipe-pref-success-bias-this-env
  [rng snipe mush]
  (s-snipe-pref-success-bias rng snipe mush (OLD-this-env-neighbors snipe)))

(defn s-snipe-pref-success-bias-cross-env
  [rng snipe mush]
  (s-snipe-pref-success-bias rng snipe mush (OLD-cross-env-neighbors snipe)))

(defn random-eat-snipe-pref
 "Decides by a coin toss whether to eat."
  [rng snipe mush]
  [snipe (pos? (pref-noise rng))])

(defn always-eat-snipe-pref
 "Always eats."
  [rng snipe mush]
  [snipe true])
