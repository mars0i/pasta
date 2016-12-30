(ns free-agent.snipe
  (:require [free-agent.level :as l])
  ;(:import [sim.engine Steppable])
  (:gen-class                 ; so it can be aot-compiled
     :name free-agent.snipe)) ; without :name other aot classes won't find it

;; Could be a bottleneck if reproduction starts happening in different threads.
;(def prev-snipe-id (atom -1)) ; first inc'ed value will be 0

;; Does gensym avoid the bottleneck??
(defn next-id 
  "Returns a unique integer for use as an id."
  [] 
  (Long. (str (gensym ""))))

;; The real difference between k- and r-snipes is in how levels is implemented,
;; but it will be useful to have two different wrapper classes to make it easier to
;; observe differences.

(defprotocol InspectedSnipeP
  (getEnergy [this]))

;(definterface InspectedSnipeI
;  (getEnergy []))

;; Note levels is a sequence of free-agent.Levels
;; The fields are apparently automatically visible to the MASON inspector system. (!)
(defrecord KSnipe [id levels energy x y]
  InspectedSnipeP
  (getEnergy [this] energy)
  Object
  (toString [this] (str "<KSnipe #" id " energy: " energy ">")))

(defrecord RSnipe [id levels energy x y]
  InspectedSnipeP
  (getEnergy [this] energy)
  Object
  (toString [this] (str "<RSnipe #" id " energy: " energy ">")))

(defn make-k-snipe 
  ([cfg-data x y]
   (let [{:keys [initial-energy k-snipe-prior]} cfg-data]
     (make-k-snipe initial-energy k-snipe-prior x y)))
  ([energy prior x y]
   (KSnipe. (next-id)
            nil ;; TODO construct levels function here using prior
            energy
            x y)))

(defn make-r-snipe
  ([cfg-data x y]
   (let [{:keys [initial-energy r-snipe-low-prior r-snipe-high-prior]} cfg-data]
     (make-r-snipe initial-energy r-snipe-low-prior r-snipe-high-prior x y)))
  ([energy low-prior high-prior x y]
   (RSnipe. (next-id)
            nil ;; TODO construct levels function here using prior (one of two values, randomly)
            energy
            x y)))

;; note underscores
(defn is-k-snipe? [s] (instance? free_agent.snipe.KSnipe s))
(defn is-r-snipe? [s] (instance? free_agent.snipe.RSnipe s))


;; Incredibly, the following is not needed in order for snipes to be inspectable.
;; MASON simply sees the record fields as properties.
;; Thank you Clojure and MASON.
;;
;;     (defprotocol InspectedSnipe (getEnergy [this]))
;;     (definterface InspectedSnipe (^double getEnergy []))
;;     To see that this method is visible for snipes, try this:
;;     (pprint (.getDeclaredMethods (class k)))
