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
(def gui-vars-html-filename "gui_vars_table.html") ; will contain html for documentation of vars in GUI

; (def mason-options 
;   (str "-repeat R         Long value > 0: Runs R jobs.  Unless overridden by a\n"
;        "                  checkpoint recovery (see -checkpoint), the random seed for\n"
;        "                  each job is the provided -seed plus the job# (starting at 0).\n"
;        "                  Default: runs once only: job number is 0.\n\n"
;        "-parallel P       Long value > 0: Runs P separate batches of jobs in parallel,\n"
;        "                  each one containing R jobs (as specified by -repeat).  Each\n"
;        "                  batch has its own independent set of checkpoint files.  Job\n"
;        "                  numbers are 0, P, P*2, ... for the first batch, then 1, P+1,\n"
;        "                  P*2+1, ... for the second batch, then 2, P+2, P*2+2, ... for\n"
;        "                  the third batch, and so on.  -parallel may not be used in\n"
;        "                  combination with -checkpoint.\n"
;        "                  Default: one batch only (no parallelism).\n\n"
;        "-seed S           Long value not 0: the random number generator seed, unless \n"
;        "                  overridden by a checkpoint recovery (see -checkpoint).\n"
;        "                  Default: the system time in milliseconds.\n\n"
;        "-until U          Double value >= 0: the simulation must stop when the\n"
;        "                  simulation time U has been reached or exceeded.\n"
;        "                  If -for is also included, the simulation terminates when\n"
;        "                  either of them is completed.\n"
;        "                  Default: don't stop.\n"
;        "-for N            Long value >= 0: the simulation must stop when N\n"
;        "                  simulation steps have transpired.   If -until is also\n"
;        "                  included, the simulation terminates when either of them is\n"
;        "                  completed.\n"
;        "                  Default: don't stop.\n"
;        "-time T           Long value >= 0: print a timestamp every T simulation steps.\n"
;        "                  If 0, nothing is printed.\n"
;        "                  Default: auto-chooses number of steps based on how many\n"
;        "                  appear to fit in one second of wall clock time.  Rounds to\n"
;        "                  one of 1, 2, 5, 10, 25, 50, 100, 250, 500, 1000, 2500, etc.\n\n"
;        "-docheckpoint D   Long value > 0: checkpoint every D simulation steps.\n"
;        "                  Default: never.\n"
;        "                  Checkpoint files named       <steps>.<job#>.NAME.checkpoint\n"
;        "                  where NAME is specified in -checkpointname\n\n"
;        "-checkpointname N String: id for the checkpoint filename (see -docheckpoint)\n"
;        "                  Default: Sim\n"
;        "-checkpoint C     String: loads the simulation from file C, recovering the job\n"
;        "                  number and the seed.  If the checkpointed simulation was begun\n"
;        "                  on the command line but was passed through the GUI for a while\n"
;        "                  (even multiply restarted in the GUI) and then recheckpointed,\n"
;        "                  then the seed and job numbers will be the same as when they\n"
;        "                  were last on the command line.  If the checkpointed simulation\n"
;        "                  was begun on the GUI, then the seed will not be recovered and\n"+
;        "                  job will be set to 0. Further jobs and seeds are incremented\n"
;        "                  from the recovered job and seed.\n"
;        "                  Default: starts a new simulation rather than loading one, at\n"
;        "                  job 0 and with the seed given in -seed.\n\n"
;        "-quiet            Does not print messages except for errors and warnings.\n"
;        "                  This option implies -time 0.\n"
;        "                  Default: prints all messages.\n"))


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
(def field-description (comp second fifth))

