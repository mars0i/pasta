(ns free-agent.SimConfig
  (:require [clojure.tools.cli])
  (:import [sim.engine Steppable Schedule]
           [ec.util MersenneTwisterFast]
           [java.lang String]))
;; import free-agent.SimConfig separately below
;; (if done here, fails when aot-compiling from a clean project)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global parameters exposed to MASON UI

;; Initial defaults for global parameters
(def default-snipe-energy 10.0)
(def default-r-snipe-priors [5.0 20.0])
(def default-num-k-snipes 20)
(def default-num-r-snipes default-num-k-snipes)


;; This will hold the global parameters in gen-class's single state field.
(defrecord SimConfigData [initial-snipe-energy
                          r-snipe-priors
                          num-k-snipes
                          num-r-snipes])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The SimState subclass specification

(gen-class :name free-agent.SimConfig
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
           :state simConfigData
           :init init-sim-config-data
           :main true)

;; TODO OH WAIT can I put the sim-config-data somewhere else or if it's here there'll be cyclic deps .... ?
(defn -init-sim-config-data 
  "Automatically initializes sim-config-data when an instance of class SimConfig is created."
  [seed]
  [[seed] (SimConfigData. (atom default-snipe-energy)
                       (atom default-r-snipe-priors)
                       (atom default-num-k-snipes)
                       (atom default-num-r-snipes))])

(import free-agent.SimConfig) ; do this after gen-class but before any type hints using SimConfig

;; Bean accessors
(defn -getInitialSnipeEnergy ^double [^SimConfig this] @(:initial-snipe-energy ^SimConfigData (.simConfigData this)))
(defn -setInitialSnipeEnergy [^SimConfig this ^double newval] (reset! (:initial-snipe-energy ^SimConfigData (.simConfigData this)) newval))
(defn -getInitialRSnipePriors [^SimConfig this] @(:initial-r-snipe-priors ^SimConfigData (.simConfigData this)))
(defn -setInitialRSnipePriors [^SimConfig this newval] (reset! (:initial-r-snipe-priors ^SimConfigData (.simConfigData this) newval)))
(defn -getNumKSnipes ^long [^SimConfig this] @(:num-k-snipes ^SimConfigData (.simConfigData this)))
(defn -setNumKSnipes [^SimConfig this ^long newval] (reset! (:num-k-snipes ^SimConfigData (.simConfigData this)) newval))
(defn -getNumRSnipes ^long [^SimConfig this] @(:num-r-snipes ^SimConfigData (.simConfigData this)))
(defn -setNumRSnipes [^SimConfig this ^long newval] (reset! (:num-r-snipes ^SimConfigData (.simConfigData this)) newval))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Command line start up

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
  (record-commandline-args! args) ; The SimConfig isn't available yet, so store commandline args for later access by start().
  (sim.engine.SimState/doLoop free-agent.SimConfig (into-array String args))
  (System/exit 0))

(defn set-sim-config-data-from-commandline!
  "Set fields in the SimConfig's simConfigData from parameters passed on the command line."
  [^SimConfig state cmdline]
  (let [{:keys [options arguments errors summary]} @cmdline]
    (when-let [newval (:energy options)] (.setEnergy state newval))
    (when-let [newval (:popsize options)] (.setNumKSnipes state newval) (.setNumRSnipes state newval)))
  (reset! commandline nil)) ; clear it so user can set params in the gui

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start main loop

(defn -start
  "Function called to (re)start a new simulation run."
  [^SimConfig this]
  (.superStart this)
  ;; If user passed commandline options, use them to set parameters, rather than defaults:
  (when @commandline (set-sim-config-data-from-commandline! this commandline))
  ;; Construct core data structures of the simulation:
  (let [^Schedule schedule (.schedule this)
        ^SimConfigData istate (.simConfigData this)]
    ;; Schedule per-tick step function(s):
    (.scheduleRepeating schedule Schedule/EPOCH 0
                        (reify Steppable 
                          (step [this sim-state]
                            (let [^SimConfig state sim-state]
                              ;(doseq [^Indiv indiv population] (copy-relig! indiv state))      ; first communicate relig (to newrelig's)
                              ;(doseq [^Indiv indiv population] (update-relig! indiv))        ; then copy newrelig to relig ("parallel" update)
                              ;(doseq [^Indiv indiv population] (update-success! indiv state))  ; update each indiv's success field (uses relig)
                              ;(let [[this-step & rest-steps] @pop-steps]
                              ;  (reset! pop-steps rest-steps)
                              ;  (ui/display-step this-step))
                              ;(collect-data state)
                              )))))
  ;(report-run-params this)
  )
