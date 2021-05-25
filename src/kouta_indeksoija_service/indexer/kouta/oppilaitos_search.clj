(ns kouta-indeksoija-service.indexer.kouta.oppilaitos-search
  (:require [clojure.string :as string]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache list-alakoodi-nimet-with-cache]]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.hakutieto :refer [get-search-hakutiedot]]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec]]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen? amm-tutkinnon-osa? julkaistu? asiasana->lng-value-map]]
            [kouta-indeksoija-service.indexer.tools.search :as search-tool]))

(def index-name "oppilaitos-kouta-search")

(defn- tarjoaja-organisaatiot
  [oppilaitos tarjoajat]
  (vec (map #(organisaatio-tool/find-from-organisaatio-and-children oppilaitos %) tarjoajat)))

(defn- tutkintonimikkeet-for-osaamisala
  [osaamisalaKoodiUri]
  (list-alakoodi-nimet-with-cache osaamisalaKoodiUri "tutkintonimikkeet"))

(defn- tutkintonimikket-for-toteutus
  [toteutus]
  ;TODO -> eperusteet
  (if (ammatillinen? toteutus)
    (->> (get-in toteutus [:metadata :osaamisalat :koodiUri])
         (mapcat tutkintonimikkeet-for-osaamisala)
         (->distinct-vec))
    []))

(defn oppilaitos-hit
  [oppilaitos]
  (search-tool/hit :koulutustyypit  (vector (search-tool/koulutustyyppi-for-organisaatio oppilaitos))
                   :opetuskieliUrit (:kieletUris oppilaitos)
                   :tarjoajat       (vector oppilaitos)
                   :oppilaitos      oppilaitos
                   :logo            (:logo oppilaitos)))

(defn koulutus-hit
  [oppilaitos koulutus]
  (search-tool/hit :koulutustyypit     (search-tool/deduce-koulutustyypit koulutus)
                   :opetuskieliUrit    (:kieletUris oppilaitos)
                   :tarjoajat          (tarjoaja-organisaatiot oppilaitos (:tarjoajat koulutus))
                   :tarjoajaOids       (:tarjoajat koulutus)
                   :oppilaitos         oppilaitos
                   :koulutusalaUrit    (search-tool/koulutusala-koodi-urit koulutus)
                   :tutkintonimikeUrit (search-tool/tutkintonimike-koodi-urit koulutus)
                   :nimet              (vector (:nimi koulutus))
                   :koulutusOid        (:oid koulutus)
                   :kuva               (:teemakuva koulutus)
                   :onkoTuleva         true
                   :nimi               (:nimi koulutus)
                   :metadata           (cond-> {:tutkintonimikkeetKoodiUrit      (search-tool/tutkintonimike-koodi-urit koulutus)
                                                :opintojenLaajuusKoodiUri        (search-tool/opintojen-laajuus-koodi-uri koulutus)
                                                :opintojenLaajuusyksikkoKoodiUri (search-tool/opintojen-laajuusyksikko-koodi-uri koulutus)
                                                :opintojenLaajuusNumero          (search-tool/opintojen-laajuus-numero koulutus)
                                                :koulutustyypitKoodiUrit         (search-tool/koulutustyyppi-koodi-urit koulutus)
                                                :koulutustyyppi                  (:koulutustyyppi koulutus)}
                                         (amm-tutkinnon-osa? koulutus) (assoc :tutkinnonOsat (search-tool/tutkinnon-osat koulutus)))))

(defn toteutus-hit
  [oppilaitos koulutus hakutiedot toteutus]
  (let [hakutieto (search-tool/get-toteutuksen-julkaistut-hakutiedot hakutiedot toteutus)
        toteutus-metadata (:metadata toteutus)
        opetus (get-in toteutus [:metadata :opetus])]
    (search-tool/hit :koulutustyypit            (search-tool/deduce-koulutustyypit koulutus (:ammatillinenPerustutkintoErityisopetuksena toteutus-metadata))
                     :opetuskieliUrit           (get-in toteutus [:metadata :opetus :opetuskieliKoodiUrit])
                     :tarjoajat                 (tarjoaja-organisaatiot oppilaitos (:tarjoajat toteutus))
                     :tarjoajaOids              (:tarjoajat toteutus)
                     :oppilaitos                oppilaitos
                     :koulutusalaUrit           (search-tool/koulutusala-koodi-urit koulutus)
                     :tutkintonimikeUrit        (search-tool/tutkintonimike-koodi-urit koulutus)
                     :opetustapaUrit            (or (some-> toteutus :metadata :opetus :opetustapaKoodiUrit) [])
                     :nimet                     (vector (:nimi koulutus) (:nimi toteutus))
                     :asiasanat                 (asiasana->lng-value-map (get-in toteutus [:metadata :asiasanat]))
                     :ammattinimikkeet          (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet]))
                     :koulutusOid               (:oid koulutus)
                     :toteutusOid               (:oid toteutus)
                     :nimi                      (:nimi toteutus)
                     :hakutiedot                (get-search-hakutiedot hakutieto)
                     :pohjakoulutusvaatimusUrit (search-tool/pohjakoulutusvaatimus-koodi-urit hakutieto)
                     :kuva                      (:teemakuva toteutus)
                     :onkoTuleva                false
                     :metadata                  {:tutkintonimikkeet  (tutkintonimikket-for-toteutus toteutus)
                                                 :opetusajatKoodiUrit (:opetusaikaKoodiUrit opetus)
                                                 :maksullisuustyyppi  (:maksullisuustyyppi opetus)
                                                 :maksunMaara         (:maksunMaara opetus)
                                                 :koulutustyyppi      (:koulutustyyppi koulutus)})))

(defn- get-kouta-oppilaitos
  [oid]
  (let [oppilaitos (kouta-backend/get-oppilaitos oid)]
    (when (julkaistu? oppilaitos)
      {:kielivalinta (:kielivalinta oppilaitos)
       :kuvaus       (get-in oppilaitos [:metadata :esittely])
       :logo         (:logo oppilaitos)})))

(defn- create-base-entry
  [oppilaitos koulutukset]
  (-> oppilaitos
      (select-keys [:oid :nimi])
      (merge (get-kouta-oppilaitos (:oid oppilaitos)))
      (assoc :koulutusohjelmia (count (filter :johtaaTutkintoon koulutukset)))))

(defn- assoc-paikkakunnat
  [entry]
  (let [paikkakuntaKoodiUrit (vec (distinct (filter #(string/starts-with? % "kunta") (mapcat :sijainti (:hits entry)))))]
    (assoc entry :paikkakunnat (vec (map get-koodi-nimi-with-cache paikkakuntaKoodiUrit)))))

(defn get-tarjoaja-entries
  [hierarkia entries]
  (->> (for [entry entries]
         (when-let [indexable-oids (seq (organisaatio-tool/filter-indexable-oids-for-hierarkia hierarkia (:tarjoajat entry)))]
           (assoc entry :tarjoajat indexable-oids)))
       (remove nil?)
       (vec)))

(defn- create-koulutus-hits
  [oppilaitos hierarkia koulutus]
  (if-let [toteutukset (seq (get-tarjoaja-entries hierarkia (kouta-backend/get-toteutus-list-for-koulutus (:oid koulutus) true)))]
    (let [hakutiedot (kouta-backend/get-hakutiedot-for-koulutus (:oid koulutus))]
      (vec (map #(toteutus-hit oppilaitos koulutus hakutiedot %) toteutukset)))
    (vector (koulutus-hit oppilaitos koulutus))))

(defn- create-oppilaitos-entry-with-hits
  [oppilaitos hierarkia]
  (let [koulutus-hits (partial create-koulutus-hits oppilaitos hierarkia)
        koulutukset (get-tarjoaja-entries hierarkia (kouta-backend/get-koulutukset-by-tarjoaja (:oid oppilaitos)))]
    (-> oppilaitos
        (create-base-entry koulutukset)
        (assoc :hits (if (seq koulutukset)
                       (vec (mapcat #(koulutus-hits %) koulutukset))
                       (vector (oppilaitos-hit oppilaitos))))
        (assoc-paikkakunnat))))

(defn create-index-entry
  [oid]
  (let [hierarkia (cache/get-hierarkia oid)]
    (when-let [oppilaitos (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia)]
      (if (organisaatio-tool/indexable? oppilaitos)
        (indexable/->index-entry (:oid oppilaitos) (create-oppilaitos-entry-with-hits oppilaitos hierarkia))
        (indexable/->delete-entry (:oid oppilaitos))))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
