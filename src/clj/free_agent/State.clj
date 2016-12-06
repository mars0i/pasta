(ns free-agent.State
  (:require [clojure.tools.cli])
  (:import [sim.engine Steppable Schedule]
           [ec.util MersenneTwisterFast]
           [java.lang String]
           [free-agent State]))

(gen-class :name free-agent.State
           :extends sim.engine.SimState        ; includes signature for the start() method
           :exposes-methods {start superStart} ; alias method start() in superclass. (Don't name it 'super-start'; use a Java name.)
           ;; Bean methods that will be exposed to the UI--need to have Java types:
           :methods [[getInitialSnipeEnergy [] double]
                     [setInitialSnipeEnergy [double] void]
                     [getRSnipePriors [] "[D"]       ; i.e. array of doubles.
                     [setRSnipePriors ["[D"] void]
                     [setNumKSnipes [long] void]
                     [getNumKSnipes [] long]
                     [setNumRSnipes [long] void]
                     [getNumRSnipes [] long]]
           :state instanceState
           :init init-instance-state
           :main true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global parameters exposed to MASON UI

;; Initial defaults for global parameters
(def default-initial-snipe-energy 10.0)
(def default-initial-snipe-priors [5.0 20.0])
(def default-num-k-snipes 20)
(def default-num-r-snipes default-num-k-snipes)

;; This holds the global parameters in gen-class's single state field:
(defrecord InstanceState [initial-snipe-energy$ snipe-priors$ num-k-snipes$ num-r-snipes$])

(defn -init-instance-state 
  "Automatically initializes instance-state when an instance of class State is created."
  [seed]
  [[seed] (InstanceState. (atom default-initial-snipe-energy)
                          (atom default-initial-snipe-priors)
                          (atom default-num-k-snipes)
                          (atom default-num-r-snipes))])

;; Bean accessors
(defn -getInitialSnipeEnergy ^double [^State this] @(:initial-snipe-energy$ ^InstanceState (.instanceState this)))
(defn -setInitialSnipeEnergy [^State this ^double newval] (reset! (:initial-snipe-energy$ ^InstanceState (.instanceState this)) newval))
(defn -getInitialSnipePriors [^State this] @(:initial-snipe-priors$ ^InstanceState (.instanceState this)))
(defn -setInitialSnipePriors [^State this newval] (reset! (:initial-snipe-priors$ ^InstanceState (.instanceState this) newval)))
(defn -getNumKSnipes ^long [^State this] @(:num-k-snipes ^InstanceState (.instanceState this)))
(defn -setNumKSnipes [^State this ^long newval] (reset! (:num-k-snipes ^InstanceState (.instanceState this)) newval))
(defn -getNumRSnipes ^long [^State this] @(:num-r-snipes ^InstanceState (.instanceState this)))
(defn -setNumRSnipes [^State this ^long newval] (reset! (:num-r-snipes ^InstanceState (.instanceState this)) newval))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Commandline start up

(def commandline (atom nil))

(defn record-commandline-args!
  "Temporarily store values of parameters passed on the command line."
  [args]
  ;; These options should not conflict with MASON's.  Example: If "-h" is the single-char help option, doLoop will never see "-help" (although "-t n" doesn't conflict with "-time") (??).
  (let [cli-options [["-?" "--help" "Print this help message."]
                     ["-e" "--energy <energy>" "Initial energy of snipes." :parse-fn #(Double. %)]
                     ["-N" "--popsize <population size>" "Size of both populations" :parse-fn #(Long. %)]]
        usage-fmt (fn [options]
                    (let [fmt-line (fn [[short-opt long-opt desc]] (str short-opt ", " long-opt ": " desc))]
                      (clojure.string/join "\n" (concat (map fmt-line options)))))
        {:keys [options arguments errors summary] :as cmdline} (clojure.tools.cli/parse-opts args cli-options)]
    (reset! commandline cmdline)
    (when (:help options)
      (println "Command line options for the free-agent:")
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
    (when-let [newval (:energy options)] (.setEnergy state newval))
    (when-let [newval (:popsize options)] (.setNumKSnipes state newval) (.setNumRSnipes state newval)))
  (reset! commandline nil)) ; clear it so user can set params in the gui

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start main loop

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
