(ns free-agent.popenv
  (:require ;[free-agent.SimConfig :as cfg]
            [free-agent.snipe :as sn]
            [sim.field.grid DoubleGrid2D ObjectGrid2D]
            [utils.random :as ran]))

;; need prior, energy, num snips
(defn make-k-snipes
  [cfg-data]
  (let [{:keys [initial-energy k-snipe-prior num-k-snipes]} cfg-data]
    (repeatedly num-k-snipes #(sn/make-k-snipe initial-energy k-snipe-prior))))

(defn make-r-snipes
  [cfg-data]
  (let [{:keys [initial-energy r-snipe-prior-0 r-snipe-prior-1 num-r-snipes]} cfg-data]
    (repeatedly num-r-snipes #(sn/make-r-snipe initial-energy r-snipe-prior-0 r-snipe-prior-1))))

(defn make-popenv
  [cfg-data] ; TODO this is where circular ref will prevent type annotation if SimConfigData is defined in the SimConfig namespace
  (let [{:keys [world-width world-height]} cfg-data
        k-snipes (make-k-snipes cfg-data)
        r-snipes (make-r-snipes cfg-data)
        mushroom-field (DoubleGrid2D. world-width world-height -1.0) ; -1 means no mushroom.  eventually make two of these
        snipe-field    (ObjectGrid2D. world-width world-height)]     ; eventually make two of these


    ))
        

(defn next-popenv
  [cfg-data prev-popenv]
  )

