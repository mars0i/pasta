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

; (do
;  (clojure.core/ns free-agent.config-data)
;  (clojure.core/defrecord
;   SimConfigData
;   [initial-energy
;    k-snipe-prior
;    r-snipe-low-prior
;    r-snipe-high-prior
;    num-k-snipes
;    num-r-snipes
;    mush-prob
;    mush-low-mean
;    mush-high-mean
;    mush-sd
;    mush-pos-nutrition
;    mush-neg-nutrition
;    env-width
;    env-height
;    env-display-size
;    env-center
;    popenv])
;  (clojure.core/ns
;   free-agent.SimConfig
;   (:require [free-agent.config-data])
;   (:import free-agent.SimConfig)
;   (:gen-class
;    :name
;    free-agent.SimConfig
;    :extends
;    sim.engine.SimState
;    :state
;    simConfigData
;    :exposes-methods
;    {start superStart}
;    :init
;    init-sim-config-data
;    :main
;    true
;    :methods
;    [[getInitialEnergy [] double]
;     [setInitialEnergy [double] void]
;     [getKSnipePrior [] double]
;     [setKSnipePrior [double] void]
;     [getRSnipeLowPrior [] double]
;     [setRSnipeLowPrior [double] void]
;     [getRSnipeHighPrior [] double]
;     [setRSnipeHighPrior [double] void]
;     [getNumKSnipes [] long]
;     [setNumKSnipes [long] void]
;     [getNumRSnipes [] long]
;     [setNumRSnipes [long] void]
;     [getMushProb [] double]
;     [setMushProb [double] void]
;     [getMushLowMean [] double]
;     [setMushLowMean [double] void]
;     [getMushHighMean [] double]
;     [setMushHighMean [double] void]
;     [getMushSd [] double]
;     [setMushSd [double] void]
;     [getMushPosNutrition [] double]
;     [setMushPosNutrition [double] void]
;     [getMushNegNutrition [] double]
;     [setMushNegNutrition [double] void]
;     [domInitialEnergy [] java.lang.Object]
;     [domKSnipePrior [] java.lang.Object]
;     [domRSnipeLowPrior [] java.lang.Object]
;     [domRSnipeHighPrior [] java.lang.Object]
;     [domNumKSnipes [] java.lang.Object]
;     [domNumRSnipes [] java.lang.Object]
;     [domMushProb [] java.lang.Object]
;     [domMushPosNutrition [] java.lang.Object]
;     [domMushNegNutrition [] java.lang.Object]]))
;  (clojure.core/defn
;   -init-sim-config-data
;   [seed]
;   [[seed] (clojure.core/atom (free-agent.config-data/->SimConfigData 10.0 10.0 5.0 20.0 50 50 0.1 4.0 16.0 2.0 1.0 -0.1 80 40 12.0 nil nil))])
;  (defn -getInitialEnergy [this] (:initial-energy @(.simConfigData this)))
;  (defn -getKSnipePrior [this] (:k-snipe-prior @(.simConfigData this)))
;  (defn -getRSnipeLowPrior [this] (:r-snipe-low-prior @(.simConfigData this)))
;  (defn -getRSnipeHighPrior [this] (:r-snipe-high-prior @(.simConfigData this)))
;  (defn -getNumKSnipes [this] (:num-k-snipes @(.simConfigData this)))
;  (defn -getNumRSnipes [this] (:num-r-snipes @(.simConfigData this)))
;  (defn -getMushProb [this] (:mush-prob @(.simConfigData this)))
;  (defn -getMushLowMean [this] (:mush-low-mean @(.simConfigData this)))
;  (defn -getMushHighMean [this] (:mush-high-mean @(.simConfigData this)))
;  (defn -getMushSd [this] (:mush-sd @(.simConfigData this)))
;  (defn -getMushPosNutrition [this] (:mush-pos-nutrition @(.simConfigData this)))
;  (defn -getMushNegNutrition [this] (:mush-neg-nutrition @(.simConfigData this)))
;  (defn -setInitialEnergy [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :initial-energy newval))
;  (defn -setKSnipePrior [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :k-snipe-prior newval))
;  (defn -setRSnipeLowPrior [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :r-snipe-low-prior newval))
;  (defn -setRSnipeHighPrior [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :r-snipe-high-prior newval))
;  (defn -setNumKSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-k-snipes newval))
;  (defn -setNumRSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-r-snipes newval))
;  (defn -setMushProb [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-prob newval))
;  (defn -setMushLowMean [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-low-mean newval))
;  (defn -setMushHighMean [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-high-mean newval))
;  (defn -setMushSd [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-sd newval))
;  (defn -setMushPosNutrition [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-pos-nutrition newval))
;  (defn -setMushNegNutrition [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-neg-nutrition newval))
;  (defn -domInitialEnergy [this] (Interval. 0.0 20.0))
;  (defn -domKSnipePrior [this] (Interval. 1.0 50.0))
;  (defn -domRSnipeLowPrior [this] (Interval. 1.0 50.0))
;  (defn -domRSnipeHighPrior [this] (Interval. 1.0 50.0))
;  (defn -domNumKSnipes [this] (Interval. 1 500))
;  (defn -domNumRSnipes [this] (Interval. 1 500))
;  (defn -domMushProb [this] (Interval. 0.0 1.0))
;  (defn -domMushPosNutrition [this] (Interval. 0.0 20.0))
;  (defn -domMushNegNutrition [this] (Interval. -10.0 0.0))
;  (clojure.core/defn
;   record-commandline-args!
;   "Temporarily store values of parameters passed on the command line."
;   [args__8847__auto__]
;   (clojure.core/let
;    [cli-options
;     [["-?" "--help" "Print this help message."]
;      ["-e" "--initial-energy <double>" "Initial energy for each snipe" :parse-fn (fn* [p1__1332#] (Double. p1__1332#))]
;      ["-k" "--k-snipe-prior <double>" "Prior for k-snipes" :parse-fn (fn* [p1__1333#] (Double. p1__1333#))]
;      ["-q" "--r-snipe-low-prior <double>" "One of two possible priors for r-snipes" :parse-fn (fn* [p1__1334#] (Double. p1__1334#))]
;      ["-r" "--r-snipe-high-prior <double>" "One of two possible priors for r-snipes" :parse-fn (fn* [p1__1335#] (Double. p1__1335#))]
;      ["-N" "--num-k-snipes <long>" "Size of k-snipe subpopulation" :parse-fn (fn* [p1__1336#] (Long. p1__1336#))]
;      ["-o" "--num-r-snipes <long>" "Size of r-snipe subpopulation" :parse-fn (fn* [p1__1337#] (Long. p1__1337#))]
;      ["-p" "--mush-prob <double>" "Average frequency of mushrooms." :parse-fn (fn* [p1__1338#] (Double. p1__1338#))]
;      ["-l" "--mush-low-mean <double>" "Mean of mushroom light distribution" :parse-fn (fn* [p1__1339#] (Double. p1__1339#))]
;      ["-h" "--mush-high-mean <double>" "Mean of mushroom light distribution" :parse-fn (fn* [p1__1340#] (Double. p1__1340#))]
;      ["-s" "--mush-sd <double>" "Standard deviation of mushroom light distribution" :parse-fn (fn* [p1__1341#] (Double. p1__1341#))]
;      ["-m" "--mush-pos-nutrition <double>" "Energy from eating a nutritious mushroom" :parse-fn (fn* [p1__1342#] (Double. p1__1342#))]
;      ["-n" "--mush-neg-nutrition <double>" "Energy from eating a nutritious mushroom" :parse-fn (fn* [p1__1343#] (Double. p1__1343#))]
;      ["-w" "--env-width <long>" "How wide is env?  Must be an even number." :parse-fn (fn* [p1__1344#] (Long. p1__1344#))]
;      ["-t" "--env-height <long>" "How tall is env? Should be an even number." :parse-fn (fn* [p1__1345#] (Long. p1__1345#))]
;      ["-d" "--env-display-size <double>" "How large to disthe env in gui by default." :parse-fn (fn* [p1__1346#] (Double. p1__1346#))]]
;     usage-fmt__8848__auto__
;     (clojure.core/fn
;      [options]
;      (clojure.core/let
;       [fmt-line (clojure.core/fn [[short-opt long-opt desc]] (clojure.core/str short-opt ", " long-opt ": " desc))]
;       (clojure.string/join "\n" (clojure.core/concat (clojure.core/map fmt-line options)))))
;     {:as cmdline, :keys [options arguments errors summary]}
;     (clojure.tools.cli/parse-opts args__8847__auto__ cli-options)]
;    (clojure.core/reset! commandline cmdline)
;    (clojure.core/when
;     (:help options)
;     (clojure.core/println "Command line options for free-agent:")
;     (clojure.core/println (usage-fmt__8848__auto__ cli-options))
;     (clojure.core/println "free-agent and MASON options can both be used:")
;     (clojure.core/println "-help (note single dash): Print help message for MASON.")
;     (java.lang.System/exit 0)))))

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
