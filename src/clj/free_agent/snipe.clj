(ns free-agent.snipe
  (:require [free-agent.perception2 :as perc] ; [free-agent.level :as l]
            [utils.random :as ran])
  (:import [sim.util Properties SimpleProperties Propertied])
  (:gen-class                 ; so it can be aot-compiled
     :name free-agent.snipe)) ; without :name other aot classes won't find it

;; The real difference between k- and r-snipes is in how levels is implemented,
;; but it will be useful to have two different wrapper classes to make it easier to
;; observe differences.

(declare next-id make-properties make-k-snipe make-r-snipe is-k-snipe? is-r-snipe?)

;; The two atom fields at the end are there solely for interactions with the UI.
;; Propertied/properties is used by GUI to allow inspectors to follow a fnlly updated agent.
(defrecord KSnipe [id perceive mush-pref energy x y age circled$ cfg-data$]
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [_] (str "<KSnipe #" id">")))

;; Creating two identical RSnipe defrecords, which will differ only
;; in the value of mush-pref, so it will be simpler to keep track
;; of the difference.

;; r-snipe that prefers small mushrooms
;; See comments on KSnipe.
(defrecord RSnipePrefSmall [id perceive mush-pref energy x y age circled$ cfg-data$]
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [this] (str "<RSnipePrefSmall #" id ">")))

;; r-snipe that prefers large mushrooms
(defrecord RSnipePrefBig [id perceive mush-pref energy x y age circled$ cfg-data$]
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [this] (str "<RSnipePrefBig #" id ">")))

(defn make-k-snipe 
  [cfg-data$ energy x y]
  (KSnipe. (next-id)
           perc/k-snipe-pref ; perceive: function for responding to mushrooms
           0.0      ; mush-pref begins with indifference
           energy
           x y
           0
           (atom false)
           cfg-data$))

(defn make-r-snipe
  [rng cfg-data$ energy x y]
  (if (< (ran/next-double rng) 0.5)
    (RSnipePrefSmall. (next-id) perc/r-snipe-pref -100.0 energy x y 0 (atom false) cfg-data$)
    (RSnipePrefBig.   (next-id) perc/r-snipe-pref  100.0 energy x y 0 (atom false) cfg-data$)))

(defn make-newborn-k-snipe 
  [cfg-data$ x y]
  (let [{:keys [initial-energy]} @cfg-data$]
    (make-k-snipe cfg-data$ initial-energy x y)))

(defn make-newborn-r-snipe
  [rng cfg-data$ x y]
  (let [{:keys [initial-energy]} @cfg-data$]
    (make-r-snipe rng cfg-data$ initial-energy x y)))

;; SHOULD THIS BE GAUSSIAN?
;; Is birth-threshold the right limit?
(defn rand-energy
  "Generate random energy value uniformly distributed in [0, birth-threshold)."
  [rng cfg-data]
  (* (:birth-threshold cfg-data)
     (ran/next-double rng)))

(defn make-rand-k-snipe 
  "Create k-snipe with random energy (from rand-energy)."
  [rng cfg-data$ x y]
  (make-k-snipe cfg-data$ (rand-energy rng @cfg-data$) x y))

(defn make-rand-r-snipe 
  "Create r-snipe with random energy (from rand-energy)."
  [rng cfg-data$ x y]
  (make-r-snipe rng cfg-data$ (rand-energy rng @cfg-data$) x y))

(defn atom? [x] (instance? clojure.lang.Atom x)) ; This is unlikely to become part of clojure.core: http://dev.clojure.org/jira/browse/CLJ-1298

;; Used by GUI to allow inspectors to follow a fnlly updated agent.
;; (Code below makes use of the fact that in Clojure, vectors can be treated as functions
;; of indexes, returning the indexed item; that keywords such as :x can be treated as 
;; functions of maps; and that defrecords such as snipes can be treated as maps.)
(defn make-properties
  "Return a Properties subclass for use by Propertied's properties method so
  that certain fields can be displayed in the GUI on request."
  [id cfg-data$]
  ;; These definitions need to be coordinated by hand:
  (let [kys [:energy :mush-pref :x :y :age :circled$]       ; CHANGE FOR NEW FIELDS
        circled-idx 5 ; HARDCODED INDEX for circled$ field  ; CHANGE FOR NEW FIELDS
        descriptions ["Energy is what snipes get from mushrooms." ; CHANGE FOR NEW FIELDS
                      "Preference for large (positive number) or small (negative number) mushrooms."
                      "x coordinate in underlying grid"
                      "y coordinate in underlying grid"
                      "Age of snipe"
                      "Boolean indicating whether circled in GUI"]
        types [java.lang.Double java.lang.Double java.lang.Integer java.lang.Integer java.lang.Integer java.lang.Boolean] ; CHANGE FOR NEW FIELDS
        read-write [false false false false false true] ; allow user to turn off circled in UI ; CHANGE FOR NEW FIELDS
        names (mapv name kys)
        num-properties (count kys)
        hidden     (vec (repeat num-properties false))
        get-curr-snipe (fn [] ((:snipes (:popenv @cfg-data$)) id))]
    (reset! (:circled$ (get-curr-snipe)) true) ; make-properties only called by inspector, in which case highlight snipe in UI
    (proxy [Properties] []
      (getObject [] (get-curr-snipe))
      (getName [i] (names i))
      (getDescription [i] (descriptions i))
      (getType [i] (types i))
      (getValue [i]
        (let [v ((kys i) (get-curr-snipe))]
          (if (atom? v) @v v)))
      (setValue [i newval]                  ; allow user to turn off circled in UI  ; POSS CHANGE FOR NEW FIELDS
        (when (= i circled-idx)             ; returns nil/null for other fields
          (reset! (:circled$ (get-curr-snipe))
                  (Boolean/valueOf newval)))) ; it's always a string that's returned from UI. (Do *not* use (Boolean. newval); it's always truthy in Clojure.)
      (isHidden [i] (hidden i))
      (isReadWrite [i] (read-write i))
      (isVolatile [] false)
      (numProperties [] num-properties)
      (toString [] (str "<SimpleProperties: snipe id=" id ">")))))


;; note underscores
(defn k-snipe? [s] (instance? free_agent.snipe.KSnipe s))
(defn r-snipe-pref-small? [s] (instance? free_agent.snipe.RSnipePrefSmall s))
(defn r-snipe-pref-big?   [s] (instance? free_agent.snipe.RSnipePrefBig s))
(defn r-snipe? [s] (or (r-snipe-pref-small? s) (r-snipe-pref-big? s)))

;; Switching to simple, non-gensym version so that this also tracks
;; total number of snipes that have lived.
(def num-snipes$ (atom 0))
(defn next-id
  "Returns a unique integer for use as an id."
  [] 
  (swap! num-snipes$ inc))

;; Does gensym avoid bottleneck??
;(defn next-id 
;  "Returns a unique integer for use as an id."
;  [] 
;  (Long. (str (gensym ""))))
