(ns kouta-indeksoija-service.rest.eperuste
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clj-log.error-log :refer [with-error-logging]]
            [kouta-indeksoija-service.rest.util :refer [get->json-body]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.time :as time]))

(defn- get-perusteet-page [page-nr last-modified]
  (let [params (cond-> {:sivu page-nr :sivukoko 100 :tuleva true :siirtyma true :voimassaolo true :poistunut true}
                       (not (nil? last-modified)) (assoc :muokattu last-modified))]
    (get->json-body (resolve-url :eperusteet-service.perusteet) params)))

(defn- find
  ([last-modified]
    (loop [page-nr 0 result []]
      (let [data (or (get-perusteet-page page-nr last-modified) {:data [] :sivuja -1})
            total-result (vec (conj result (map #(-> % :id str) (:data data))))]
          (if (<= (:sivuja data) (+ 1 page-nr))
            (flatten total-result)
            (recur (+ 1 page-nr) total-result)))))
  ([] (find nil)))

(defn get-doc
  [eperuste-id]
  (get->json-body
    (resolve-url :eperusteet-service.peruste.kaikki eperuste-id)))

(defn get-tutkinnonosat
  [tutkinnonosat-id]
  (get->json-body
    (resolve-url :eperusteet-service.internal.api.tutkinnonosat tutkinnonosat-id)))

(defn get-osaamisalakuvaukset
  [eperuste-id]
  (when-let [res (get->json-body (resolve-url :eperusteet-service.peruste.osaamisalakuvaukset eperuste-id))]
    (let [suoritustavat (keys res)
          osaamisalat (fn [suoritustapa] (apply concat (-> res suoritustapa vals)))
          assoc-values (fn [suoritustapa osaamisala] (assoc osaamisala :suoritustapa suoritustapa
                                                                       :type "osaamisalakuvaus"
                                                                       :oid (:id osaamisala)
                                                                       :eperuste-oid eperuste-id))]
      (vec (flatten (map (fn [st] (map (partial assoc-values st) (osaamisalat st))) suoritustavat))))))

(defn find-all
  []
  (let [res (find)]
    (log/info (str "Found total " (count res) " docs from ePerusteet"))
    res))

(defn find-changes
  [last-modified]
  (let [res (find last-modified)]
    (when (seq res)
      (log/info (str "Found " (count res) " changes since " (time/long->date-time-string (long last-modified)) " from ePerusteet")))
    res))

(defn- search-and-get-first-eperuste
  [params]
  (let [r (get->json-body (resolve-url :eperusteet-service.perusteet) params)]
    (when-let [id (some-> r :data (first) :id)]
      (get-doc id))))

(defn get-by-koulutuskoodi
  [koulutuskoodi]
  (or (search-and-get-first-eperuste {:tuleva false :siirtyma false :voimassaolo true :poistunut false :koulutuskoodi koulutuskoodi})
      (search-and-get-first-eperuste {:tuleva false :siirtyma true :voimassaolo false :poistunut false :koulutuskoodi koulutuskoodi})))