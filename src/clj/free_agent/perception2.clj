;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free-agent.perception2
  (:require [clojure.core.matrix :as m]
            ;[clojure.math.numeric-tower :as math]
            ;[free-agent.level :as lvl]
            [free-agent.matrix :as fm]
            [free-agent.mush :as mu]
            [utils.random :as ran]))

;; Simple algorithm for k-snipes that's supposed to:
;; a. learn what size the nutritious mushrooms are around here, and
;; b. then tend to eat only those, and not the poisonous ones

;; For r-snipes, keeps only a minor random effect from preceding
;; algorithm, so r-snipes eat randomly (at present) while
;; k-snipes learn.

;; See free-fn.nt9 for motivation, analysis, derivations, etc.
;; (mush-pref is called 'eat' there.)


;; Put these somewhere else?
(def pref-dt 0.001)
(def pref-noise-sd (double 1/16))

(defn pref-noise [rng]
  (ran/next-gaussian rng 0 pref-noise-sd))

;; See free-fn.nt9 for discussion
(defn calc-k-pref
  "Calculate a new mush-pref for a k-snipe.  Calculates an incremental change
  in mush-pref, and then adds the increment to mush-pref, along with a small
  Gaussian noise value so that even if mush-pref is zero and the increment is
  zero, there's a chance that the snipe will eat.  The core idea in the 
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
        {:keys [mush-mean-size mush-size-scale]} @cfg-data$
        pref-inc (* pref-dt
                   nutrition
                   (- appearance mush-mean-size)
                   mush-size-scale)]
    (+ mush-pref pref-inc (pref-noise rng))))

;; SIMPLIFY this stuff?

(defn k-snipe-pref
  "Updates mush-pref field and decides whether to eat."
  [rng snipe mush]
  (let [{:keys [cfg-data$]} snipe
        {:keys [mush-mean-size]} @cfg-data$
        scaled-appearance (- (mu/appearance mush) mush-mean-size) ; needed here and in calc-k-pref
        mush-pref (calc-k-pref rng snipe mush scaled-appearance)] ; both to update stored value and to decide whether to eat this one
    [(assoc snipe :mush-pref mush-pref)
     (pos? (* mush-pref scaled-appearance))])) ; eat if scaled appearance has same sign as mush-pref

(defn r-snipe-pref
 "Decides randomly whether to eat--like an initial base case of k-snipe-pref."
  [rng snipe mush]
  [snipe (pos? (pref-noise rng))])
