(ns oasiege.core
  (:require [clojure.spec :as s]
            [swagger.reader.json :as json]
            [swagger.reader.yaml :as yaml]
            [clojure.spec.gen :as gen]
            swagger.spec
            clojure.string))


;; --------------------------------------------------------
;; Spec generation


(defn prefixed-key [prefix key]
  (keyword (namespace prefix)
    (str (name prefix) "." (name key))))


(defn request-spec [definition]
  ;; It is possible to use s/def-impl do add definitions and avoid macroses
  ;; but then spec's explanation functions that show predicates' source
  ;; would not work correctly.
  ;; Explanation is not necessary for generation but helps with debugging.
  (let [last-id (atom 0)
        new-tag! (fn [prefix]
                   (prefixed-key prefix (str (swap! last-id inc))))
        param-spec (fn [{pname :name ptype-name :type in :in required? :required pattern :pattern :as caramba}]
                     (prn caramba)
                     (let [value-spec (case ptype-name
                                        "integer" `integer?
                                        ;; TODO Implementation use pattern to restrict string values
                                        "string" `string?
                                        ;; FIXME Handle other types (type might not be specified)
                                        nil `string?)
                           form `(s/tuple #{~pname} #{~in} ~value-spec)]
                       [(keyword pname)
                        (if required?
                          form
                          `(s/? ~form))]))
        method-spec (fn [[method props]]
                      [method
                       `(s/tuple #{~method}
                          (s/cat
                            ~@(mapcat param-spec (:parameters props))))])
        path-spec (fn [[path methods]]
                    [(new-tag! :path)
                     `(s/tuple #{~(name path)}
                        (s/alt ~@(mapcat method-spec methods)))])
        api-paths (->> definition :paths)]
    `(s/alt ~@(mapcat path-spec api-paths))))


;; --------------------------------------------------------
;; Requests


(defn- generated-to-map [[[path [[method params]]]]]
  {:method method
   :path path
   :params (map (fn [[n in value]]
                  {:name n
                   :in (keyword in)
                   :value value}) params)})


(defn embed-parameter [request {param-name :name
                                value :value
                                in :in}]
  ;; Unsupported values :query :header :formData :cookie
  (case in
    :path (update request :path
            (fn [p]
              (clojure.string/replace p
                (str "{" param-name "}")
                value)))
    :body (assoc request :body value)))


(defn prepare-request [base-path request]
  ;; TODO use base path
  (-> (reduce embed-parameter
        (dissoc request :params)
        (:params request))
    (update :path #(str base-path %))))


;; --------------------------------------------------------
;; Main


(defn generate-calls [openapi-definition-str sample-count]
  (let [definition (s/conform :swagger/definition
                     (yaml/parse-string openapi-definition-str))]
    (assert (clojure.string/starts-with? (:swagger definition) "2.")) ;; just in case
    (let [generated-records (-> (request-spec definition)
                              eval
                              s/gen
                              (gen/sample sample-count))]
      (map (comp
             #(prepare-request (:basePath definition) %)
             generated-to-map)
        generated-records))))


(defn proof-of-concept-run []
  (doseq [call (generate-calls (slurp "test/oasiege/data/hummus.yaml") 200)]
    (prn call)))
