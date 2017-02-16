;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free-agent.perception
  (:require [clojure.algo.generic.math-functions :as amath]
            [free-agent.mush :as mu]
            [utils.random :as ran]
            [utils.random-utils :as ranu])
  (:import [sim.field.grid Grid2D ObjectGrid2D]
           [sim.util IntBag]))

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

(defn s-snipe-pref-success-bias
  "Adopts mushroom preference with a sign like that of its most successful 
  neighbor, where success is measured by current energy.  Ties are broken
  randomly.  (Note this simple measure means that a snipe that's recently 
  given birth and lost the birth cost may appear less successful than one 
  who's e.g. never given birth but is approaching the birth threshold.)"
  [rng snipe mush neighbors]
  (let [{:keys [cfg-data$]} snipe
        {:keys [mush-mid-size]} @cfg-data$
        best-neighbors (reduce (fn [best-neighbors neighbor]
                                 (let [best-energy (:energy (first best-neighbors))
                                       neighbor-energy (:energy neighbor)]
                                   (cond (< neighbor-energy best-energy) best-neighbors
                                         (> neighbor-energy best-energy) [neighbor]
                                         :else (conj best-neighbors neighbor))))
                               [{:energy -1 :mush-pref 0}] ; start with dummy "snipe", returned only if there are no neighbors
                               neighbors) 
        mush-pref (+ (:mush-pref (ranu/sample-one rng best-neighbors))
                     (pref-noise rng)) ; allows s-snipes to explore preference space even when all snipes are s-snipes
        scaled-appearance (- (mu/appearance mush) mush-mid-size)
        eat? (pos? (* mush-pref scaled-appearance))]  ; eat if scaled appearance has same sign as mush-pref
    [(assoc snipe :mush-pref mush-pref) eat?])) ; mush-pref will just be replaced next time, but this allows inspection

(defn this-env-neighbors
  [snipe]
  (let [{:keys [x y cfg-data$]} snipe
        {:keys [popenv neighbor-radius]} @cfg-data$
        {:keys [snipe-field]} popenv]
    (.getHexagonalNeighbors snipe-field x y neighbor-radius Grid2D/TOROIDAL false)))

(defn cross-env-neighbors
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

(defn s-snipe-pref-success-bias-this-env
  [rng snipe mush]
  (s-snipe-pref-success-bias rng snipe mush (this-env-neighbors snipe)))

(defn s-snipe-pref-success-bias-cross-env
  [rng snipe mush]
  (s-snipe-pref-success-bias rng snipe mush (cross-env-neighbors snipe)))

(defn random-eat-snipe-pref
 "Decides by a coin toss whether to eat."
  [rng snipe mush]
  [snipe (pos? (pref-noise rng))])

(defn always-eat-snipe-pref
 "Always eats."
  [rng snipe mush]
  [snipe true])
