(ns free-agent.snipe
  (:require [clojure.math.numeric-tower :as math]
            [free-agent.perception :as perc]
            [utils.random :as ran])
  (:import [sim.util Properties SimpleProperties Propertied])
  (:gen-class                 ; so it can be aot-compiled
     :name free-agent.snipe)) ; without :name other aot classes won't find it


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; INITIAL UTILITY DEFS

(declare next-id make-properties make-k-snipe make-r-snipe is-k-snipe? is-r-snipe? rand-energy atom?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DEFRECORD CLASS DEFS

;; The real difference between snipe types is in perception/preferences occurs,
;; so you don't really need separate defrecords--except that it's easier to
;; display snipes of different types differently in the UI if they're represented
;; by different Java classes.

;; The two atom fields at the end are there solely for interactions with the UI.
;; Propertied/properties is used by GUI to allow inspectors to follow a fnlly updated agent.

;; K-strategy snipes use individual learning to determine which size of mushrooms 
;; are nutritious.  This takes time and can involve eating many poisonous mushrooms.
(defrecord KSnipe [id perceive mush-pref energy x y age circled$ cfg-data$]
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [_] (str "<KSnipe #" id">")))

;; r-strategy snipes don't learn: They go right to work eating their preferred
;; size mushrooms, which may be the poisonous kind in their environment--or not.
;; Their children might have either size preference.  This means that the ones
;; that have the "right" preference can usually reproduce more quickly than k-snipes.
(defrecord RSnipePrefSmall [id perceive mush-pref energy x y age circled$ cfg-data$] ; r-snipe that prefers small mushrooms
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [this] (str "<RSnipePrefSmall #" id ">")))

(defrecord RSnipePrefBig [id perceive mush-pref energy x y age circled$ cfg-data$] ; r-snipe that prefers large mushrooms
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [this] (str "<RSnipePrefBig #" id ">")))

;; Social snipes learn from the preferences of other nearby snipes.
(defrecord SSnipe [id perceive mush-pref energy x y age circled$ cfg-data$]
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [_] (str "<SSnipe #" id">")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SNIPE MAKER FUNCTIONS

(defn make-k-snipe 
  [cfg-data$ energy x y]
  (KSnipe. (next-id)
           perc/k-snipe-pref ; perceive: function for responding to mushrooms
           0.0               ; mush-pref begins with indifference
           energy            ; initial energy level
           x y               ; location of snipe on grid
           0                 ; age of snipe
           (atom false)      ; is snipe displayed circled in the GUI?
           cfg-data$))       ; contains global parameters for snipe operation

(defn make-r-snipe
  [rng cfg-data$ energy x y]
  (let [extreme-pref (:extreme-pref @cfg-data$)]
    (if (< (ran/next-double rng) 0.5)
      (RSnipePrefSmall. (next-id) perc/r-snipe-pref (- extreme-pref) energy x y 0 (atom false) cfg-data$)
      (RSnipePrefBig.   (next-id) perc/r-snipe-pref extreme-pref     energy x y 0 (atom false) cfg-data$))))

(defn make-s-snipe 
  [cfg-data$ energy x y]
  (SSnipe. (next-id)
           perc/s-snipe-pref-success-bias
           ;perc/s-snipe-pref-freq-bias
           0.0 
           energy
           x y
           0
           (atom false)
           cfg-data$))

(defn make-newborn-k-snipe 
  [cfg-data$ x y]
  (let [{:keys [initial-energy]} @cfg-data$]
    (make-k-snipe cfg-data$ initial-energy x y)))

(defn make-newborn-r-snipe
  [rng cfg-data$ x y]
  (let [{:keys [initial-energy]} @cfg-data$]
    (make-r-snipe rng cfg-data$ initial-energy x y)))

(defn make-newborn-s-snipe 
  [cfg-data$ x y]
  (let [{:keys [initial-energy]} @cfg-data$]
    (make-s-snipe cfg-data$ initial-energy x y)))

(defn make-rand-k-snipe 
  "Create k-snipe with random energy (from rand-energy)."
  [rng cfg-data$ x y]
  (make-k-snipe cfg-data$ (rand-energy rng @cfg-data$) x y))

(defn make-rand-r-snipe 
  "Create r-snipe with random energy (from rand-energy)."
  [rng cfg-data$ x y]
  (make-r-snipe rng cfg-data$ (rand-energy rng @cfg-data$) x y))

(defn make-rand-s-snipe 
  "Create s-snipe with random energy (from rand-energy)."
  [rng cfg-data$ x y]
  (make-s-snipe cfg-data$ (rand-energy rng @cfg-data$) x y))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MAKE-PROPERTIES FUNCTION

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MISCELLANEOUS LITTLE FUNCTIONS

(defn atom? [x] (instance? clojure.lang.Atom x)) ; This is unlikely to become part of clojure.core: http://dev.clojure.org/jira/browse/CLJ-1298

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
  cfg-data$ contains popenv which contains a hash of all snipes)."
  [snipe]
  (dissoc snipe :cfg-data$))

;; note underscores
(defn k-snipe? [s] (instance? free_agent.snipe.KSnipe s))
(defn r-snipe-pref-small? [s] (instance? free_agent.snipe.RSnipePrefSmall s))
(defn r-snipe-pref-big?   [s] (instance? free_agent.snipe.RSnipePrefBig s))
(defn r-snipe? [s] (or (r-snipe-pref-small? s) (r-snipe-pref-big? s)))
(defn s-snipe? [s] (instance? free_agent.snipe.SSnipe s))

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
