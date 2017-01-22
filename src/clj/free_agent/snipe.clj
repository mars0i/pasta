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
(defrecord KSnipe [id perceive mush-pref energy x y circled$ cfg-data$]
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [_] (str "<KSnipe #" id">")))

;; OBSOLETE
;(defrecord RSnipe [id perceive mush-pref energy x y circled$ cfg-data$]
;  Propertied
;  (properties [original-snipe] (make-properties id cfg-data$))
;  Object
;  (toString [this] (str "<RSnipe #" id ">")))

;; Creating two identical RSnipe defrecords, which will differ only
;; in the value of mush-pref, so it will be simpler to keep track
;; of the difference.

;; r-snipe that prefers small mushrooms
;; See comments on KSnipe.
(defrecord RSnipePrefSmall [id perceive mush-pref energy x y circled$ cfg-data$]
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [this] (str "<RSnipe #" id ">")))

;; r-snipe that prefers large mushrooms
(defrecord RSnipePrefBig [id perceive mush-pref energy x y circled$ cfg-data$]
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [this] (str "<RSnipe #" id ">")))

(defn make-k-snipe 
  ([cfg-data$ x y]
   (let [{:keys [initial-energy k-snipe-prior]} @cfg-data$]
     (make-k-snipe cfg-data$ initial-energy k-snipe-prior x y)))
  ([cfg-data$ energy prior x y]
   (KSnipe. (next-id)
            perc/k-snipe-pref ; perceive: function for responding to mushrooms
            0.0      ; mush-pref begins with indifference
            energy
            x y
            (atom false)
            cfg-data$)))

(defn make-r-snipe
  ([rng cfg-data$ x y]
   (let [{:keys [initial-energy]} @cfg-data$]
     (make-r-snipe rng cfg-data$ initial-energy x y)))
  ([rng cfg-data$ energy x y]
   (if (< (ran/next-double rng) 0.5)
   (RSnipePrefSmall. (next-id) perc/r-snipe-pref -100.0 energy x y (atom false) cfg-data$)
   (RSnipePrefBig.   (next-id) perc/r-snipe-pref  100.0 energy x y (atom false) cfg-data$))))

;(defn old-make-r-snipe
;  ([rng cfg-data$ x y]
;   (let [{:keys [initial-energy r-snipe-low-prior r-snipe-high-prior]} @cfg-data$]
;     (make-r-snipe rng cfg-data$ initial-energy r-snipe-low-prior r-snipe-high-prior x y)))
;  ([rng cfg-data$ energy low-prior high-prior x y] ; priors currently unused
;   (RSnipe. (next-id)
;            perc/r-snipe-pref
;            (if (< (ran/next-double rng) 0.5) -100.0 100.0)
;            energy
;            x y
;            (atom false)
;            cfg-data$)))

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
  (let [kys [:energy :mush-pref :x :y :circled$]
        circled-idx 4 ; HARDCODED INDEX for circled$ field
        descriptions ["Energy is what snipes get from mushrooms."
                      "Preference for large (positive number) or small (negative number) mushrooms."
                      "x coordinate in underlying grid"
                      "y coordinate in underlying grid"
                      "Boolean indicating whether circled in GUI"]
        types [java.lang.Double java.lang.Double java.lang.Integer java.lang.Integer java.lang.Boolean]
        names (mapv name kys)
        num-properties (count kys)
        hidden     (vec (repeat num-properties false))
        read-write [false false false false true] ; allow user to turn off circled in UI
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
      (setValue [i newval]                  ; allow user to turn off circled in UI
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

;; Does gensym avoid bottleneck??
(defn next-id 
  "Returns a unique integer for use as an id."
  [] 
  (Long. (str (gensym ""))))
