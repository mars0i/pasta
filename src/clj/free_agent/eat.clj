(ns free-agent.eat
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

(def eat-dt 0.001) ; TODO PUT THIS SOMEWHERE ELSE?
(def eat-noise-sd (double 1/16))

(defn eat-noise [rng]
  (ran/next-gaussian rng 0 eat-noise-sd))

;; See free-fn.nt9 for discussion
(defn calc-k-eat
  [rng snipe mush appearance]
  (let [appearance (mu/appearance mush)
        {:keys [nutrition]} mush
        {:keys [eat cfg-data$]} snipe
        {:keys [mush-mean-size mush-size-scale]} @cfg-data$
        eat-inc (* eat-dt
                   nutrition
                   (- appearance mush-mean-size)
                   mush-size-scale)]
    (+ eat eat-inc (eat-noise rng))))

;; SIMPLIFY this stuff?

(defn k-snipe-eat
  "Updates eat field and decides whether to eat."
  [rng snipe mush]
  (let [{:keys [cfg-data$]} snipe
        {:keys [mush-mean-size]} @cfg-data$
        scaled-appearance (- (mu/appearance mush) mush-mean-size) ; needed here and in calc-k-eat
        eat (calc-k-eat rng snipe mush scaled-appearance)]
    [(assoc snipe :eat eat) (pos? (* eat scaled-appearance))])) ; eat if scaled appearance has same sign as eat

(defn r-snipe-eat
 "Decides randomly whether to eat--like initial base state of k-snipe-eat."
  [rng snipe mush]
  [snipe (pos? (eat-noise rng))])
