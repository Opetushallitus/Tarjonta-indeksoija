(ns kouta-indeksoija-service.elastic.settings)

(def index-settings
  {:index.mapping.total_fields.limit 2000
   :analysis {:filter {:ngram_compound_words_and_conjugations {:type "ngram" ;automaa utomaat tomaati omaatio maatioi aatioin atioins tioinsi ioinsin oinsinö insinöö nsinöör
                                                               :min_gram "3"
                                                               :max_gram "30"
                                                               :max_ngram_diff "35"
                                                               :token_chars ["letter", "digit"]}
                       :finnish_stop {:type "stop"
                                      :stopwords "_finnish_"}
                       :finnish_stemmer {:type "stemmer"
                                         :language "finnish"}
                       :finnish_stemmer_for_long_words {:type "condition"
                                                        :filter ["finnish_stemmer"]
                                                        :script {:source "token.getTerm().length() > 5"}}
                       :swedish_stop {:type "stop"
                                      :stopwords "_swedish_"}
                       :swedish_stemmer {:type "stemmer"
                                         :language "swedish"}
                       :swedish_stemmer_for_long_words {:type "condition"
                                                        :filter ["swedish_stemmer"]
                                                        :script {:source "token.getTerm().length() > 5"}}
                       :english_stop {:type "stop"
                                      :stopwords "_english_"}
                       :english_keywords {:type "keyword_marker"
                                          :keywords "_english_keywords_"}
                       :english_stemmer {:type "stemmer"
                                         :language "english"}
                       :english_possessive_stemmer {:type "stemmer"
                                                    :language "possessive_english"}},
              :analyzer {:finnish {:type "custom"
                                   :tokenizer "standard"
                                   :filter ["lowercase"
                                            "finnish_stop"
                                            "ngram_compound_words_and_conjugations"
                                            "remove_duplicates"]}
                         :finnish_keyword {:type "custom"
                                           :tokenizer "standard"
                                           :filter ["lowercase"
                                                    "finnish_stop"
                                                    "finnish_stemmer_for_long_words"]}
                         :swedish {:type "custom"
                                   :tokenizer "standard"
                                   :filter ["lowercase"
                                            "swedish_stop"
                                            "ngram_compound_words_and_conjugations"
                                            "remove_duplicates"]}
                         :swedish_keyword {:type "custom"
                                           :tokenizer "standard"
                                           :filter ["lowercase"
                                                    "swedish_stop"
                                                    "swedish_stemmer_for_long_words"]}
                         :english {:tokenizer "standard"
                                   :filter ["english_possessive_stemmer"
                                            "lowercase"
                                            "english_stop"
                                            "english_keywords"
                                            "english_stemmer"]}}
              :normalizer {:case_insensitive {:filter "lowercase"}}}})

(def index-settings-eperuste (merge index-settings {:index.mapping.total_fields.limit 4000}))

(def index-settings-lokalisointi
  {:index.mapping.total_fields.limit 2000})

(def lokalisointi-mappings
  {:dynamic_templates [{:all {:match "*",
                             :match_mapping_type "string",
                             :mapping {:type "keyword",
                                       :norms {:enabled false}}}}]})

(def eperuste-mappings
  {:dynamic_templates [{:fi {:match "kieli_fi"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "finnish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}
                       {:sv {:match "kieli_sv"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "swedish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}
                       {:en {:match "kieli_en"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "english"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}]})

(def koodisto-mappings
  {:dynamic_templates [{:nested {:match "koodit"
                                 :match_mapping_type "object"
                                 :mapping { :type "nested" }}}
                       {:fi {:match "fi"
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
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}]})

(def kouta-search-mappings
  {:properties {:hits {:type "nested",
                       :properties {:hakutiedot {:type "nested"
                                                 :properties {:hakutapa {:type "keyword"}
                                                              :yhteishakuOid {:type "keyword"}
                                                              :pohjakoulutusvaatimukset {:type "keyword"}
                                                              :valintatavat {:type "keyword"}
                                                              :hakuajat {:type "nested"
                                                                         :properties {:alkaa   {:type "date" }
                                                                                      :paattyy {:type "date" }}}}}}}}
   :dynamic_templates [{:nested {:match "hits"
                                 :match_mapping_type "object"
                                 :mapping { :type "nested" }}}
                       {:fi {:match "fi"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "finnish"
                                       :search_analyzer "finnish_keyword"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:sv {:match "sv"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "swedish"
                                       :search_analyzer "swedish_keyword"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:en {:match "en"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "english"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:tila {:match "tila"
                               :match_mapping_type "string"
                               :mapping {:type "text"
                                         :analyzer "finnish"
                                         :norms { :enabled false}
                                         :fields { :keyword { :type "keyword" :ignore_above 256}}}}}]})

(def kouta-mappings
  {:dynamic_templates [{:muokkaaja {:match "muokkaaja.nimi"
                                    :match_mapping_type "string"
                                    :mapping {:type "text"
                                              :analyzer "finnish"
                                              :norms { :enabled false}
                                              :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}
                       {:fi {:match "fi"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "finnish"
                                       :search_analyzer "finnish_keyword"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}
                       {:sv {:match "sv"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "swedish"
                                       :search_analyzer "swedish_keyword"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}
                       {:tila {:match "tila"
                               :match_mapping_type "string"
                               :mapping {:type "text"
                                         :analyzer "finnish"
                                         :norms { :enabled false}
                                         :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}]})