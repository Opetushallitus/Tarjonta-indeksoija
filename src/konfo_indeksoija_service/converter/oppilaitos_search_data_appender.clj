(ns konfo-indeksoija-service.converter.oppilaitos-search-data-appender
  (:require [konfo-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [konfo-indeksoija-service.rest.koodisto :as koodisto-client]
            [konfo-indeksoija-service.converter.tyyppi-converter :refer [oppilaitostyyppi-uri-to-tyyppi]]
            [clojure.tools.logging :as log]))

(defn- find-parent-oppilaitos-tyyppi-uri [oid]
  (defn- recursive-find-oppilaitostyyppi [organisaatio]
    (if (nil? organisaatio)
      nil
      (if-let [children (not-empty (:children organisaatio))]
        (if-let [type-from-child (recursive-find-oppilaitostyyppi (first children))]
          type-from-child
          (:oppilaitostyyppi organisaatio))
        (:oppilaitostyyppi organisaatio))))

  (let [hierarkia (organisaatio-client/get-tyyppi-hierarkia oid)]
    (if-let [organisaatiot (not-empty (:organisaatiot hierarkia))]
      (recursive-find-oppilaitostyyppi (first organisaatiot))
      nil)))

(defn- find-oppilaitos-tyyppi-uri [organisaatio]
  (if-let [oppilaitosTyyppiUri (:oppilaitosTyyppiUri organisaatio)]
    oppilaitosTyyppiUri
    (find-parent-oppilaitos-tyyppi-uri (:oid organisaatio))))

(defn- find-oppilaitos-tyyppi-nimi [oppilaitostyyppi-uri]
  (if (not (nil? oppilaitostyyppi-uri))
    (let [koodi-uri (first (clojure.string/split oppilaitostyyppi-uri #"#"))
          koodisto (koodisto-client/get-koodi-with-cache "oppilaitostyyppi" koodi-uri)
          metadata (:metadata koodisto)]

      (into {} (map (fn [x] {(keyword (clojure.string/lower-case (:kieli x))) (:nimi x)}) metadata)))))

(defn append-search-data
  [organisaatio]
  (let [oppilaitostyyppiUri (find-oppilaitos-tyyppi-uri organisaatio)
        oppilaitostyyppiNimi (find-oppilaitos-tyyppi-nimi oppilaitostyyppiUri)
        tyyppi (oppilaitostyyppi-uri-to-tyyppi oppilaitostyyppiUri)]
    (let [searchData (-> {}
                         (cond-> tyyppi (assoc :tyyppi tyyppi))
                         (cond-> oppilaitostyyppiUri (assoc :oppilaitostyyppi {:koodiUri oppilaitostyyppiUri :nimi oppilaitostyyppiNimi})))]
      (assoc organisaatio :searchData searchData))))