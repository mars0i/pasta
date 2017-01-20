(ns free-agent.SimConfig
  (:require [clojure.tools.cli]
            [utils.defsimconfig :as defcfg]
            [free-agent.snipe :as sn]
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
(defcfg/defsimconfig [[num-k-snipes       50    long   [1 500]     ["-N" "Size of k-snipe subpopulation" :parse-fn #(Long. %)]]
                      [num-r-snipes       50    long   [1 500]     ["-o" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]]
                      [k-snipe-prior      10.0  double [1.0 50.0]  ["-k" "Prior for k-snipes" :parse-fn #(Double. %)]]
                      [r-snipe-low-prior   5.0  double [1.0 50.0]  ["-q" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]]
                      [r-snipe-high-prior 20.0  double [1.0 50.0]  ["-r" "One of two possible priors for r-snipes" :parse-fn #(Double. %)]]
                      [mush-prob           0.1  double [0.0 1.0]   ["-f" "Average frequency of mushrooms." :parse-fn #(Double. %)]]
                      [mush-low-size       4.0  double true        ["-l" "Mean of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-high-size     16.0  double true        ["-h" "Mean of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-mean-size      0    double false] ; calculated from the previous values
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
  :methods [[getPopSize [] long] ; additional options here. this one is for def below; it will get merged into the generated :methods component.
            [getRSnipeFreq [] double]])

(defn -getPopSize
  [^SimConfig this]
  (count (:snipes     ; would it be faster to use (.elements snipe-field)?
           (:popenv @(.simConfigData this))))) 

(defn -getRSnipeFreq
  [^SimConfig this]
  (let [count-r-snipes (fn [n id snipe] (if (sn/is-r-snipe? snipe) (inc n) n))
        snipes (:snipes (:popenv @(.simConfigData this)))
        pop-size (count snipes)
        r-snipe-count (reduce-kv count-r-snipes 0 snipes)]
    (if (pos? pop-size)                   ; when UI first starts, it tries to calc this even though there's no pop, and divs by zero
      (double (/ r-snipe-count pop-size)) 
      0))) ; avoid spurious div by zero

;(defn -getRSnipeFreq
;  [^SimConfig this]
;  (let [count-r-snipes (fn [n snipe] (if (sn/is-r-snipe? snipe) (inc n) n))
;        snipes (vals (:snipes (:popenv @(.simConfigData this))))
;        pop-size (count snipes)
;        r-snipe-count (reduce count-r-snipes 0 snipes)]
;    (if (pos? pop-size)                   ; when UI first starts, it tries to calc this even though there's no pop, and divs by zero
;      (double (/ r-snipe-count pop-size)) 
;      0))) ; avoid spurious div by zero



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

(defn report-params
  "Print parameters in cfg-data to standard output."
  [cfg-data]
  (let [kys (sort (keys cfg-data))]
    (println (map #(str (name %) "=" (% cfg-data)) kys))))

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
    (report-params @cfg-data$)
    (swap! cfg-data$ assoc :popenv (pe/make-popenv rng cfg-data$)) ; create new popenv
    ;; Run it:
    (.scheduleRepeating schedule Schedule/EPOCH 0
                        (reify Steppable 
                          (step [this sim-state]
                            (swap! cfg-data$ update :popenv (partial pe/next-popenv rng cfg-data$)))))))
