(ns tblibrary.updater
    (:require [clojure.core.async :as async]
              [tblibrary.bot :as bot]
              [tblibrary.helpers :as helpers]
              [tblibrary.webserver :as server]
              [cheshire.core :as cheshire]))

(defn start_webhook
    ([token listen port url_path certificate keystore pswd]
        (bot/remove_webhook token)
        (let [listen_url (str "https://" listen ":" port "/" url_path)
              c (async/chan)]
            (println (str "Listen: " listen_url))
            (bot/set_webhook token listen_url certificate)
            (async/go (server/start_server port keystore pswd 
                (fn [request]
                    (let [json (cheshire/parse-string (slurp (:body request)) true)]
                        (println "REQUEST")
                        (println json)
                        (async/go (async/>! c [json]))))))
            c)))

(defn make_poll [token c offset limit timeout]
    (try
        (let [updates (bot/get_updates token offset limit timeout)
              json (helpers/body_json updates)
              updates (count json)]
            (if (= updates 0)
                offset
                (do
                    (async/go (async/>! c json))
                    (inc (reduce #(max %1 (get %2 :update_id)) offset json)))))
        (catch Exception e
            (println (str "Caught exception: " (.getMessage e)))
            offset)))

(defn long_polling [token c limit timeout pause]
    (async/go-loop [offset 0]
        (async/<! (async/timeout pause))   ;; pause
        (recur (make_poll token c offset limit timeout)))
    c)

(defn start_polling 
    ([token]
        (start_polling token 100 0 1000))
    ([token limit]
        (start_polling token limit 0 1000))
    ([token limit timeout]
        (start_polling token limit timeout 1000))
    ([token limit timeout pause]
        (let [c (async/chan)]
            (long_polling token c limit timeout pause))))

(defn _handle [json [handler & other]]
    (if (helpers/wrap ((:pr handler) json))
        (helpers/wrap ((:f handler) json))
        (if (> (count other) 0)
          (_handle json other))))

(defn start_handlers [handlers c]
    (async/go-loop []
        (let [data (async/<! c)]
            (println "Got a value in this loop:" data)
            (doseq [json data]
                (_handle json handlers))
            (recur)))
    c)

(defn idle 
    ([] 
        (idle 1000))
    ([timeout] 
        (loop []
            (async/<!! (async/timeout timeout))
            (recur))))