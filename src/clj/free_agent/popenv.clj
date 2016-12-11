(ns free-agent.popenv
  (:require ;[free-agent.SimConfig :as cfg]
            [free-agent.snipe :as sn]
            [utils.random :as ran]))

(defn make-k-snipes
  [cfg-data]
  (repeatedly (:num-k-snipes @cfg-data) 
              (partial sn/make-k-snipe cfg-data)))

(defn make-r-snipes
  [cfg-data]
  (repeatedly (:num-r-snipes @cfg-data) 
              (partial sn/make-r-snipe cfg-data)))
  


