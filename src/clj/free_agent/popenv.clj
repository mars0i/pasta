(ns free-agent.popenv
  (:require ;[free-agent.SimConfig :as cfg]
            [free-agent.snipe :as sn]
            [free-agent.mushroom :as mu]
            [utils.random :as ran])
  (:import [sim.field.grid ObjectGrid2D]))

(defn add-snipe
  [rng field width height snipe]
  (loop []
    (let [x (ran/rand-idx rng width)
          y (ran/rand-idx rng height)]
      (if-not (.get field x y) ; empty slots contain Java nulls, i.e. Clojure nils
        (.set field x y snipe)
        (recur)))))

(defn add-k-snipes
  [rng cfg-data field]
  (let [{:keys [world-width world-height initial-energy k-snipe-prior num-k-snipes]} cfg-data]
    (dotimes [_ (:num-k-snipes cfg-data)]
      (add-snipe rng field world-width world-height 
                 (sn/make-k-snipe initial-energy k-snipe-prior)))))

(defn add-r-snipes
  [rng cfg-data field]
  (let [{:keys [world-width world-height initial-energy r-snipe-prior-0 r-snipe-prior-1 num-r-snipes]} cfg-data]
    (dotimes [_ num-r-snipes]
      (add-snipe rng field world-width world-height 
                 (sn/make-r-snipe initial-energy r-snipe-prior-0 r-snipe-prior-1)))))

(defn maybe-add-mushroom
  [rng field x y mushroom-prob mean-0 mean-1 sd]
  (when (< (ran/next-double rng) mushroom-prob)
    (.set field x y (mu/make-mushroom rng 
                                               (if (< (ran/next-double rng) 0.5) mean-0 mean-1)
                                               sd))))

(defn add-mushrooms
  "For each patch in mushroom-field, optionally add a new mushroom, with 
  probability (:mushroom-prob cfg-data)."
  [rng cfg-data field]
  (let [{:keys [world-width world-height mushroom-prob mushroom-mean-0 mushroom-mean-1 mushroom-sd]} cfg-data]
    (doseq [x (range world-width)
            y (range world-height)]
      (maybe-add-mushroom rng field x y mushroom-prob
                          mushroom-mean-0 mushroom-mean-1 mushroom-sd))))

(defrecord PopEnv [snipe-field mushroom-field])

(defn make-popenv
  [rng cfg-data]
  (let [{:keys [world-width world-height]} cfg-data
        snipe-field    (ObjectGrid2D. world-width world-height) ; eventually make two of each
        mushroom-field (ObjectGrid2D. world-width world-height)]
    (add-k-snipes rng cfg-data snipe-field)
    (add-r-snipes rng cfg-data snipe-field)
    (add-mushrooms rng cfg-data mushroom-field) ; no need for a separate list of mushrooms
    (PopEnv. snipe-field mushroom-field)))


(defn next-popenv
  [popenv cfg-data] ; put popenv first so we can swap! it
  ;; TODO
  ;; snipes move and/or eat
  ;; mushrooms spawn
  popenv) 

