;; Output of free-agent.defsimconfig/defsimconfig as called in 
;; SimConfig.clj in commit 7eaa683 (3/4/2017).  In the next commit
;; I added apostrophes before the instances of java.lang.Object
;; that begin appearing at line 59.  I produced this by running
;; pprint on output of macroexpand-1 on the defsimconfig call, and
;; then doing a little bit more pretty-printing clean up by hand.
;; Note: This code generates errors in Clojure 1.9.0-alpha14.  I 
;; produced the code below using Clojure 1.8.0.  The addition of
;; the apostrophes in the next commit allowed it to compile and run 
;; in 1.9.0-alpha14.

(do
  (clojure.core/ns free-agent.config-data)
  (clojure.core/defrecord
    SimConfigData
    [num-k-snipes num-r-snipes num-s-snipes mush-prob mush-low-size
     mush-high-size mush-sd mush-mid-size mush-size-scale mush-pos-nutrition
     mush-neg-nutrition initial-energy birth-threshold birth-cost max-energy
     car-proportion neighbor-radius report-every max-ticks env-width
     env-height env-display-size extreme-pref max-pop-size popenv])
  (clojure.core/ns free-agent.SimConfig
    (:require [free-agent.config-data])
    (:import free-agent.SimConfig)
    (:gen-class
      :name free-agent.SimConfig
      :extends sim.engine.SimState
      :state simConfigData
      :exposes-methods {start superStart}
      :init init-sim-config-data
      :main
      true
      :methods [[getNumKSnipes [] long]
                [setNumKSnipes [long] void]
                [getNumRSnipes [] long]
                [setNumRSnipes [long] void]
                [getNumSSnipes [] long]
                [setNumSSnipes [long] void]
                [getMushProb [] double]
                [setMushProb [double] void]
                [getMushLowSize [] double]
                [setMushLowSize [double] void]
                [getMushHighSize [] double]
                [setMushHighSize [double] void]
                [getMushSd [] double]
                [setMushSd [double] void]
                [getMushPosNutrition [] double]
                [setMushPosNutrition [double] void]
                [getMushNegNutrition [] double]
                [setMushNegNutrition [double] void]
                [getInitialEnergy [] double]
                [setInitialEnergy [double] void]
                [getBirthThreshold [] double]
                [setBirthThreshold [double] void]
                [getBirthCost [] double]
                [setBirthCost [double] void]
                [getMaxEnergy [] double]
                [setMaxEnergy [double] void]
                [getCarProportion [] double]
                [setCarProportion [double] void]
                [getNeighborRadius [] long]
                [setNeighborRadius [long] void]
                [getReportEvery [] long]
                [setReportEvery [long] void]
                [getEnvDisplaySize [] double]
                [setEnvDisplaySize [double] void]
                [domNumKSnipes [] java.lang.Object]
                [domNumRSnipes [] java.lang.Object]
                [domNumSSnipes [] java.lang.Object]
                [domMushProb [] java.lang.Object]
                [domMushPosNutrition [] java.lang.Object]
                [domMushNegNutrition [] java.lang.Object]
                [domInitialEnergy [] java.lang.Object]
                [domBirthThreshold [] java.lang.Object]
                [domBirthCost [] java.lang.Object]
                [domMaxEnergy [] java.lang.Object]
                [domCarProportion [] java.lang.Object]
                [domNeighborRadius [] java.lang.Object]
                [getPopSize [] long]
                [getKSnipeFreq [] double]
                [getRSnipePrefSmallFreq [] double]
                [getRSnipePrefBigFreq [] double]
                [getSSnipeFreq [] double]]))
  (clojure.core/defn -init-sim-config-data
    [seed]
    [[seed] (clojure.core/atom 
              (free-agent.config-data/->SimConfigData 25 25 25 0.2 4.0 6.0 2.0 0 0 1.0 -1.0 10.0 20.0 5.0 30.0 0.25 5 0 0 40 40 12.0 100.0 0 nil))])
  (defn -getNumKSnipes [this] (:num-k-snipes @(.simConfigData this)))
  (defn -getNumRSnipes [this] (:num-r-snipes @(.simConfigData this)))
  (defn -getNumSSnipes [this] (:num-s-snipes @(.simConfigData this)))
  (defn -getMushProb [this] (:mush-prob @(.simConfigData this)))
  (defn -getMushLowSize [this] (:mush-low-size @(.simConfigData this)))
  (defn -getMushHighSize [this] (:mush-high-size @(.simConfigData this)))
  (defn -getMushSd [this] (:mush-sd @(.simConfigData this)))
  (defn -getMushPosNutrition [this] (:mush-pos-nutrition @(.simConfigData this)))
  (defn -getMushNegNutrition [this] (:mush-neg-nutrition @(.simConfigData this)))
  (defn -getInitialEnergy [this] (:initial-energy @(.simConfigData this)))
  (defn -getBirthThreshold [this] (:birth-threshold @(.simConfigData this)))
  (defn -getBirthCost [this] (:birth-cost @(.simConfigData this)))
  (defn -getMaxEnergy [this] (:max-energy @(.simConfigData this)))
  (defn -getCarProportion [this] (:car-proportion @(.simConfigData this)))
  (defn -getNeighborRadius [this] (:neighbor-radius @(.simConfigData this)))
  (defn -getReportEvery [this] (:report-every @(.simConfigData this)))
  (defn -getEnvDisplaySize [this] (:env-display-size @(.simConfigData this)))
  (defn -setNumKSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-k-snipes newval))
  (defn -setNumRSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-r-snipes newval))
  (defn -setNumSSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-s-snipes newval))
  (defn -setMushProb [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-prob newval))
  (defn -setMushLowSize [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-low-size newval))
  (defn -setMushHighSize [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-high-size newval))
  (defn -setMushSd [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-sd newval))
  (defn -setMushPosNutrition [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-pos-nutrition newval))
  (defn -setMushNegNutrition [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-neg-nutrition newval))
