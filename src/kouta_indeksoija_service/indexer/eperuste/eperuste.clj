(ns kouta-indeksoija-service.indexer.eperuste.eperuste
  (:require [kouta-indeksoija-service.rest.eperuste :as eperuste-service]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "eperuste")

(defn create-index-entry
  [oid]
  (when-let [eperuste (eperuste-service/get-doc oid)]
    (let [id (str (:id eperuste))]
      (indexable/->index-entry id (assoc eperuste :oid id :tyyppi "eperuste")))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))