;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

;; simple vector/matrix example

(ns free-agent.example-3
  (:require [free-agent.level :as lvl]
            [clojure.core.matrix :as mx] ; needed for arithmetic macros even if not used explicitly
            [free.random :as ran] ; clj or cljs depending on dialect
            [free.arithmetic :as ar]))

(mx/set-current-implementation :vectorz)

;; Since these next three functions run on every tick, maybe slightly
;; faster not to use ar/col-mat:

(defn gen [phi] (let [x1 (mx/mget phi 0 0)
                      x2 (mx/mget phi 1 0)]
                  (mx/matrix [[(* x1 x1 x2)]
                              [(* x2 x2 x1)]])))

(defn gen' [phi] (let [x1 (mx/mget phi 0 0)
                       x2 (mx/mget phi 1 0)]
                   (mx/matrix [[(* x2 2.0 x1)]
                               [(* x1 2.0 x2)]])))

(def next-bottom (lvl/make-next-bottom 
                   #(mx/matrix [[(ran/next-gaussian  2 5)]
                                [(ran/next-gaussian -1 3)]])))

(def init-theta (ar/make-identity-obj 2)) ; i.e. initially pass value of gen(phi) through unchanged

; what phi is initialized to, and prior mean at top:
(def v-p (ar/col-mat [3.0 3.0]))

(def bot-map {:phi   (ar/col-mat [0.0 0.0]) ; immediately replaced by next-bottom
              :epsilon   (ar/col-mat [0.0 0.0])
              :sigma (mx/matrix [[2.0  0.25]  ; it's a covariance matrix, so
                                 [0.25 2.0]]) ; should be symmetric
              :theta init-theta
              :gen  nil
              :gen' nil
              :phi-dt    0.01
              :epsilon-dt    0.01
              :sigma-dt  0.0
              :theta-dt 0.0})

(def mid-map {:phi v-p
              :epsilon   (ar/col-mat [0.0 0.0])
              :sigma (mx/matrix [[2.0  0.25]
                                 [0.25 2.0]])
              :theta init-theta
              :gen  gen
              :gen' gen'
              :phi-dt    0.0001
              :epsilon-dt    0.01
              :sigma-dt  0.0001
              :theta-dt 0.01})

(def init-bot (lvl/map->Level bot-map))
(def init-mid (lvl/map->Level mid-map))
(def top      (lvl/make-top-level v-p))

(defn make-stages [] (iterate (partial lvl/next-levels next-bottom)
                              [init-bot init-mid top]))
