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
    (filter fourth fields)) ; i.e. 

(defn get-range-fields
  "Given a fields argument to defsimconfig, return a sequence containing 
  only those field specifications that include specification of the default
  range of values for the field--i.e. those field specs that have a sequence,
  presumably with exactly two elements, as the fourth element."
  [fields]
  (filter (comp sequential? fourth) fields))

(defn make-cli-spec
  "If a partial cli specification vector is present as the fifth element
  of field, returns a cli specification vector completed by inserting
  the long-option string as after the initial short-option string.  The
  rest of the partial cli specification vector should contain a description
  and any other keyword-based arguments allowed by clojure.tools.cli/parse-opts.
  The constructed long-option string will have the form 
  \"--key-sym <val-type>\"."
  [field]
  (let [[key-sym _ val-type _ [short-opt & rest-of-cli-spec]] field]
    (when short-opt
      (into [short-opt 
             (str "--" key-sym " <" val-type ">")]
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
          (reset! ~'commandline ~'cmdline) ; commandline should be defined previously in SimConfig
          (when (:help ~'options)
            (println "Command line options for free-agent:")
            (println (usage-fmt# ~'cli-options))
            (println "free-agent and MASON options can both be used:")
            (println "-help (note single dash): Print help message for MASON.")
            (System/exit 0)))))))


;; TODO add type annotations. (maybe iff they're symbols??)
;; Maybe some of gensym pound signs are overkill. Can't hurt?
(defmacro defsimconfig
  "fields is a sequence of 3- or 4-element sequences starting with names of 
  fields in which configuration data will be stored and accessed, followed
  by initial values and a Java type identifiers for the field.  The fourth
  element is either false to indicate that the field should not be configurable
  from the UI, or truthy if it is.  In the latter case, it may be a two-element 
  sequence containing default min and max values to be used for sliders in the
  UI.  (This range doesn't constraint fields' values in any other respect.) 
  The following gen-class options will automatically be provided: :state, 
  :exposes-methods, :init, :main, :methods.  The fifth element, if present,
  specifies short commandline option lists for use by cli-options, except that
  the second, long option specifier should be left out; it will be generated
  from the parameter name.  Additional options can be provided 
  in addl-gen-class-opts by alternating gen-class option keywords with their
  intended values.  If addl-gen-class-opts includes :exposes-methods or :methods,
  the value(s) will be combined with automatically generated values for these
  gen-class options.  The generated class will be named <namespace prefix>.SimConfig,
  where <namespace prefix> is the path before the last dot of the current namespace.
  Java-bean style and other MASON-style accessors will be defined.  
  Note: SimConfig must be aot-compiled in order for gen-class to work."
  [fields & addl-gen-class-opts]
   (let [addl-opts-map (apply hash-map addl-gen-class-opts)
         field-syms# (map first fields)
         field-inits# (map second fields)
         ui-fields# (get-ui-fields fields)
         ui-field-syms# (map first ui-fields#)
         ui-field-types# (map third ui-fields#)
         ui-field-keywords# (map keyword ui-field-syms#)
         accessor-stubs# (map hyphed-sym-to-studly-str field-syms#)
         get-syms#  (map (partial prefix-sym "get") accessor-stubs#)
         set-syms#  (map (partial prefix-sym "set") accessor-stubs#)
         -get-syms# (map (partial prefix-sym "-") get-syms#)
         -set-syms# (map (partial prefix-sym "-") set-syms#)
         range-fields# (get-range-fields ui-fields#)
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
                         :exposes-methods (into '{start superStart} (:exploses-methods addl-opts-map))
                         :init init-genclass-sym
                         :main true
                         :methods (vec (concat (make-accessor-sigs get-syms# set-syms# ui-field-types#)
                                               (map #(vector % [] java.lang.Object) dom-syms#)
                                               (:methods addl-opts-map)))} 
         gen-class-opts# (into gen-class-opts# (dissoc addl-opts-map :exposes-methods :methods))]
     `(do
        ;; Put following in its own namespace so that other namespaces can access it without cyclicly referencing SimConfig:
        ;; DEFINE CONFIG DATA RECORD:
        (ns ~qualified-data-class#)
        (defrecord ~data-rec-sym ~(vec field-syms#))

        ;; The rest is in the main config namespace:
        ;; DEFINE SIM CONFIG CLASS:
        (ns ~qualified-cfg-class# 
          (:require [~qualified-data-class#])
          (:import ~qualified-cfg-class#)
          (:gen-class ~@(apply concat gen-class-opts#)))  ; NOTE qualified-data-rec must be aot-compiled, or you'll get class not found errors.

        ;; FUNCTION THAT INITIALIZES DATA RECORD STORED IN SIM CONFIG CLASS:
        (defn ~init-defn-sym [~'seed] [[~'seed] (atom (~qualified-data-rec-constructor# ~@field-inits#))])

        ;; DEFINE BEAN AND OTHER ACCESSORS FOR MASON UI:
        ~@(map (fn [sym# keyw#] (list 'defn sym# '[this] `(~keyw# @(.simConfigData ~'this))))
               -get-syms# ui-field-keywords#)
        ~@(map (fn [sym# keyw#] (list 'defn sym# '[this newval] `(swap! (~data-accessor ~'this) assoc ~keyw# ~'newval)))
               -set-syms# ui-field-keywords#)
        ~@(map (fn [sym# keyw# range-pair#] (list 'defn sym# '[this] `(Interval. ~@range-pair#)))
               -dom-syms# dom-keywords# ranges#)

        ;; DEFINE COMMANDLINE OPTIONS:
        ~@(make-commandline-processing-defs fields)
        )))
