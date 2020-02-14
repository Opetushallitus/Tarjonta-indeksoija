(ns kouta-indeksoija-service.queue.admin
  (:require [kouta-indeksoija-service.queue.sqs :as sqs]
            [kouta-indeksoija-service.queue.conf :as conf]
            [clojure.tools.logging :as log]))

(defn status
  []
  (->> (for [priority (conf/priorities)]
         {(keyword priority) (sqs/get-queue-attributes priority)})
       (into {})))

(defn- parse-int
  [x]
  (try
    (Integer/parseInt x)
    (catch Exception e
      nil)))

(defn- healthy?
  [apprx-messages health-threshold]
  (if-let [nr-of-messages (parse-int apprx-messages)]
    (<= nr-of-messages health-threshold)
    false))

(defn healthcheck
  []
  (let [status (atom 200)
        body   (try
                 (->> (for [priority (conf/priorities)
                            :let [health-threshold  (conf/health-threshold priority)
                                  queue-attributes (sqs/get-queue-attributes priority "ApproximateNumberOfMessages" "QueueArn")
                                  apprx-messages   (some-> queue-attributes :ApproximateNumberOfMessages)
                                  health           (healthy? apprx-messages health-threshold)]]
                        (do
                          (when (not health)
                            (reset! status 500))
                          {(keyword priority) {:QueueArn (some-> queue-attributes :QueueArn)
                                               :ApproximateNumberOfMessages apprx-messages
                                               :healthy health
                                               :health-threshold health-threshold}}))
                      (into {}))
                    (catch Exception e
                      (reset! status 500)
                      (log/error e)
                      {:error (.getMessage e)}))]
    [@status body]))