(ns oasiege.core
  (:require [clojure.spec :as s]
            [swagger.reader.json :as json]
            [swagger.reader.yaml :as yaml]
            [clojure.spec.gen :as gen]
            swagger.spec
            clojure.string))

(def param-in #{:query
                :header
                :path
                :formData
                :cookie})

(def ere (s/conform :swagger/definition
           (yaml/parse-string (slurp "test/oasiege/data/hummus.yaml"))))
(s/def ::hummus (s/keys :req [::path ::method ::parameters]))
(s/def ::path #{"/label/{key}" "/number/{key}" "/other-number/{key}" "/event-types/{key}/events"})
(s/def ::method #{:get :post :delete})
(s/def ::parameters (s/keys :req [::key]))
(s/def ::key (s/keys :req [::value ::in]))
(s/def ::value string?)
(s/def ::in #{:path})
;;;; (gen/sample (s/gen ::hummus))

(def generated-request-record ;;; example
  {:path "/label/{key}"
   :method :get
   :parameters {:key {;;; Generated
                      :value "123"
                      ;;; Aux data
                      ;;; Get :name from the parameters map entry
                      :in :path}}})

(defn embed-parameter [request param-name {value ::value in ::in}]
  ;;; TODO Use multimethod (dispatch on :in)?
  (case in
    :path (update request ::path
            (fn [p]
              #_(prn {:p p :param-name param-name :value value :result (clojure.string/replace p
                                                                       (str "{" (name param-name) "}")
                                                                       value)})
              (clojure.string/replace p
                (str "{" (name param-name) "}")
                value)))))

(defn prepare-request [generated-request]
  (reduce-kv embed-parameter
    (dissoc generated-request ::parameters)
    (::parameters generated-request)))

(defn proof-of-concept-run []
  (let [definition (s/conform :swagger/definition
                     (yaml/parse-string (slurp "test/oasiege/data/hummus.yaml")))]
    ;; Can parse OpenAPI definition
    (prn definition)
    ;; Generate request records from hand-crafted specs corresponding to the definition
    (let [generated-records (gen/sample (s/gen ::hummus) 10)]
      (prn generated-records)
      ;; Embed parameters to make generated records suitable for HTTP calls
      (prn (map prepare-request generated-records)))))

(comment
  (-> ere
    :paths
    (get "/label/{key}") ;; pick single
    :get ;; pick single
    :parameters ;; pick set of mandatory parameters + subset of optional ones
    [;; // chosen set
     "a parameter"
     ;; (parameter.in, parameter.name should be accounted for when generating request.)
     ;; here generate a value from allowed set, using parameter.type
     ])

  ;;;;;;;;;;;;;;;;; Dynamic spec decl example
  (defmacro sm1 [aaa]
    `(s/keys :req ~aaa))

  (let [sp (sm1 [::aaa])]
    (s/conform sp {::aaa 2}))

  )
