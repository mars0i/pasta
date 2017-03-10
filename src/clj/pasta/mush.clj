;; This software is copyright 2016, 2017 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns pasta.mush
  (:require [utils.random :as ran])
  (:gen-class                ; so it can be aot-compiled
    :name pasta.mush))  ; without :name other aot classes won't find it

;; Could be a bottleneck if reproduction starts happening in different threads.  In that case I could switch to e.g. (str (gensym "i")) as in intermmitran.
;(def prev-mush-id (atom -1)) ; first inc'ed value will be 0
(defn next-id 
  "Returns a unique integer for use as an id."
  [] 
  (Long. (str (gensym ""))))


(defrecord Mush [id size sd nutrition rng])

(defn make-mush [size sd nutrition rng]
  (Mush. (next-id) size sd nutrition rng))

(defn appearance
  [mush]
  (let [{:keys [size sd rng]} mush]
    (ran/next-gaussian rng size sd))) ; size used as mean
