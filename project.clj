(defproject tblibrary "0.1.2-SNAPSHOT"
  :description "Simple library to create bots with telegram API"
  :url "https://github.com/Tzapil/tblibrary"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;; Client Server
                 [http-kit "2.2.0"]
                 [cheshire "5.5.0"]
                 ;; Async
                 [org.clojure/core.async "0.2.374"]
                 ;; Ring Server
                 [ring/ring-core "1.5.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 ;; Logs
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]])
