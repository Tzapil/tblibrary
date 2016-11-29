(ns tblibrary.updater
    (:require [clojure.core.async :as async]
              [tblibrary.bot :as bot]
              [tblibrary.helpers :as helpers]
              [tblibrary.webserver :as server]
              [cheshire.core :as cheshire]
              [clojure.tools.logging :as log]))

(defn start_webhook
    ([token listen port url_path]
        (start_webhook token listen port url_path port))
    ([token listen port url_path listen_port]
        (bot/remove_webhook token)
        (log/info token "Webhook removed")
        (log/info "After remove_webhook required 2s pause...")

        (async/<!! (async/timeout 2000))

        (let [c (async/chan)
              listen_url (str "https://" listen ":" port "/" url_path "/")]
            (bot/set_webhook token listen_url)
            (log/info token "Webhook set: " listen_url)

            (server/start_server 
                listen_port
                (fn [ring-request]
                    ;; Handle request asynchronously
                    (let [json (cheshire/parse-string (slurp (:body ring-request)) true)]
                        (log/info "Request: " json)
                        (async/go (async/>! c [json])))))
            (log/info token "Server started.")
            c)))

(defn make_poll [token c offset limit timeout]
    (log/info "Long poll offset!" offset)
    (try
        (let [updates (bot/get_updates token offset limit timeout)
              json (helpers/body_json updates)
              updates (count json)]
            (if (= updates 0)
                offset
                (do
                    (log/info "New message!!" json)
                    (async/go (async/>! c json))
                    (inc (reduce #(max %1 (get %2 :update_id)) offset json)))))
        (catch Exception e
            (log/error e "Long polling exception!")
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
        (bot/remove_webhook token) ;; need to remove webhook to pull updates
        (log/info token "Webhook removed")

        (long_polling token (async/chan) limit timeout pause)))

(defn _handle [json [handler & other]]
    (if (helpers/wrap ((:pr handler) json))
        (async/go (helpers/wrap ((:f handler) json)))
        (if (> (count other) 0)
          (_handle json other))))

(defn start_handlers [handlers c]
    (async/go-loop []
        (let [data (async/<! c)]
            (log/info "Start handle data:" data)
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