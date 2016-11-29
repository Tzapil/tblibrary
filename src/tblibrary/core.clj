(ns tblibrary.core
  (:require [clojure.core.async :as async]
            [org.httpkit.client :as client]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as navigation]
            [tblibrary.bot :as bot]
            [tblibrary.helpers :as helpers]
            [tblibrary.handlers :as handlers]
            [tblibrary.updater :as updater]
            [tblibrary.inline :as inline]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:gen-class))

(def bot-token "285037035:AAEXfDvpfvAgpRaRKjBxGIQSwZU9Vn_sP5c")

(def myanimelist-get-list-api "http://myanimelist.net/malappinfo.php") 

(def user_agent "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.116 Safari/537.36")

(defn third [a]
    (get a 2))

(defn zip-str2 [s]
  (zip/xml-zip 
      (xml/parse (java.io.ByteArrayInputStream. (.getBytes s)))))

(defn zip-str [s]
   (let [input_stream (java.io.ByteArrayInputStream. (.getBytes s))
         input_reader (java.io.InputStreamReader. input_stream "UTF-8")
         input_source (org.xml.sax.InputSource. input_reader)]
        (.setEncoding input_source "UTF-8")
        (zip/xml-zip  
          (xml/parse input_source))))

(defn sync-get [url params]
  (let [{:keys [status headers body error] :as resp} @(client/get url params)]
      (if error
        (log/info "Failed, exception: " error)
        body)))

(defn get-tag-text [container tag]
    (first (navigation/xml-> container
        tag
        navigation/text)))

(defn anime-status [id]
    (let [statuses ["unknown" "watching" "completed" "on-hold" "dropped" "unknown" "plan-to-watch"]]
        (get statuses (helpers/parse_int id))))

(defn serialize-anime [anm]
     (let [name (first anm)
              episodes (second anm)
              watching (third anm)
              status (get anm 3)
              score (get anm 4)
              image (str "https://myanimelist.net/anime/" (get anm 5))]
          (str "<b>" name "</b> " watching "/" episodes "\nStatus: " (anime-status status) "\nScore: " score "\n" image)))

(defn anime-list [user]
    (log/info "Send request to myanimelist.")
    (let [user-id (second user)
          answer (sync-get myanimelist-get-list-api
                            {:headers {"User-Agent" user_agent}
                             :query-params {:u user-id
                                            :status "all"
                                            :type "anime"}})
          body (zip-str answer)         
          entries (navigation/xml-> body
                  :myanimelist
                  :anime)
          anm-lst (vec (map (fn [entry] (vec (map #(get-tag-text entry %) [:series_title :series_episodes :my_watched_episodes :my_status :my_score :series_animedb_id]))) entries))]
            anm-lst))

(defn inline_handler [data]
  (let [id (get-in data [:inline_query :id])
        request (string/split (get-in data [:inline_query :query]) #" ")
        user_name (first request)
        anime_arr (rest request)]
        (log/info "Request: {User: " user_name ", Anime: " anime_arr "}")
        (if (not (nil? user_name))
            (let [anime_name (string/lower-case (string/join " " anime_arr))
                  answer (take 10 (filter #(string/includes? (string/lower-case (first %)) anime_name) (anime-list [nil user_name nil])))
                  r (map #(inline/create_result_article (first %) (str "User: " user_name "\n" (serialize-anime %)) "" "HTML") answer)]
                    (log/info "Got " (count r) " results. Send to user.")
                    (bot/answer_inline_query bot-token id r)))))

(def h [(handlers/create_inline_query_handler inline_handler)])

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;;(doseq [a (range 10)]
  ;;  (log/info "BEGIN" a)
  ;;  (sync-get myanimelist-get-list-api
  ;;                          {:headers {"User-Agent" user_agent}
  ;;                           :query-params {:u "tzapil"
  ;;                                          :status "all"
  ;;                                          :type "anime"}})
  ;;  (log/info "END" a))
  (log/info "Start" (bot/get_me bot-token))
  ;;(println (bot/send_message "141043767:AAGOD1ZEAvzNUuii6_Zxy-zydbU2x5z77so" 53941045 "kokoko"))
  ;;(updater/start_handlers h (updater/start_webhook bot-token "tzapil.tk" 8443 "mal" 7773))
  (updater/start_handlers h (updater/start_polling bot-token 100 1000 0))
  (updater/idle))
