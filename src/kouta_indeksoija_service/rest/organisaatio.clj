(ns kouta-indeksoija-service.rest.organisaatio
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.rest.util :refer [get->json-body post->json-body]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.time :refer :all]
            [clojure.core.memoize :as memoize]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as o]))

(defn get-doc
  ([oid include-image]
   (-> (resolve-url :organisaatio-service.organisaatio oid)
       (get->json-body {:includeImage include-image})))
  ([oid]
   (get-doc oid false)))

(defn find-docs
  [oid]
  (->> (get->json-body (resolve-url :organisaatio-service.v2.hae)
                       {:aktiiviset true :suunnitellut false :lakkautetut false :oid oid})
       :organisaatiot
       (map #(conj (string/split (:parentOidPath %) #"/") (:oid %)))
       flatten
       distinct
       vec))

(defn get-all-oppilaitos-oids
  []
  (->> (get->json-body (resolve-url :organisaatio-service.v2.hae.tyyppi)
                       {:aktiiviset true :suunnitellut false :lakkautetut false :organsaatiotyyppi "Oppilaitos"})
       :organisaatiot
       (map #(:oid %))
       vec))

(defn get-tyyppi-hierarkia
  [oid]
  (get->json-body (resolve-url :organisaatio-service.v2.hierarkia.tyyppi)
                  {:aktiiviset true :suunnitellut true :lakkautetut true :oid oid}))

(defonce oph-oid "1.2.246.562.10.00000000001")

(defn get-all-organisaatiot
  []
  (let [url (resolve-url :organisaatio-service.v4.oid.jalkelaiset oph-oid)]
    (log/info "Fetching all organisaatiot from organisaatio service " url)
    (get->json-body url)))

(defn clear-get-all-organisaatiot-cache
  []
  (memoize/memo-clear! get-all-organisaatiot))

(def get-all-organisaatiot-with-cache
  (memoize/memo get-all-organisaatiot))

(defn get-hierarkia-v4
  [oid]
  (some-> (get-all-organisaatiot-with-cache) (o/find-hierarkia oid)))

(defn find-last-changes
  [last-modified]
  (let [date-string (long->date-time-string last-modified)]
    (let [res (->> (get->json-body (resolve-url :organisaatio-service.v4.muutetut) {:lastModifiedSince date-string})
                   (map :oid))]
      (when (seq res)
        (log/info "Found " (count res) " changes since " date-string " from organisaatiopalvelu"))
      res)))

(defn find-by-oids
  [oids]
  (if (empty? oids)
    []
    (let [url  (resolve-url :organisaatio-service.v4.find-by-oids)
          body (str "[\"" (string/join "\", \"" oids) "\"]")]
      (post->json-body url body))))

(defn get-by-oid
  [oid]
  (get->json-body (resolve-url :organisaatio-service.v4.oid oid)))

(def get-by-oid-cached
  (memoize/ttl get-by-oid {} :ttl/threshold 86400000))