(defn -setInitialEnergy [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :initial-energy newval))
(defn -setBirthThreshold [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :birth-threshold newval))
(defn -setBirthCost [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :birth-cost newval))
(defn -setMaxEnergy [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :max-energy newval))
(defn -setCarProportion [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :car-proportion newval))
(defn -setNeighborRadius [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :neighbor-radius newval))
(defn -setReportEvery [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :report-every newval))
(defn -setEnvDisplaySize [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :env-display-size newval))
(defn -domNumKSnipes [this] (Interval. 0 500))
(defn -domNumRSnipes [this] (Interval. 0 500))
(defn -domNumSSnipes [this] (Interval. 0 500))
(defn -domMushProb [this] (Interval. 0.0 1.0))
(defn -domMushPosNutrition [this] (Interval. 0.0 20.0))
(defn -domMushNegNutrition [this] (Interval. -20.0 0.0))
(defn -domInitialEnergy [this] (Interval. 0.0 50.0))
(defn -domBirthThreshold [this] (Interval. 1.0 50.0))
(defn -domBirthCost [this] (Interval. 0.0 10.0))
(defn -domMaxEnergy [this] (Interval. 1.0 100.0))
(defn -domCarProportion [this] (Interval. 0.1 0.9))
(defn -domNeighborRadius [this] (Interval. 1 10))
(clojure.core/defn record-commandline-args!
  "Temporarily store values of parameters passed on the command line."
  [args__694__auto__]
  (clojure.core/let
    [cli-options
     [["-?" "--help" "Print this help message."]
      ["-K" "--num-k-snipes <long> (25)" "Size of k-snipe subpopulation" :parse-fn (fn* [p1__1348#] (Long. p1__1348#))]
      ["-R" "--num-r-snipes <long> (25)" "Size of r-snipe subpopulation" :parse-fn (fn* [p1__1349#] (Long. p1__1349#))]
      ["-S" "--num-s-snipes <long> (25)" "Size of s-snipe subpopulation" :parse-fn (fn* [p1__1350#] (Long. p1__1350#))]
      ["-M" "--mush-prob <double> (0.2)" "Average frequency of mushrooms." :parse-fn (fn* [p1__1351#] (Double. p1__1351#))]
      ["-s" "--mush-low-size <double> (4.0)" "Size of small mushrooms (mean of light distribution)" :parse-fn (fn* [p1__1352#] (Double. p1__1352#))]
      ["-l" "--mush-high-size <double> (6.0)" "Size of large mushrooms (mean of light distribution)" :parse-fn (fn* [p1__1353#] (Double. p1__1353#))]
      ["-v" "--mush-sd <double> (2.0)" "Standard deviation of mushroom light distribution" :parse-fn (fn* [p1__1354#] (Double. p1__1354#))]
      ["-p" "--mush-pos-nutrition <double> (1.0)" "Energy from eating a nutritious mushroom" :parse-fn (fn* [p1__1355#] (Double. p1__1355#))]
      ["-n" "--mush-neg-nutrition <double> (-1.0)" "Energy from eating a poisonous mushroom" :parse-fn (fn* [p1__1356#] (Double. p1__1356#))]
      ["-e" "--initial-energy <double> (10.0)" "Initial energy for each snipe" :parse-fn (fn* [p1__1357#] (Double. p1__1357#))]
      ["-b" "--birth-threshold <double> (20.0)" "Energy level at which birth takes place" :parse-fn (fn* [p1__1358#] (Double. p1__1358#))]
      ["-o" "--birth-cost <double> (5.0)" "Energetic cost of giving birth to one offspring" :parse-fn (fn* [p1__1359#] (Double. p1__1359#))]
      ["-x" "--max-energy <double> (30.0)" "Max energy that a snipe can have." :parse-fn (fn* [p1__1360#] (Double. p1__1360#))]
      ["-c" "--car-proportion <double> (0.25)" "Snipes are randomly culled when number exceed this times # of cells." :parse-fn (fn* [p1__1361#] (Double. p1__1361#))]
      ["-r" "--neighbor-radius <long> (5)" "s-snipe neighbors are no more than this distance away." :parse-fn (fn* [p1__1362#] (Long. p1__1362#))]
      ["-i" "--report-every <long> (0)" "Report basic stats every i ticks after the first one (0 = never)." :parse-fn (fn* [p1__1363#] (Long. p1__1363#))]
      ["-t" "--max-ticks <long> (0)" "Stop after this number of timesteps have run, or never if 0." :parse-fn (fn* [p1__1364#] (Long. p1__1364#))]
      ["-w" "--env-width <long> (40)" "Width of env.  Must be an even number." :parse-fn (fn* [p1__1365#] (Long. p1__1365#))]
      ["-h" "--env-height <long> (40)" "Height of env. Must be an even number." :parse-fn (fn* [p1__1366#] (Long. p1__1366#))]
      ["-D" "--env-display-size <double> (12.0)" "How large to display the env in gui by default." :parse-fn (fn* [p1__1367#] (Double. p1__1367#))]]
     usage-fmt__695__auto__ (clojure.core/fn [options] (clojure.core/let
                                                         [fmt-line (clojure.core/fn [[short-opt long-opt desc]] (clojure.core/str short-opt ", " long-opt ": " desc))]
                                                         (clojure.string/join "\n" (clojure.core/concat (clojure.core/map fmt-line options)))))
     {:as cmdline, :keys [options arguments errors summary]}
     (clojure.tools.cli/parse-opts args__694__auto__ cli-options)]
    (clojure.core/reset! commandline cmdline)
    (clojure.core/when
      (:help options)
      (clojure.core/println "Command line options (defaults in parentheses):")
      (clojure.core/println (usage-fmt__695__auto__ cli-options))
      (clojure.core/println "MASON options can also be used:")
      (clojure.core/println "-help (note single dash): Print help message for MASON.")
      (java.lang.System/exit 0)))))
