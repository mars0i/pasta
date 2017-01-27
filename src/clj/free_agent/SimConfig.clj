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
;;    (require '[utils.defsimconfig :as cfg])
;;    (pprint (macroexpand-1 '<insert defsimconfig call>))

(def commandline (atom nil)) ; Needed by defsimconfig and other code below if we're defining commandline options

;;                 field name      initial-value type  in ui? with range?
(defcfg/defsimconfig [[num-k-snipes       50    long   [1 500]     ["-k" "Size of k-snipe subpopulation" :parse-fn #(Long. %)]]
                      [num-r-snipes       50    long   [1 500]     ["-r" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]]
                      [mush-prob           0.1  double [0.0 1.0]   ["-f" "Average frequency of mushrooms." :parse-fn #(Double. %)]]
                      [mush-low-size       4.0  double true        ["-s" "Size of small mushrooms (mean of light distribution)" :parse-fn #(Double. %)]]
                      [mush-high-size     16.0  double true        ["-l" "Size of large mushrooms (mean of light distribution)" :parse-fn #(Double. %)]]
                      [mush-sd             2.0  double true        ["-d" "Standard deviation of mushroom light distribution" :parse-fn #(Double. %)]]
                      [mush-pos-nutrition  1.0  double [0.0 20.0]  ["-n" "Energy from eating a nutritious mushroom" :parse-fn #(Double. %)]]
                      [mush-neg-nutrition -1.0  double [-20.0 0.0] ["-p" "Energy from eating a poisonous mushroom" :parse-fn #(Double. %)]]
                      [initial-energy     10.0  double [0.0 50.0]  ["-e" "Initial energy for each snipe" :parse-fn #(Double. %)]]
                      [birth-threshold    15.0  double [1.0 50.0]  ["-b" "Energy level at which birth takes place" :parse-fn #(Double. %)]]
                      [birth-cost          5.0  double [0.0 10.0]  ["-o" "Energetic cost of giving birth to one offspring" :parse-fn #(Double. %)]]
                      [max-energy         30.0  double [1.0 100.0] ["-x" "Max energy that a snipe can have." :parse-fn #(Double. %)]]
                      [max-proportion      0.25 double [0.1 0.9]   ["-c" "Snipes are randomly culled when number exceed this times # of cells." :parse-fn #(Double. %)]]
                      ;; Do not put the false entries mixed in above.  It confuses defsimconfig or the gui
                      [mush-mid-size       0    double false] ; calculated from the previous values
                      [mush-size-scale     0    double false] ; calculated from the previous values
                      [max-ticks           0    long   false       ["-t" "Stop after this number of timesteps have run, or never if 0." :parse-fn #(Long. %)]]
                      [env-width          88    long   false       ["-W" "How wide is env?  Must be an even number." :parse-fn #(Long. %)]] ; can be set from command line but not in running app
                      [env-height         40    long   false       ["-T" "How tall is env? Should be an even number." :parse-fn #(Long. %)]] ; ditto
                      [env-display-size   12.0  double false       ["-g" "How large to display the env in gui by default." :parse-fn #(Double. %)]]
                      [max-pop-size        0    long   false]
                      [env-center         nil   double false]
                      [popenv             nil   free-agent.popenv.PopEnv false]]
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
    ;(report-params @cfg-data$)
    (swap! cfg-data$ assoc :popenv (pe/make-popenv rng cfg-data$)) ; create new popenv
    ;; Run it:
    (let [stoppable (.scheduleRepeating schedule Schedule/EPOCH 0 ; epoch = starting at beginning, 0 means run this first during timestep
                        (reify Steppable 
                          (step [this sim-state]
                            (swap! cfg-data$ update :popenv (partial pe/next-popenv rng cfg-data$)))))]
      ;; Stop simulation when condition satisfied:
      (.scheduleRepeating schedule Schedule/EPOCH 1 ; 1 = i.e. after the previous Steppable, which runs the simulation
                          (reify Steppable
                            (step [this sim-state]
                              (let [max-ticks (:max-ticks @cfg-data$)]
                                (when (and (pos? max-ticks) ; run forever if max-ticks = 0
                                           (>= (.getSteps schedule) max-ticks)) ; = s/b enough, but >= as failsafe
                                  (.stop stoppable)
                                  (stats/report-stats @cfg-data$)
                                  (.kill sim-state))))))))) ; end program after cleaning up Mason stuff

;; https://listserv.gmu.edu/cgi-bin/wa?A2=ind0610&L=MASON-INTEREST-L&D=0&1=MASON-INTEREST-L&9=A&J=on&d=No+Match%3BMatch%3BMatches&z=4&P=14576
;; 1. Make a Steppable which shuts down the simulation.
;; 
;; public class KillSteppable implements Steppable
;; 	{
;; 	public void step(SimState state)
;; 		{
;; 		state.kill();
;; 		}
;; 	}
;; 
;; 2. Stick it in a MultiStep (a convenience class we wrote which only  
;; calls its subsidiary Steppable once every N times, among other options):
;; 
;; Steppable a = new MultiStep(new KillSteppable(), numTimeSteps, true);
;; 
;; 3. Schedule the MultiStep repeating every timestep.  After  
;; numTimeSteps, it'll fire its KillSteppable, which will stop the  
;; simulation.
