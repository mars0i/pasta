;; This software is copyright 2017 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free-agent.perception1
  (:require [clojure.core.matrix :as m]
            ;[clojure.math.numeric-tower :as math]
            [free-agent.level :as lvl]
            [free-agent.matrix :as fm]
            [free-agent.mush] ; temporary--for testing
            [utils.random :as ran]))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DOESN'T DO WHAT I INTENDED.
;;;;;;;;;;;;;;;;;;;;;;;;;;

(m/set-current-implementation :vectorz)

;; FOR TESTING; will be replaced by mushroom effects from mushrooms in locations (or not? This could be the simulation, almost.):
(defn next-bottom [rng] 
  (lvl/make-next-bottom 
    (fn []
      (let [nutr (if (> (ran/next-double rng) 0.5) 1 -1)
            size (if (= 1 nutr) 16 4) ; big mushrooms are nutritious
            mush (free-agent.mush/make-mush size 2.0 nutr rng)
            appear (free-agent.mush/appearance mush)] ; random data
        ;(println nutr size appear)
        (m/matrix [[appear]
                   [nutr]]))))) ; nutrition perception is not noisy

;; What I'm doing, at least with k-snipes, is (a) estimating whether
;; this is a big or small mushroom I'm on, and then (b) estimating what
;; nutritional value I *will get* when I eat.
;; So the first part is just regular single-parameter estimation as in
;; Bogacz and my scalar models.  AND THEN you calculate the nutrition from
;; that, and adjust a nutrition function to get that right.
;; So there's the scalar adjustment with a radius parameter and of course
;; learn and all that.  And then a second multiplier parameter. 

(defn get-size [mat] (m/mget mat 0 0))
(defn get-nutr [mat] (m/mget mat 1 0))

(defn size-gen  [size-hypoth] (* size-hypoth size-hypoth))
(defn size-gen' [size-hypoth] (* size-hypoth 2.0))
(defn nutr-gen  [size-hypoth nutr-mult] (* nutr-mult (size-gen size-hypoth)))
(defn nutr-gen' [size-hypoth nutr-mult] (* 2.0 nutr-mult size-hypoth))

(defn gen [hypoth] (let [size-hypoth (get-size hypoth ) ; hypoth is a column matrix
                         nutr-mult   (get-nutr hypoth) ; 2nd index gets the sole column
                         new-size-hypoth (size-gen size-hypoth)
                         new-nutr-mult   (nutr-gen size-hypoth nutr-mult)]
                     (m/matrix [[new-size-hypoth]
                                [new-nutr-mult]])))

(defn gen' [hypoth] (let [size-hypoth (get-size hypoth) ; hypoth is a column matrix
                          nutr-mult   (get-nutr hypoth) ; 2nd index gets the sole column
                          new-size-hypoth' (size-gen' size-hypoth)
                          new-nutr-mult'   (nutr-gen' size-hypoth nutr-mult)]
                      (m/matrix [[new-size-hypoth']
                                 [new-nutr-mult']])))

;; FOR TESTING will be replaced by mushroom effects:
;(defn next-bottom [rng] (lvl/make-next-bottom 
;                          #(m/matrix [[(ran/next-gaussian rng 2 5)]       ; replace with mushroom size 
;                                      [(ran/next-gaussian rng -1 3)]])))  ; replace with mushroom nutritiousness

(def init-learn (m/identity-matrix 2)) ; i.e. initially pass value of gen(hypoth) through unchanged

; what hypoth is initialized to, and prior mean at top:
(def v-p (fm/col-mat [3.0 3.0]))

(def bot-map {:hypoth     (fm/col-mat [0.0 0.0]) ; immediately replaced by next-bottom
              :error (fm/col-mat [0.0 0.0])
              :covar (m/matrix [[2.0  1.0]  ; it's a covariance matrix, so
                                [1.0 2.0]]) ; should be symmetric
              :learn init-learn
              :gen  nil
              :gen' nil
              ;:attn (fn [_ covar] covar)
              :attn (fn [level covar] (m/mmul covar (if (pos? (get-nutr (:hypoth level))) 1 0))) ; simplistic. can't be right.
              :hypoth-dt  0.01
              :error-dt   0.01
              :covar-dt 0.0001
              :learn-dt 0.0})

(def mid-map {:hypoth v-p
              :error (fm/col-mat [0.0 0.0])
              :covar (m/matrix [[1.0 0.5]
                                [0.5 1.0]])
              :learn init-learn
              :gen  gen
              :gen' gen'
              :attn (fn [_ covar] covar)
              ;:attn (fn [level covar] (m/mmul covar (if (pos? (get-nutr (:hypoth level))) 1 0))) ; simplistic. can't be right.
              :hypoth-dt 0.0001
              :error-dt 0.01
              :covar-dt 0.0001
              :learn-dt 0.01})

(def init-bot (lvl/map->Level bot-map))
(def init-mid (lvl/map->Level mid-map))
(def top      (lvl/make-top-level v-p))

(defn make-stages [rng] (iterate (partial lvl/next-levels (next-bottom rng))
                                 [init-bot init-mid top]))
