(ns kouta-indeksoija-service.indexer.kouta.koulutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.toteutus :refer [to-list-item]]))

(def index-name "koulutus-kouta")

;TODO: amm tutkintonimikkeet, koulutuksen laajuus, opetuskielet, suunniteltu kesto ja koulutusaste

(defn create-index-entry
  [oid]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus oid))
        toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid))]
    (-> koulutus
        (common/assoc-organisaatiot)
        (assoc :toteutukset (map to-list-item toteutukset)))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))