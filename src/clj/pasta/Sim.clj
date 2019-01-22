;; This software is copyright 2016, 2017 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns pasta.Sim
  (:require [clojure.tools.cli]
            [clojure.data.csv :as csv]
            [clojure.java.io]
            [utils.defsim :as defsim]
            [utils.map2csv :as m2c]
            [pasta.snipe :as sn]
            [pasta.popenv :as pe]
            [pasta.stats :as stats])
  (:import [sim.engine Steppable Schedule]
           [sim.util Interval]
           [ec.util MersenneTwisterFast]
           [java.lang String]
           [pasta.popenv PopEnv]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generate Sim class as subclass of SimState using genclass, with an init 
;; function, import statement, and Bean/MASON field accessors.
;; To see what code will be generated, try this in a repl:
;;    (require '[utils.defsim :as defsim])
;;    (pprint (macroexpand-1 '<insert defsim call>))

(def commandline$ (atom nil)) ; Used by record-commandline-args!, which is defined by defsim, and below

;; Note: There is no option below for max number of steps.  Use MASON's -for instead.
;;              field name    initial-value type   in ui? with range?
(defsim/defsim [[num-k-snipes       25      long    [0 500]     ["-K" "Size of k-snipe subpopulation" :parse-fn #(Long. %)]]
                [num-r-snipes       25      long    [0 500]     ["-R" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]]
                [num-s-snipes       25      long    [0 500]     ["-S" "Size of s-snipe subpopulation" :parse-fn #(Long. %)]]
                [mush-prob           0.2    double  [0.0 1.0]   ["-M" "Average frequency of mushrooms." :parse-fn #(Double. %)]]
                [mush-low-size       4.0    double  true        ["-s" "Size of small mushrooms (mean of light distribution)" :parse-fn #(Double. %)]]
                [mush-high-size      6.0    double  true        ["-l" "Size of large mushrooms (mean of light distribution)" :parse-fn #(Double. %)]]
                [mush-sd             2.0    double  true        ["-v" "Standard deviation of mushroom light distribution" :parse-fn #(Double. %)]]
                [mush-mid-size       0      double  false] ; calculated from the previous values
                [mush-size-scale     0      double  false] ; calculated from the previous values
                [mush-pos-nutrition  1.0    double  [0.0 20.0]  ["-p" "Energy from eating a nutritious mushroom" :parse-fn #(Double. %)]]
                [mush-neg-nutrition -1.0    double  [-20.0 0.0] ["-n" "Energy from eating a poisonous mushroom" :parse-fn #(Double. %)]]
                [initial-energy     10.0    double  [0.0 50.0]  ["-e" "Initial energy for each snipe" :parse-fn #(Double. %)]]
                [birth-threshold    20.0    double  [1.0 50.0]  ["-b" "Energy level at which birth takes place" :parse-fn #(Double. %)]]
                [k-pref-noise-sd     0.0625 double  true        ["-a" "Standard deviation of internal noise in k-snipe preference determination." :parse-fn #(Double. %)]]
                [birth-cost          5.0    double  [0.0 10.0]  ["-o" "Energetic cost of giving birth to one offspring" :parse-fn #(Double. %)]]
                [max-energy         30.0    double  [1.0 100.0] ["-m" "Max energy that a snipe can have." :parse-fn #(Double. %)]]
                [carrying-proportion 0.25   double  [0.1 0.9]   ["-c" "Snipes are randomly culled when number exceed this times # of cells." :parse-fn #(Double. %)]]
                [neighbor-radius     5      long    [1 10]      ["-r" "s-snipe neighbors (for copying) are no more than this distance away." :parse-fn #(Long. %)]]
                [env-width          40      long    [10 250]    ["-W" "Width of env.  Must be an even number." :parse-fn #(Long. %)]] ; Haven't figured out how to change 
                [env-height         40      long    [10 250]    ["-H" "Height of env. Must be an even number." :parse-fn #(Long. %)]] ;  within app without distortion
                [env-display-size   12.0    double  false       ["-D" "How large to display the env in gui by default." :parse-fn #(Double. %)]]
                [use-gui           false    boolean false       ["-g" "If -g, use GUI; otherwise use GUI if and only if +g or there are no commandline options." :parse-fn #(Boolean. %)]]
                [extreme-pref        1.0    double  true        ["-x" "Absolute value of r-snipe preferences." :parse-fn #(Double. %)]]
                [report-every        0      double  true        ["-i" "Report basic stats every i ticks after the first one (0 = never); format depends on -w." :parse-fn #(Double. %)]]
                [write-csv         false    boolean false       ["-w" "Write data to file instead of printing it to console." :parse-fn #(Boolean. %)]]
                [csv-basename       nil java.lang.String false  ["-F" "Base name of files to append data to.  Otherwise new filenames generated from seed." :parse-fn #(String. %)]]
                [csv-writer         nil java.io.BufferedWriter false]
                [max-pop-size        0      long    false]
                [seed               nil     long    false] ; convenience field to store Sim's seed
		[in-gui           false     boolean false] ; convenience field to store Boolean re whether in GUI
                [popenv             nil  pasta.popenv.PopEnv false]]
  :exposes-methods {finish superFinish} ; name for function to call finish() in the superclass
  :methods [[getPopSize [] long] ; additional options here. this one is for def below; it will get merged into the generated :methods component.
            [getKSnipeFreq [] double]
            [getRSnipeFreq [] double]
            [getSSnipeFreq [] double]])

(defn curr-step [sim] (.getSteps (.schedule sim)))
(defn curr-popenv [sim] (:popenv @(.simData sim)))
;; NOTE these get called on every tick in GUI even if not reported:
(defn -getPopSize    [^Sim this] (stats/get-pop-size @(.simData this)))
(defn -getKSnipeFreq [^Sim this] (stats/maybe-get-freq-for-gui (curr-step this) :k-snipe (curr-popenv this)))
(defn -getRSnipeFreq [^Sim this] (stats/maybe-get-freq-for-gui (curr-step this) :r-snipe (curr-popenv this)))
(defn -getSSnipeFreq [^Sim this] (stats/maybe-get-freq-for-gui (curr-step this) :s-snipe (curr-popenv this)))

;; no good reason to put this into the defsim macro since it doesn't include any
;; field-specific code.  Easier to redefine if left here.
(defn set-sim-data-from-commandline!
  "Set fields in the Sim's simData from parameters passed on the command line."
  [^Sim sim cmdline$]
  (let [options (:options @cmdline$)
        sim-data (.simData sim)]
    (run! #(apply swap! sim-data assoc %) ; arg is a MapEntry, which is sequential? so will function like a list or vector
          options)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  [& args]
  (sim.engine.SimState/doLoop pasta.Sim (into-array String args))
  (System/exit 0))

(defn mein
  "Externally available wrapper for -main."
  [args]
  (apply -main args)) ; have to use apply since already in a seq

;; Is this ever called??
(defn -stop
  [^Sim this]
  (let [^SimData sim-data$ (.simData this)
        writer (:csv-writer @sim-data$)]
    (when writer
      (.close writer)
      (swap! sim-data$ :csv-writer nil))))

(defn cleanup
  [^Sim this]
  (let [^SimData sim-data$ (.simData this)
        sim-data @sim-data$
        stoppable (:stoppable sim-data)
        seed (:seed sim-data)
        report-every (:report-every sim-data)
        ^Schedule schedule (.schedule this)
	steps (.getSteps schedule)]
    (.stop stoppable)
    (when (pos? report-every)
      (stats/report-stats sim-data seed steps)
      (when (not (:write-csv sim-data))
        (stats/write-params-to-console sim-data)))
    (when-let [writer (:csv-writer sim-data)]
      (.close writer))))

;; This should not call the corresponding function in the superclass; that
;; function will call this one.  So if you want to call this function
;; explicitly, you may want to do so by calling superFinish, which 
;; should be defined in the defsim statement above using :exposes-methods.
;; However, if you always use MASON capabilities to end simulations (e.g.
;; using -for or -until on the command line), you don't need to call
;; superFinish, and this function here will automatically get called.
;; (I think that line 662 of SimState.java might be where this happens.)
(defn -finish
  [^Sim this]
  (cleanup this))

;; Note finish is never called here.  Stopping a simulation in any
;; normal MASON way will result in finish() above being called.
(defn run-sim
  [sim-sim rng sim-data$ seed]
  (let [^Schedule schedule (.schedule sim-sim)
        report-every (:report-every @sim-data$)
        max-ticks (:max-ticks @sim-data$)
        ;; This runs the simulation:
        stoppable (.scheduleRepeating schedule Schedule/EPOCH 0 ; epoch = starting at beginning, 0 means run this first during timestep
                                      (reify Steppable 
                                        (step [this sim-state]
                                          (swap! sim-data$ update :popenv pe/next-popenv rng sim-data$))))]
    (swap! sim-data$ assoc :stoppable stoppable) ; make it available to finish()
    ;; maybe report stats periodically
    (when (pos? report-every)
      (.scheduleRepeating schedule report-every 1 ; first tick to report at; ordering within tick
                          (reify Steppable
                            (step [this sim-state]
                              (let [steps (.getSteps schedule)]
                                  (stats/report-stats @sim-data$ seed steps))))
                          report-every))))

(def first-run-shared-basename$ (atom true)) ; when different runs share a basename, some things happen once

(defn -start
  "Function that's called to (re)start a new simulation run."
  [^Sim this]
  (.superStart this)
  ;; Construct core data structures of the simulation:
  (let [^SimData sim-data$ (.simData this)
        ^MersenneTwisterFast rng (.-random this)
        seed (.seed this)]
  ;; If user passed commandline options, use them to set parameters, rather than defaults:
    (when (and @commandline$ (not (:in-gui @sim-data$))) ; see issue #56 in github for the logic here
      (set-sim-data-from-commandline! this commandline$))
    (swap! sim-data$ assoc :seed seed)
    (pe/setup-popenv-config! sim-data$)
    (swap! sim-data$ assoc :popenv (pe/make-popenv rng sim-data$)) ; create new popenv
    ;; Run it:
    (when-let [write-csv (:write-csv @sim-data$)]
      (let [initial-basename (:csv-basename @sim-data$) ; might be nil
            basename (or initial-basename (str "pasta" seed))
            data-filename (str basename ".csv")
            header-filename (str basename "_header.csv")
            add-to-file? (.exists (clojure.java.io/file data-filename)) ; should we create new file, or add to an older one?
            writer (clojure.java.io/writer data-filename :append add-to-file?)]
        (swap! sim-data$ assoc :csv-writer writer) ; store handle
	(if initial-basename                 ; if we do have a shared basename
	  (when @first-run-shared-basename$  ; write parameters only during the first run when there is a shared basename
	    (reset! first-run-shared-basename$ false)
	    (stats/write-params-to-file @sim-data$))
	  (stats/write-params-to-file @sim-data$)) ; if no shared basename, every run gets its own params file
        (when-not add-to-file?  ; if not adding to existing file, write a separate header file (whether old file is from prev run or earlier process)
	  (m2c/spit-csv header-filename [stats/csv-header]))))
    (run-sim this rng sim-data$ seed)))
