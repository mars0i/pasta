(ns free-agent.SimConfig
  (:require [clojure.tools.cli]
            [utils.defsimconfig :as cfg])
  (:import [sim.engine Steppable Schedule]
           [sim.util Interval]
           [ec.util MersenneTwisterFast]
           [java.lang String]))
;; import free-agent.SimConfig separately below
;; (if done here, fails when aot-compiling from a clean project)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generate SimConfig class as subclass of SimState using genclass, with an init 
;; function, import statement, and Bean/MASON field accessors.
;; To see what code will be generated, try this in a repl:
;;    (require '[utils.defsimconfig :as cfg])
;;    (pprint (macroexpand-1 '<insert defsimconfig call>))

;;                 field name   initial value   type   optional default range
(cfg/defsimconfig [[initial-energy   10.0       double [0.0 20.0]]
                   [k-snipe-prior    10.0       double [1.0 50.0]]
                   [r-snipe-prior-0   5.0       double [1.0 50.0]]
                   [r-snipe-prior-1  20.0       double [1.0 50.0]]
                   [num-k-snipes     20         long   [1 200]]
                   [num-r-snipes     20         long   [1 200]]])

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
  (sim.engine.SimState/doLoop free-agent.SimConfig (into-array String args)) ;; FIXME RUNTIME EXCEPTION HERE
  (System/exit 0))

(defn set-sim-config-data-from-commandline!
  "Set fields in the SimConfig's simConfigData from parameters passed on the command line."
  [^SimConfig state cmdline]
  (let [{:keys [options arguments errors summary]} @cmdline]
    (when-let [newval (:energy options)] (.setInitialEnergy state newval))
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
        ^SimConfigData istate (.simConfigData this)
        ;; make snipes here
        ;; and make field/fields for them to move on
        ;; add mushrooms to field(s)
        ]
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
