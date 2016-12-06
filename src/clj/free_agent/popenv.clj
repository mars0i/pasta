(ns free-agent.popenv
  (:require [free-agent.State :as s]
            [free-agent.snipe :as sn]
            [utils.random :as ran]))

(defn make-k-snipes
  []
  (repeatedly (s/getNumKSnipes. s/sim) sn/make-k-snipe))
  


