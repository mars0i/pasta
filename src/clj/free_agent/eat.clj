(ns free-agent.eat
  (:require [clojure.core.matrix :as m]
            ;[clojure.math.numeric-tower :as math]
            ;[free-agent.level :as lvl]
            [free-agent.matrix :as fm]
            ;[free-agent.mush] ; temporary--for testing
            [utils.random :as ran]))


;;; THIS IS GOING TO REPLACE SOME CODE IN free-agent.popenv,
;;; but it should be external to popenv so that it can be
;;; replaced with a different approach later.
;;; The code there separates perception and eating, and does the former before
;;; the latter.  Well maybe that's OK.

(defn eat-dt 0.001) ; TODO PUT THIS SOMEWHERE ELSE

(defn calc-taste
  [snipe mush]
  (let [{:keys [size nutrition]} mush
        {:keys [eat cfg-data$]} snipe
        {:keys [mush-mean-size mush-size-scale]} @cfg-data$
        eat-inc (* eat-dt nutrition mush-size-scale 
                   (- size mush-mean-size))]
    (+ eat eat-inc)))