(defn get-class-prefix
  "Given a Java/Clojure class identifier symbol or string, or class object (found
  e.g. in *ns*), returns a string containing only the path part before the last 
  period, stripping off the class name at the end."
  [class-rep]
  (s/join "." (butlast 
                (s/split (str class-rep) #"\."))))

(defn snake-to-camel-str
  "Converts a hyphenated string into the corresponding camel caps string."
  [string]
  (let [parts (s/split string #"-")]
    (reduce str (map s/capitalize parts))))

(defn snake-sym-to-camel-str
  "Convience wrapper for snake-to-camel-str that converts symbol to
  string before calling it."
  [sym]
  (snake-to-camel-str (name sym)))

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
  "Given a fields argument to defsimconfig, return a sequence containing 
  only those field specifications suitable for modification in the UI.
  These are those have a truthy fourth element"
  [fields]
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
          ;(println ~'errors)
          (reset! ~'commandline$ ~'cmdline) ; commandline should be defined previously in Sim
          (when (:help ~'options)
            (println "Command line options (defaults in parentheses):")
            (println (usage-fmt# ~'cli-options))
            (println "MASON options can also be used after these options:")
	    (println "For example, you can use -for to stop after a specific number of steps.")
            (println "-help (note single dash): Print help message for MASON.")
            (System/exit 0)))))))

(defn make-gui-vars-html
  "Given a sequence of Java variable name strings and descriptions of them
  formats the names and descriptions into an HTML table that can be inserted,
  for example, into the app's index.html or a README.md file."
  [java-var-names descriptions]
  (apply str
         (conj (vec (cons "<table style=\"width:100%\">"
                          (map (fn [v d]
                                 (format "<tr><td valign=top>%s:</td> <td>%s</td></tr>\n" v d))
                               java-var-names
                               descriptions)))
               "</table>")))

;; TODO add type annotations. (maybe iff they're symbols??)
;; Maybe some of gensym pound signs are overkill. Can't hurt?
(defmacro defsim
  "defsim generates Java-bean style and other MASON-style accessors; a gen-class 
  expression in which their signatures are defined along with an instance 
  variable containing a Clojure map for their corresponding values; an 
  initializer function for the map; and a call to clojure.tools.cli/parse-opts
  to define corresponding commandline options.  fields is a sequence of 4- or 
  5-element sequences starting with names of fields in which configuration 
  data will be stored and accessed, followed by initial values and Java 
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
  gen-class to work.  When run, this macro also generates a file named
  gui_vars_table.html containing documentation on generated Java vars that will
  be manipulable from within the GUI.  This file can be included into other
  documentation such as the index.html file displayed in the app."
  [fields & addl-gen-class-opts]
   (let [addl-opts-map (apply hash-map addl-gen-class-opts)
         field-syms# (map field-sym fields)   ; symbols for data object fields (?)
         field-inits# (map field-init fields) ; data field initial values (?)
         ui-fields# (get-ui-fields fields)    ; names of fields in GUI (?)
         ui-field-syms# (map field-sym ui-fields#) ; sybmols for fields in GUI (?)
         ui-field-descriptions# (map field-description ui-fields#)
         ui-field-types# (map field-type ui-fields#)
         ui-field-keywords# (map keyword ui-field-syms#)
         accessor-stubs# (map snake-sym-to-camel-str ui-field-syms#)
         get-syms#  (map (partial prefix-sym "get") accessor-stubs#) ; bean getter symbols
         set-syms#  (map (partial prefix-sym "set") accessor-stubs#) ; bean setter symbols
         -get-syms# (map (partial prefix-sym "-") get-syms#)         ; getter symbols with "-" prefix
         -set-syms# (map (partial prefix-sym "-") set-syms#)         ; setter symbols with "-" prefix
         range-fields# (get-range-fields ui-fields#)
         dom-syms#  (map (comp (partial prefix-sym "dom") snake-sym-to-camel-str first) ; dom range special MASON "bean" symbols
                        range-fields#)
         -dom-syms# (map (partial prefix-sym "-") dom-syms#) ; dom range symbols with "-" prefix
         dom-keywords# (map keyword dom-syms#)
         ranges# (map field-ui? range-fields#)
         gui-vars-html# (make-gui-vars-html accessor-stubs# ui-field-descriptions#)
         class-prefix (get-class-prefix *ns*)
         qualified-sim-class# (symbol (str class-prefix "." sim-class-sym))
         qualified-data-class# (symbol (str class-prefix "." data-class-sym))
         qualified-data-rec# (symbol (str class-prefix "." data-rec-sym))
         qualified-data-rec-constructor# (symbol (str class-prefix "." data-class-sym "/" data-rec-constructor))
         gen-class-opts# {:name qualified-sim-class#
                         :extends 'sim.engine.SimState
                         :state data-field-sym ; Experiment: :state (vary-meta data-field-sym assoc :tag qualified-data-rec#)
                         :exposes-methods (into '{start superStart} (:exposes-methods addl-opts-map))
                         :init init-genclass-sym
                         :main true
                         :methods (vec (concat (make-accessor-sigs get-syms# set-syms# ui-field-types#)
                                               (map #(vector % [] 'java.lang.Object) dom-syms#)
                                               (:methods addl-opts-map)))} 
         gen-class-opts# (into gen-class-opts# (dissoc addl-opts-map :exposes-methods :methods))
         this# (vary-meta 'this assoc :tag qualified-sim-class#)] ; add type hint to Sim arg of bean accessors to avoid reflection
         ;; Note re type-hinting the newval param of the setters below, see WhatIsThisBranch.md in branch type-hinted-newval.

     ;; GENERATE HTML TABLE DOCUMENTING VARIABLES POSSIBLY VISIBLE IN GUI
     ;; Note this will only happen whem Sim.clj is recompiled.
     (println "Writing GUI vars html table to file" gui-vars-html-filename ".")
     (spit gui-vars-html-filename gui-vars-html#)

     ;; GENERATE CODE FOR Sim.clj:
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
        ~@(map (fn [sym# keyw#] (list 'defn sym# (vector this#) `(~keyw# @(~data-accessor ~'this))))
               -get-syms# ui-field-keywords#)
        ~@(map (fn [sym# keyw#] (list 'defn sym# (vector this# 'newval) `(swap! (~data-accessor ~'this) assoc ~keyw# ~'newval)))
               -set-syms# ui-field-keywords#)
        ~@(map (fn [sym# keyw# range-pair#] (list 'defn sym# (vector this#) `(Interval. ~@range-pair#)))
               -dom-syms# dom-keywords# ranges#)

        ;; DEFINE COMMANDLINE OPTIONS:
        ~@(make-commandline-processing-defs fields)
      )))
