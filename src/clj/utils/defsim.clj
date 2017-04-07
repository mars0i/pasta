;;; This software is copyright 2016, 2017 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

;; Defines a defsymstate macro that will define a subclass of MASON's
;; SimState with associated instance state variable, accessors, etc.

;; NOTE this will not work unless project.clj specifies that Sim
;; is aot-compiled.  e.g. if your overarching namespace path is named
;; "myproject", you need a line like this in project.clj:
;;     :aot [myproject.Sim]
;; or like this:
;;     :aot [myproject.Sim myproject.UI]

;; TODO consider moving command line processing into this macro.

(ns utils.defsim
  (:require [clojure.string :as s]))

(def sim-class-sym 'Sim)
(def data-class-sym 'data)
(def data-rec-sym 'SimData)
(def data-rec-constructor '->SimData)
(def data-field-sym 'simData)
(def data-accessor '.simData)
(def init-genclass-sym 'init-sim-data)
(def init-defn-sym '-init-sim-data)


;; Positional functions
;; clojure.core's first and second return nil if xs is too short (because
;; they're defined using next), while nth throws an exception in that case.  
;; For the principle of least surprise, and since count is O(1) in most 
;; cases (and all cases here), I'm reproducing the nil-if-too-short 
;; functionality in the defs below.  Also, I sometimes positively want this 
;; behavior.

(defn third 
  "Returns the third element of xs or nil if xs is too short."
  [xs] 
  (if (>= (count xs) 3)
    (nth xs 2)
    nil))

(defn fourth 
  "Returns the fourth element of xs or nil if xs is too short."
  [xs] 
  (if (>= (count xs) 4)
    (nth xs 3)
    nil))

(defn fifth
  "Returns the fifth element of xs or nil if xs is too short."
  [xs] 
  (if (>= (count xs) 5)
    (nth xs 4)
    nil))

;; Positional function abbreviations for accessing components 
;; of the fields argument below:
(def field-sym  first)
(def field-init second)
(def field-type third)
(def field-ui?  fourth)

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

(defn get-ui-fields
  [fields]
  "Given a fields argument to defsimconfig, return a sequence containing 
  only those field specifications suitable for modification in the UI.
  These are those have a truthy fourth element"
    (filter field-ui? fields)) ; i.e. 

(defn get-range-fields
  "Given a fields argument to defsimconfig, return a sequence containing 
  only those field specifications that include specification of the default
  range of values for the field--i.e. those field specs that have a sequence,
  presumably with exactly two elements, as the fourth element."
  [fields]
  (filter (comp sequential? field-ui?) fields))

(defn make-cli-spec
  "If a partial cli specification vector is present as the fifth element
  of field, returns a cli specification vector completed by inserting
  the long-option string as after the initial short-option string.  The
  rest of the partial cli specification vector should contain a description
  and any other keyword-based arguments allowed by clojure.tools.cli/parse-opts.
  The constructed long-option string will have the form 
  \"--key-sym <val-type>\"."
  [field]
  (let [[key-sym init val-type _ [short-opt & rest-of-cli-spec]] field]
    (when short-opt
      (into [short-opt 
             (str "--" key-sym 
                  (when (not= val-type 'boolean)          ; for booleans, don't require an argument
                    (str " <" val-type "> (" init ")")))]
            rest-of-cli-spec))))

(defn get-cli-specs
  [fields]
  (filter identity (map make-cli-spec fields)))

(defn make-commandline-processing-defs
  "If there any element of fields includes a fifth element, i.e. command-line 
  specification, generate commandline processing code; otherwise return nil."
  [fields]
  (when (some #(> (count %) 4) fields)
    `((defn ~'record-commandline-args!
        "Temporarily store values of parameters passed on the command line."
        [args#]
        ;; These options should not conflict with MASON's.  Example: If "-h" is the single-char help option, doLoop will never see "-help" (although "-t n" doesn't conflict with "-time") (??).
        (let [~'cli-options [["-?" "--help" "Print this help message."] ~@(get-cli-specs fields)]
              usage-fmt# (fn [~'options]
                           (let [~'fmt-line (fn [[~'short-opt ~'long-opt ~'desc]] (str ~'short-opt ", " ~'long-opt ": " ~'desc))]
                             (clojure.string/join "\n" (concat (map ~'fmt-line ~'options)))))
              {:keys [~'options ~'arguments ~'errors ~'summary] :as ~'cmdline} (clojure.tools.cli/parse-opts args# ~'cli-options)]
          (reset! ~'commandline$ ~'cmdline) ; commandline should be defined previously in Sim
          (when (:help ~'options)
            (println "Command line options (defaults in parentheses):")
            (println (usage-fmt# ~'cli-options))
            (println "MASON options can also be used:")
            (println "-help (note single dash): Print help message for MASON.")
            (System/exit 0)))))))

;; TODO add type annotations. (maybe iff they're symbols??)
;; Maybe some of gensym pound signs are overkill. Can't hurt?
(defmacro defsim
  "defsim generates Java-bean style and other MASON-style accessors; a gen-class 
  expression in which their signatures are defined along with an instance 
  variable containing a Clojure map for their corresponding values; an 
  initializer function for the map; and a call to clojure.tools.cli/parse-opts
  to define corresponding commandline options.  fields is a sequence of 4- or 
  5-element sequences starting with names of fields in which configuration 
  data will be stored and accessed, followed by initial values and a Java 
  type identifiers for the field.  The fourth element is either false to 
  indicate that the field should not be configurable from the UI, or truthy
  if it is.  In the latter case, it may be a two-element sequence containing 
  default min and max values to be used for sliders in the UI.  (This range 
  doesn't constraint fields' values in any other respect.) The fifth element,
  if present, specifies short commandline option lists for use by parse-opts,
  except that the second, long option specifier should be left out; it will be 
  generated from the parameter name.  The following gen-class options will
  automatically be provided in the macroexpansion of defsim: :state, 
  :exposes-methods, :init, :main, :methods.  Additional options can be provided
  in addl-gen-class-opts by alternating gen-class option keywords with their
  intended values.  If addl-gen-class-opts includes :exposes-methods or 
  :methods, the value(s) will be combined with automatically generated values
  for these gen-class options.  Note: defsim must be used only in a namespace
  named <namespace prefix>.Sim, where <namespace prefix> is the path before the
  last dot of the current namespace.  Sim must be aot-compiled in order for 
  gen-class to work."
  [fields & addl-gen-class-opts]
   (let [addl-opts-map (apply hash-map addl-gen-class-opts)
         field-syms# (map field-sym fields)
         field-inits# (map field-init fields)
         ui-fields# (get-ui-fields fields)
         ui-field-syms# (map field-sym ui-fields#)
         ;ui-field-inits# (map field-init ui-fields#)
         ui-field-types# (map field-type ui-fields#)
         ui-field-keywords# (map keyword ui-field-syms#)
         accessor-stubs# (map hyphed-sym-to-studly-str ui-field-syms#)
         get-syms#  (map (partial prefix-sym "get") accessor-stubs#)
         set-syms#  (map (partial prefix-sym "set") accessor-stubs#)
         -get-syms# (map (partial prefix-sym "-") get-syms#)
         -set-syms# (map (partial prefix-sym "-") set-syms#)
         range-fields# (get-range-fields ui-fields#)
         dom-syms#  (map (comp (partial prefix-sym "dom") hyphed-sym-to-studly-str first)
                        range-fields#)
         -dom-syms# (map (partial prefix-sym "-") dom-syms#)
         dom-keywords# (map keyword dom-syms#)
         ranges# (map field-ui? range-fields#)
         class-prefix (get-class-prefix *ns*)
         qualified-sim-class# (symbol (str class-prefix "." sim-class-sym))
         qualified-data-class# (symbol (str class-prefix "." data-class-sym))
         qualified-data-rec# (symbol (str class-prefix "." data-rec-sym))
         qualified-data-rec-constructor# (symbol (str class-prefix "." data-class-sym "/" data-rec-constructor))
         gen-class-opts# {:name qualified-sim-class#
                         :extends 'sim.engine.SimState
                         :state data-field-sym
                         :exposes-methods (into '{start superStart} (:exploses-methods addl-opts-map))
                         :init init-genclass-sym
                         :main true
                         :methods (vec (concat (make-accessor-sigs get-syms# set-syms# ui-field-types#)
                                               (map #(vector % [] 'java.lang.Object) dom-syms#)
                                               (:methods addl-opts-map)))} 
         gen-class-opts# (into gen-class-opts# (dissoc addl-opts-map :exposes-methods :methods))]
     `(do
        ;; Put following in its own namespace so that other namespaces can access it without cyclicly referencing Sim:
        ;; DEFINE CONFIG DATA RECORD:
        (ns ~qualified-data-class#)
        (defrecord ~data-rec-sym ~(vec field-syms#))

        ;; The rest is in the main config namespace:
        ;; DEFINE SIM CONFIG CLASS:
        (ns ~qualified-sim-class# 
          (:require [~qualified-data-class#])
          (:import ~qualified-sim-class#)
          (:gen-class ~@(apply concat gen-class-opts#)))  ; NOTE qualified-data-rec must be aot-compiled, or you'll get class not found errors.

        ;; FUNCTION THAT INITIALIZES DATA RECORD STORED IN SIM CONFIG CLASS:
        (defn ~init-defn-sym [~'seed] [[~'seed] (atom (~qualified-data-rec-constructor# ~@field-inits#))])

        ;; DEFINE BEAN AND OTHER ACCESSORS FOR MASON UI:
        ~@(map (fn [sym# keyw#] (list 'defn sym# '[this] `(~keyw# @(.simData ~'this))))
               -get-syms# ui-field-keywords#)
        ~@(map (fn [sym# keyw#] (list 'defn sym# '[this newval] `(swap! (~data-accessor ~'this) assoc ~keyw# ~'newval)))
               -set-syms# ui-field-keywords#)
        ~@(map (fn [sym# keyw# range-pair#] (list 'defn sym# '[this] `(Interval. ~@range-pair#)))
               -dom-syms# dom-keywords# ranges#)

        ;; DEFINE COMMANDLINE OPTIONS:
        ~@(make-commandline-processing-defs fields)
        )))
