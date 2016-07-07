(ns tblibrary.filters
    (:require [clojure.string :as string]))

(def not-nil? (complement nil?))

(defn command [u]
    (let [t (get-in u [:message :text])]
        (and (not-nil? t) (string/starts-with? t "/"))))

(defn text [u]
    (let [t (get-in u [:message :text])]
        (and (not-nil? t) (not (string/starts-with? t "/")))))

(defn audio [u]
    (let [a (get-in u [:message :audio])]
        (not-nil? a)))

(defn document [u]
    (let [d (get-in u [:message :document])]
        (not-nil? d)))

(defn sticker [u]
    (let [s (get-in u [:message :sticker])]
        (not-nil? s)))

(defn video [u]
    (let [v (get-in u [:message :video])]
        (not-nil? v)))

(defn voice [u]
    (let [v (get-in u [:message :voice])]
        (not-nil? v)))

(defn contact [u]
    (let [c (get-in u [:message :contact])]
        (not-nil? c)))

(defn location [u]
    (let [l (get-in u [:message :location])]
        (not-nil? l)))

(defn venue [u]
    (let [v (get-in u [:message :venue])]
        (not-nil? v)))

(defn status_update [su]
    (let [message (get su :message)
          statuses [:new_chat_member :left_chat_member :new_chat_title :new_chat_photo :delete_chat_photo
                  :group_chat_created :supergroup_chat_created :channel_chat_created :migrate_to_chat_id 
                  :migrate_from_chat_id :pinned_message]]
        (some not-nil? (map #(get message %) statuses))))