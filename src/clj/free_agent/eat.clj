(ns free-agent.eat
  (:require [clojure.core.matrix :as m]
            ;[clojure.math.numeric-tower :as math]
            ;[free-agent.level :as lvl]
            [free-agent.matrix :as fm]
            [free-agent.mush :as mu]
            [utils.random :as ran]))


;;; THIS IS GOING TO REPLACE SOME CODE IN free-agent.popenv,
;;; but it should be external to popenv so that it can be
;;; replaced with a different approach later.
;;; The code there separates perception and eating, and does the former before
;;; the latter.  Well maybe that's OK.

(def eat-dt 0.001) ; TODO PUT THIS SOMEWHERE ELSE
(def eat-noise-sd (double 1/16))

;; See free-fn.nt9 for discussion
(defn calc-eat
  [snipe mush]
  (let [mush-appearance (mu/appearance mush)
        {:keys [nutrition]} mush
        {:keys [eat cfg-data$]} snipe
        {:keys [rng mush-mean-size mush-size-scale]} @cfg-data$
        eat-inc (* eat-dt
                   nutrition
                   (- appearance mush-mean-size)
                   mush-size-scale)]
    (+ eat eat-inc)))

(defn eat
  [snipe mush]
  (let [new-eat (calc-eat snipe mush)]
    [(assoc snipe :eat new-eat) (pos? new-eat)]))
