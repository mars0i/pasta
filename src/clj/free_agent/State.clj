(ns free-agent.State
  (:require [clojure.tools.cli]) ; needed here?
  (:import [ec.util MersenneTwisterFast])
  (:gen-class :name free-agent.State
              :extends sim.engine.SimState        ; includes signature for the start() method
              :exposes-methods {start superStart} ; alias method start() in superclass. (Don't name it 'super-start'; use a Java name.)
              ;; These are bean methods that will be exposed to the UI, so they need to have Java types:
              :methods [[getInitialSnipeEnergy [] double]
                        [setInitialSnipeEnergy [double] void]
                        [getRSnipePriors [] "[D"]       ; i.e. array of doubles.
                        [setRSnipePriors ["[D"] void]]
              :state instanceState
              :init init-instance-state
              :main true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global parameters exposed to MASON UI

;; Initial defaults for global parameters
(def default-initial-snipe-energy 10.0)
(def default-initial-snipe-priors [5.0 20.0])

;; This holds the global parameters in gen-class's single state field:
(defrecord InstanceState [initial-snipe-energy$ snipe-priors$])

(defn -init-instance-state 
  "Automatically initializes instance-state when an instance of class State is created."
  [seed]
  (InstanceState. (atom default-initial-snipe-energy)
                  (atom default-initial-snipe-priors)))

;; Bean accessors
(defn -getInitialSnipeEnergy ^double [^State this] @(:initial-snipe-energy$ ^InstanceState (.instanceState this)))
(defn -setInitialSnipeEnergy [^State this ^double newval] (reset! (:initial-snipe-energy$ ^InstanceState (.instanceState this)) newval))
(defn -getInitialSnipePriors [^State this] @(:initial-snipe-priors$ ^InstanceState (.instanceState this)))
(defn -setInitialSnipePriors [^State this newval] (reset! (:initial-snipe-priors$ ^InstanceState (.instanceState this) newval)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn record-commandline-args! [args] ) ; TODO

(defn -main
  [& args]
  (record-commandline-args! args) ; The Sim isn't available yet, so store commandline args for later access by start().
  (sim.engine.SimState/doLoop free-agent.State (into-array String args))
  (System/exit 0))
