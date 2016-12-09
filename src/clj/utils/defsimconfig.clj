;; Define a defsymstate macro that will define a subclass of MASON's
;; SimState with associated instance state variable, accessors, etc.

(ns utils.defsimconfig
  (:require [clojure.string :as s]))

(defn hyphed-to-studly-str
  "Converts a hyphenated string into the corresponding studly caps string."
  [hyphed-str]
  (let [parts (s/split hyphed-str #"-")]
    (reduce str (map s/capitalize parts))))

(defn make-accessor-sym
  "Given a prefix string and a Clojure symbol, returns a Java 
  Bean-style accessor symbol using the prefix.  e.g.:
  (make-accessor-sym \"get\" this-and-that) ;=> getThisAndThat"
  [prefix stub-str]
  (symbol (str prefix stub-str)))

(defn make-accessor-sigs
  [get-syms set-syms classes]
  (mapcat (fn [get-sym set-sym cls] [[get-sym [] cls] [set-sym [cls] 'void]])
               get-syms set-syms classes))

(defmacro defsimconfig
  "sim-state-class is a fully-qualified name for the new class. fields is a
  sequence of 2- or 4-element sequences starting with names of fields in which 
  configuration data will be stored and accessed.  Second elements are Java type 
  identifiers for these fields.  If there is a third and fourth element, they
  are the min and max values for the field.  The following gen-class options will be 
  automatically provided: :state, :exposes-methods, :init, :main, :methods.  
  Additional options can be provided in addl-gen-class-opts."
  [sim-state-class fields & addl-gen-class-opts]
   (let [field-syms (map first fields)
         field-keywords (map keyword field-syms)
         default-syms (map #(symbol (str "default-" %)) field-syms)
         accessor-stubs (map hyphed-to-studly-str field-syms)
         get-syms  (map (partial make-accessor-sym "get") accessor-stubs)
         set-syms  (map (partial make-accessor-sym "set") accessor-stubs)
         -get-syms (map (partial make-accessor-sym "-") get-syms)
         -set-syms (map (partial make-accessor-sym "-") set-syms)
         dom-syms  (map (comp (partial make-accessor-sym "dom") hyphed-to-studly-str name first) ; TODO doesn't work
                        (filter #(= 4 (count %)) fields))
         ;(->> fields
         ;     (filter #(= 4 (count %)))
         ;     first
         ;     name
         ;     hyphed-to-studly-str
         ;     (partial make-accessor-sym "dom"))
         _ (println dom-syms)
         gen-class-opts {:name sim-state-class
                         :state 'configData
                         :exposes-methods '{start superStart}
                         :init 'init-config-data
                         :main true
                         :methods (vec (concat (make-accessor-sigs get-syms set-syms (map second fields))
                                               (map #(vector % [] java.lang.Object) dom-syms)))} ;; TODO NOT WORKING
         gen-class-opts (into gen-class-opts 
                              (map vec (partition 2 addl-gen-class-opts)))]
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
               -get-syms field-keywords)
        ~@(map (fn [sym keyw] (list 'defn sym '[this newval] `(swap! (.configData ~'this) assoc ~keyw  ~'newval)))
               -set-syms field-keywords)
        ;; define domX for elements in domains
        )))



