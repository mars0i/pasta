(ns free-agent.mush
  (:require [utils.random :as ran])
  (:gen-class                ; so it can be aot-compiled
    :name free-agent.mush))  ; without :name other aot classes won't find it

;; Could be a bottleneck if reproduction starts happening in different threads.  In that case I could switch to e.g. (str (gensym "i")) as in intermmitran.
(def prev-mush-id (atom -1)) ; first inc'ed value will be 0

(defrecord Mush [id f])

(defn make-mush [rng mean sd]
  (Mush. (swap! prev-mush-id inc)
             #(ran/next-gaussian rng mean sd)))
