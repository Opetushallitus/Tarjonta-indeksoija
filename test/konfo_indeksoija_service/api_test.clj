(ns konfo-indeksoija-service.api-test
  (:require [konfo-indeksoija-service.api :refer :all]
            [konfo-indeksoija-service.elastic-client :as elastic-client]
            [konfo-indeksoija-service.test-tools :as tools :refer [parse-body stop-elastic-test init-elastic-test]]
            [konfo-indeksoija-service.indexer :as indexer]
            [mocks.externals-mock :refer [with-externals-mock]]
            [cheshire.core :as cheshire]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]))

(with-externals-mock
  (facts "Api should"
    (against-background
      [(before :contents (init-elastic-test))
       (after :contents (stop-elastic-test))]
        (fact "reindex hakukohde"
          (indexer/start-indexer-job)
          (let [response (app (mock/request :get "/konfo-indeksoija/api/reindex/hakukohde?oid=1.2.246.562.20.28810946823"))
                body (parse-body (:body response))]
            (:status response) => 200)
          (tools/block-until-indexed 15000)
          (elastic-client/get-queue) => [])

      (fact "fetch hakukohde"
        ;; uses result from previous test.
        (tools/refresh-and-wait "hakukohde" 1000)
        (let [response (app (mock/request :get "/konfo-indeksoija/api/admin/hakukohde?oid=1.2.246.562.20.28810946823"))
              body (parse-body (:body response))]
          (:hakuOid body) => "1.2.246.562.29.44465499083"))

      (comment fact "fetch koulutus tulos" :skip
        ;; This test uses tarjonta QA and organisaatio
        (elastic-client/delete-index "hakukohde")
        (elastic-client/upsert-indexdata [{:type "koulutus" :oid "1.2.246.562.17.53874141319"}
                                          {:type "hakukohde" :oid "1.2.246.562.20.67506762722"}
                                          {:type "hakukohde" :oid "1.2.246.562.20.715691882710"}
                                          {:type "hakukohde" :oid "1.2.246.562.20.82790530479"}
                                          {:type "hakukohde" :oid "1.2.246.562.20.17663370199"}
                                          {:type "haku" :oid "1.2.246.562.29.86197271827"}
                                          {:type "haku" :oid "1.2.246.562.29.59856749474"}
                                          {:type "haku" :oid "1.2.246.562.29.53522498558"}
                                          {:type "organisaatio" :oid "1.2.246.562.10.39920288212"}])
        (tools/block-until-indexed 10000)

        (tools/refresh-and-wait "hakukohde" 0)
        (tools/refresh-and-wait "haku" 0)
        (tools/refresh-and-wait "organisaatio" 0)
        (tools/refresh-and-wait "koulutus" 1000)
        (let [response (app (mock/request :get "/konfo-indeksoija/api/ui/koulutus/1.2.246.562.17.53874141319"))
              body (parse-body (:body response))
              koulutus (:koulutus body)
              haut (:haut body)
              hakukohteet (:hakukohteet body)
              organisaatiot (:organisaatiot body)]
          (:status response) => 200
          (count hakukohteet) => 4
          (count haut) => 3
          (count organisaatiot) => 1

          (empty? koulutus) => false?

          (:oid (:1.2.246.562.17.53874141319 koulutus)) => "1.2.246.562.17.53874141319"

          (doseq [x (map :koulutukset (vals hakukohteet))]
            x => (contains "1.2.246.562.17.53874141319"))

          (sort (distinct (map :hakuOid (vals hakukohteet)))) => (sort (map :oid (vals haut)))))

      (comment fact "fetch text search result"
        (let [response (app (mock/request :get "/konfo-indeksoija/api/ui/search?query=Tekn.%20kand.,%20tietotekniikka"))
              body (parse-body (:body response))]
          body => [{:nimi {:kieli_en "MSc, Information Technology"
                           :kieli_fi "Dipl.ins., tietotekniikka"
                           :kieli_sv "Dipl.ing., datateknik"}, :oid "1.2.246.562.17.53874141319"
                    :score 0.8630463, :tarjoaja "Aalto-yliopisto, Perustieteiden korkeakoulu"}])) ;TODO: scoring was 0.7594807 -> scoring changed due to Elastic version difference?

      (comment fact "fetch performance info"
        (tools/refresh-and-wait "query_perf" 1000)
        (let [response (app (mock/request :get "/konfo-indeksoija/api/admin/performance_info"))
              body (parse-body (:body response))]
          (empty? (get-in body [:indexing_performance :results]))=> false
          (empty? (get-in body [:query_performance :results]))=> false)))))
