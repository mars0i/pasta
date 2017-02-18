(ns free-agent.SimConfig
  (:require [clojure.tools.cli]
            [utils.defsimconfig :as defcfg]
            [free-agent.snipe :as sn]
            [free-agent.popenv :as pe]
            [free-agent.stats :as stats])
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
;;    (require '[utils.defsimconfig :as defcfg])
;;    (pprint (macroexpand-1 '<insert defsimconfig call>))

(def commandline (atom nil)) ; Needed by defsimconfig and other code below if we're defining commandline options

;;                 field name      initial-value type  in ui? with range?
(defcfg/defsimconfig [[num-k-snipes       50    long    [0 500]     ["-K" "Size of k-snipe subpopulation" :parse-fn #(Long. %)]]
                      [num-r-snipes       50    long    [0 500]     ["-R" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]]
                      [num-s-snipes       50    long    [0 500]     ["-S" "Size of s-snipe subpopulation" :parse-fn #(Long. %)]]
                      [mush-prob           0.2  double  [0.0 1.0]   ["-M" "Average frequency of mushrooms." :parse-fn #(Double. %)]]
                      [mush-low-size       4.0  double  true        ["-s" "Size of small mushrooms (mean of light distribution)" :parse-fn #(Double. %)]]
                      [mush-high-size     16.0  double  true        ["-l" "Size of large mushrooms (mean of light distribution)" :parse-fn #(Double. %)]]
                      [mush-sd             2.0  double  true        ["-v" "Standard deviation of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-mid-size       0    double  false] ; calculated from the previous values
                      [mush-size-scale     0    double  false] ; calculated from the previous values
                      [mush-pos-nutrition  1.0  double  [0.0 20.0]  ["-p" "Energy from eating a nutritious mushroom" :parse-fn #(Double. %)]]
                      [mush-neg-nutrition -1.0  double  [-20.0 0.0] ["-n" "Energy from eating a poisonous mushroom" :parse-fn #(Double. %)]]
                      [initial-energy     10.0  double  [0.0 50.0]  ["-e" "Initial energy for each snipe" :parse-fn #(Double. %)]]
                      [birth-threshold    20.0  double  [1.0 50.0]  ["-b" "Energy level at which birth takes place" :parse-fn #(Double. %)]]
                      [birth-cost          5.0  double  [0.0 10.0]  ["-o" "Energetic cost of giving birth to one offspring" :parse-fn #(Double. %)]]
                      [max-energy         30.0  double  [1.0 100.0] ["-x" "Max energy that a snipe can have." :parse-fn #(Double. %)]]
                      [carrying-proportion 0.25 double  [0.1 0.9]   ["-c" "Snipes are randomly culled when number exceed this times # of cells." :parse-fn #(Double. %)]]
                      [neighbor-radius     5    long    [1 10]      ["-r" "s-snipe neighbors are no more than this distance away." :parse-fn #(Long. %)]]
                      [report-every        0    long    true        ["-i" "Report basic stats every i ticks after the first one (0 = never)." :parse-fn #(Long. %)]]
                      [max-ticks           0    long    false       ["-t" "Stop after this number of timesteps have run, or never if 0." :parse-fn #(Long. %)]]
                      [env-width          40    long    false       ["-w" "Width of env.  Must be an even number." :parse-fn #(Long. %)]] ; Haven't figured out how to change 
                      [env-height         40    long    false       ["-h" "Height of env. Must be an even number." :parse-fn #(Long. %)]] ;  within app without distortion
                      [env-display-size   12.0  double  false       ["-D" "How large to display the env in gui by default." :parse-fn #(Double. %)]]
                      [show-grid         false  boolean false      ["-g" "If present, display underlying hexagonal grid." :parse-fn #(Boolean. %)]]
                      [extreme-pref      100.0  double  false] ; mush preference value for r-snipes and s-snipes
                      [max-pop-size        0    long    false]
                      [west-popenv        nil   free-agent.popenv.PopEnv false]
                      [east-popenv        nil   free-agent.popenv.PopEnv false]]
  :methods [[getPopSize [] long] ; additional options here. this one is for def below; it will get merged into the generated :methods component.
            [getKSnipeFreq [] double]])

(defn -getPopSize
  [^SimConfig this]
  (stats/get-pop-size @(.simConfigData this)))

(defn -getKSnipeFreq
  [^SimConfig this]
  (stats/get-k-snipe-freq @(.simConfigData this)))


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
    (pe/setup-popenv-config! cfg-data$)
    (swap! cfg-data$ assoc :west-popenv (pe/make-popenv rng cfg-data$ :west)) ; create new popenv
    (swap! cfg-data$ assoc :east-popenv (pe/make-popenv rng cfg-data$ :east)) ; create new popenv
    ;; Run it:
    (let [report-every (double (:report-every @cfg-data$))
          max-ticks (:max-ticks @cfg-data$)
          stoppable (.scheduleRepeating schedule Schedule/EPOCH 0 ; epoch = starting at beginning, 0 means run this first during timestep
                        (reify Steppable 
                          (step [this sim-state]
                            (swap! cfg-data$ update :west-popenv (partial pe/next-popenv rng cfg-data$))
                            (swap! cfg-data$ update :east-popenv (partial pe/next-popenv rng cfg-data$)))))]
      ;; Stop simulation when condition satisfied (TODO will add additional conditions later):
      (.scheduleRepeating schedule Schedule/EPOCH 1 ; 1 = i.e. after main previous Steppable that runs the simulation
                          (reify Steppable
                            (step [this sim-state]
                                (when (and (pos? max-ticks) ; run forever if max-ticks = 0
                                           (>= (.getSteps schedule) max-ticks)) ; = s/b enough, but >= as failsafe
                                  (.stop stoppable)
                                  (stats/report-stats @cfg-data$ schedule) ; FIXME BROKEN FOR NEW TWO-ENV CONFIG
                                  (println)
                                  ;(stats/report-params @cfg-data$)
                                  (.kill sim-state))))) ; end program after cleaning up Mason stuff
      ;; maybe report stats periodically
      (when (pos? report-every)
        (.scheduleRepeating schedule report-every 1 ; first tick to report at; ordering within tick
                            (reify Steppable
                              (step [this sim-state]
                                (when (< (.getSteps schedule) max-ticks) ; don't report if this is the last tick
                                  (stats/report-stats @cfg-data$ schedule) ; FIXME BROKEN FOR NEW TWO-ENV CONFIG
                                  (println))))
                            report-every))))) ; repeat this often
