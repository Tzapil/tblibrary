(ns tblibrary.helpers-test
  (:require [clojure.test :refer :all]
            [tblibrary.helpers :refer :all]))

(def parse-int-tests 10)
(def parse-int-max 10000)

(deftest parse-int-test
  (testing "test parse_int function"
    (doseq [_ (range parse-int-tests)
            :let [n (rand-int parse-int-max)
                  s (str n)]]
          (is (= n (parse_int s))))))