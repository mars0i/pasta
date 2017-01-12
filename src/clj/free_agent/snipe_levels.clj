;; This software is copyright 2017 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free-agent.snipe-levels
  (:require [clojure.core.matrix :as m]
            ;[clojure.math.numeric-tower :as math]
            [free-agent.level :as lvl]
            [free-agent.matrix :as fm]
            [free-agent.mush] ; temporary--for testing
            [utils.random :as ran]))

(m/set-current-implementation :vectorz)

;; What I'm doing, at least with k-snipes, is (a) estimating whether
;; this is a big or small mushroom I'm on, and then (b) estimating what
;; nutritional value I *will get* when I eat.
;; So the first part is just regular single-parameter estimation as in
;; Bogacz and my scalar models.  AND THEN you calculate the nutrition from
;; that, and adjust a nutrition function to get that right.
;; So there's the scalar adjustment with a radius parameter and of course
;; theta and all that.  And then a second multiplier parameter. 

(defn size-gen  [size-phi] (* size-phi size-phi))
(defn size-gen' [size-phi] (* size-phi 2.0))
(defn nutr-gen  [size-phi nutr-mult] (* nutr-mult (size-gen size-phi)))
(defn nutr-gen' [size-phi nutr-mult] (* 2.0 nutr-mult size-phi))

(defn gen [phi] (let [size-phi  (m/mget phi 0 0) ; phi is a column matrix
                      nutr-mult (m/mget phi 1 0) ; 2nd index gets the sole column
                      new-size-phi  (size-gen size-phi)
                      new-nutr-mult (nutr-gen size-phi nutr-mult)]
                  (m/matrix [[new-size-phi]
                             [new-nutr-mult]])))

(defn gen' [phi] (let [size-phi  (m/mget phi 0 0) ; phi is a column matrix
                       nutr-mult (m/mget phi 1 0) ; 2nd index gets the sole column
                       new-size-phi'  (size-gen' size-phi)
                       new-nutr-mult' (nutr-gen' size-phi nutr-mult)]
                   (m/matrix [[new-size-phi']
                              [new-nutr-mult']])))

;; FOR TESTING will be replaced by mushroom effects:
;(defn next-bottom [rng] (lvl/make-next-bottom 
;                          #(m/matrix [[(ran/next-gaussian rng 2 5)]       ; replace with mushroom size 
;                                      [(ran/next-gaussian rng -1 3)]])))  ; replace with mushroom nutritiousness

;; FOR TESTING; will be replaced by mushroom effects from mushrooms in locations:
(defn next-bottom [rng] 
  (lvl/make-next-bottom 
    (fn []
      (let [mush (free-agent.mush/make-mush 16.0 2.0 1 rng)
            appear (free-agent.mush/appearance mush)
            nutr (:nutrition mush)]
        (m/matrix [[appear]
                   [nutr]])))))

(def init-theta (m/identity-matrix 2)) ; i.e. initially pass value of gen(phi) through unchanged

; what phi is initialized to, and prior mean at top:
(def v-p (fm/col-mat [3.0 3.0]))

(def bot-map {:phi     (fm/col-mat [0.0 0.0]) ; immediately replaced by next-bottom
              :epsilon (fm/col-mat [0.0 0.0])
              :sigma (m/matrix [[2.0  0.25]  ; it's a covariance matrix, so
                                [0.25 2.0]]) ; should be symmetric
              :theta init-theta
              :gen  nil
              :gen' nil
              :phi-dt  0.01
              :epsilon-dt    0.01
              :sigma-dt  0.0
              :theta-dt 0.0})

(def mid-map {:phi v-p
              :epsilon (fm/col-mat [0.0 0.0])
              :sigma (m/matrix [[0.5 0.0]
                                [0.0 0.5]])
              :theta init-theta
              :gen  gen
              :gen' gen'
              :phi-dt 0.0001
              :epsilon-dt 0.01
              :sigma-dt 0.0001
              :theta-dt 0.01})

(def init-bot (lvl/map->Level bot-map))
(def init-mid (lvl/map->Level mid-map))
(def top      (lvl/make-top-level v-p))

(defn make-stages [rng] (iterate (partial lvl/next-levels (next-bottom rng))
                                 [init-bot init-mid top]))
