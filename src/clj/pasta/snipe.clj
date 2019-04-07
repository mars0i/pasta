;; This software is copyright 2016, 2017, 2018, 2019 by Marshall Abrams, 
;; and is distributed under the Gnu General Public License version 3.0 
;; as specified in the the file LICENSE.

;(set! *warn-on-reflection* true)

(ns pasta.snipe
  (:require [clojure.math.numeric-tower :as math]
            [pasta.perception :as perc]
            [utils.random :as ran]
            [masonclj.properties :as props])
  (:import [sim.util Properties SimpleProperties Propertied] ;; TODO CAN I DELETE
           [sim.portrayal Oriented2D])
  (:gen-class                 ; so it can be aot-compiled
     :name pasta.snipe)) ; without :name other aot classes won't find it


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; INITIAL UTILITY DEFS

(declare -properties make-k-snipe make-r-snipe is-k-snipe? is-r-snipe? rand-energy)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DEFRECORD CLASS DEFS

;; The real difference between snipe types is in perception/preferences occurs,
;; so you don't really need separate defrecords--except that it's easier to
;; display snipes of different types differently in the UI if they're represented
;; by different Java classes.

;; Notes on Orientation2D, etc.:
;; value   orientation
;;   0       3:00
;;  pi/2     6:00
;;   pi      9:00
;;  -pi      9:00
;; 1.5*pi   12:00
;; -pi/2    12:00
;; FIXME THIS IS *REALLY* SLOW
(defn pref-orientation
  [minimum maximum value]
  (let [size (- maximum minimum) ; can I move this out so it's not recalc'ed every time?
        normalized-value (- (/ (- value minimum) size) ; scale value so it's in [-0.5 0.5]
                            0.5)
        orientation (* -1 normalized-value Math/PI)]
    (min (* 0.5 Math/PI)
         (max (* -0.5 Math/PI) 
              orientation)))) ; even given normalization some schemes might produce values outside the range

(defn make-get-curr-obj
  "Return a function that can be the value of getObject in Properties,
  i.e. that will return the current time-slice of a particular snipe.
  The function passed to defagent should take a single argument--
  is the original time-slice of the snipe.  Use partial to make that
  function from this one."
  [cfg-data$ original-snipe] ; pass cfg-data$ and not @cfg-data$ so the fn always uses the latest data.
  (fn [] ((:snipe-map (:popenv @cfg-data$)) (:id original-snipe))))

;; NOTE: defagent defines a defrecord ClassName and a special constructor -->ClassName
;; using make-get-curr-obj above:

;; K-strategy snipes use individual learning to determine which size of mushrooms 
;; are nutritious.  This takes time and can involve eating many poisonous mushrooms.
(props/defagent KSnipe [id perceive mush-pref energy subenv x y age lifespan cfg-data$]
  (partial make-get-curr-obj cfg-data$) ; get-object function will be a closure over cfg-data$
  [[:energy    java.lang.Double "Energy is what snipes get from mushrooms."]
   [:mush-pref java.lang.Double "Preference for large (positive number) or small (negative number) mushrooms."]
    [:subenv    java.lang.String "Name of snipe's subenv"]
    [:x         java.lang.Integer "x coordinate in underlying grid"]
    [:y         java.lang.Integer "y coordinate in underlying grid"]
    [:age       java.lang.Integer "Age of snipe"]
    [:lifespan  java.lang.Integer "Maximum age"]]
  Oriented2D
   (orientation2D [this] (pref-orientation -0.0004 0.0004 (:mush-pref this)))) ; TODO FIX THESE HARCODED VALUES?

;; Social snipes learn from the preferences of other nearby snipes.
(props/defagent SSnipe [id perceive mush-pref energy subenv x y age lifespan cfg-data$]
  (partial make-get-curr-obj cfg-data$) ; get-object function will be a closure over cfg-data$
  [[:energy    java.lang.Double "Energy is what snipes get from mushrooms."]
   [:mush-pref java.lang.Double "Preference for large (positive number) or small (negative number) mushrooms."]
    [:subenv    java.lang.String "Name of snipe's subenv"]
    [:x         java.lang.Integer "x coordinate in underlying grid"]
    [:y         java.lang.Integer "y coordinate in underlying grid"]
    [:age       java.lang.Integer "Age of snipe"]
    [:lifespan  java.lang.Integer "Maximum age"]]
  Oriented2D
   (orientation2D [this] (pref-orientation -0.0004 0.0004 (:mush-pref this)))) ; TODO FIX THESE HARCODED VALUES?

