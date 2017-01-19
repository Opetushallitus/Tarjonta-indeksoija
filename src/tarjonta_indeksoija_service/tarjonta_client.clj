(ns tarjonta-indeksoija-service.tarjonta-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [clj-http.client :as client]))

(defn get-hakukohde
  [oid]
  (-> (str (:tarjonta-service-url env) "hakukohde/" oid)
      (client/get {:as :json})
      :body
      :result))
