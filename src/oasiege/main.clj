(ns oasiege.main
  "Example http caller for oasiege.core"
  (:require [oasiege.core :as core]
            [org.httpkit.client :as client]))

(defn call [base-url oauth2-token
            {method :method
             path :path
             body :body
             headers :headers}]
  (client/request {:method method
                   :url (str base-url path)
                   :headers (merge {"Authorization" (str "Bearer " oauth2-token)
                                    "Accept" "sapplication/json"}
                              headers)
                   :body body
                   :timeout 30000}))


(defn do-calls [swagger-yaml-filename
                api-base-url
                oauth2-token]
  (let [api (core/load-api (slurp swagger-yaml-filename))
        request-spec (core/request-spec api)]
    (->> (core/generate-calls api request-spec 500)
      (pmap #(call api-base-url oauth2-token %)))))


(defn -main [swagger-yaml-filename
            api-base-url
            oauth2-token]
  (doseq [r (do-calls swagger-yaml-filename api-base-url oauth2-token)]
    (prn (-> @r
           (select-keys #{:status :opts})
           (update :opts select-keys [:method :url])))))
