;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free-agent.perception2
  (:require [free-agent.mush :as mu]
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

(defn random-eat-snipe-pref
 "Decides by a coin toss whether to eat."
  [rng snipe mush]
  [snipe (pos? (pref-noise rng))])

(defn always-eat-snipe-pref
 "Always eats."
  [rng snipe mush]
  [snipe true])
