(ns free-agent.State
  (:require [clojure.tools.cli]) ; needed here?
  (:import [ec.util MersenneTwisterFast]
           ;[free-agent.UI :as ui]
           )
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

(def commandline (atom nil))

(defn record-commandline-args!
  "Temporarily store values of parameters passed on the command line."
  [args]
  ;; These options should not conflict with MASON's.  Example: If "-h" is the single-char help option, doLoop will never see "-help" (although "-t n" doesn't conflict with "-time") (??).
  (let [cli-options [["-?" "--help" "Print this help message."]
                     ["-e" "--energy <energy>" "Initial energy of snipes." :parse-fn #(Double. %)]]
        usage-fmt (fn [options]
                    (let [fmt-line (fn [[short-opt long-opt desc]] (str short-opt ", " long-opt ": " desc))]
                      (clojure.string/join "\n" (concat (map fmt-line options)))))
        {:keys [options arguments errors summary] :as cmdline} (clojure.tools.cli/parse-opts args cli-options)]
    (reset! commandline cmdline)
    (when (:help options)
      (println "Command line options for the Intermittent simulation:")
      (println (usage-fmt cli-options))
      (println "Intermittent and MASON options can both be used:")
      (println "-help (note single dash): Print help message for MASON.")
      (System/exit 0))))

(defn -main
  [& args]
  (record-commandline-args! args) ; The State isn't available yet, so store commandline args for later access by start().
  (sim.engine.SimState/doLoop free-agent.State (into-array String args))
  (System/exit 0))

(defn set-instance-state-from-commandline!
  "Set fields in the State's instanceState from parameters passed on the command line."
  [^State state cmdline]
  (let [{:keys [options arguments errors summary]} @cmdline]
    (when-let [newval (:energy options)] (.setEnergy state newval)))
  (reset! commandline nil)) ; clear it so user can set params in the gui

(defn -start
  "Function called to (re)start a new simulation run."
  [^State this]
  (.superStart this)
  ;; If user passed commandline options, use them to set parameters, rather than defaults:
  (when @commandline (set-instance-state-from-commandline! this commandline))
  ;; Construct core data structures of the simulation:
  (let [^Schedule schedule (.schedule this)
        ^InstanceState istate (.instanceState this)]
    ;; Schedule per-tick step function(s):
    (.scheduleRepeating schedule Schedule/EPOCH 0
                        (reify Steppable 
                          (step [this sim-state]
                            (let [^State state sim-state]
                              ;(doseq [^Indiv indiv population] (copy-relig! indiv state))      ; first communicate relig (to newrelig's)
                              ;(doseq [^Indiv indiv population] (update-relig! indiv))        ; then copy newrelig to relig ("parallel" update)
                              ;(doseq [^Indiv indiv population] (update-success! indiv state))  ; update each indiv's success field (uses relig)
                              ;(let [[this-step & rest-steps] @pop-steps$]
                              ;  (reset! pop-steps$ rest-steps)
                              ;  (ui/display-step this-step))
                              ;(collect-data state)
                              )))))
  ;(report-run-params this)
  )
