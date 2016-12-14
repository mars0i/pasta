(ns free-agent.snipe
  (:require [free-agent.level :as l]))

;; Could be a bottleneck if reproduction starts happening in different threads. In that case I could switch to e.g. (str (gensym "i")) as in intermmitran.
;(def prev-snipe-id (atom -1)) ; first inc'ed value will be 0

(defn next-id 
  "Returns a unique integer for use as an id."
  [] 
  #(Long. (str (gensym ""))))

;; The real difference between k- and r-snipes is in how levels is implemented,
;; but it will be useful to have two different wrapper classes to make it easier to
;; observe differences.
(defrecord KSnipe [id levels energy]) ; levels is a sequence of free-agent.Levels
(defrecord RSnipe [id levels energy]) ; levels is a sequence of free-agent.Levels

(defn make-k-snipe [energy prior]
  (KSnipe. (next-id)
           nil ;; TODO construct levels here using prior
           energy))

(defn make-r-snipe [energy prior-0 prior-1]
  (RSnipe. (next-id)
           nil ;; TODO construct levels here using prior (one of two values, randomly)
           energy))
