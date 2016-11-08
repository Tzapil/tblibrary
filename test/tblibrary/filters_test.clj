(ns tblibrary.filters-test
  (:require [clojure.test :refer :all]
            [tblibrary.filters :refer :all]))

(deftest not-nil-test
    (testing "test not nil"
        (is (not-nil? {}))
        (is (not (not-nil? nil)))))

(def commands [{:message {:text "/text"}} {:message {:text "/list"}} {:message {:text "/subscribe"}} {:message {:text "/plus 10 20"}} {:message {:text "/subs 10 20"}}])
(def messages [{:message {:text "text"}} {:message {:text "another text"}} {:message {:text "and another text"}}])
(def audios [{:message {:audio {:duration 200 :mime_type "audio/mpeg" :title "Sleep Away" :performer "Bob Acri"}}}])
(def documents [{:message {:document {:file_name "new.txt" :mime_type "text/plain" :file_id "BQADAgAD4AADNRM3A_9p0QFThSD0Ag" :file_size 1260}}}])
(def stickers [{:message {:sticker {:width 467 :height 512 :emoji "a" :thumb {:file_size 6142 :width 117 :height 128}}}}])
(def videos [{:message {:video {:file_name "Wildlife.wmv" :mime_type "video/x-ms-wmv" :file_id "BQADAgAD4QADNRM3A2qaQoBpnyJlAg" :file_size 26246026}}}])
(def voices [{:message {:voice {:duration 1 :mime_type "audio/ogg" :file_id "AwADAgAD4gADNRM3AzQZmnqX01VqAg" :file_size 4358}}}])
(def contacts [{:message {:contact {:phone_number "+79263146630" :first_name "Ann" :user_id 247297}}}])
(def locations [{:message {:location {:latitude "58.0482", :longitude "38.840466"}}}])
(def venues [{:message {:venue {:id 1}}}]) ;; TODO
(def statuses [{:message {:group_chat_created true}} {:message {:migrate_from_chat_id "-2828282"}} {:message {:new_chat_title "sososos"}}]) ;; TODO

(deftest command-test
  (testing "test command filter"
    (doseq [c commands]
          (is (command c)))
    (doseq [m (concat messages audios documents stickers videos voices contacts locations venues statuses)]
          (is (not (command m))))))

(deftest text-test
  (testing "test text filter"
    (doseq [c (concat audios commands documents stickers videos voices contacts locations venues statuses)]
          (is (not (text c))))
    (doseq [m messages]
          (is (text m)))))

(deftest audio-test
  (testing "test audio filter"
    (doseq [a audios]
          (is (audio a)))
    (doseq [m (concat messages commands documents stickers videos voices contacts locations venues statuses)]
          (is (not (audio m))))))

(deftest document-test
  (testing "test document filter"
    (doseq [a documents]
          (is (document a)))
    (doseq [m (concat messages commands audios stickers videos voices contacts locations venues statuses)]
          (is (not (document m))))))

(deftest sticker-test
  (testing "test sticker filter"
    (doseq [a stickers]
          (is (sticker a)))
    (doseq [m (concat messages commands audios documents videos voices contacts locations venues statuses)]
          (is (not (sticker m))))))

(deftest video-test
  (testing "test video filter"
    (doseq [a videos]
          (is (video a)))
    (doseq [m (concat messages commands audios documents stickers voices contacts locations venues statuses)]
          (is (not (video m))))))

(deftest voice-test
  (testing "test voice filter"
    (doseq [a voices]
          (is (voice a)))
    (doseq [m (concat messages commands audios documents stickers videos contacts locations venues statuses)]
          (is (not (voice m))))))

(deftest contact-test
  (testing "test contact filter"
    (doseq [a contacts]
          (is (contact a)))
    (doseq [m (concat messages commands audios documents stickers videos voices locations venues statuses)]
          (is (not (contact m))))))

(deftest location-test
  (testing "test location filter"
    (doseq [a locations]
          (is (location a)))
    (doseq [m (concat messages commands audios documents stickers videos voices contacts venues statuses)]
          (is (not (location m))))))


(deftest venue-test
  (testing "test venue filter"
    (doseq [a venues]
          (is (venue a)))
    (doseq [m (concat messages commands audios documents stickers videos voices contacts locations statuses)]
          (is (not (venue m))))))

(deftest status-test
  (testing "test status filter"
    (doseq [a statuses]
          (is (status_update a)))
    (doseq [m (concat messages commands audios documents stickers videos voices contacts locations venues)]
          (is (not (status_update m))))))