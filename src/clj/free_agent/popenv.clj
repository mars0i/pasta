(ns free-agent.popenv
  (:require ;[free-agent.SimConfig :as cfg]
            [free-agent.snipe :as sn]
            [free-agent.mushroom :as mu]
            [utils.random :as ran])
  (:import [sim.field.grid ObjectGrid2D]))

;; need prior, energy, num snips
(defn make-k-snipes
  [cfg-data snipe-field]
  (let [{:keys [world-width world-height initial-energy k-snipe-prior num-k-snipes]} cfg-data]
    (repeatedly num-k-snipes #(sn/make-k-snipe initial-energy k-snipe-prior)))) ; TODO add them to field
   ; TODO ADD SNIPES TO FIELD

(defn make-r-snipes
  [cfg-data snipe-field]
  (let [{:keys [world-width world-height initial-energy r-snipe-prior-0 r-snipe-prior-1 num-r-snipes]} cfg-data]
    (repeatedly num-r-snipes #(sn/make-r-snipe initial-energy r-snipe-prior-0 r-snipe-prior-1))))
   ; TODO ADD SNIPES TO FIELD

(defn maybe-add-mushroom
  [rng mushroom-field x y mushroom-prob mean-0 mean-1 sd]
  (when (< (ran/next-double rng) mushroom-prob)
    (.set mushroom-field x y (mu/make-mushroom rng 
                                               (if (< (ran/next-double rng) 0.5) mean-0 mean-1)
                                               sd))))

(defn make-mushrooms
  "For each patch in mushroom-field, optionally add a new mushroom, with 
  probability (:mushroom-prob cfg-data)."
  [rng cfg-data mushroom-field]
  (let [{:keys [world-width world-height mushroom-prob mushroom-mean-0 mushroom-mean-1 mushroom-sd]} cfg-data]
    (doseq [x (range world-width)
            y (range world-height)]
      (maybe-add-mushroom rng mushroom-field x y mushroom-prob
                          mushroom-mean-0 mushroom-mean-1 mushroom-sd))))

(defrecord PopEnv [snipe-field mushroom-field])

(defn make-popenv
  [rng cfg-data]
  (let [{:keys [world-width world-height]} cfg-data
        snipe-field    (ObjectGrid2D. world-width world-height) ; eventually make two of each
        mushroom-field (ObjectGrid2D. world-width world-height)]
    (make-k-snipes cfg-data snipe-field)
    (make-r-snipes cfg-data snipe-field)
    (make-mushrooms rng cfg-data mushroom-field) ; no need for a separate list of mushrooms
    (PopEnv. snipe-field mushroom-field)))


(defn next-popenv
  [cfg-data prev-popenv]
  ;; snipes move and/or eat
  ;; mushrooms spawn
  )

