;; This software is copyright 2016, 2017 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns utils.map2csv
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as st]))

(defn keyword-to-column-name
  "Create a column label string from keyword k."
  [k]
  (st/replace (name k) #"-" "_"))

(defn column-names
  "Returns sorted column label strings made from keys in map m."
  [m]
  (map keyword-to-column-name
       (sort (keys m))))

(defn spit-csv
  "Given a sequence of sequences of data, opens a file and writes to it
  using write-csv.  Options are those that can be passed to spit or writer."
  [f rows & options]
   (with-open [w (apply io/writer f options)]
     (csv/write-csv w rows)))
