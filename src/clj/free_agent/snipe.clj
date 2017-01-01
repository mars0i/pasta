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

(defn get-curr-snipe
  [id cfg-data$]
  (let [snipe-field (:snipe-field (:popenv @cfg-data$))]
    (some #(= id (:id %)) (.elements snipe-field))))

(defn record-curr-snipe!
  [id cfg-data$ snipe$]
  (swap! snipe$ assoc :snipe (get-curr-snipe id cfg-data$)))

(def num-snipe-now-accs 3)

(defn maybe-clear-snipe!
  [accs-called$ snipe$]
  (when (= @accs-called$ num-snipe-now-accs)
    (reset! snipe$ nil)
    (reset! accs-called$ 0)))

(defn get-field!
  [k id cfg-data$ snipe$ accs-called$]
  (when-not @snipe$ (record-curr-snipe! id cfg-data$ snipe$)) ; if snipe$ empty, go get current snipe
  (let [data (k @snipe$)]    ; read data from current snipe
    (swap! accs-called$ inc) ; count how many methods called
    (maybe-clear-snipe! accs-called$ snipe$) ; if all called, flush out the curr snipe for next time
    data))

(defprotocol InspectedSnipeP
  (getEnergy [this])
  (getX [this])
  (getY [this]))

;; An inspector proxy that will go out and get the current snipe for a given id and return its data
(defrecord SnipeNow [serialVersionUID id cfg-data$ snipe$ accs-called$] ; first arg required by Mason for serialization
  InspectedSnipeP
  (getEnergy [this] (get-field! :energy id cfg-data$ snipe$ accs-called$))
  (getX [this] (get-field! :x id cfg-data$ snipe$ accs-called$))
  (getY [this] (get-field! :y id cfg-data$ snipe$ accs-called$))
  Object
  (toString [this] (str "<SnipeNow #" id ">")))

(defprotocol SnipeP
  (getEnergy [this])
  (get-energy [this])
  (get-x [this])
  (get-y [this])
  (set-energy! [this new-val])
  (set-x! [this new-val])
  (set-y! [this new-val]))

;; Note levels is a sequence of free-agent.Levels
;; The fields are apparently automatically visible to the MASON inspector system. (!)
(deftype KSnipe [id levels energy x y]
  Proxiable ; for inspectors
  (propertiesProxy [this] (println "called propertiesProxy") (SnipeNow. 1 id cfg-data$ nil 0))
  SnipeP
  (getEnergy [this] energy)
  (get-energy [this] energy)
  (get-x [this] x)
  (get-y [this] y)
  (set-energy! [this new-val] (set! energy new-val))
  (set-x! [this new-val] (set! x new-val))
  (set-y! [this new-val] (set! y new-val))
  Object
  (toString [this] (str "<KSnipe #" id " energy: " energy ">")))

(deftype RSnipe [id levels energy x y]
  Proxiable ; for inspectors
  (propertiesProxy [this] (println "called propertiesProxy") (SnipeNow. 1 id cfg-data$ nil 0))
  SnipeP
  (getEnergy [this] energy)
  (get-energy [this] energy)
  (get-x [this] x)
  (get-y [this] y)
  (set-energy! [this new-val] (set! energy new-val))
  (set-x! [this new-val] (set! x new-val))
  (set-y! [this new-val] (set! y new-val))
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
