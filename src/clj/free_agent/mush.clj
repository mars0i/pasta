(ns free-agent.mush
  (:require [utils.random :as ran])
  (:gen-class                ; so it can be aot-compiled
    :name free-agent.mush))  ; without :name other aot classes won't find it

;; Could be a bottleneck if reproduction starts happening in different threads.  In that case I could switch to e.g. (str (gensym "i")) as in intermmitran.
;(def prev-mush-id (atom -1)) ; first inc'ed value will be 0
(defn next-id 
  "Returns a unique integer for use as an id."
  [] 
  (Long. (str (gensym ""))))


(defrecord Mush [id mean sd nutrition rng])

(defn make-mush [mean sd nutrition rng]
  (Mush. (next-id) mean sd nutrition rng))

(defn appearance
  [mush]
  (let [{:keys [mean sd rng]} mush]
    (ran/next-gaussian rng mean sd)))
