(ns free-agent.popenv
  (:require ;[free-agent.SimConfig :as cfg]
            [free-agent.snipe :as sn]
            [sim.field.grid DoubleGrid2D ObjectGrid2D]
            [utils.random :as ran]))

;; need prior, energy, num snips
(defn make-k-snipes
  [num-snipes energy prior]
  (repeatedly num-snipes #(sn/make-k-snipe energy prior)))

(defn make-r-snipes
  [num-snipes energy prior-0 prior-1]
  (repeatedly num-snipes #(sn/make-r-snipe energy prior-0 prior-1)))

(defn make-popenv
  [cfg-data] ; TODO this is where circular ref will prevent type annotation if SimConfigData is defined in the SimConfig namespace
  (let [{:keys [world-width world-height
                initial-energy k-snipe-prior 
               r-snipe-prior-0 r-snipe-prior-1 
               num-k-snipes num-r-snipes]} cfg-data
        k-snipes (make-k-snipes num-k-snipes initial-energy k-snipe-prior)
        r-snipes (make-r-snipes num-r-snipes initial-energy 
                                r-snipe-prior-0 r-snipe-prior-1)
        mushroom-field (DoubleGrid2D. world-width world-height -1.0) ; -1 means no mushroom.  eventually make two of these
        snipe-field    (ObjectGrid2D. world-width world-height)]     ; eventually make two of these


    ))
        

(defn next-popenv
  [cfg-data prev-popenv]
  )

