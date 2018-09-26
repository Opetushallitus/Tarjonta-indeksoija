(ns konfo-indeksoija-service.elastic.settings)

(def index-settings
  {:index.mapping.total_fields.limit 2000
   :analysis {:filter {:finnish_stop {:type "stop"
                                      :stopwords "_finnish_"}
                       :finnish_keywords {:type "keyword_marker"
                                          :keywords "_finnish_keywords_"}
                       :finnish_stemmer {:type "stemmer"
                                         :language "finnish"}
                       :swedish_stop {:type "stop"
                                      :stopwords "_swedish_"}
                       :swedish_keywords {:type "keyword_marker"
                                          :keywords "_swedish_keywords_"}
                       :swedish_stemmer {:type "stemmer"
                                         :language "swedish"}
                       :english_stop {:type "stop"
                                      :stopwords "_english_"}
                       :english_keywords {:type "keyword_marker"
                                          :keywords "_english_keywords_"}
                       :english_stemmer {:type "stemmer"
                                         :language "english"}
                       :english_possessive_stemmer {:type "stemmer"
                                                    :language "possessive_english"}}
              :analyzer {:finnish {:tokenizer "standard"
                                   :filter ["lowercase"
                                            "finnish_stop"
                                            "finnish_keywords"
                                            "finnish_stemmer"]}
                         :swedish {:tokenizer "standard"
                                   :filter ["lowercase"
                                            "swedish_stop"
                                            "swedish_keywords"
                                            "swedish_stemmer"]}
                         :english {:tokenizer "standard"
                                   :filter ["english_possessive_stemmer"
                                            "lowercase"
                                            "english_stop"
                                            "english_keywords"
                                            "english_stemmer"]}}}})

(def stemmer-settings
  {:dynamic_templates [{:fi {:match "kieli_fi"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "finnish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:sv {:match "kieli_sv"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "swedish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:en {:match "kieli_en"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "english"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       ]})

(def stemmer-settings-organisaatio
  {:dynamic_templates [{:fi {:match "fi"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "finnish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:sv {:match "sv"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "swedish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:en {:match "en"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "english"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:osoite {:match "osoite"
                                 :match_mapping_type "string"
                                 :mapping {:type "text"
                                           :analyzer "finnish"
                                           :norms { :enabled false}
                                           :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:postitoimipaikka {:match ":postitoimipaikka"
                                           :match_mapping_type "string"
                                           :mapping {:type "text"
                                                     :analyzer "finnish"
                                                     :norms { :enabled false}
                                                     :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:postinumeroUri {:match "postinumeroUri"
                                         :match_mapping_type "string"
                                         :mapping {:type "text"
                                                   :norms { :enabled false}
                                                   :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:lakkautusPvm {:match "lakkautusPvm"
                                       :mapping {:type "date"}}}
                       ]})

(def indexdata-mappings
  {:properties {:oid {:type "text"
                      :fields {:keyword {:type "keyword"
                                         :ignore_above 256}}}
                :timestamp {:type "long"}
                :type {:type "text"
                       :fields {:keyword {:type "keyword"
                                          :ignore_above 256}}}}})

(def boost-values
  ["*fi"
   "*sv"
   "*en"
   "organisaatio.nimi^30"
   "tutkintonimikes.nimi*^30"
   "koulutusohjelma.nimi*^30"
   "koulutusohjelmanNimiKannassa*^30"
   "koulutuskoodi.nimi^30"
   "ammattinimikkeet.nimi*^30"
   "aihees.nimi*^30"])