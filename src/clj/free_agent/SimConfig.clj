(ns free-agent.SimConfig
  (:require [clojure.tools.cli]
            [utils.defsimconfig :as defcfg]
            [free-agent.popenv :as pe])
  (:import [sim.engine Steppable Schedule]
           [sim.util Interval]
           [ec.util MersenneTwisterFast]
           [java.lang String]
           [free-agent.popenv.PopEnv]))
;; import free-agent.SimConfig separately below
;; (if done here, fails when aot-compiling from a clean project)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generate SimConfig class as subclass of SimState using genclass, with an init 
;; function, import statement, and Bean/MASON field accessors.
;; To see what code will be generated, try this in a repl:
;;    (require '[utils.defsimconfig :as cfg])
;;    (pprint (macroexpand-1 '<insert defsimconfig call>))

(def commandline (atom nil)) ; Needed by defsimconfig and other code below if we're defining commandline options

;;                 field name      initial-value type  in ui? with range?
(defcfg/defsimconfig [[initial-energy     10.0  double [0.0 20.0]  ["-e" "Initial energy for each snipe" :parse-fn #(Double. %)]]
                      [k-snipe-prior      10.0  double [1.0 50.0]  ["-k" "Prior for k-snipes" :parse-fn #(Double. %)]]
                      [r-snipe-low-prior   5.0  double [1.0 50.0]  ["-q" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]]
                      [r-snipe-high-prior 20.0  double [1.0 50.0]  ["-r" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]]
                      [num-k-snipes       50    long   [1 500]     ["-N" "Size of k-snipe subpopulation" :parse-fn #(Long. %)]]
                      [num-r-snipes       50    long   [1 500]     ["-o" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]]
                      [mush-prob           0.1  double [0.0 1.0]   ["-p" "Average frequency of mushrooms." :parse-fn #(Double. %)]]
                      [mush-low-mean       4.0  double true        ["-l" "Mean of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-high-mean     16.0  double true        ["-h" "Mean of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-sd             2.0  double true        ["-s" "Standard deviation of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-pos-nutrition  1.0  double [0.0 20.0]  ["-m" "Energy from eating a nutritious mushroom" :parse-fn #(Double. %)]]
                      [mush-neg-nutrition -0.1  double [-10.0 0.0] ["-n" "Energy from eating a nutritious mushroom" :parse-fn #(Double. %)]]
                      [env-width          80    long   false       ["-w" "How wide is env?  Must be an even number." :parse-fn #(Long. %)]] ; can be set from command line but not in running app
                      [env-height         40    long   false       ["-t" "How tall is env? Should be an even number." :parse-fn #(Long. %)]] ; ditto
                      [env-display-size   12.0  double false       ["-d" "How large to display the env in gui by default." :parse-fn #(Double. %)]]
                      [env-center         nil   double false]
                      [popenv             nil   free-agent.popenv.PopEnv false]])

;; no good reason to put this into the defsimconfig macro since it doesn't include any
;; field-specific code.  Easier to redefine if left here.  Note though that commandline
(defn set-sim-config-data-from-commandline!
  "Set fields in the SimConfig's simConfigData from parameters passed on the command line."
  [^SimConfig sim-config cmdline]
  (let [options (:options @cmdline)
        sim-config-data (.simConfigData sim-config)]
    (run! #(apply swap! sim-config-data assoc %) ; arg is a MapEntry, which is sequential? so will function like a list or vector
          options))
  (reset! cmdline nil)) ; clear it so user can set params in the gui

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  [& args]
  (record-commandline-args! args) ; The SimConfig isn't available yet, so store commandline args for later access by start().
  (sim.engine.SimState/doLoop free-agent.SimConfig (into-array String args)) ;; FIXME RUNTIME EXCEPTION HERE
  (System/exit 0))

(defn -start
  "Function that's called to (re)start a new simulation run."
  [^SimConfig this]
  (.superStart this)
  ;; If user passed commandline options, use them to set parameters, rather than defaults:
  (when @commandline (set-sim-config-data-from-commandline! this commandline))
  ;; Construct core data structures of the simulation:
  (let [^Schedule schedule (.schedule this)
        ^SimConfigData cfg-data$ (.simConfigData this)
        ^MersenneTwisterFast rng (.-random this)]
    ;; create and populate initial popenv:
    (swap! cfg-data$ assoc :env-center (/ (:env-width @cfg-data$) 2.0))
    (swap! cfg-data$ assoc :popenv (pe/populate rng @cfg-data$ ; it's ok to pass in cfg-data to update cfg-data; make-popenv will use the old version
                                                (pe/make-popenv rng @cfg-data$)))
    ;; Run it:
    (.scheduleRepeating schedule Schedule/EPOCH 0
                        (reify Steppable 
                          (step [this sim-state]
                            (let [^SimConfig state sim-state]
                              (swap! cfg-data$ update-in [:popenv] pe/next-popenv rng @cfg-data$))))))) ; i.e. call next-popenv with old popenv and cfg-data, and replace popenv in cfg-data
