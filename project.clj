(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject kouta-indeksoija-service "8.0.0-SNAPSHOT"
  :description "Kouta-indeksoija"
  :repositories [["releases" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"
                              :username :env/artifactory_username
                              :password :env/artifactory_password
                              :sign-releases false
                              :snapshots false}]
                 ["snapshots" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"
                               :username :env/artifactory_username
                               :password :env/artifactory_password
                               :sign-releases false
                               :snapshots true}]]
  :managed-dependencies [[org.flatland/ordered "1.5.7"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [metosin/compojure-api "1.1.10" :exclusions [cheshire
                                                              com.fasterxml.jackson.core/jackson-core
                                                              com.fasterxml.jackson.dataformat/jackson-dataformat-smile
                                                              com.fasterxml.jackson.dataformat/jackson-dataformat-cbor
                                                              ring/ring-codec
                                                              clj-time
                                                              joda-time
                                                              org.clojure/core.cache
                                                              org.clojure/core.memoize]]
                 [clojurewerkz/quartzite "2.0.0" :exclusions [clj-time]]
                 [cheshire "5.8.0"]
                 [clj-http "2.3.0"]
                 [mount "0.1.11"]
                 [environ "1.1.0"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.clojure/core.memoize "0.7.1"]
                 [org.postgresql/postgresql "9.4-1200-jdbc41"]
                 [base64-clj "0.1.1"]
                 [clj-time "0.14.3"]
                 [org.clojure/algo.generic "0.1.3"]
                 ;Configuration
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [cprop "0.1.10"]
                 ;Elasticsearch
                 [oph/clj-elasticsearch "0.3.2-SNAPSHOT"]
                 ;Cas
                 [clj-soup/clojure-soup "0.1.3"]
                 ;;Logging
                 [oph/clj-log "0.2.2-SNAPSHOT"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.13.2"]
                 [org.apache.logging.log4j/log4j-api "2.13.2"]
                 [org.apache.logging.log4j/log4j-core "2.13.2"]
                 [clj-log4j2 "0.2.0"]
                 [ring-cors "0.1.11"]
                 ;;SQS Handling
                 [amazonica "0.3.48" :exclusions [com.amazonaws/aws-java-sdk
                                                  com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core "1.11.479"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.479"]]
  :ring {:handler kouta-indeksoija-service.api/app
         :init kouta-indeksoija-service.api/init
         :destroy kouta-indeksoija-service.api/stop
         :browser-uri "kouta-indeksoija/swagger"}
  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  [ring/ring-mock "0.3.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [criterium "0.4.4"]]
                   :plugins [[lein-ring "0.12.5"]
                             [jonase/eastwood "0.3.5"]
                             [lein-kibit "0.1.3" :exclusions [org.clojure/clojure]]
                             [lein-environ "1.1.0"]
                             [lein-cloverage "1.1.1" :exclusions [org.clojure/clojure]]]
                   :resource-paths ["dev_resources"]
                   :env {:dev "true"}
                   :ring {:reload-paths ["src"]}
                   :jvm-opts ["-Daws.accessKeyId=randomKeyIdForLocalstack"
                              "-Daws.secretKey=randomKeyForLocalstack"]}
             :test {:env {:test "true"} :dependencies [[cloud.localstack/localstack-utils "0.1.22"]
                                                       [fi.oph.kouta/kouta-backend "6.5.0-SNAPSHOT"]
                                                       [fi.oph.kouta/kouta-backend "6.5.0-SNAPSHOT" :classifier "tests"]
                                                       [fi.oph.kouta/kouta-common "2.2.0-SNAPSHOT" :classifier "tests"]
                                                       [oph/clj-test-utils "0.2.8-SNAPSHOT"]]
                    :resource-paths ["test_resources"]
                    :jvm-opts ["-Daws.accessKeyId=randomKeyIdForLocalstack"
                               "-Daws.secretKey=randomKeyForLocalstack"]
                    :injections [(require '[clj-test-utils.elasticsearch-docker-utils :as utils])
                                 (utils/global-docker-elastic-fixture)]}
             :ci-test {:env {:test "true"}
                       :dependencies [[ring/ring-mock "0.3.2"]
                                      [cloud.localstack/localstack-utils "0.1.22"]
                                      [fi.oph.kouta/kouta-backend "6.5.0-SNAPSHOT"]
                                      [fi.oph.kouta/kouta-backend "6.5.0-SNAPSHOT" :classifier "tests"]
                                      [fi.oph.kouta/kouta-common "2.2.0-SNAPSHOT" :classifier "tests"]
                                      [oph/clj-test-utils "0.2.8-SNAPSHOT"]]
                       :jvm-opts ["-Dlog4j.configurationFile=dev_resources/log4j2.properties"
                                  "-Dconf=ci_resources/config.edn"
                                  "-Daws.accessKeyId=randomKeyIdForLocalstack"
                                  "-Daws.secretKey=randomKeyForLocalstack"]
                       :injections [(require '[clj-test-utils.elasticsearch-docker-utils :as utils])
                                    (utils/global-docker-elastic-fixture)]}
             :uberjar {:ring {:port 8080}}
             :jar-with-test-fixture {:source-paths ["src", "test"]
                                     :jar-exclusions [#"perf|resources|mocks"]}} ;TODO: Better exclusion
  :aliases {"run" ["with-profile" "+dev" "ring" "server"]
            "test" ["with-profile" "+test" "test"]
            "deploy" ["with-profile" "+jar-with-test-fixture" "deploy"]
            "install" ["with-profile" "+jar-with-test-fixture" "install"]
            "ci-test" ["with-profile" "+ci-test" "test"]
            "eastwood" ["eastwood" "{:test-paths []}"]
            "cloverage" ["with-profile" "+test" "cloverage"]
            "uberjar" ["do" "clean" ["ring" "uberjar"]]
            "testjar" ["with-profile" "+jar-with-test-fixture" "jar"]}
  :resource-paths ["resources"]
  :jvm-opts ["-Dlog4j.configurationFile=dev_resources/log4j2.properties"])
