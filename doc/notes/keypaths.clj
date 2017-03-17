;; Ways to list key paths in embedded maps

;; From 
;; http://stackoverflow.com/questions/21768802/how-can-i-get-the-nested-keys-of-a-map-in-clojure:
;; Responses to David Rz Ayala's question.
;; Not included:
;; Arthur Ulfeldt's main answer responds to the question behind Ayala's
;; question about generating keypaths.

;; David Rz Ayala's original data example:
(def dra {:a :A :b :B :c {:d :D} :e {:f {:g :G :h :H}}})

;; Other data examples:
(def b {:b1 {:c1 1, :c2 2} :b2 {:c1 3, :c2 4}})

(def a {:a1 {:b1 {:c1 1, :c2 2} :b2 {:c2 3}} :a2 {:b2 {:c1 4 :c2 5}}}) 

;; miner49r's:
(defn miner49r-keypaths
  ([m] (miner49r-keypaths [] m))
  ([prev m]
   (reduce-kv (fn [res k v] 
                (if (map? v)
                  (into res (miner49r-keypaths (conj prev k) v))
                  (conj res (conj prev k))))
              []
              m)))

;; A. Webb's:
(require '[clojure.zip :as z])

(defn AWebb-keypaths [m] 
  (letfn [(branch? [[path m]] (map? m)) 
          (children [[path m]] (for [[k v] m] [(conj path k) v]))] 
    (if (empty? m) 
      []
      (loop [t (z/zipper branch? children nil [[] m]), paths []] 
        (cond (z/end? t) paths 
              (z/branch? t) (recur (z/next t), paths) 
              :leaf (recur (z/next t), (conj paths (first (z/node t)))))))))

;; Alex Miller's:
(defn AlexMiller-keypaths [m]
  (if (map? m)
    (vec 
     (mapcat (fn [[k v]]
               (let [sub (AlexMiller-keypaths v)
                     nested (map #(into [k] %)
                                 (filter (comp not empty?) sub))]
                 (if (seq nested)
                   nested
                   [[k]])))
             m))
    []))

;; amalloy's (note you have to wrap result in doall or vec for time trials):
(defn amalloy-keypaths [m]
  (if (or (not (map? m))
          (empty? m))
    '(())
    (for [[k v] m
          subkey (amalloy-keypaths v)]
      (cons k subkey))))

;; noisesmith's at duplicate question:
;; http://stackoverflow.com/questions/25268818/get-key-chains-of-a-tree-in-clojure
(defn noisesmith-keypaths
  ([trie] (noisesmith-keypaths trie []))
  ([trie prefix]
     (if (map? trie)
       (mapcat (fn [[k v]]
                 (noisesmith-keypaths v (conj prefix k)))
               trie)
       [prefix])))

;; Nathan Marz's versions, from 
;; https://clojurians.slack.com/archives/C0FVDQLQ5/p1489779215484550
;; (I subsequently added them as answers to the question above.)

;; Simple versioN:
(defn simple-specter-keypaths [m]
  (let [p (recursive-path [] p
            (if-path map?
              [ALL (collect-one FIRST) LAST p]
              STAY))]
    (map butlast (select p m))))

;; More efficient version:
(defn fast-specter-keypaths [m]
  (let [p (recursive-path [] p
            (if-path map?
              [ALL
               (if-path [LAST map?]
                [(collect-one FIRST) LAST p]
                FIRST)]))]
    (select p m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Versions that return intermediate keypaths as well:

;; Laurent's:
(require '[clojure.set :as set])

(defn Laurent-keypaths-all
  ([m current]
   ;; base case: map empty or not a map
   (if (or (not (map? m)) (empty? m))
     #{current}
   ;; else: recursive call for every (key, value) in the map
     (apply set/union #{current}
            (map (fn [[k v]]
                   (Laurent-keypaths-all v (conj current k)))
                 m))))
  ([m]
   (-> m (all-paths []) (disj []))))

;; Aaron Cummings':
(defn AaronCummings-keypaths-all
  ([m] (if (map? m) (AaronCummings-keypaths (seq m) [])))
  ([es c]
   (lazy-seq
    (when-let [e (first es)]
      (let [c* (conj c (key e))]
        (cons c* (concat (if (map? (val e))
                           (AaronCummings-keypaths (seq (val e)) c*))
                         (AaronCummings-keypaths (rest es) c))))))))

;; miner49r's, allowing also vectors:
(defn miner49r-keypaths-all
  ([m] (miner49r-keypaths-all [] m))
  ([prev m]
   (reduce-kv (fn [res k v] (if (associative? v)
                              (let [kp (conj prev k)]
                                (conj (into res (miner49r-keypaths-all kp v)) kp))
                              (conj res (conj prev k))))
              []
              m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; David Rz Ayala's doesn't produce the right answer:
;(defn genkeys [parent data]
;  (let [mylist (transient [])]
;    (doseq [k (keys data)]
;      (do
;        (if ( = (class (k data)) clojure.lang.PersistentHashMap )
;          (#(reduce conj! %1 %2) mylist (genkeys (conj parent  k ) (k data) ))
;          (conj! mylist  (conj parent  k ) )
;          )))
;    (persistent! mylist)))
;
;(defn DavidRzAyala-keypaths [data] (genkeys [] data))
