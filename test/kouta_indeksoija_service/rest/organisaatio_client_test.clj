(ns kouta-indeksoija-service.rest.organisaatio-client-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.test-tools :as tools]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.rest.util :as client]))

(defn mock-last-modified [url opts]
  {:status 200 :body (tools/parse (str "test/resources/organisaatiot/last-modified.json"))})

(deftest organisaatio-client-test
  (testing "Organisaatio client should"
    (testing "get last modified"
      (with-redefs [client/get mock-last-modified]
        (let [res      (organisaatio-client/find-last-changes (System/currentTimeMillis))
              expected (seq ["1.2.246.562.10.83431223615" "1.2.246.562.10.86077555174" "1.2.246.562.10.919116475010" "1.2.246.562.10.50880626531"])]
          (is (= expected res)))))))