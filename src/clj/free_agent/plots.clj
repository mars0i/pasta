;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free-agent.plots
  (:require [clojure.core.matrix :as m]
            [incanter.charts :as ch]
            [incanter.core :as co]))

(def hypoth-base-color    (java.awt.Color. 0   0   0))
(def error-base-color    (java.awt.Color. 255 0   0))
(def covar-base-color  (java.awt.Color. 0   255 0))
(def learn-adj-base-color (java.awt.Color. 0   0   255))
(def no-color (java.awt.Color. 0 0 0 0))
;; Can use java.awt.Color.brighter() and .darker() to get 5-10 variations:
;; wrap Java methods in functions so they can be passed:
(defn brighter [color] (.brighter color))
(defn darker   [color] (.darker color))


(defn plot-param-stages
  "Plot the stages for a single parameter--hypoth, error, etc."
  [chart base-color color-inc first-line-num plot-fn level-stages level-param]
  (let [param-stages (map level-param level-stages)
        idxs-seq (m/index-seq (first param-stages)) ; TODO for covar only use two, since it's symmetric?
        num-idxs (count idxs-seq)
        last-line-num (+ first-line-num num-idxs)]
    (doseq [[idxs color line-num] (map vector 
                                       idxs-seq 
                                       (iterate color-inc base-color) ; seq of similar but diff colors
                                       (range first-line-num last-line-num))]
      (plot-fn chart (range) 
                     (map #(apply m/mget % idxs) param-stages)
                     :series-label (name level-param))
                     ;; if parts of the same vector/matrix have different colors, consider using this:
                     ; :series-label (str (name level-param) " " idxs))
      (when color
        (ch/set-stroke-color chart color :dataset line-num))
      (ch/set-point-size chart 1 :dataset line-num)) ; used only for points; ignored for lines
    last-line-num))

(defn plot-level
  "plot-level for vectors and matrices."
  ([stages level-num n every]
   (plot-level (take-nth every (take n stages)) level-num))
  ([stages level-num n]
   (plot-level (take n stages) level-num))
  ([stages level-num]
   ;; Uses undocumented "*" function versions of Incanter chart macros:
   (let [level-stages (map #(nth % level-num) stages)
         chart (ch/scatter-plot nil nil :legend true :series-label "")
         first-plot-fn  (if (== 0 level-num) ch/add-points* ch/add-lines*) ; level-0 hypoth is sensory data, need points since less regular
         hypoth-color      (if (== 0 level-num) nil hypoth-base-color) ; let Incanter set different color for each dataset
         ;; Using identity to not adjust colors within category, but might later:
         line-num (plot-param-stages chart hypoth-color         identity 1        first-plot-fn level-stages :hypoth) ; number 0 used up in scatter-plot call
         line-num (plot-param-stages chart error-base-color    identity line-num ch/add-lines* level-stages :error)
         line-num (plot-param-stages chart covar-base-color  identity line-num ch/add-lines* level-stages :covar)]
     (plot-param-stages              chart learn-adj-base-color identity line-num ch/add-lines* level-stages :learn-adj)
     (ch/set-stroke-color chart no-color :dataset 0) ; set spurious nil/nil dataset to no color so it's invisible in legend
     (co/view chart :width 800 :height 600)
     chart)))
