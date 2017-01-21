(ns free-agent.snipe
  (:require [free-agent.eat :as perc]) ; [free-agent.level :as l]
  (:import [sim.util Properties SimpleProperties Propertied])
  (:gen-class                 ; so it can be aot-compiled
     :name free-agent.snipe)) ; without :name other aot classes won't find it

;; The real difference between k- and r-snipes is in how levels is implemented,
;; but it will be useful to have two different wrapper classes to make it easier to
;; observe differences.

(declare next-id make-properties make-k-snipe make-r-snipe is-k-snipe? is-r-snipe?)

;; The two atom fields at the end are there solely for interactions with the UI.
;; Propertied/properties is used by GUI to allow inspectors to follow a fnlly updated agent.
(defrecord KSnipe [id levels perceive eat energy x y circled$ cfg-data$]
  Propertied
  (properties [original-snipe] (make-properties id cfg-data$))
  Object
  (toString [_] (str "<KSnipe #" id">")))

;; See comments on KSnipe.
(defrecord RSnipe [id levels perceive eat energy x y circled$ cfg-data$]
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
            nil ;; TODO construct levels function here using prior
            perc/k-snipe-eat ; perceive
            0.0      ; eat
            energy
            x y
            (atom false)
            cfg-data$)))

(defn make-r-snipe
  ([cfg-data$ x y]
   (let [{:keys [initial-energy r-snipe-low-prior r-snipe-high-prior]} @cfg-data$]
     (make-r-snipe cfg-data$ initial-energy r-snipe-low-prior r-snipe-high-prior x y)))
  ([cfg-data$ energy low-prior high-prior x y]
   (RSnipe. (next-id)
            nil ;; TODO construct levels function here using prior (one of two values, randomly)
            perc/r-snipe-eat ; perceive
            0.0      ; eat
            energy
            x y
            (atom false)
            cfg-data$)))

;; Does gensym avoid bottleneck??
(defn next-id 
  "Returns a unique integer for use as an id."
  [] 
  (Long. (str (gensym ""))))

;; This is unlikely to become part of clojure.core: http://dev.clojure.org/jira/browse/CLJ-1298
(defn atom? [x] (instance? clojure.lang.Atom x))

;; Used by GUI to allow inspectors to follow a fnlly updated agent.
;; (Code below makes use of the fact that in Clojure, vectors can be treated as functions
;; of indexes, returning the indexed item; that keywords such as :x can be treated as 
;; functions of maps; and that defrecords such as snipes can be treated as maps.)
(defn make-properties
  "Return a Properties subclass for use by Propertied's properties method."
  [id cfg-data$]
  ;; These definitions need to be coordinated by hand:
  (let [kys [:energy :x :y :circled$]
        circled-idx 3 ; HARDCODED INDEX for circled$ field
        descriptions ["Energy is what snipes get from mushrooms."
                      "x coordinate in underlying grid"
                      "y coordinate in underlying grid"
                      "Boolean indicating whether circled in GUI"]
        types [java.lang.Double java.lang.Integer java.lang.Integer java.lang.Boolean]
        names (mapv name kys)
        num-properties (count kys)
        hidden     (vec (repeat num-properties false))
        read-write [false false false true] ; allow user to turn off circled in UI
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
(defn is-k-snipe? [s] (instance? free_agent.snipe.KSnipe s))
(defn is-r-snipe? [s] (instance? free_agent.snipe.RSnipe s))
