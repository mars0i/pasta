;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

;; simple vector/matrix example

(ns free-agent.example
  (:require [free-agent.level :as lvl]
            [clojure.core.matrix :as m] ; needed for arithmetic macros even if not used explicitly
            [free-agent.matrix :as fm]
            [free-agent.core :as fc]
            [utils.random :as ran]))

(m/set-current-implementation :vectorz)

;; Since these next three functions run on every tick, maybe slightly
;; faster not to use ar/col-mat:

;; WHY THESE FUNCTIONS?
(defn gen [hypoth] (let [x1 (m/mget hypoth 0 0)  ; hypoth is a column matrix
                      x2 (m/mget hypoth 1 0)] ; 2nd index gets the sole column
                  (m/matrix [[x1]            ; x1 estimates nutrition (???)
                             [(* x2 x2)]]))) ; x2 estimates radius (?)
;; old version:
; (m/matrix [[(* x1 x1 x2)]    
;            [(* x2 x2 x1)]])

;; derivative of gen:
(defn gen' [hypoth] (let [;x1 (m/mget hypoth 0 0)
                       x2 (m/mget hypoth 1 0)]
                   (m/matrix [[1]
                              [(* 2.0 x2)]])))
;; old version:
; (m/matrix [[(* x2 2.0 x1)]
 ;           [(* x1 2.0 x2)]])))

;; THIS WILL BE REPLACED BY MUSHROOM EFFECTS:
(def next-bottom (lvl/make-next-bottom 
                   #(m/matrix [[(ran/next-gaussian fc/rng 2 5)]       ; replace with mushroom nutritiousness 
                               [(ran/next-gaussian fc/rng -1 3)]])))  ; replace with mushroom size signal

(def init-learn (m/identity-matrix 2)) ; i.e. initially pass value of gen(hypoth) through unchanged

; what hypoth is initialized to, and prior mean at top:
(def v-p (fm/col-mat [3.0 3.0]))

(def bot-map {:hypoth   (fm/col-mat [0.0 0.0]) ; immediately replaced by next-bottom
              :error   (fm/col-mat [0.0 0.0])
              :covar (m/matrix [[2.0  0.25]  ; it's a covariance matrix, so
                                [0.25 2.0]]) ; should be symmetric
              :learn init-learn
              :gen  nil
              :gen' nil
              :hypoth-dt  0.01
              :error-dt    0.01
              :covar-dt  0.0
              :learn-dt 0.0})

(def mid-map {:hypoth v-p
              :error (fm/col-mat [0.0 0.0])
              :covar (m/matrix [[0.5 0.5]
                                [0.5 0.5]])
              :learn init-learn
              :gen  gen
              :gen' gen'
              :hypoth-dt 0.0001
              :error-dt 0.01
              :covar-dt 0.0001
              :learn-dt 0.01})

(def init-bot (lvl/map->Level bot-map))
(def init-mid (lvl/map->Level mid-map))
(def top      (lvl/make-top-level v-p))

(defn make-stages [] (iterate (partial lvl/next-levels next-bottom)
                              [init-bot init-mid top]))
