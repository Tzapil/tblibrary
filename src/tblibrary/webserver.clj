(ns tblibrary.webserver
    (:require 
        [ring.adapter.jetty :as jetty]
        [org.httpkit.server :as server]))

(defn start_server
    [port handler]
        (server/run-server handler {:port port}))