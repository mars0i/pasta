;; Define a defsymstate macro that will define a subclass of MASON's
;; SimState with associated instance state variable, accessors, etc.

(ns utils.defsimstate
  (:require [clojure.string :as s]))

(defn hyphed-to-studly-str
  "Converts a hyphenated string into the corresponding studly caps string."
  [hyphed-str]
  (let [parts (s/split hyphed-str #"-")]
    (reduce str (map s/capitalize parts))))

(defn hyphed-to-camel-str
  "Converts a hyphenated string into the corresponding camelcase string."
  [hyphed-str]
  (let [[first-part & other-parts] (s/split hyphed-str #"-")]
    (reduce str 
            first-part 
            (map s/capitalize other-parts))))

;(defmacro clj-to-capped-java-sym
;  [sym]
;  `(symbol (hyphed-to-capped (name '~sym))))
;
;(defmacro clj-to-capped-butfirst-java-sym
;  [sym]
;  `(symbol (hyphed-to-capped-butfirst (name '~sym))))

(defmacro make-accessor-sym
  "Given a prefix string and a Clojure symbol with zero or more hyphens,
  returns a Java Bean-style accessor symbol using the prefix.  e.g.:
  (make-accessor-sym \"get\" this-and-that) ;=> getThisAndThat"
  [prefix sym]
  `(symbol 
     (str ~prefix 
          (hyphed-to-studly-str (name '~sym)))))

(defmacro defsimstate
  "Requires option :field followed by a vector of field names, and :name for the name of 
  the class to be defined by gen-class."
  [& opts-seq]
   (let [opts (into {} (map vec (partition 2 opts-seq)))
         fields (:fields opts)   ; take out my special entries
         opts (dissoc opts :state-vars)  ; leave only entries gen-class knows
         opts (assoc opts                ; add in gen-class options we want for sure
                     :state 'instanceState
                     :exposes-methods '{start superStart}
                     :init 'init-instance-state
                     :main true)]
     `(do
        (defrecord ~'InstanceState ~fields) ; TODO make sure InstanceState comes out in the right namespace
        (gen-class ~@(apply concat opts)))))