;; r-strategy snipes don't learn: They go right to work eating their preferred
;; size mushrooms, which may be the poisonous kind in their environment--or not.
;; Their children might have either size preference.  This means that the ones
;; that have the "right" preference can usually reproduce more quickly than k-snipes.
(props/defagent RSnipe [id perceive mush-pref energy subenv x y age lifespan cfg-data$]
  (partial make-get-curr-obj cfg-data$) ; get-object function will be a closure over cfg-data$
  [[:energy    java.lang.Double "Energy is what snipes get from mushrooms."]
   [:mush-pref java.lang.Double "Preference for large (positive number) or small (negative number) mushrooms."]
    [:subenv    java.lang.String "Name of snipe's subenv"]
    [:x         java.lang.Integer "x coordinate in underlying grid"]
    [:y         java.lang.Integer "y coordinate in underlying grid"]
    [:age       java.lang.Integer "Age of snipe"]
    [:lifespan  java.lang.Integer "Maximum age"]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SNIPE MAKER FUNCTIONS

(defn calc-lifespan
  [rng cfg-data]
  (let [mean (:lifespan-mean cfg-data)
        sd (:lifespan-sd cfg-data)]
    (if (pos? mean)
      (math/round (ran/next-gaussian rng mean sd))
      0)))

;; NOTE use of special defagent constructors -->?Snipe below:

(defn make-k-snipe 
  [rng cfg-data$ energy subenv new-id x y]
  (-->KSnipe new-id
             perc/k-snipe-pref ; perceive: function for responding to mushrooms
             0.0               ; mush-pref begins with indifference
             energy            ; initial energy level
             subenv            ; :west or :east
             x y               ; location of snipe on grid
             0                 ; age of snipe
             (calc-lifespan rng @cfg-data$) ; lifespan
             cfg-data$))       ; contains global parameters for snipe operation

(defn make-r-snipe
  [rng cfg-data$ energy subenv new-id x y]
  (let [extreme-pref (:extreme-pref @cfg-data$)]
    (let [pref (if (< (ran/next-double rng) 0.5)
                 (- extreme-pref)
                 extreme-pref)]
      (-->RSnipe new-id 
                 perc/r-snipe-pref
                 pref 
                 energy 
                 subenv x y 0 
                 (calc-lifespan rng @cfg-data$)
                 cfg-data$))))

(defn make-s-snipe 
  [rng cfg-data$ energy subenv new-id x y]
  (-->SSnipe new-id
             perc/s-snipe-pref ; use simple r-snipe method but a different starting strategy
             0.0               ; will be set soon by s-snipe-pref
             energy
             subenv
             x y
             0
             (calc-lifespan rng @cfg-data$)
             cfg-data$))

(defn make-newborn-k-snipe 
  [rng cfg-data$ subenv new-id x y]
  (let [{:keys [initial-energy]} @cfg-data$]
    (make-k-snipe rng cfg-data$ initial-energy subenv new-id x y)))

(defn make-newborn-r-snipe
  [rng cfg-data$ subenv new-id x y]
  (let [{:keys [initial-energy]} @cfg-data$]
    (make-r-snipe rng cfg-data$ initial-energy subenv new-id x y)))

(defn make-newborn-s-snipe 
  [rng cfg-data$ subenv new-id x y]
  (let [{:keys [initial-energy]} @cfg-data$]
    (make-s-snipe rng cfg-data$ initial-energy subenv new-id x y)))

(defn make-rand-k-snipe 
  "Create k-snipe with random energy (from rand-energy)."
  [rng cfg-data$ subenv new-id x y]
  (make-k-snipe rng cfg-data$ (rand-energy rng @cfg-data$) subenv new-id x y))

(defn make-rand-r-snipe 
  "Create r-snipe with random energy (from rand-energy)."
  [rng cfg-data$ subenv new-id x y]
  (make-r-snipe rng cfg-data$ (rand-energy rng @cfg-data$) subenv new-id x y))

(defn make-rand-s-snipe 
  "Create s-snipe with random energy (from rand-energy)."
  [rng cfg-data$ subenv new-id x y]
  (make-s-snipe rng cfg-data$ (rand-energy rng @cfg-data$) subenv new-id x y))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MISCELLANEOUS LITTLE FUNCTIONS

;; SHOULD THIS BE GAUSSIAN?
;; Is birth-threshold the right limit?
(defn rand-energy
  "Generate random energy value uniformly distributed in [0, birth-threshold)."
  [rng cfg-data]
  (math/round (* (:birth-threshold cfg-data) ; round isn't essential. just makes it easier to watch individual snipes.
                 (ran/next-double rng))))

(defn clean
  "Returns a copy of the snipe with its cfg.data$ atom removed so that
  it can be displayed in a repl without creating an infinite loop (since
  cfg-data$ contains a subenv which contains a hash of all snipes)."
  [snipe]
  (dissoc snipe :cfg-data$))

;; note underscores
(defn k-snipe? [s] (instance? pasta.snipe.KSnipe s))
(defn r-snipe? [s] (instance? pasta.snipe.RSnipe s))
(defn s-snipe? [s] (instance? pasta.snipe.SSnipe s))
