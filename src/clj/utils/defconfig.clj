;; Define a defsymstate macro that will define a subclass of MASON's
;; SimState with associated instance state variable, accessors, etc.

(ns utils.defconfig
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

(defn make-accessor-sym
  "Given a prefix string and a Clojure symbol, returns a Java 
  Bean-style accessor symbol using the prefix.  e.g.:
  (make-accessor-sym \"get\" this-and-that) ;=> getThisAndThat"
  [prefix stub-str]
  (symbol (str prefix stub-str)))

(defmacro defconfig
  "sim-state-class is a fully-qualified name for the new class. fields is a
  map with keys whose (symbol, not keyword) names will name fields in which 
  atoms will be stored and accessed.  Values are Java type identifiers.  
  domains is a possibly empty map whose keys are symbols that are among the 
  keys in fields, and whose values are pairs of numbers specifying min and
  max values for the field.  The following gen-class options will be 
  automatically provided: :state, :exposes-methods, :init, :main, :methods.  
  Additional options can be provided in addl-gen-class-opts."
  [sim-state-class fields domains & addl-gen-class-opts]
   (let [gen-class-opts {:name sim-state-class
                         :state 'configData
                         :exposes-methods '{start superStart}
                         :init 'init-config-data
                         :main true}
         gen-class-opts (into gen-class-opts 
                              (map vec (partition 2 addl-gen-class-opts)))
         field-syms (keys fields)
         field-keywords (map keyword field-syms)
         field-strs (map name field-syms)
         default-syms (map #(symbol (str "default-" %)) field-strs)
         accessor-stubs (map hyphed-to-studly-str field-strs)
         get-syms (map (partial make-accessor-sym "-get") accessor-stubs)  ; TODO why are these coming out not fully studly?
         set-syms (map (partial make-accessor-sym "-set") accessor-stubs)]
     `(do
        ;;;; should be in a different namespace (so simulation code can access it without cyclicly referencing State):
        (defrecord ~'ConfigData ~(vec field-syms)) ; TODO make sure ConfigData comes out in the right namespace
        ;;;; should be in State namespace:
        (gen-class ~@(apply concat gen-class-opts))
        ;;;; Should be in same namespace as gen-class:
        (defn ~'-init-config-data [~'seed] [[~'seed] (atom (ConfigData. ~@default-syms))]) ; NOTE will fail if default-syms are not yet defined.
        ;; need to add type annotations:
        (import ~sim-state-class) ; must go after gen-class but before any type annotations using the class
        ~@(map (fn [sym keyw] (list 'defn sym '[this] `(~keyw @(.configData ~'this))))
               get-syms field-keywords)
        ~@(map (fn [sym keyw] (list 'defn sym '[this newval] `(swap! (.configData ~'this) assoc ~keyw  ~'newval)))
               set-syms field-keywords)
        ;; define domX for elements in domains
        )))

