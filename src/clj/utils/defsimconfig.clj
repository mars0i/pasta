;; Define a defsymstate macro that will define a subclass of MASON's
;; SimState with associated instance state variable, accessors, etc.

;; TODO consider moving command line processing into this macro.

;; NOTE this will not work unless project.clj specifies that SimConfig
;; is aot-compiled.  e.g. if your overarching namespace path is named
;; "myproject", you need a line like this in project.clj:
;;     :aot [myproject.SimConfig]
;; or like this:
;;     :aot [myproject.SimConfig myproject.UI]

(ns utils.defsimconfig
  (:require [clojure.string :as s]))

(def cfg-class-sym 'SimConfig)
(def data-class-sym 'config-data)
(def data-rec-sym 'SimConfigData)
(def data-rec-constructor '->SimConfigData)
(def data-field-sym 'simConfigData)
(def data-accessor '.simConfigData)
(def init-genclass-sym 'init-sim-config-data)
(def init-defn-sym '-init-sim-config-data)

(defn get-class-prefix
  "Given a Java/Clojure class identifier symbol or string, or class object (found
  e.g. in *ns*), returns a string containing only the path part before the last 
  period, stripping off the class name at the end."
  [class-rep]
  (s/join "." (butlast 
                (s/split (str class-rep) #"\."))))

(defn hyphed-to-studly-str
  "Converts a hyphenated string into the corresponding studly caps string."
  [string]
  (let [parts (s/split string #"-")]
    (reduce str (map s/capitalize parts))))

(defn hyphed-sym-to-studly-str
  "Convience wrapper for hyphed-to-studly-str that converts symbol to
  string before calling it."
  [sym]
  (hyphed-to-studly-str (name sym)))

(defn prefix-sym
  "Given a prefix string and a Clojure symbol, returns a Java 
  Bean-style accessor symbol using the prefix.  e.g.:
  (prefix-sym \"get\" this-and-that) ;=> getThisAndThat"
  [prefix stub-str]
  (symbol (str prefix stub-str)))

(defn make-accessor-sigs
  [get-syms set-syms classes]
  (mapcat (fn [get-sym set-sym cls] [[get-sym [] cls] [set-sym [cls] 'void]])
               get-syms set-syms classes))

(defn get-range-fields
  "Given a fields argument to defsimconfig, return a sequence containing 
  only those field specifications that include specification of the default
  range of values for the field."
  [fields]
  (filter #(= 4 (count %)) 
          fields))

(defn third [xs] (nth xs 2))
(defn fourth [xs] (nth xs 3))

;; TODO add type annotations. (maybe iff they're symbols??)
;; TODO put data structure in its own namespace to avoid circular references
;; Maybe some of gensym pound signs are overkill. Can't hurt?
(defmacro defsimconfig
  "fields is a sequence of 3- or 4-element sequences starting with names of 
  fields in which configuration data will be stored and accessed, followed
  by initial values and a Java type identifiers for the field.  The optional 
  fourth element is a two-element sequence containing default min and max 
  values to be used for sliders in the UI.  (This range doesn't constraint
  fields' values in any other respect.) The following gen-class options will 
  automatically be provided: :state, :exposes-methods, :init, :main, :methods.  
  Additional options can be provided in addl-gen-class-opts.  The generated 
  class will be named <namespace prefix>.SimConfig, where <namespace prefix> 
  is the path before the last dot of the current namespace.  Java bean style 
  and other MASON-style accessors will be defined.  Note: SimConfig must
  be aot-compiled in order for gen-class to work."
  [fields & addl-gen-class-opts]
   (let [field-syms# (map first fields)
         field-inits# (map second fields)
         field-types# (map third fields)
         field-keywords# (map keyword field-syms#)
         accessor-stubs# (map hyphed-sym-to-studly-str field-syms#)
         get-syms#  (map (partial prefix-sym "get") accessor-stubs#)
         set-syms#  (map (partial prefix-sym "set") accessor-stubs#)
         -get-syms# (map (partial prefix-sym "-") get-syms#)
         -set-syms# (map (partial prefix-sym "-") set-syms#)
         range-fields# (get-range-fields fields)
         dom-syms#  (map (comp (partial prefix-sym "dom") hyphed-sym-to-studly-str first)
                        range-fields#)
         -dom-syms# (map (partial prefix-sym "-") dom-syms#)
         dom-keywords# (map keyword dom-syms#)
         ranges# (map fourth range-fields#)
         class-prefix (get-class-prefix *ns*)
         qualified-cfg-class# (symbol (str class-prefix "." cfg-class-sym))
         qualified-data-class# (symbol (str class-prefix "." data-class-sym))
         qualified-data-rec# (symbol (str class-prefix "." data-rec-sym))
         qualified-data-rec-constructor# (symbol (str class-prefix "." data-class-sym "/" data-rec-constructor))
         gen-class-opts# {:name qualified-cfg-class#
                         :extends 'sim.engine.SimState
                         :state data-field-sym
                         :exposes-methods '{start superStart}
                         :init init-genclass-sym
                         :main true
                         :methods (vec (concat (make-accessor-sigs get-syms# set-syms# field-types#)
                                               (map #(vector % [] java.lang.Object) dom-syms#)))} 
         gen-class-opts# (into gen-class-opts# (map vec (partition 2 addl-gen-class-opts)))]
     `(do
        ;;
        ;; Put following in its own namespace so that other namespaces can access it without cyclicly referencing SimConfig:
        (ns ~qualified-data-class#)
        (defrecord ~data-rec-sym ~(vec field-syms#))

        ;; The rest is in the main config namespace:
        (ns ~qualified-cfg-class# 
          (:require [~qualified-data-class#])
          (:import ~qualified-cfg-class#)
          (:gen-class ~@(apply concat gen-class-opts#)))  ; NOTE qualified-data-rec must be aot-compiled, or you'll get class not found errors.

        (defn ~init-defn-sym [~'seed] [[~'seed] (atom (~qualified-data-rec-constructor# ~@field-inits#))])
        ;; TODO need to add type annotations:
        ~@(map (fn [sym# keyw#] (list 'defn sym# '[this] `(~keyw# @(.simConfigData ~'this))))
               -get-syms# field-keywords#)
        ~@(map (fn [sym# keyw#] (list 'defn sym# '[this newval] `(swap! (~data-accessor ~'this) assoc ~keyw# ~'newval)))
               -set-syms# field-keywords#)
        ~@(map (fn [sym# keyw# range-pair#] (list 'defn sym# '[this] `(Interval. ~@range-pair#)))
               -dom-syms# dom-keywords# ranges#))))
