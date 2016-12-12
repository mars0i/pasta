(ns free-agent.popenv
  (:require ;[free-agent.SimConfig :as cfg]
            [free-agent.snipe :as sn]
            [sim.field.grid ObjectGrid2D]
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
  (let [{:keys [initial-energy k-snipe-prior 
               r-snipe-prior-0 r-snipe-prior-1 
               num-k-snipes num-r-snipes]} cfg-data
        k-snipes (make-k-snipes num-k-snipes initial-energy k-snipe-prior)
        r-snipes (make-r-snipes num-r-snipes initial-energy 
                                r-snipe-prior-0 r-snipe-prior-1)]

    ;; make mushrooms
    ;; HERE make one or two ObjectGrid2D's and stick the snipes and mushrooms in it.
    ;; or maybe mushrooms are in a DoubleGrid2D.

    ))
        

(defn next-popenv
  [cfg-data prev-popenv]
  )

