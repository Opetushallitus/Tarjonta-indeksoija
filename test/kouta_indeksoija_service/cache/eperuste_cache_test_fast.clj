(ns kouta-indeksoija-service.cache.eperuste-cache-test-fast
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-indeksoija-service.indexer.cache.eperuste :as cache.eperuste]))


(deftest set-default-muodostumissaanto
  (testing "sets default muodostumissaanto for Talonrakennuksen osaamisala that does not have one"
    (is (= (cache.eperuste/set-default-muodostumissaanto {:osat [{:osat []
                                                                  :muodostumisSaanto {:laajuus {:minimi 50
                                                                                                :maksimi 50}}
                                                                  :nimi {:fi "Pakollinen tutkinnon osa"}}]
                                                          :osaamisala {:nimi {:fi "Talonrakennuksen osaamisala"}
                                                                       :osaamisalakoodiArvo "1759"}
                                                          :muodostumisSaanto nil
                                                          :nimi {:fi "Talonrakennuksen osaamisala"}
                                                          :rooli "osaamisala"}
                                                         {:laajuus {:minimi 145 :maksimi 145}})
           {:osat [{:osat []
                    :muodostumisSaanto {:laajuus {:minimi 50
                                                  :maksimi 50}}
                    :nimi {:fi "Pakollinen tutkinnon osa"}}]
            :osaamisala {:nimi {:fi "Talonrakennuksen osaamisala"}
                         :osaamisalakoodiArvo "1759"}
            :muodostumisSaanto {:laajuus {:minimi 145 :maksimi 145}}
            :nimi {:fi "Talonrakennuksen osaamisala"}
            :rooli "osaamisala"})))

  (testing "keeps the original muodostumissaanto for 'Pakollinen tutkinnon osa' that has it already defined"
    (is (= (cache.eperuste/set-default-muodostumissaanto {:osat []
                                                          :muodostumisSaanto {:laajuus {:minimi 50
                                                                                        :maksimi 50}}
                                                          :nimi {:fi "Pakollinen tutkinnon osa"}}
                                                         {:laajuus {:minimi 145 :maksimi 145}})
           {:osat []
            :muodostumisSaanto {:laajuus {:minimi 50
                                          :maksimi 50}}
            :nimi {:fi "Pakollinen tutkinnon osa"}})))

  (testing "recursively sets muodostumisSaanto from 'Ammatilliset tutkinnon osat' for Talonrakennuksen and Maarakennuksen osaamisalat"
    (is (= (cache.eperuste/set-default-muodostumissaanto {:osat [{:osat []
                                                                  :muodostumisSaanto {:laajuus {:minimi 25
                                                                                                :maksimi 25}}
                                                                  :nimi {:fi "Pakollinen tutkinnon osa"}}
                                                                 {:osat [{:osat []
                                                                          :muodostumisSaanto {:laajuus {:minimi 50
                                                                                                        :maksimi 50}}
                                                                          :nimi {:fi "Pakollinen tutkinnon osa"}}]
                                                                  :osaamisala {:nimi {:fi "Talonrakennuksen osaamisala"}
                                                                               :osaamisalakoodiArvo "1759"}
                                                                  :muodostumisSaanto nil
                                                                  :nimi {:fi "Talonrakennuksen osaamisala"}
                                                                  :rooli "osaamisala"}
                                                                 {:osat [{:osat []
                                                                          :muodostumisSaanto {:laajuus {:minimi 50
                                                                                                        :maksimi 50}}
                                                                          :nimi {:fi "Pakollinen tutkinnon osa"}}]
                                                                  :osaamisala {:nimi {:fi "Maarakennuksen osaamisala"}
                                                                               :osaamisalakoodiArvo "1757"}
                                                                  :muodostumisSaanto nil
                                                                  :nimi {:fi "Maarakennuksen osaamisala"}
                                                                  :rooli "osaamisala"}]
                                                          :osaamisala nil
                                                          :muodostumisSaanto {:laajuus {:minimi 145
                                                                                        :maksimi 145}}
                                                          :nimi {:fi "Ammatilliset tutkinnon osat"}}
                                                         nil)
           {:osat [{:osat []
                    :muodostumisSaanto {:laajuus {:minimi 25
                                                  :maksimi 25}}
                    :nimi {:fi "Pakollinen tutkinnon osa"}}
                   {:osat [{:osat []
                            :muodostumisSaanto {:laajuus {:minimi 50
                                                          :maksimi 50}}
                            :nimi {:fi "Pakollinen tutkinnon osa"}}]
                    :osaamisala {:nimi {:fi "Talonrakennuksen osaamisala"}
                                 :osaamisalakoodiArvo "1759"}
                    :muodostumisSaanto {:laajuus {:minimi 145 :maksimi 145}}
                    :nimi {:fi "Talonrakennuksen osaamisala"}
                    :rooli "osaamisala"}
                   {:osat [{:osat []
                            :muodostumisSaanto {:laajuus {:minimi 50
                                                          :maksimi 50}}
                            :nimi {:fi "Pakollinen tutkinnon osa"}}]
                    :osaamisala {:nimi {:fi "Maarakennuksen osaamisala"}
                                 :osaamisalakoodiArvo "1757"}
                    :muodostumisSaanto {:laajuus {:minimi 145 :maksimi 145}}
                    :nimi {:fi "Maarakennuksen osaamisala"}
                    :rooli "osaamisala"}]
            :osaamisala nil
            :muodostumisSaanto {:laajuus {:minimi 145
                                          :maksimi 145}}
            :nimi {:fi "Ammatilliset tutkinnon osat"}}))))
