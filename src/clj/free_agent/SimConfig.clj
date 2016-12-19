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

;;                 field name   initial value  type  in ui? with range?
(defcfg/defsimconfig [[initial-energy    10.0   double [0.0 20.0] ["-e" "Initial energy for each snipe" :parse-fn #(Double. %)]]
                      [k-snipe-prior     10.0   double [1.0 50.0] ["-k" "Prior for k-snipes" :parse-fn #(Double. %)]]
                      [r-snipe-prior-0    5.0   double [1.0 50.0] ["-q" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]]
                      [r-snipe-prior-1   20.0   double [1.0 50.0] ["-r" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]]
                      [num-k-snipes      30     long   [1 200]    ["-N" "Size of k-snipe subpopulation" :parse-fn #(Long. %)]]
                      [num-r-snipes      30     long   [1 200]    ["-o" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]]
                      [mushroom-prob      0.005 double [0.0 1.0]  ["-p" "Probability that a mushroom will appear on a given patch." :parse-fn #(Double. %)]]
                      [mushroom-mean-0    4.0   double true       ["-m" "Mean of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mushroom-mean-1   16.0   double true       ["-n" "Mean of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mushroom-sd        2.0   double true       ["-s" "Standard deviation of mushroom light distribution" :parse-fn #(Double. %)]]
                      [world-width      250     double false      ["-w" "How wide is world?" :parse-fn #(Long. %)]] ; can be set from command line but not in running app
                      [world-height     250     double false      ["-h" "How tall is world?" :parse-fn #(Long. %)]] ; ditto
                      [popenv            nil    free-agent.popenv.PopEnv false]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Command line start up

(def commandline (atom nil))

;(defn record-commandline-args!
;  "Temporarily store values of parameters passed on the command line."
;  [args]
;  ;; These options should not conflict with MASON's.  Example: If "-h" is the single-char help option, doLoop will never see "-help" (although "-t n" doesn't conflict with "-time") (??).
;  (let [cli-options [["-?" "--help" "Print this help message."]
;                     ["-w" "--world-width <width>" "How wide is world?" :parse-fn #(Long. %)]
;                     ["-h" "--world-height <height>" "How tall is world?" :parse-fn #(Long. %)]
;                     ["-e" "--initial-energy <energy>" "Initial energy for each snipe" :parse-fn #(Double. %)]
;                     ["-k" "--k-snipe-prior <prior>" "Prior for k-snipes" :parse-fn #(Double. %)]
;                     ["-p" "--r-snipe-prior-0 <prio>" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]
;                     ["-q" "--r-snipe-prior-1 <prior>" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]
;                     ["-n" "--num-k-snipes <size>" "Size of k-snipe subpopulation" :parse-fn #(Long. %)]
;                     ["-o" "--num-r-snipes <size>" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]
;                     ["-m" "--mushroom-prob <probability>" "Probability that a mushroom will appear on a given patch." :parse-fn #(Double. %)]]
;        usage-fmt (fn [options]
;                    (let [fmt-line (fn [[short-opt long-opt desc]] (str short-opt ", " long-opt ": " desc))]
;                      (clojure.string/join "\n" (concat (map fmt-line options)))))
;        {:keys [options arguments errors summary] :as cmdline} (clojure.tools.cli/parse-opts args cli-options)]
;    (reset! commandline cmdline)
;    (when (:help options)
;      (println "Command line options for free-agent:")
;      (println (usage-fmt cli-options))
;      (println "free-agent and MASON options can both be used:")
;      (println "-help (note single dash): Print help message for MASON.")
;      (System/exit 0))))


(defn set-sim-config-data-from-commandline!
  "Set fields in the SimConfig's simConfigData from parameters passed on the command line."
  [^SimConfig sim-config cmdline]
  (let [options (:options @cmdline)
        sim-config-data (.simConfigData sim-config)]
    (run! #(apply swap! sim-config-data assoc %) ; arg is a MapEntry, which is sequential? so will function like a list or vector
          options)))

;(defn set-sim-config-data-from-commandline!
;  "Set fields in the SimConfig's simConfigData from parameters passed on the command line."
;  [^SimConfig state cmdline]
;  (let [{:keys [options arguments errors summary]} @cmdline]
;    ;; FIXME
;    (when-let [newval (:initial-energy options)] (.setInitialEnergy state newval))
;    (when-let [newval (:num-k-snipes options)] (.setNumKSnipes state newval))
;    (when-let [newval (:num-r-snipes options)] (.setNumRSnipes state newval)))
;  (reset! commandline nil)) ; clear it so user can set params in the gui

(defn -main
  [& args]
  (record-commandline-args! args) ; The SimConfig isn't available yet, so store commandline args for later access by start().
  (sim.engine.SimState/doLoop free-agent.SimConfig (into-array String args)) ;; FIXME RUNTIME EXCEPTION HERE
  (System/exit 0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start main loop

(defn -start
  "Function that's called to (re)start a new simulation run."
  [^SimConfig this]
  (.superStart this)
  ;; If user passed commandline options, use them to set parameters, rather than defaults:
  (when @commandline 
    (set-sim-config-data-from-commandline! this commandline)
    (reset! commandline nil)) ; clear it so user can set params in the gui
  ;; Construct core data structures of the simulation:
  (let [^Schedule schedule (.schedule this)
        ^SimConfigData cfg-data$ (.simConfigData this)
        ^MersenneTwisterFast rng (.-random this)]
    ;; create and populate initial popenv:
    (swap! cfg-data$ assoc :popenv (pe/populate rng @cfg-data$ ; it's ok to pass in cfg-data to update cfg-data; make-popenv will use the old version
                                                (pe/make-popenv rng @cfg-data$)))
    ;; Run it:
    (.scheduleRepeating schedule Schedule/EPOCH 0
                        (reify Steppable 
                          (step [this sim-state]
                            (let [^SimConfig state sim-state]
                              (swap! cfg-data$ update-in [:popenv] pe/next-popenv @cfg-data$))))))) ; i.e. call next-popenv with old popenv and cfg-data, and replace popenv in cfg-data
