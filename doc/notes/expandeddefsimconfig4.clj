;; 1/7/2017, commit 6f069c3

;; This:
(defcfg/defsimconfig [[num-k-snipes       50    long   [1 500]     ["-N" "Size of k-snipe subpopulation" :parse-fn #(Long. %)]]
                      [num-r-snipes       50    long   [1 500]     ["-o" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]]
                      [k-snipe-prior      10.0  double [1.0 50.0]  ["-k" "Prior for k-snipes" :parse-fn #(Double. %)]]
                      [r-snipe-low-prior   5.0  double [1.0 50.0]  ["-q" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]]
                      [r-snipe-high-prior 20.0  double [1.0 50.0]  ["-r" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]]
                      [mush-prob           0.1  double [0.0 1.0]   ["-f" "Average frequency of mushrooms." :parse-fn #(Double. %)]]
                      [mush-low-mean       4.0  double true        ["-l" "Mean of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-high-mean     16.0  double true        ["-h" "Mean of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-sd             2.0  double true        ["-s" "Standard deviation of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-pos-nutrition  1.0  double [0.0 20.0]  ["-p" "Energy from eating a nutritious mushroom" :parse-fn #(Double. %)]]
                      [mush-neg-nutrition -1.0  double [-20.0 0.0] ["-n" "Energy from eating a poisonous mushroom" :parse-fn #(Double. %)]]
                      [initial-energy     10.0  double [0.0 50.0]  ["-e" "Initial energy for each snipe" :parse-fn #(Double. %)]]
                      [birth-threshold    15.0  double [1.0 50.0]  ["-b" "Energy level at which birth takes place" :parse-fn #(Double. %)]]
                      [birth-cost          5.0  double [0.0 10.0]  ["-c" "Energetic cost of giving birth to one offspring" :parse-fn #(Double. %)]]
                      [max-energy         30.0  double [1.0 100.0] ["-x" "Max energy that a snipe can have." :parse-fn #(Double. %)]]
                      [max-proportion      0.25 double [0.1 0.9]   ["-m" "Snipes are randomly culled when number exceed this times # of cells." :parse-fn #(Double. %)]]
                      [env-width          88    long   false       ["-w" "How wide is env?  Must be an even number." :parse-fn #(Long. %)]] ; can be set from command line but not in running app
                      [env-height         40    long   false       ["-t" "How tall is env? Should be an even number." :parse-fn #(Long. %)]] ; ditto
                      [max-pop-size        0    long   false]
                      [env-display-size   12.0  double false       ["-d" "How large to display the env in gui by default." :parse-fn #(Double. %)]]
                      [env-center         nil   double false]
                      [popenv             nil   free-agent.popenv.PopEnv false]]
                      :methods [[getPopSize [] long]]) ; NOTE THIS


;; produces this (after minor reformatting):
(do
  (clojure.core/ns free-agent.config-data)

  (clojure.core/defrecord SimConfigData [num-k-snipes num-r-snipes k-snipe-prior r-snipe-low-prior
                                         r-snipe-high-prior mush-prob mush-low-mean mush-high-mean mush-sd
                                         mush-pos-nutrition mush-neg-nutrition initial-energy birth-threshold
                                         birth-cost max-energy max-proportion env-width env-height max-pop-size
                                         env-display-size env-center popenv])

  (clojure.core/ns
    free-agent.SimConfig
    (:require [free-agent.config-data])
    (:import free-agent.SimConfig)
    (:gen-class :name free-agent.SimConfig
                :extends sim.engine.SimState
                :state simConfigData
                :exposes-methods {start superStart}
                :init init-sim-config-data
                :main true
                :methods [[getNumKSnipes [] long]
                          [setNumKSnipes [long] void]
                          [getNumRSnipes [] long]
                          [setNumRSnipes [long] void]
                          [getKSnipePrior [] double]
                          [setKSnipePrior [double] void]
                          [getRSnipeLowPrior [] double]
                          [setRSnipeLowPrior [double] void]
                          [getRSnipeHighPrior [] double]
                          [setRSnipeHighPrior [double] void]
                          [getMushProb [] double]
                          [setMushProb [double] void]
                          [getMushLowMean [] double]
                          [setMushLowMean [double] void]
                          [getMushHighMean [] double]
                          [setMushHighMean [double] void]
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
                          [getMaxProportion [] double]
                          [setMaxProportion [double] void]
                          [domNumKSnipes [] java.lang.Object]
                          [domNumRSnipes [] java.lang.Object]
                          [domKSnipePrior [] java.lang.Object]
                          [domRSnipeLowPrior [] java.lang.Object]
                          [domRSnipeHighPrior [] java.lang.Object]
                          [domMushProb [] java.lang.Object]
                          [domMushPosNutrition [] java.lang.Object]
                          [domMushNegNutrition [] java.lang.Object]
                          [domInitialEnergy [] java.lang.Object]
                          [domBirthThreshold [] java.lang.Object]
                          [domBirthCost [] java.lang.Object]
                          [domMaxEnergy [] java.lang.Object]
                          [domMaxProportion [] java.lang.Object]]
                :methods [[getPopSize [] long]])) ; YES YOU CAN HAVE MULTIPLE :methods


  (clojure.core/defn -init-sim-config-data
    [seed]
    [[seed] (clojure.core/atom (free-agent.config-data/->SimConfigData 50 50 10.0 5.0 20.0 0.1 4.0 16.0 2.0 1.0 -1.0 10.0 15.0 5.0 30.0 0.25 88 40 0 12.0 nil nil))])

  (defn -getNumKSnipes [this] (:num-k-snipes @(.simConfigData this)))
  (defn -getNumRSnipes [this] (:num-r-snipes @(.simConfigData this)))
  (defn -getKSnipePrior [this] (:k-snipe-prior @(.simConfigData this)))
  (defn -getRSnipeLowPrior [this] (:r-snipe-low-prior @(.simConfigData this)))
  (defn -getRSnipeHighPrior [this] (:r-snipe-high-prior @(.simConfigData this)))
  (defn -getMushProb [this] (:mush-prob @(.simConfigData this)))
  (defn -getMushLowMean [this] (:mush-low-mean @(.simConfigData this)))
  (defn -getMushHighMean [this] (:mush-high-mean @(.simConfigData this)))
  (defn -getMushSd [this] (:mush-sd @(.simConfigData this)))
  (defn -getMushPosNutrition [this] (:mush-pos-nutrition @(.simConfigData this)))
  (defn -getMushNegNutrition [this] (:mush-neg-nutrition @(.simConfigData this)))
  (defn -getInitialEnergy [this] (:initial-energy @(.simConfigData this)))
  (defn -getBirthThreshold [this] (:birth-threshold @(.simConfigData this)))
  (defn -getBirthCost [this] (:birth-cost @(.simConfigData this)))
  (defn -getMaxEnergy [this] (:max-energy @(.simConfigData this)))
  (defn -getMaxProportion [this] (:max-proportion @(.simConfigData this)))
  (defn -setNumKSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-k-snipes newval))
  (defn -setNumRSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-r-snipes newval))
  (defn -setKSnipePrior [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :k-snipe-prior newval))
  (defn -setRSnipeLowPrior [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :r-snipe-low-prior newval))
  (defn -setRSnipeHighPrior [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :r-snipe-high-prior newval))
  (defn -setMushProb [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-prob newval))
  (defn -setMushLowMean [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-low-mean newval))
  (defn -setMushHighMean [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-high-mean newval))
  (defn -setMushSd [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-sd newval))
  (defn -setMushPosNutrition [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-pos-nutrition newval))
  (defn -setMushNegNutrition [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mush-neg-nutrition newval))
  (defn -setInitialEnergy [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :initial-energy newval))
  (defn -setBirthThreshold [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :birth-threshold newval))
  (defn -setBirthCost [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :birth-cost newval))
  (defn -setMaxEnergy [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :max-energy newval))
  (defn -setMaxProportion [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :max-proportion newval))
  (defn -domNumKSnipes [this] (Interval. 1 500))
  (defn -domNumRSnipes [this] (Interval. 1 500))
  (defn -domKSnipePrior [this] (Interval. 1.0 50.0))
  (defn -domRSnipeLowPrior [this] (Interval. 1.0 50.0))
  (defn -domRSnipeHighPrior [this] (Interval. 1.0 50.0))
  (defn -domMushProb [this] (Interval. 0.0 1.0))
  (defn -domMushPosNutrition [this] (Interval. 0.0 20.0))
  (defn -domMushNegNutrition [this] (Interval. -20.0 0.0))
  (defn -domInitialEnergy [this] (Interval. 0.0 50.0))
  (defn -domBirthThreshold [this] (Interval. 1.0 50.0))
  (defn -domBirthCost [this] (Interval. 0.0 10.0))
  (defn -domMaxEnergy [this] (Interval. 1.0 100.0))
  (defn -domMaxProportion [this] (Interval. 0.1 0.9))

  (clojure.core/defn record-commandline-args!
    "Temporarily store values of parameters passed on the command line."
    [args__59__auto__]
    (clojure.core/let
      [cli-options [["-?" "--help" "Print this help message."]
                    ["-N" "--num-k-snipes <long>" "Size of k-snipe subpopulation" :parse-fn (fn* [p1__1508#] (Long. p1__1508#))]
                    ["-o" "--num-r-snipes <long>" "Size of r-snipe subpopulation" :parse-fn (fn* [p1__1509#] (Long. p1__1509#))]
                    ["-k" "--k-snipe-prior <double>" "Prior for k-snipes" :parse-fn (fn* [p1__1510#] (Double. p1__1510#))]
                    ["-q" "--r-snipe-low-prior <double>" "One of two possible priors for r-snipes" :parse-fn (fn* [p1__1511#] (Double. p1__1511#))]
                    ["-r" "--r-snipe-high-prior <double>" "One of two possible priors for r-snipes" :parse-fn (fn* [p1__1512#] (Double. p1__1512#))]
                    ["-f" "--mush-prob <double>" "Average frequency of mushrooms." :parse-fn (fn* [p1__1513#] (Double. p1__1513#))]
                    ["-l" "--mush-low-mean <double>" "Mean of mushroom light distribution" :parse-fn (fn* [p1__1514#] (Double. p1__1514#))]
                    ["-h" "--mush-high-mean <double>" "Mean of mushroom light distribution" :parse-fn (fn* [p1__1515#] (Double. p1__1515#))]
                    ["-s" "--mush-sd <double>" "Standard deviation of mushroom light distribution" :parse-fn (fn* [p1__1516#] (Double. p1__1516#))]
                    ["-p" "--mush-pos-nutrition <double>" "Energy from eating a nutritious mushroom" :parse-fn (fn* [p1__1517#] (Double. p1__1517#))]
                    ["-n" "--mush-neg-nutrition <double>" "Energy from eating a poisonous mushroom" :parse-fn (fn* [p1__1518#] (Double. p1__1518#))]
                    ["-e" "--initial-energy <double>" "Initial energy for each snipe" :parse-fn (fn* [p1__1519#] (Double. p1__1519#))]
                    ["-b" "--birth-threshold <double>" "Energy level at which birth takes place" :parse-fn (fn* [p1__1520#] (Double. p1__1520#))]
                    ["-c" "--birth-cost <double>" "Energetic cost of giving birth to one offspring" :parse-fn (fn* [p1__1521#] (Double. p1__1521#))]
                    ["-x" "--max-energy <double>" "Max energy that a snipe can have." :parse-fn (fn* [p1__1522#] (Double. p1__1522#))]
                    ["-m" "--max-proportion <double>" "Snipes are randomly culled when number exceed this times # of cells." :parse-fn (fn* [p1__1523#] (Double. p1__1523#))]
                    ["-w" "--env-width <long>" "How wide is env?  Must be an even number." :parse-fn (fn* [p1__1524#] (Long. p1__1524#))]
                    ["-t" "--env-height <long>" "How tall is env? Should be an even number." :parse-fn (fn* [p1__1525#] (Long. p1__1525#))]
                    ["-d" "--env-display-size <double>" "How large to display the env in gui by default." :parse-fn (fn* [p1__1526#] (Double. p1__1526#))]]
       usage-fmt__60__auto__ (clojure.core/fn
                               [options]
                               (clojure.core/let
                                 [fmt-line (clojure.core/fn [[short-opt long-opt desc]] (clojure.core/str short-opt ", " long-opt ": " desc))]
                                 (clojure.string/join "\n" (clojure.core/concat (clojure.core/map fmt-line options)))))
       {:as cmdline, :keys [options arguments errors summary]}
       (clojure.tools.cli/parse-opts args__59__auto__ cli-options)]
      (clojure.core/reset! commandline cmdline)
      (clojure.core/when
        (:help options)
        (clojure.core/println "Command line options for free-agent:")
        (clojure.core/println (usage-fmt__60__auto__ cli-options))
        (clojure.core/println "free-agent and MASON options can both be used:")
        (clojure.core/println "-help (note single dash): Print help message for MASON.")
        (java.lang.System/exit 0)))))
