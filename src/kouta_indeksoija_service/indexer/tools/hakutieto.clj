(ns kouta-indeksoija-service.indexer.tools.hakutieto
  (:require [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer [koodiuri-yhteishaku-hakutapa]]
            [kouta-indeksoija-service.indexer.tools.search :refer [pohjakoulutusvaatimus-koodi-urit]]))

(defn- kaytetaanHaunAikatauluaHakukohteessa?
  [hakukohde]
  (true? (:kaytetaanHaunAikataulua hakukohde)))

(defn- get-hakukohde-hakutieto
  [hakukohde haku]
  (-> {}
      (assoc :hakuajat (:hakuajat (if kaytetaanHaunAikatauluaHakukohteessa? haku hakukohde)))
      (assoc :hakutapa (:hakutapaKoodiUri haku))
      (assoc :yhteishakuOid (when (= koodiuri-yhteishaku-hakutapa (remove-uri-version (:hakutapaKoodiUri haku))) (:hakuOid haku)))
      (assoc :pohjakoulutusvaatimukset (pohjakoulutusvaatimus-koodi-urit hakukohde))
      (assoc :valintatavat (:valintatapaKoodiUrit hakukohde))))

(defn- map-haut
  [haku]
  (map #(get-hakukohde-hakutieto % haku) (:hakukohteet haku)))

(defn get-search-hakutiedot
  [hakutieto]  (vec (flatten (map map-haut (:haut hakutieto)))))
