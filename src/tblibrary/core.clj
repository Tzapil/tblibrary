(ns tblibrary.core
  (:require [clojure.core.async :as async]
            [clj-http.client :as client]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as navigation]
            [tblibrary.bot :as bot]
            [tblibrary.helpers :as helpers]
            [tblibrary.updater :as updater]
            [tblibrary.handlers :as handlers]
            [tblibrary.filters :as filters]
            [tblibrary.inline :as inline]
            [tblibrary.chat-action :as chat-action]
            [clojure.string :as string]
            [tblibrary.emoji :as emoji])
  (:gen-class))

(def bot-token "141043767:AAGOD1ZEAvzNUuii6_Zxy-zydbU2x5z77so")

(def myanimelist-search-api "http://myanimelist.net/api/anime/search.xml")
(def myanimelist-get-list-api "http://myanimelist.net/malappinfo.php") ;;?u=domokun1134&status=all&type=anime
(def myanimelist-auth ["tzapil" "132s36"])
(def default-user ["1" "tzapil" "132s36"])

(def users-file "./users.txt")
(def ^:dynamic users [])
(def ^:dynamic states [])

(defn third [a]
    (get a 2))

(defn zip-str [s]
    ;;(-> s xml/parse zip/xml-zip))
  (zip/xml-zip 
    (let [input_stream (java.io.ByteArrayInputStream. (.getBytes s))
          input_reader (java.io.InputStreamReader. input_stream "UTF-8")
          input_source (org.xml.sax.InputSource. input_reader)]
        (.setEncoding input_source "UTF-8")
        (xml/parse input_source))))

