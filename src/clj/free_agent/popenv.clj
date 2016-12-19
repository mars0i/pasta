(ns free-agent.popenv
  (:require [free-agent.snipe :as sn]
            [free-agent.mushroom :as mu]
            [utils.random :as ran])
  (:import [sim.field.grid ObjectGrid2D]))


(defrecord PopEnv [snipe-field next-snipe-field mushroom-field])

(defn make-popenv
  [rng cfg-data]
  (let [{:keys [world-width world-height]} cfg-data
        snipe-field    (ObjectGrid2D. world-width world-height)    ; eventually make two of each (for two sides of the mountain)
        next-snipe-field  (ObjectGrid2D. world-width world-height) ; two of these, too
        mushroom-field (ObjectGrid2D. world-width world-height)]   ; also this one
    (PopEnv. snipe-field next-snipe-field mushroom-field)))


(defn add-snipe
  [rng field width height snipe]
  (loop []
    (let [x (ran/rand-idx rng width)
          y (ran/rand-idx rng height)]
      (if-not (.get field x y) ; don't clobber another snipe; empty slots contain Java nulls, i.e. Clojure nils
        (.set field x y snipe)
        (recur)))))

(defn add-k-snipes
  [rng cfg-data field]
  (let [{:keys [world-width world-height initial-energy k-snipe-prior num-k-snipes]} cfg-data]
    (dotimes [_ (:num-k-snipes cfg-data)] ; don't use lazy method--it may never be executed
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

(defn populate
  [rng cfg-data popenv]
  (let [{:keys [world-width world-height]} cfg-data
        mushroom-field (:mushroom-field popenv)
        snipe-field    (:snipe-field popenv)
        next-snipe-field (:next-snipe-field popenv)] ; passed through unchanged for now
    (.clear mushroom-field)
    (add-mushrooms rng cfg-data mushroom-field)
    (.clear snipe-field)
    (add-k-snipes rng cfg-data snipe-field)
    (add-r-snipes rng cfg-data snipe-field)
    (PopEnv. snipe-field next-snipe-field mushroom-field)))

(defn next-popenv
  [popenv cfg-data] ; put popenv first so we can swap! it
  (let [{:keys [snipe-field next-snipe-field mushroom-field]} popenv]
  ;; snipes move and/or eat
    ; for each filled patch in snipe-field
    ; if unseen mushroom, decide whether to eat
    ; else get neighbor coords and randomly pick one
    ; add self to next-snipe-field there, or if space is already filled, add self to a set there
    ; then
    ; for each set in next-snipe-field, randomly pick member and replace set with it (or something like that)
    ; then swap next-snipe-field and snipe-field
    ; and clear the new next-snipe-field.
    ; also replace in portrayal in gui
    ; hmm so maybe don't do the swap.  copy back to original instead. ?
  ;; mushrooms spawn
  popenv))

