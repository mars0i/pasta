;; This software is copyright 2019 by Marshall Abrams, 
;; and is distributed under the Gnu General Public License version 3.0 
;; as specified in the the file LICENSE.

;(set! *warn-on-reflection* true)

(ns utils.pink
  (:require [utils.random :as r])
  (:import PinkNoiseFast))

(defn make-pink
  "Return a PinkNoise PRNG based on ec.util.MersenneTwisterFast."
  ([alpha poles] (make-pink (make-mst) alpha poles))
  ([rng alpha poles] (PinkNoiseFast. alpha poles rng)))

(defn normalizing-logistic
  "Logistic function with max value 1, mid x value 0, and user-specified
  growth rate.  Maps [-inf,inf] into [0,1].  (The range is a closed interval 
  because doubles are not arbitrary-precision.)"
  [growth x]
  (/ (inc (java.lang.Math/exp (- (* growth x))))))

(defn next-double
  "Returns a random double in the half-open range from [0.0,1.0) from a pink-noise
  PRNG normalized by normalizing-logistic."
  [rng growth]
  (normalizing-logistic growth (.nextDouble rng)))

