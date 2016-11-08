(ns tblibrary.helpers
    (:require 
        [cheshire.core :as cheshire]
        [clojure.tools.logging :as log]))

(defn body_json [req]
    (let [result (cheshire/parse-string (:body req) true)]
          (:result result)))

(defn json_stringify [data]
    (cheshire/generate-string data))

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(defn filter_simple_hash [hm]
    (into {} (filter #(not (nil? (second %))) hm)))

(defn filter_hash [hm]
    (filter_simple_hash (into {} (map (fn [[name value]]
        (if (instance? clojure.lang.PersistentArrayMap value) 
            {name (filter_hash value)}
            (if (instance? clojure.lang.PersistentVector value) 
                {name (map #(filter_hash %) value)}
                {name value}))) hm))))

(defn filter_multipart [m]
    (filter #(not (nil? (:content %))) m))

(defn parse_int [s]
   (Integer. (re-find  #"\d+" s)))

(defmacro wrap [& f]
    `(try
        ~@f
        (catch Exception e#
            (log/error (str "Caught exception: " (.getMessage e#))))))