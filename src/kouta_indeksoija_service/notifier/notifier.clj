(ns kouta-indeksoija-service.notifier.notifier
  (:require [kouta-indeksoija-service.rest.util :refer [post]]
            [clj-log.error-log :refer [with-error-logging-value]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clojure.string :as str]
            [kouta-indeksoija-service.indexer.indexer :as indexer]))

(def receivers (filter not-empty (str/split (:notifier-targets env) #",")))

(defn- to-message
  [type id url object]
  (-> {}
      (assoc :type type)
      (assoc id (id object))
      (assoc :organisaatioOid (:oid (:organisaatio object)))
      (assoc :modified (:modified object))
      (assoc :url url)))

(defn- koulutus->message
  [koulutus]
  (let [url (resolve-url :kouta-external.koulutus.oid (:oid koulutus))
        tarjoajat (map :oid (:tarjoajat koulutus))
        message (to-message "koulutus" :oid url koulutus)]
    (assoc message :tarjoajat tarjoajat)))

(defn- haku->message
  [haku]
  (let [url (resolve-url :kouta-external.haku.oid (:oid haku))]
    (to-message "haku" :oid url haku)))

(defn- hakukohde->message
  [hakukohde]
  (let [url (resolve-url :kouta-external.hakukohde.oid (:oid hakukohde))]
    (to-message "hakukohde" :oid url hakukohde)))

(defn- toteutus->message
  [toteutus]
  (let [url (resolve-url :kouta-external.toteutus.oid (:oid toteutus))
        tarjoajat (map :oid (:tarjoajat toteutus))
        message (to-message "toteutus" :oid url toteutus)]
    (assoc message :tarjoajat tarjoajat)))

(defn- valintaperuste->message
  [valintaperuste]
  (let [url (resolve-url :kouta-external.valintaperuste.id (:id valintaperuste))]
    (to-message "valintaperuste" :id url valintaperuste)))

(defn- send-notification-messages
  [body]
  (let [msg {:form-params body
             :content-type :json}]
    (doall (map (fn [receiver]
                  (log/debug "Sending notification message" body "to" receiver)
                  (post receiver msg)) receivers))))

(defn- send-notifications
  [->message objects]
  (with-error-logging-value
   objects
   (let [messages (map ->message objects)]
     (doall (map send-notification-messages messages))
     objects)))

(defn notify
  [objects]
  (send-notifications koulutus->message (:koulutukset objects))
  (send-notifications haku->message (:haut objects))
  (send-notifications hakukohde->message (:hakukohteet objects))
  (send-notifications toteutus->message (:toteutukset objects))
  (send-notifications valintaperuste->message (:valintaperusteet objects)))