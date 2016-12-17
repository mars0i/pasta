(ns free-agent.mushroom
  (:require [utils.random :as ran])
  (:gen-class)) ; so can be aot-compiled

;; Could be a bottleneck if reproduction starts happening in different threads.  In that case I could switch to e.g. (str (gensym "i")) as in intermmitran.
(def prev-mush-id (atom -1)) ; first inc'ed value will be 0

(defrecord Mushroom [id f])

(defn make-mushroom [rng mean sd]
  (Mushroom. (swap! prev-mush-id inc)
             #(ran/next-gaussian rng mean sd)))
