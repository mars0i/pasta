(ns free-agent.snipe
  (:require [free-agent.level :as l])
  (:gen-class                 ; so it can be aot-compiled
     :name free-agent.snipe)) ; without :name other aot classes won't find it

;; Could be a bottleneck if reproduction starts happening in different threads. In that case I could switch to e.g. (str (gensym "i")) as in intermmitran.
;(def prev-snipe-id (atom -1)) ; first inc'ed value will be 0

(defn next-id 
  "Returns a unique integer for use as an id."
  [] 
  (Long. (str (gensym ""))))

;(defprotocol InspectedSnipe
;  "Methods to allow MASON to inspect snipe states."
;  (getEnergy ^double [this]))

(definterface InspectedSnipe
  (^double getEnergy []))

;; to see that this method is visible for snipes, try this:
;; (pprint (.getDeclaredMethods (class k)))

;; The real difference between k- and r-snipes is in how levels is implemented,
;; but it will be useful to have two different wrapper classes to make it easier to
;; observe differences.

(defrecord KSnipe [id levels energy x y] ; levels is a sequence of free-agent.Levels
  InspectedSnipe
  (getEnergy [this] (:energy this)))

(defrecord RSnipe [id levels energy x y] ; levels is a sequence of free-agent.Levels
  InspectedSnipe
  (getEnergy [this] (:energy this)))

(defn make-k-snipe 
  ([cfg-data x y]
   (let [{:keys [energy k-snipe-prior]} cfg-data]
     (make-k-snipe energy k-snipe-prior x y)))
  ([energy prior x y]
   (KSnipe. (next-id)
            nil ;; TODO construct levels function here using prior
            energy
            x y)))

(defn make-r-snipe
  ([cfg-data x y]
   (let [{:keys [energy r-snipe-low-prior r-snipe-high-prior]} cfg-data]
     (make-r-snipe energy r-snipe-low-prior r-snipe-high-prior x y)))
  ([energy low-prior high-prior x y]
   (RSnipe. (next-id)
            nil ;; TODO construct levels function here using prior (one of two values, randomly)
            energy
            x y)))

;; note underscores
(defn is-k-snipe? [s] (instance? free_agent.KSnipe s))
(defn is-r-snipe? [s] (instance? free_agent.RSnipe s))
