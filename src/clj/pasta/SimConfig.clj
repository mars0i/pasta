;; This software is copyright 2016, 2017 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns pasta.SimConfig
  (:require [clojure.tools.cli]
            [clojure.data.csv :as csv]
            [clojure.java.io]
            [utils.defsimconfig :as defcfg]
            [pasta.snipe :as sn]
            [pasta.popenv :as pe]
            [pasta.stats :as stats])
  (:import [sim.engine Steppable Schedule]
           [sim.util Interval]
           [ec.util MersenneTwisterFast]
           [java.lang String]
           [pasta.popenv.PopEnv]))
;; import pasta.SimConfig separately below
;; (if done here, fails when aot-compiling from a clean project)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generate SimConfig class as subclass of SimState using genclass, with an init 
;; function, import statement, and Bean/MASON field accessors.
;; To see what code will be generated, try this in a repl:
;;    (require '[utils.defsimconfig :as defcfg])
;;    (pprint (macroexpand-1 '<insert defsimconfig call>))

(def commandline$ (atom nil)) ; Needed by defsimconfig and other code below if we're defining commandline options

;;                 field name      initial-value type  in ui? with range?
(defcfg/defsimconfig [[num-k-snipes       25    long    [0 500]     ["-K" "Size of k-snipe subpopulation" :parse-fn #(Long. %)]]
                      [num-r-snipes       25    long    [0 500]     ["-R" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]]
                      [num-s-snipes       25    long    [0 500]     ["-S" "Size of s-snipe subpopulation" :parse-fn #(Long. %)]]
                      [mush-prob           0.2  double  [0.0 1.0]   ["-M" "Average frequency of mushrooms." :parse-fn #(Double. %)]]
                      [mush-low-size       4.0  double  true        ["-s" "Size of small mushrooms (mean of light distribution)" :parse-fn #(Double. %)]]
                      [mush-high-size      6.0  double  true        ["-l" "Size of large mushrooms (mean of light distribution)" :parse-fn #(Double. %)]]
                      [mush-sd             2.0  double  true        ["-v" "Standard deviation of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-mid-size       0    double  false] ; calculated from the previous values
                      [mush-size-scale     0    double  false] ; calculated from the previous values
                      [mush-pos-nutrition  1.0  double  [0.0 20.0]  ["-p" "Energy from eating a nutritious mushroom" :parse-fn #(Double. %)]]
                      [mush-neg-nutrition -1.0  double  [-20.0 0.0] ["-n" "Energy from eating a poisonous mushroom" :parse-fn #(Double. %)]]
                      [initial-energy     10.0  double  [0.0 50.0]  ["-e" "Initial energy for each snipe" :parse-fn #(Double. %)]]
                      [birth-threshold    20.0  double  [1.0 50.0]  ["-b" "Energy level at which birth takes place" :parse-fn #(Double. %)]]
                      [birth-cost          5.0  double  [0.0 10.0]  ["-o" "Energetic cost of giving birth to one offspring" :parse-fn #(Double. %)]]
                      [max-energy         30.0  double  [1.0 100.0] ["-m" "Max energy that a snipe can have." :parse-fn #(Double. %)]]
                      [carrying-proportion 0.25 double  [0.1 0.9]   ["-c" "Snipes are randomly culled when number exceed this times # of cells." :parse-fn #(Double. %)]]
                      [neighbor-radius     5    long    [1 10]      ["-r" "s-snipe neighbors are no more than this distance away." :parse-fn #(Long. %)]]
                      [report-every        0    long    true        ["-i" "Report basic stats every i ticks after the first one (0 = never)." :parse-fn #(Long. %)]]
                      [max-ticks           0    long    true        ["-t" "Stop after this number of timesteps have run, or never if 0." :parse-fn #(Long. %)]]
                      [env-width          40    long    [10 250]    ["-W" "Width of env.  Must be an even number." :parse-fn #(Long. %)]] ; Haven't figured out how to change 
                      [env-height         40    long    [10 250]    ["-H" "Height of env. Must be an even number." :parse-fn #(Long. %)]] ;  within app without distortion
                      [env-display-size   12.0  double  false       ["-D" "How large to display the env in gui by default." :parse-fn #(Double. %)]]
                      [use-gui           false  boolean false       ["-g" "If -g, use GUI; otherwise use GUI if and only if +g or there are no commandline options." :parse-fn #(Boolean. %)]]
                      [extreme-pref        1.0  double  true        ["-x" "Absolute value of r-snipe preferences." :parse-fn #(Double. %)]]
                      [write-csv         false  boolean false       ["-w" "Write data to file instead of printing it to console." :parse-fn #(Boolean. %)]]
                      [csv-basename       nil  java.lang.String false ["-f" "Base name of files to append data to.  Otherwise new filenames generated from seed." :parse-fn #(String. %)]]
                      [csv-writer        nil   java.io.BufferedWriter false]
                      [max-pop-size        0    long    false]
                      [seed              nil    long    false] ; convenience field to store SimConfig's seed
                      [popenv            nil   pasta.popenv.PopEnv false]]
  :methods [[getPopSize [] long] ; additional options here. this one is for def below; it will get merged into the generated :methods component.
            [getKSnipeFreq [] double]
            [getRSnipeFreq [] double]
            [getSSnipeFreq [] double]])

(defn curr-step [cfg] (.getSteps (.schedule cfg)))
(defn curr-popenv [cfg] (:popenv @(.simConfigData cfg)))
;; NOTE these get called on every tick in GUI even if not reported:
(defn -getPopSize    [^SimConfig this] (stats/get-pop-size @(.simConfigData this)))
(defn -getKSnipeFreq [^SimConfig this] (stats/maybe-get-freq (curr-step this) :k-snipe (curr-popenv this)))
(defn -getRSnipeFreq [^SimConfig this] (stats/maybe-get-freq (curr-step this) :r-snipe (curr-popenv this)))
(defn -getSSnipeFreq [^SimConfig this] (stats/maybe-get-freq (curr-step this) :s-snipe (curr-popenv this)))

;; no good reason to put this into the defsimconfig macro since it doesn't include any
;; field-specific code.  Easier to redefine if left here.
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
  (sim.engine.SimState/doLoop pasta.SimConfig (into-array String args))
  (System/exit 0))

(defn mein
  "Externally available wrapper for -main."
  [args]
  (apply -main args)) ; have to use apply since already in a seq

(defn -stop
  [^SimConfig this]
  (let [^SimConfigData cfg-data$ (.simConfigData this)
        writer (:csv-writer @cfg-data$)]
    (when writer
      (.close writer)
      (swap! cfg-data$ :csv-writer nil))))

(defn -start
  "Function that's called to (re)start a new simulation run."
  [^SimConfig this]
  (.superStart this)
  ;; If user passed commandline options, use them to set parameters, rather than defaults:
  (when @commandline$ (set-sim-config-data-from-commandline! this commandline$))
  ;; Construct core data structures of the simulation:
  (let [^Schedule schedule (.schedule this)
        ^SimConfigData cfg-data$ (.simConfigData this)
        ^MersenneTwisterFast rng (.-random this)
        seed (.seed this)]
    (swap! cfg-data$ assoc :seed seed)
    (pe/setup-popenv-config! cfg-data$)
    (swap! cfg-data$ assoc :popenv (pe/make-popenv rng cfg-data$)) ; create new popenv
    ;; Run it:
    (let [write-csv (:write-csv @cfg-data$)
          report-every (double (:report-every @cfg-data$))
          max-ticks (:max-ticks @cfg-data$)]
      ;; TODO probably need to wrap this in a try/catch:
      (when write-csv
        (let [basename (or (:csv-basename @cfg-data$) (str "pasta" seed))
              filename (str basename ".csv")
              writer (clojure.java.io/writer filename)] ; open file
          (csv/write-csv writer [stats/csv-header]) ; wrap vector in vector--that's what write-csv wants
          (swap! cfg-data$ assoc :csv-writer writer))) ; store handle
      ;; This runs the simulation:
      (let [stoppable (.scheduleRepeating schedule Schedule/EPOCH 0 ; epoch = starting at beginning, 0 means run this first during timestep
                                          (reify Steppable 
                                            (step [this sim-state]
                                              (swap! cfg-data$ update :popenv (partial pe/next-popenv rng cfg-data$)))))]
        ;; Stop simulation when condition satisfied (TODO will add additional conditions later):
        (.scheduleRepeating schedule Schedule/EPOCH 1 ; 1 = i.e. after main previous Steppable that runs the simulation
                            (reify Steppable
                              (step [this sim-state]
                                (when (pos? max-ticks) ; run forever if max-ticks = 0
                                  (let [steps (.getSteps schedule)]
                                    (when (>= steps max-ticks) ; = s/b enough, but >= as failsafe
                                      (.stop stoppable)
                                      (stats/report-stats @cfg-data$ seed steps)
                                      (when-let [writer (:csv-writer @cfg-data$)]
                                        (.close writer))
                                      (.kill sim-state))))))) ; end program after cleaning up Mason stuff
        ;; maybe report stats periodically
        (when (pos? report-every)
          (.scheduleRepeating schedule report-every 1 ; first tick to report at; ordering within tick
                              (reify Steppable
                                (step [this sim-state]
                                  (let [steps (.getSteps schedule)]
                                    (when (<  steps max-ticks) ; don't report if this is the last tick
                                      (stats/report-stats @cfg-data$ seed steps)))))
                                report-every)))))) ; repeat this often
