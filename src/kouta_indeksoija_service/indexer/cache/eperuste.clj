(ns kouta-indeksoija-service.indexer.cache.eperuste
  (:require [kouta-indeksoija-service.rest.eperuste :as eperuste-service]
            [kouta-indeksoija-service.indexer.tools.organisaatio :refer :all]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [clojure.core.cache :as cache]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version eperuste-laajuusyksikko->opintojen-laajuusyksikko]]))

(defonce eperuste_cache_time_millis (* 1000 60 20))

(defonce EPERUSTE_CACHE (atom (cache/ttl-cache-factory {} :ttl eperuste_cache_time_millis)))

(defn- get-opintojen-laajuus
  [opintojenLaajuusNumero]
  (when opintojenLaajuusNumero
    (let [opintojenLaajuusNumeroAsInt (if (string? opintojenLaajuusNumero) (Integer/parseInt opintojenLaajuusNumero) (int opintojenLaajuusNumero))
          opintojenLaajuusKoodiUri (str "opintojenlaajuus_" opintojenLaajuusNumeroAsInt)
          {:keys [nimi koodiUri]} (get-koodi-nimi-with-cache opintojenLaajuusKoodiUri)]
      {:opintojenLaajuusNumero opintojenLaajuusNumero
       :opintojenLaajuus (if (not (or (nil? nimi) (= {} nimi))) {:nimi nimi :koodiUri koodiUri} {})})))

(defn- get-tutkinnon-osat
  [eperuste]
  (let [tutkinnonOsat (some-> eperuste :tutkinnonOsat)
        tutkinnonOsaViitteet (some-> eperuste :suoritustavat (first) :tutkinnonOsaViitteet)]
    (vec (for [osa tutkinnonOsat
               :let [viite (first (filter #(= (str (:_tutkinnonOsa %)) (str (:id osa))) tutkinnonOsaViitteet))]]
           (merge
             {:id (:id osa)
              :koodiUri (get-in osa [:koodi :uri])
              :nimi (get-in osa [:koodi :nimi])}
             (get-opintojen-laajuus (:laajuus viite)))))))

(defn- filter-osaamisalat
  [osat]
  (filter #(not (nil? (:osaamisala %))) osat))

(defn- get-osaamisalat-recursive
  [osat]
  (concat (filter-osaamisalat osat) (mapcat #(get-osaamisalat-recursive (:osat %)) osat)))

(defn set-default-muodostumissaanto
  [osa default-muodostumissaanto]
  (let [muodostumissaanto (or (:muodostumisSaanto osa) default-muodostumissaanto)]
    (assoc osa
           :muodostumisSaanto muodostumissaanto
           :osat (map #(set-default-muodostumissaanto % muodostumissaanto)
                      (:osat osa)))))

(defn- get-osaamisalat
  [eperuste]
  (let [osat (some-> eperuste :suoritustavat (first) :rakenne :osat)
        osat-with-muodostumissaanto (map #(set-default-muodostumissaanto % nil) osat)]
    (vec (for [osaamisala (get-osaamisalat-recursive osat-with-muodostumissaanto)
               :let [muodostumissaanto (:muodostumisSaanto osaamisala)]]
           (merge
            {:nimi (get-in osaamisala [:osaamisala :nimi])
             :koodiUri (get-in osaamisala [:osaamisala :osaamisalakoodiUri])
             :tunniste (:tunniste osaamisala)}
            (get-opintojen-laajuus (get-in muodostumissaanto [:laajuus :minimi])))))))

(defn- strip
  [eperuste]
  (if-let [suoritustapa (some-> eperuste :suoritustavat (first))]
    (let [opintojenLaajuus                (get-opintojen-laajuus (get-in suoritustapa [:rakenne :muodostumisSaanto :laajuus :minimi]))
          opintojenLaajuusyksikkoKoodiUri (eperuste-laajuusyksikko->opintojen-laajuusyksikko (:laajuusYksikko suoritustapa))
          opintojenLaajuusyksikko         (when opintojenLaajuusyksikkoKoodiUri (get-koodi-nimi-with-cache opintojenLaajuusyksikkoKoodiUri))
          tutkinnonOsat                   (get-tutkinnon-osat eperuste)
          osaamisalat                     (get-osaamisalat eperuste)]
      (cond-> (select-keys eperuste [:id :diaarinumero :voimassaoloLoppuu :tutkintonimikkeet :koulutukset])
              (not (nil? opintojenLaajuus))                (merge opintojenLaajuus)
              (not (nil? opintojenLaajuusyksikko))         (assoc :opintojenLaajuusyksikko opintojenLaajuusyksikko)
              (not (nil? tutkinnonOsat))                   (assoc :tutkinnonOsat tutkinnonOsat)
              (not (nil? osaamisalat))                     (assoc :osaamisalat osaamisalat)))
    (select-keys eperuste [:id :diaarinumero :voimassaoloLoppuu :tutkintonimikkeet :koulutukset])))

(defn cache-eperuste
  [koodi]
  (when-let [eperuste (eperuste-service/get-by-koulutuskoodi koodi)]
    (let [stripped (strip eperuste)]
      (swap! EPERUSTE_CACHE cache/through-cache koodi (constantly stripped)))))

(defn get-eperuste-by-koulutuskoodi
  [koulutuskoodiUri]
  (let [koodi (remove-uri-version koulutuskoodiUri)]
    (if-let [eperuste (cache/lookup @EPERUSTE_CACHE koodi)]
      eperuste
      (do (cache-eperuste koodi)
          (cache/lookup @EPERUSTE_CACHE koodi)))))

(defn cache-eperuste-by-id
  [id]
  (when-let [eperuste (eperuste-service/get-doc id)]
    (let [stripped (strip eperuste)]
      (swap! EPERUSTE_CACHE cache/through-cache id (constantly stripped)))))

(defn get-eperuste-by-id
  [id]
  (if-let [eperuste (cache/lookup @EPERUSTE_CACHE id)]
    eperuste
    (do (cache-eperuste-by-id id)
        (cache/lookup @EPERUSTE_CACHE id))))

(defn filter-tutkinnon-osa
  [eperuste tutkinnon-osa-id]
  (some->> eperuste
           :tutkinnonOsat
           (filter #(= (:id %) tutkinnon-osa-id))
           (first)))