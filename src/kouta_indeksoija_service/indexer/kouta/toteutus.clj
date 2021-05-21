(ns kouta-indeksoija-service.indexer.kouta.toteutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec]]
            [clojure.string :refer [blank?]]))

(def index-name "toteutus-kouta")

(defn- determine-correct-aikataulu-and-hakulomake
  [ht-haku ht-hakukohde]
  (let [hakulomakeKeys  [:hakulomaketyyppi :hakulomakeAtaruId :hakulomakeKuvaus :hakulomakeLinkki]
        alkamisaikaKey  [:koulutuksenAlkamiskausi]
        aikatauluKeys   [:hakuajat]
        hakuOid         (:hakuOid ht-haku)]
    (merge {}
           (if (true? (:kaytetaanHaunHakulomaketta ht-hakukohde))
             (conj (select-keys ht-haku hakulomakeKeys) (common/create-hakulomake-linkki ht-haku hakuOid))
             (conj (select-keys ht-hakukohde hakulomakeKeys) (common/create-hakulomake-linkki ht-hakukohde hakuOid)))
           (if (true? (:kaytetaanHaunAlkamiskautta ht-hakukohde))
             (select-keys ht-haku alkamisaikaKey)
             (select-keys ht-hakukohde alkamisaikaKey))
           (if (true? (:kaytetaanHaunAikataulua ht-hakukohde))
             (select-keys ht-haku aikatauluKeys)
             (select-keys ht-hakukohde aikatauluKeys)))))

(defn- create-hakukohteiden-hakutiedot
  [ht-haku]
  (for [ht-hakukohde (:hakukohteet ht-haku)]
    (-> (select-keys ht-hakukohde [:hakukohdeOid
                                   :nimi
                                   :modified
                                   :tila
                                   :esikatselu
                                   :valintaperusteId
                                   :pohjakoulutusvaatimusKoodiUrit
                                   :pohjakoulutusvaatimusTarkenne
                                   :aloituspaikat
                                   :hakukohteenLinja
                                   :jarjestyspaikkaOid
                                   :organisaatioOid])
        (merge (determine-correct-aikataulu-and-hakulomake ht-haku ht-hakukohde))
        (common/decorate-koodi-uris)
        (common/assoc-jarjestyspaikka)
        (common/assoc-organisaatio))))

(defn- determine-correct-hakutiedot
  [ht-toteutus]
  (-> (for [ht-haku (:haut ht-toteutus)]
        (-> (select-keys ht-haku [:hakuOid :nimi :hakutapaKoodiUri :koulutuksenAlkamiskausi])
            (common/decorate-koodi-uris)
            (assoc :hakukohteet (vec (create-hakukohteiden-hakutiedot ht-haku)))))
      (vec)))

(defn- assoc-hakutiedot
  [toteutus hakutiedot]
  (if-let [ht-toteutus (first (filter (fn [x] (= (:toteutusOid x) (:oid toteutus))) hakutiedot))]
    (assoc toteutus :hakutiedot (determine-correct-hakutiedot ht-toteutus))
    toteutus))

(defn- assoc-tarjoajien-oppilaitokset
  [toteutus]
  (assoc toteutus
    :oppilaitokset
    (->> (:tarjoajat toteutus)
         (map :oid)
         (map cache/get-hierarkia)
         (map organisaatio-tool/find-oppilaitos-from-hierarkia)
         (remove nil?)
         (filter organisaatio-tool/indexable?)
         (map :oid)
         (->distinct-vec))))

(defn create-index-entry
  [oid]
  (let [toteutus (kouta-backend/get-toteutus oid)
        hakutiedot (kouta-backend/get-hakutiedot-for-koulutus (:koulutusOid toteutus))]
    (indexable/->index-entry oid (-> toteutus 
                                     (common/complete-entry)
                                     (common/assoc-organisaatiot)
                                     (assoc-tarjoajien-oppilaitokset)
                                     (assoc-hakutiedot hakutiedot)))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