(defn read-users [file]
    (if (.exists (clojure.java.io/as-file file))
        (with-open [rdr (clojure.java.io/reader file)]
          (for [line (line-seq rdr)]
            (string/split line #" ")))
        []))

(defn write-users [users file]
    (with-open [wrtr (clojure.java.io/writer file)]
          (doseq [user users]
            (.write wrtr (string/join " " user))))
    users)

(defn get-user [user users]
    (some #(and (= (first %) user) %) users))

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
              image (get anm 5)]
              (println status " " (anime-status status))
          (str "<b>" name "</b> " watching "/" episodes "\nStatus: " (anime-status status) "\nScore: " score "\n" image)))

(defn serialize-anime-list [anm-lst]
    (let [result (reduce (fn [accum ent] 
        (let [name (first ent)
              episodes (second ent)
              watching (third ent)
              status (get ent 3)
              score (get ent 4)]
          (println status " " (anime-status status))
          (str accum "<b>" name "</b> " watching "/" episodes " status: " (anime-status status) " score: " score "\n" ))) "" anm-lst)]
    (println result)
    result))

(defn anime-list [user]
    (let [user-id (second user)
          answer (client/get myanimelist-get-list-api
                            {:query-params {:u user-id
                                            :status "all"
                                            :type "anime"}})
          body (zip-str (:body answer))
          entries (navigation/xml-> body
                  :myanimelist
                  :anime)
          anm-lst (vec (map (fn [entry] (vec (map #(get-tag-text entry %) [:series_title :series_episodes :my_watched_episodes :my_status :my_score :series_image]))) entries))]
            anm-lst))

(defn echo [data]
  (let [id (get-in data [:message :chat :id])
        text (get-in data [:message :text])]
        (println "ECHO_ANSWER: ")
        (println id)
        (println text)
        (bot/send_message bot-token id text nil "Markdown")))

(defn timer [data]
  (let [id (get-in data [:message :chat :id])
        text (get-in data [:message :text])
        args (handlers/parse_command_arguments text)]
        (if (nil? (re-find  #"\d+" (first args)))
            (bot/send_message bot-token id "Argument must be a number!")
            (let [t (helpers/parse_int (first args))]
              (async/go
                  (async/<! (async/timeout t))
                  (bot/send_message bot-token id (str "Beep after " t " ms!")))))))

(defn send_photo [data]
  (let [id (get-in data [:message :chat :id])]
    (bot/send_photo bot-token id (clojure.java.io/file "touhou.jpg"))))

(defn send_audio [data]
  (let [id (get-in data [:message :chat :id])]
    (bot/send_chat_action bot-token id chat-action/upload_audio)
    (bot/send_audio bot-token id (clojure.java.io/file "Radio Protector.mp3"))))

(defn send_document [data]
  (let [id (get-in data [:message :chat :id])]
    (bot/send_document bot-token id (clojure.java.io/file "LICENSE"))))

(defn get_user [data]
  (let [id (get-in data [:message :from :id])]
    (println (bot/get_user_profile_photos bot-token id))))

(defn get_anime_title [anime]
  (first (navigation/xml-> anime
        :title
        navigation/text)))

(defn get_anime_image [anime]
  (first (navigation/xml-> anime
        :image
        navigation/text)))

(defn create_inline_query [body]  
  (let [anime (navigation/xml-> body
                   :anime
                   :entry)]
        (vec (map #(inline/create_result_article (get_anime_title %) (get_anime_image %)) anime))))

(defn inline_handler [data]
  (let [id (get-in data [:inline_query :id])
        iq (get-in data [:inline_query :query])
        request (string/split iq #" ")
        user_name (first request)
        anime_arr (rest request)]
        (println "USER")
        (println user_name)
        (if (not (nil? user_name))
            (let [anime_name (string/lower-case (string/join " " anime_arr))
                  answer (take 10 (filter #(string/includes? (string/lower-case (first %)) anime_name) (anime-list [nil user_name nil])))
                  r (map #(inline/create_result_article (first %) (str "User: " user_name "\n" (serialize-anime %)) "" "HTML") answer)]
                    (println "RESULT:")
                    (println r)
                    (println anime_arr)
                    (println anime_name)
                    (bot/answer_inline_query bot-token id r))))) ;; (bot/answer_inline_query bot-token id results) [(inline/create_result_article "Anime" (first r))]

(def h [
  (handlers/create_command "start" #(bot/send_message bot-token (get-in % [:message :chat :id]) (str "HI! " emoji/WINKING_FACE)))
  (handlers/create_command "help" #(bot/send_message bot-token (get-in % [:message :chat :id]) "HELP!"))
  (handlers/create_command "set" timer)
  (handlers/create_command "cirno" send_photo)
  (handlers/create_command "song" send_audio)
  (handlers/create_command "license" send_document)
  (handlers/create_command "list" #(bot/send_message bot-token (get-in % [:message :chat :id]) (serialize-anime-list (take 10 (anime-list default-user))) nil "HTML"))
  (handlers/create_status_handler :new_chat_title #(bot/send_message bot-token (get-in % [:message :chat :id]) "WOW!"))
  (handlers/create_status_handler :left_chat_member #(bot/send_message bot-token (get-in % [:message :chat :id]) "WAIT!"))
  (handlers/create_inline_query_handler inline_handler)
  (handlers/create_handler filters/text get_user)
  ])

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (bot/get_me bot-token))
  (alter-var-root #'users (fn [v] (read-users users-file)))
  (let [r (client/get myanimelist-search-api {:query-params {:q "full metal asd asd asd adas asd asd"}
                                                                     :basic-auth myanimelist-auth})]
    (println (:body r)))
  ;;(println (bot/send_message "141043767:AAGOD1ZEAvzNUuii6_Zxy-zydbU2x5z77so" 53941045 "kokoko"))
  (updater/start_handlers h (updater/start_polling bot-token 100 1000 0))
  ;;(updater/start_handlers h (updater/start_webhook bot-token "tzapil.tk" 8443 "hook" "cert.pem" "cert.keystore" "LjvbrGfhbn1"))
  (updater/idle))
