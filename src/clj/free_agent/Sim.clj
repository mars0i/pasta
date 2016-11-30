;;; This software is copyright 2015 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

;; IN THIS VERSION:
;; * There is NOT a step function in each agent.
;; * There is a single step function for the entire simulation.
;; * Indivs update their states update "in parallel" by updating a "new" 
;;   version of a variable from others "old" versions.

;; Note: Traditional MASON models put e.g. Continuous2D and Network here, 
;; and then the GUIState class would normally use those instances from 
;; this class, passing them to portrayals created in the GUIState class.
;; Since the underlying model doesn't need spatial relations or explicit
;; link representations, I only create the Continuous2D and Network objects
;; in the GUIState class (SimWithGUI), where they're needed to be used by portrayals.

;; Tip: Methods named "getBlahBlah" or "setBlahBlah" will be found by the UI via reflection.

;; Convention: I use the "->Abc" constructor function iff a class was defined by deftype
;; or defrecord, even though "Abc." is available as well.

;(set! *warn-on-reflection* true)

;; Put gen-class Sim first so we can type-hint methods in Indiv etc.
;; But put intermit.Sim's methods at end, so we can type-hint references to Indiv, etc. in them.
(ns intermit.Sim
  (:require [clojure.tools.cli :as cli]
            [clojure.pprint :as pp])
  (:import [sim.engine Steppable Schedule]
           [sim.portrayal Oriented2D]
           [sim.util Interval Double2D]
           [sim.util.distribution Poisson Normal Beta]
           [ec.util MersenneTwisterFast]
           [java.lang String]
           [java.util Collection]
           [intermit Sim]) ; import rest of classes after each is defined
  (:gen-class :name intermit.Sim
              :extends sim.engine.SimState                         ; includes signature for the start() method
              :exposes-methods {start superStart}                  ; alias method start() in superclass. (Don't name it 'super-start'; use a Java name.)
              :methods [[getNumCommunities [] long]                ; these methods are defined much further down
                        [setNumCommunities [long] void]
                        [domNumCommunities [] java.lang.Object]
                        [getIndivsPerCommunity [] long]
                        [setIndivsPerCommunity [long] void]
                        [domIndivsPerCommunity [] java.lang.Object]
                        [getLinkProb [] double]
                        [setLinkProb [double] void]
                        [domLinkProb [] java.lang.Object]
                        [getTranStddev [] double]
                        [setTranStddev [double] void]
                        [domTranStddev [] java.lang.Object]
                        [getGlobalInterlocMean [] double]     ; i.e. mean # of interlocutors from global population
                        [setGlobalInterlocMean [double] void]
                        [domGlobalInterlocMean [] java.lang.Object]
                        [getSuccessSampleSize [] double]
                        [setSuccessSampleSize [double] void]
                        [domSuccessSampleSize [] java.lang.Object]
                        [getSuccessThreshold [] double]
                        [setSuccessThreshold [double] void]
                        [domSuccessThreshold [] java.lang.Object]
                        [getReligDistribution [] "[D" ]
                        [getMeanReligDistribution [] "[D" ]
                        [getMeanReligTimeSeries [] "[Lsim.util.Double2D;"]
                        [getSuccessDistribution [] "[D" ]
                        [getMeanSuccessDistribution [] "[D" ]
                        [getMeanSuccessTimeSeries [] "[Lsim.util.Double2D;"]
                        [getLinkStyle [] long]
                        [setLinkStyle [long] void]
                        [domLinkStyle [] java.lang.Object]]
              :state instanceState
              :init init-instance-state
              :main true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DEFAULTS AND GENERAL UTILITY CODE

(declare ;; constructor functions defined by deftype or defrecord:
         ->InstanceState ->Indiv ->Community
         ;; method functions defined by defprotocol or definterface:
         getId getSuccess getRelig getNeighbors get-rest-of-pop get-prev-speaker add-neighbor! set-rest-of-community! set-rest-of-pop! copy-relig! update-relig! update-success! get-members
         ;; regular functions defined by defn:
         remove-if-identical -init-instance-state -getNumCommunities -setNumCommunities -domNumCommunities -getIndivsPerCommunity -setIndivsPerCommunity -domIndivsPerCommunity 
         -getLinkProb -setLinkProb -domLinkProb -getTranStddev -setTranStddev -domTranStddev -getGlobalInterlocMean -setGlobalInterlocMean -domGlobalInterlocMean -getSuccessSampleSize 
         -setSuccessSampleSize -domSuccessSampleSize -getLinkStyle -setLinkStyle -domLinkStyle get-communities get-population 
         -getReligDistribution -getMeanReligTimeSeries -getMeanReligDistribution -getSuccessDistribution -getMeanSuccessTimeSeries -getMeanSuccessDistribution add-relig add-success 
         bag-shuffle bag-sample take-rand choose-others-from-pop choose-most-successful calc-success normal-noise beta-noise make-indiv binomial-link-indivs! sequential-link-indivs! 
         both-link-indivs! link-style-name-to-idx link-indivs!  make-community-of-indivs make-communities-into-pop! collect-data report-run-params record-commandline-args! 
         set-instance-state-from-commandline! -main -start relig-to-success
         ;; non-functions not defined immediately below:
         sum-relig sum-success link-style-names link-style-fns binomial-link-style-idx sequential-link-style-idx both-link-style-idx commandline)

(def initial-num-communities 12) ; use something that factors into x and y dimensions
(def initial-indivs-per-community 15)
(def initial-link-prob 0.20)
(def initial-tran-stddev 0.03)
(def initial-global-interloc-mean 0.1)     ; i.e. Poisson-mean interlocutors from global population
(def initial-success-sample-size 10.0)
(def initial-link-style-idx 1) ; This is an index into link-style-names and link-style-fns, defined below.
(def initial-success-threshold 0.95) ; used by relig-to-success, which is used by calc-success
;; (We can't put link-style-fns here; eval'ing them at this point produces nothing.)


(def slider-max-num-communities 50)
(def slider-max-indivs-per-community 50)
(def slider-max-tran-stddev 3.0)
(def slider-max-global-interloc-mean 200.0)
(def slider-max-success-sample-size 50.0)

(defn remove-if-identical
  "Removes from coll any object that's identical to obj."
  [obj coll]
  (remove #(identical? obj %) coll))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; INSTANCESTATE FOR SIM CLASS
;; Used to hold mutable data in Sim's instanceState variable
;; NOTE: This must come before the methods for Sim so that we can use type hints there.

;; Note some of these have to be atoms so that that we can allow restarting with a different setup.
(deftype InstanceState [; run parameters:
                        numCommunities      ; number of communities
                        indivsPerCommunity  ; exact number of indivs in each
                        linkStyleIdx ; see sect 3.4.2, "MASON Extensions",  of MASON manual v. 19
                        linkProb
                        tranStddev
                        globalInterlocMean ; mean number of interlocutors from global pop
                        successSampleSize
                        successThreshold
                        ; runtime storage slots:
                        communities             ; holds the communities
                        population              ; holds all individuals
                        poisson                 ; a Poisson-distribution wrapper
                        gaussian                ; a normally-distribution wrapper
                        beta                    ; a beta-distribution wrapper
                        meanReligSeries         ; records mean relig values at timestep
                        meanSuccessSeries])     ; records mean success values at timestep

(defn -init-instance-state
  "Initializes instance-state when an instance of class Sim is created."
  [seed]
  [[seed] (->InstanceState (atom initial-num-communities)
                          (atom initial-indivs-per-community) 
                          (atom initial-link-style-idx)
                          (atom initial-link-prob)
                          (atom initial-tran-stddev)
                          (atom initial-global-interloc-mean)
                          (atom initial-success-sample-size)
                          (atom initial-success-threshold)
                          (atom nil)   ; communities
                          (atom nil)   ; population
                          (atom nil)   ; poisson
                          (atom nil)   ; gaussian
                          (atom nil)   ; beta
                          (atom [])    ; meanReligSeries
                          (atom []))]) ; meanSuccessSeries

;; NOTE The numeric -domXY fns set limits for sliders BUT DON'T RESTRICT VALUES TO THAT RANGE.  You can type in values outside the range, and they'll get used.
(defn -getNumCommunities ^long [^Sim this] @(.numCommunities ^InstanceState (.instanceState this)))
(defn -setNumCommunities [^Sim this ^long newval] (reset! (.numCommunities ^InstanceState (.instanceState this)) newval))
(defn -domNumCommunities [this] (Interval. 1 ^long slider-max-num-communities))
(defn -getIndivsPerCommunity ^long [^Sim this] @(.indivsPerCommunity ^InstanceState (.instanceState this)))
(defn -setIndivsPerCommunity [^Sim this ^long newval] (reset! (.indivsPerCommunity ^InstanceState (.instanceState this)) newval))
(defn -domIndivsPerCommunity [this] (Interval. 1 ^long slider-max-indivs-per-community))
(defn -getLinkProb ^double [^Sim this] @(.linkProb ^InstanceState (.instanceState this)))
(defn -setLinkProb [^Sim this ^double newval] (reset! (.linkProb ^InstanceState (.instanceState this)) newval))
(defn -domLinkProb [this] (Interval. 0.0 1.0))
(defn -getTranStddev ^double [^Sim this] @(.tranStddev ^InstanceState (.instanceState this)))
(defn -setTranStddev [^Sim this ^double newval] (reset! (.tranStddev ^InstanceState (.instanceState this)) newval))
(defn -domTranStddev [this] (Interval. 0.0 ^double slider-max-tran-stddev))
(defn -getGlobalInterlocMean ^double [^Sim this] @(.globalInterlocMean ^InstanceState (.instanceState this)))
(defn -setGlobalInterlocMean [^Sim this ^double newval] 
  (let [^InstanceState istate (.instanceState this)]
    (reset! (.globalInterlocMean istate) newval)    ; store it so that UI can display its current value
    (when-let [^Poisson poisson @(.poisson istate)] ; avoid npe: poisson isn't created until start is run (at which point it will be init'ed with value of globalInterlocMean)
      (.setMean poisson newval))))                  ; allows changing value during the middle of a run.
(defn -domGlobalInterlocMean [this] (Interval. 0.0 ^double slider-max-global-interloc-mean)) ; Poisson dist mean: how many indivs each person talks to from entire pop (including neighbors).
(defn -getSuccessSampleSize ^double [^Sim this] @(.successSampleSize ^InstanceState (.instanceState this)))
(defn -setSuccessSampleSize [^Sim this ^double newval] (reset! (.successSampleSize ^InstanceState (.instanceState this)) newval))
(defn -domSuccessSampleSize [this] (Interval. 0.01 ^double slider-max-success-sample-size)) ; use small non-zero as the min; zero should work, theoretically, but hangs the app and isn't needed.
(defn -getSuccessThreshold ^double [^Sim this] @(.successThreshold ^InstanceState (.instanceState this)))
(defn -setSuccessThreshold [^Sim this ^double newval] (reset! (.successThreshold ^InstanceState (.instanceState this)) newval))
(defn -domSuccessThreshold [this] (Interval. 0.0 1.0)) 

;; We set the function that decides how to link nodes using MASON's popup menu functionality,
;; which uses a mapping between strings in an array and their indexes.  It's the string that's
;; displayed; it's the index that's returned, and that we need to use to choose the appropriate function.
;; See related defs below, and sect 3.4.2, "MASON Extensions",  of MASON manual v. 19.
(defn -getLinkStyle ^long [^Sim this] @(.linkStyleIdx ^InstanceState (.instanceState this)))
(defn -setLinkStyle [^Sim this ^long newval] (reset! (.linkStyleIdx ^InstanceState (.instanceState this)) newval))
(defn -domLinkStyle [^Sim this] (into-array link-style-names))

;; Useful since the fields contain atoms:
(defn get-communities [^Sim this] @(.communities ^InstanceState (.instanceState this)))
(defn get-population [^Sim this] @(.population ^InstanceState (.instanceState this)))

(defn -getReligDistribution
  "Returns array of doubles of relig values in population at current timestep."
  [^Sim this] 
  (double-array (map getRelig (get-population this))))

(defn -getMeanReligTimeSeries
  "Returns array of sim.util.Double2D's in which the first element is a
  timestep and the second is the population's mean relig at that timestep."
  [^Sim this] 
  (into-array sim.util.Double2D @(.meanReligSeries ^InstanceState (.instanceState this)))) ; Double2D version: just convert Clojure vector to Java array

(defn -getMeanReligDistribution
  "Returns array of doubles containing the population's mean relig values at
  all timesteps until and including the current timestep.  (Useful for generating
  a histogram over all timesteps so far.)"
  [^Sim this]
  (double-array (map #(.y ^Double2D %) @(.meanReligSeries ^InstanceState (.instanceState this)))))    ; Double2D version: extract data in y element

(defn -getSuccessDistribution 
 "Returns array of doubles of success values in population at current timestep."
  [^Sim this]
  (double-array (map getSuccess (get-population this))))

(defn -getMeanSuccessTimeSeries
  "Returns array of sim.util.Double2D's in which the first element is a
  timestep and the second is the population's mean success at that timestep."
  [^Sim this] 
  (into-array sim.util.Double2D @(.meanSuccessSeries ^InstanceState (.instanceState this)))) ; Double2D version: just convert Clojure vector to Java array

(defn -getMeanSuccessDistribution
  "Returns array of doubles containing the population's mean success values at
  all timesteps until and including the current timestep.  (Useful for generating
  a histogram over all timesteps so far.)"
  [^Sim this]
  (double-array (map #(.y ^Double2D %) @(.meanSuccessSeries ^InstanceState (.instanceState this)))))    ; Double2D version: extract data in y element

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SIM: Definitions for main class in this file
;; NOTE: The definition of InstanceState must come first, so that we can type-hint it below.

(defn -main
  [& args]
  (record-commandline-args! args) ; The Sim isn't available yet, so store commandline args for later access by start().
  (sim.engine.SimState/doLoop intermit.Sim (into-array String args))
  (System/exit 0))

;; doall all sequences below.  They're short, so there's no point in waiting for them to get realized who knows where/when.
(defn -start
  "Function called to (re)start a new simulation run.  Initializes a new
  set of communities, each with a new set of community members."
  [^Sim this]
  (.superStart this)
  ;; If user passed commandline options, use them to set parameters, rather than defaults:
  (when @commandline
    (set-instance-state-from-commandline! this commandline)
    (reset! commandline nil)) ; do the preceding only the first time (a kludge), e.g. not if user has changed params in the gui
  ;; Construct core data structures of the simulation:
  (let [^Schedule schedule (.schedule this)
        ^InstanceState istate (.instanceState this)
        num-communities  @(.numCommunities istate)
        indivs-per-community @(.indivsPerCommunity istate)
        communities (vec (repeatedly num-communities
                                     #(make-community-of-indivs this indivs-per-community)))
        population (make-communities-into-pop! communities)
        meanReligSeriesAtom (.meanReligSeries istate)]
    ;; Record core data structures and utility states:
    (reset! (.poisson istate) (Poisson. @(.globalInterlocMean istate) (.random this)))
    (reset! (.gaussian istate) (Normal. 0.0 1.0 (.random this))) ; mean and sd here can be overridden later
    (reset! (.beta istate) (Beta. 0.5 0.5 (.random this))) ; alpha and beta here will be overridden later
    (reset! (.communities istate) communities)
    (reset! (.population istate) population)
    (reset! (.meanReligSeries istate) [])
    ;; Schedule per-tick step function(s):
    (.scheduleRepeating schedule Schedule/EPOCH 0
                        (reify Steppable 
                          (step [this sim-state]
                            (let [^Sim sim sim-state
                                  ^InstanceState istate (.instanceState sim)]
                              (doseq [^Indiv indiv population] (copy-relig! indiv sim))      ; first communicate relig (to newrelig's)
                              (doseq [^Indiv indiv population] (update-relig! indiv))        ; then copy newrelig to relig ("parallel" update)
                              (doseq [^Indiv indiv population] (update-success! indiv sim))  ; update each indiv's success field (uses relig)
                              (collect-data sim))))))
  (report-run-params this)) ; At beginning of run, tell user what parameters we're using

(defn collect-data
  "Record per-tick data."
  [^Sim sim]
  (let [^Schedule schedule (.schedule sim)
        ^InstanceState istate (.instanceState sim)
        population @(.population istate)
        pop-size (count population)
        tick (double (.getSteps schedule))] ; coercion will happen automatically; I made it explicit. (getTime incorrect if funny scheduling.)
    (swap! (.meanReligSeries istate)   conj (Double2D. tick (/ (sum-relig 0.0 population) pop-size)))
    (swap! (.meanSuccessSeries istate) conj (Double2D. tick (/ (sum-success 0.0 population) pop-size)))))

(defn report-run-params
  [^Sim sim]
  (let [istate (.instanceState sim)]
    (pp/cl-format true
                  "~ax~a indivs, link style = ~a, link prob (if relevant) = ~a, tran stddev = ~a, global interlocutor mean = ~a, succ sample size = ~a, succ threshold = ~a~%"
                  @(.numCommunities istate)
                  @(.indivsPerCommunity istate)
                  (link-style-names @(.linkStyleIdx istate))
                  @(.linkProb istate)
                  @(.tranStddev istate)
                  @(.globalInterlocMean istate)
                  @(.successSampleSize istate)
                  @(.successThreshold istate))))

;; Var to pass info from main to start.  Must be a better, proper, way.  Really a big kludge.
;; Note this is a "static" var--it's in the class, so to speak, and not in the instance (i.e. not in instanceState)
;; but that's OK since its purpose is to be used from main() the first time through.
;; This may need to be called before a Sim is created, so it can't be part of the Sim's instanceState.
(def commandline (atom nil))

(defn record-commandline-args!
  "Temporarily store values of parameters passed on the command line."
  [args]
  ;; These options should not conflict with MASON's.  Example: If "-h" is the single-char help option, doLoop will never see "-help" (although "-t n" doesn't conflict with "-time") (??).
  (let [cli-options [["-?" "--help" "Print this help message."]
                     ["-n" "--number-of-communities <number of communities>" "Number of communities." :parse-fn #(Integer. %)]
                     ["-i" "--indivs-per-community <number of indivs" "Number of indivs per community." :parse-fn #(Integer. %)]
                     ["-l" "--link-style {binomial|sequential|both}" (str "Create within-community links with method:\n"
                                                                          "          binomial: Link indivs randomly using the Erdos-Renyi/binomial/Poisson method.\n"
                                                                          "          sequential: Link indivs in a sequence, with 2 links per indiv except on ends of sequence.\n"
                                                                          "          both: Use both methods.") :parse-fn link-style-name-to-idx]
                     ["-p" "--link-prob <number in [0,1]>" "Probability that each pair of indivs will be linked (for binomial and both)." :parse-fn #(Double. %)]
                     ["-t" "--tran-stddev <non-negative number>" "Standard deviation of Normally distributed noise in relig transmission." :parse-fn #(Double. %)]
                     ["-g" "--global-interloc-mean <non-negative number>" "Mean number of Poisson-distributed interlocutors from entire population" :parse-fn #(Double. %)]
                     ["-s" "--success-sample-size <non-negative number>" "\"Sample size\" (alpha + beta) of Beta distributed success" :parse-fn #(Double. %)]]
        usage-fmt (fn [options]
                    (let [fmt-line (fn [[short-opt long-opt desc]] (str short-opt ", " long-opt ": " desc))]
                      (clojure.string/join "\n" (concat (map fmt-line options)))))
        ;error-fmt (fn [errors] (str "The following errors occurred while parsing your command:\n\n" (apply str errors))) ; not in use
        {:keys [options arguments errors summary] :as commline} (clojure.tools.cli/parse-opts args cli-options)]
    (reset! commandline commline)
    (when (:help options)
      (println "Command line options for the Intermittent simulation:")
      (println (usage-fmt cli-options))
      (println "Intermittent and MASON options can both be used:")
      (println "-help (note single dash): Print help message for MASON.")
      (System/exit 0))))

(defn set-instance-state-from-commandline!
  "Set fields in the Sim's instanceState from parameters passed on the command line."
  [^Sim sim commline]
  (let [{:keys [options arguments errors summary]} @commline]
    (when-let [newval (:number-of-communities options)] (.setNumCommunities sim newval))
    (when-let [newval (:indivs-per-community options)] (.setIndivsPerCommunity sim newval))
    (when-let [newval (:link-style options)] (.setLinkStyle sim newval))
    (when-let [newval (:link-prob options)] (.setLinkProb sim newval))
    (when-let [newval (:tran-stddev options)] (.setTranStddev sim newval))
    (when-let [newval (:global-interloc-mean options)]  (.setGlobalInterlocMean sim newval))
    (when-let [newval (:success-sample-size options)] (.setSuccessSampleSize sim newval)))
  (reset! commandline nil)) ; clear it so user can set params in the gui

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; INDIV: class for individuals who communicate with each other.
;; These could be persons, villages, subaks, etc.
;; Initial version implements Steppable.

;; NOTE: Objects created by deftype are faster than those created by
;; defrecord when they are hashed (e.g. in the set that's used in sample-wout-repl).
;; 
;; volatile-mutable is a bit inconvenient since it requires accessors,
;; but it's faster than atoms, and these fields get accessed a lot.

(defprotocol IndivP
  "Protocol for Indivs."
  (getId [this])
  (getSuccess [this])   
  (getRelig [this])     
  (getNeighbors [this]) 
  (get-rest-of-community [this]) 
  (get-rest-of-pop [this]) 
  (get-prev-speaker [this])
  (add-neighbor! [this newval])
  (set-rest-of-community! [this newval])
  (set-rest-of-pop! [this newval])
  (copy-relig! [this sim])
  (update-relig! [this])
  (update-success! [this sim]))

(deftype Indiv [id
                ^:unsynchronized-mutable success 
                ^:unsynchronized-mutable relig 
                ^:unsynchronized-mutable newrelig
                ^:unsynchronized-mutable neighbors 
                ^:unsynchronized-mutable restofcommunity
                ^:unsynchronized-mutable restofpop
                ^:unsynchronized-mutable prevspeaker]
  IndivP
    (getId [this] id)
    (getSuccess [this] success)
    (getRelig [this] relig)
    (getNeighbors [this] neighbors)
    (get-rest-of-community [this] restofcommunity)
    (get-rest-of-pop [this] restofpop)
    (get-prev-speaker [this] prevspeaker)
    (add-neighbor! [this new-neighbor] (set! neighbors (conj neighbors new-neighbor)))
    (set-rest-of-community! [this indivs] (set! restofcommunity indivs))
    (set-rest-of-pop! [this indivs] (set! restofpop indivs))
    (copy-relig! [this sim-state]
      "If there is a neighbor or other interlocutor who is more successful
      than I am, copy the relig value of the best such neighbor into my newrelig."
      (let [^Sim sim sim-state ; can't type hint ^Sim in the parameter list
            ^MersenneTwisterFast rng (.random sim)
            ^InstanceState istate (.instanceState sim)
            ^Poisson poisson @(.poisson istate)
            ^Normal gaussian @(.gaussian istate)
            ^double stddev @(.tranStddev istate)]
        (set! prevspeaker nil) ; maybe refactor when-let, when below to make this the alt condition
        (when-let [^Indiv best-model (choose-most-successful 
                                       rng
                                       (into neighbors ;   (a) neighbors, (b) 0 or more random indivs from entire pop
                                             (choose-others-from-pop rng poisson this)))]
          (when (> (getSuccess best-model) success)     ; is most successful other, better than me?
            (set! newrelig (normal-noise gaussian 0.0 stddev (getRelig best-model)))
            (set! prevspeaker best-model)))))
    (update-relig! [this]
      "Copy newrelig to relig."
      (set! relig newrelig)) ; note that relig may be unchanged; then this is wasted computation
    (update-success! [this sim-state]
      (let [^Sim sim sim-state ; can't type hint ^Sim in the parameter list
            ^InstanceState istate (.instanceState sim)
            ^Beta beta @(.beta istate)
            ^double sample-size @(.successSampleSize istate)
            ^double threshold @(.successThreshold istate)] ;; CURRENTLY UNUSED?
        (set! success (beta-noise beta sample-size (calc-success threshold relig restofcommunity)))))
  Oriented2D ; display pointer in GUI
    (orientation2D [this] (+ (/ Math/PI 2) (* Math/PI success))) ; pointer goes from down (=0) to up (=1)
  Object
    (toString [this] (str id ": " success " " relig " " (vec (map #(.id %) neighbors)))))

;;; Runtime functions:

;; passing my-relig and restofcommunity is slightly faster since they're already available in update-success!
(defn calc-success
  "Calculate success of individual based on its relig value (my-relig) and
  a collection of indivs, passing r-to-s-param to relig-to-success."
  ^double [^double r-to-s-param ^double my-relig ^Collection indivs]
  (relig-to-success r-to-s-param
                    (/ (sum-relig my-relig indivs) (inc (count indivs))))) ; inc to count my-relig as well

;; DEF IS IN FLUX
;; OBSOLETE DESCRIPTION:
;; This version uses a threshold function.  In BaliPlus.nlogo, I sometimes use a threshold function for relig-effect,
;; which calculates an effect on success from relig.  That's similar to this function.  However, in BaliPlus, there is
;; *also* an implicit threshold-ey function, it appears, based on the relig values of neighbors several steps away,
;; since harvest (success) is affected by whether they coordinate their crop patterns.  This success function combines
;; both effects.
(defn relig-to-success
  "OBSOLETE: Maps (averaged) relig value to a success value, using success-threshold
  to control the mapping.  Current version returns 1 if 
  relig >= success-thredhold, 0 otherwise."
  ^double [^double success-threshold ^double relig]
  relig)
  ;(if (>= relig success-threshold) 1.0 0.0))

;; NOTE this means that e.g. if indiv is isolated, then when it happens to get high relig, it will also have high success.  Is that realistic?
;(defn old-old-calc-success
;  "Returns the average of relig values of a collection of indivs and
;  one more indiv, who has relig value init-value.  i.e. the sum of all
;  these values is divided by (count indivs) + 1."
;  ^double [^double init-value indivs]
;  (/ (sum-relig init-value indivs) 
;     (inc (count indivs))))

(defn add-relig 
  "Add's indiv's relig value to acc.  Designed for use with reduce."
  [^double acc ^Indiv indiv]
  (+ acc ^double (getRelig indiv)))

(def sum-relig
  "Given a double initial value and a collection of indivs, sums the relig 
  values of indivs along with initial value.  Suitable for use with reduce."
  (partial reduce add-relig))

(defn add-success 
  "Add's indiv's success value to acc.  Designed for use with reduce."
  [^double acc ^Indiv indiv]
  (+ acc ^double (getSuccess indiv)))

(def sum-success
  "Given a double initial value and a collection of indivs, sums the success 
  values of indivs along with initial value.  Suitable for use with reduce."
  (partial reduce add-success))

;; nextGaussian has mean 0 and stddev 1, I believe
(defn normal-noise
 "Add normally distributed noise with stddev to value, clipping to extrema 0.0 and 1.0."
  ^double [^Normal gaussian ^double mean ^double stddev ^double value]
  (max 0.0 (min 1.0 (+ value ^double (.nextDouble gaussian mean stddev)))))

(defn beta-noise
 "Generate a beta-distributed value with given mean, and \"sample-size\", i.e.
 the sum of the usual alpha and beta parameters to a Beta distribution
 [alpha = sample-size * mean, and beta = sample-size * (1 - mean)]."
  ^double [^Beta beta-dist ^double sample-size ^double mean]
  (.nextDouble beta-dist (* mean sample-size) (* (- 1 mean) sample-size)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Shuffling/sampling functions
;; NOTE: For alternative shuffling and sampling functions see notinuse/Shuffle.clj

;; This is probably going overboard in service of speed, even though the 
;; optimizations do make big differences.  It's an experiment.
(defn choose-others-from-pop
  "Randomly sample a Poisson-distributed number of indivs from population,
  excluding me.  (The mean for the Poisson distribution is stored in the
  poisson object.)"
  [^MersenneTwisterFast rng ^Poisson poisson ^Indiv me]
  (let [restofpop (get-rest-of-pop me)
        size (count restofpop)
        rand-num (.nextInt poisson)
        num-to-choose (if (< rand-num size) rand-num size)] ; When Poisson mean is large, result may be larger than number of indivs.
    ;; Choose a method that's likely to be fast for this situation (with guesses at cutoffs based on informal tests):
    (cond (== num-to-choose size)  (bag-shuffle rng restofpop)   ; return pop in random order
          (zero? num-to-choose) []
          (== num-to-choose 1) (vector (nth restofpop (.nextInt rng size)))
          (== num-to-choose 2) (letfn [(one-more [[oldelt :as coll]]                        ; if we only want a small sample, do the stupid thing and just sample until they're unique
                                         (let [newelt (nth restofpop (.nextInt rng size))]  ; note that this is not precisely the correct probability.
                                           (if (identical? newelt oldelt)
                                             (recur coll) ; direct recursion may be slightly faster even, but there is a low-probability possibility of blowing the stack
                                             (conj coll newelt))))]
                                 (one-more (vector (nth restofpop (.nextInt rng size)))))
          (<= num-to-choose 5) (letfn [(a-few-more [still-needed coll]                      ; if we only want a small sample, do the stupid thing and just sample until they're unique
                                         (let [newelt (nth restofpop (.nextInt rng size))]  ; don't let the cutoff be too large; the probs aren't precisely correct here
                                           (cond (some #(identical? newelt %) coll) (recur still-needed coll) ; can't use this one--already have it
                                                 (== still-needed 1) (conj coll newelt)                       ; needed just one more, and we found it, so we're done
                                                 :else (recur (dec still-needed) (conj coll newelt)))))]      ; got a new one, but we need more
                                 (a-few-more (dec num-to-choose) (vector (nth restofpop (.nextInt rng size))))) ; get the first one
          (> (/ num-to-choose size) 1/50) (bag-sample rng num-to-choose restofpop) ; for large samples, use the bag shuffle and take
          :else  (take-rand rng num-to-choose restofpop))))

;; Faster than Clojure's shuffle, at least for pops of size 200 or so
(defn bag-shuffle
  [^MersenneTwisterFast rng ^Collection coll]
  (let [bag (sim.util.Bag. coll)]
    (.shuffle bag rng)
    bag))

;; Note that the 'take' gives a 2X drop in perf over mere bag-shuffle, so 
;; if you know you want the whole thing, just shuffle it.
(defn bag-sample
  [rng n coll]
  (subvec (vec (bag-shuffle rng coll)) 0 n))

;; IS THIS A FISHER-YATES(KNUTH) SHUFFLE?
;; Excellent on small samples.  Slow on large samples. (take-rand5 in notinuse/Shuffle.clj)
;; from https://gist.github.com/pepijndevos/805747, by amalloy, with small mod by Marshall
(defn take-rand [^MersenneTwisterFast rng nr coll]
  (take nr
        ((fn shuffle [coll]
           (lazy-seq
             (let [c (count coll)]
               (when-not (zero? c)
                 (let [n (.nextInt rng c)]
                   (cons (get coll n)
                         (shuffle (pop! (assoc! coll n (get coll (dec c)))))))))))
           (transient coll))))

;; Note that the analogous procedure in LKJPlus.nlogo, find-best, uses NetLogo's ask, which means
;; that subaks are always compared in random order.  In effect they're shuffled before the comparison
;; process starts.
;; Can I avoid repeated accesses to the same field, caching them?  Does it matter?
(defn choose-most-successful
  "Given a collection of Indiv's, returns the one with the greatest success, or
  nil if the collection is empty.  NOTE if there are ties, this will always
  use the first one found, so collection must already be in random order if you want
  a random member of the winners."
  ^Indiv [^MersenneTwisterFast rng models]
  (letfn [(compare-success 
            ([] nil) ; what reduce does if collection is empty
            ([^Indiv i1 ^Indiv i2]
             (let [^double success1 (getSuccess i1)
                   ^double success2 (getSuccess i2)]
               (cond (== success1 success2) i1 ; NOTE always returns the first winner
                     (> success1 success2) i1
                     :else i2))))]
    (reduce compare-success models)))
    ;(reduce compare-success (take-rand3 rng (count models) models))))

;;; Initialization functions:

(defn make-indiv
  "Make an indiv with appropriate defaults."
  [sim-state]
  (let [relig   (.nextDouble (.random sim-state))
        success (.nextDouble (.random sim-state))]
    (->Indiv
      (str (gensym "i"))  ; id
      success             ; success
      relig               ; relig
      relig               ; newrelig (need to repeat value so it's there until relig gets a new value from someone else)
      []                  ; neighbors
      []                  ; restofcommunity
      []                  ; restofpop
      nil)))              ; prevspeaker

(defn binomial-link-indivs!
  "For each pair of indivs, with probability prob, make them each others' neighbors.
  Set prob to 1 to link all indivs to each other.  (This is a 'binomial' [edge
  dist], 'Poisson' [degree dist], or 'Erdös-Rényi' random graph.)"
  [rng prob indivs]
  (doseq [i (range (count indivs))
          j (range i)          ; lower triangle without diagonal
          :when (< (.nextDouble rng) prob)
          :let [indiv-i (nth indivs i)     ; fires only if when does
                indiv-j (nth indivs j)]]
      (add-neighbor! indiv-i indiv-j)
      (add-neighbor! indiv-j indiv-i)))

(defn sequential-link-indivs!
  "Links each indiv to the next indiv in the sequence.  Each
  indiv except the first and last will have two links."
  ([indivs]
   (let [size (count indivs)
         dec-size (dec size)]
     (doseq [i (range size)
             :when (< i dec-size)
             :let [indiv-i (nth indivs i)     ; fires only if when does
                   indiv-j (nth indivs (inc i))]]
       (add-neighbor! indiv-i indiv-j)
       (add-neighbor! indiv-j indiv-i))))
  ([rng prob indivs] (sequential-link-indivs! indivs)))

(defn both-link-indivs!
  "Runs sequential-link-indivs! and then runs binomial-link-indivs!"
  [rng prob indivs]
  (sequential-link-indivs! indivs)
  (binomial-link-indivs! rng prob indivs))

;; These defs must match up.  Not very Clojurely; needed for MASON auto-dropdown.
;; See comment above near domLinkStyles, and sect 3.4.2, "MASON Extensions",  of MASON manual v. 19.
(def link-style-names ["binomial"             "sequential"             "both"])
(def link-style-fns   [binomial-link-indivs!  sequential-link-indivs!  both-link-indivs!] )
(def binomial-link-style-idx 0)
(def sequential-link-style-idx 1)
(def both-link-style-idx 2)

(defn link-style-name-to-idx
  [link-style-name]
  (let [idx (.indexOf link-style-names link-style-name)]
    (when (neg? idx) (throw (Exception. (str link-style-name " is not a link style."))))
    idx))

(defn link-indivs!
  [idx rng prob indivs]
  ((get link-style-fns idx (get link-style-fns initial-link-style-idx)) ; the fallback case works in first run, when the atom contains a nil
   rng
   prob
   indivs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; COMMUNITY: class for collections of Indivs or collections of Communities.

(defprotocol CommunityP
  (get-members [this]))

(deftype Community [id members]
  CommunityP
    (get-members [this] members) ; so I don't have to remember whether I used atoms
  Object
    (toString [this] (str id ": " (vec (map #(.id %) members)))))

;;; Initialization functions:

(defn make-community-of-indivs
  "Make a community with size number of indivs in it."
  [sim size]
  (let [indivs (vec (repeatedly size #(make-indiv sim))) ; it's short; don't wait for late-realization bugs.
        rng (.random sim)
        link-style-idx @(.linkStyleIdx (.instanceState sim))]
    (link-indivs! link-style-idx rng @(.linkProb (.instanceState sim)) indivs)
    (doseq [indiv indivs] 
      (set-rest-of-community! indiv (vec (remove-if-identical indiv indivs))))
    (->Community (str (gensym "c")) indivs)))

(defn make-communities-into-pop!
  "Given a collection of communities, returns a vector of individuals in all 
  of the communities after doing any additional housekeeping needed."
  [communities]
  (let [population (vec (mapcat get-members communities))]
    (doseq [indiv population]
      (set-rest-of-pop! indiv (vec (remove-if-identical indiv population))))
    population))
