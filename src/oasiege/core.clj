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

(defn prefixed-key [prefix key]
  (keyword (namespace prefix)
    (str (name prefix) "." (name key))))

(defn values-to-keys [key-prefix values]
  (->> values
    (map-indexed
      (fn [i v]
        [(prefixed-key key-prefix (str i)) v]))
    (into {})))

(defn define-spec [spec-key spec]
  ;; We could use s/def-impl do add definitions but then spec's explanation functions
  ;; that show predicates' source would not work correctly.
  (eval `(s/def ~spec-key ~spec)))

(defn declare-specs! [definition prefix]
  (let [skey #(prefixed-key prefix %)
        sdef (fn [sub-key spec-form]
               (define-spec (skey sub-key) spec-form))]
    ;; The struct we want to generate:
    (sdef :api-call `(s/keys :req [(skey :path) (skey :method) (skey :parameters)]))

    (let [path-keys (->> definition :paths keys (values-to-keys prefix))]
      (sdef :path (into #{} (vals path-keys))))
    (sdef :method #{:get :post :delete})
    (sdef :parameters (s/keys :req [(skey :key)]))
    (sdef :key (s/keys :req [(skey :value) (skey :in)]))
    (sdef :value string?)
    (sdef :in #{:path})

    ;; !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    ;; Better spec form: match sequences instead of assocs,
    ;; use s/tuple to tag choices.
    ;; !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    (gen/sample
      (s/gen
        (s/alt
          :path1 (s/tuple #{"path1"}
                   (s/alt
                     :get (s/tuple #{:get}
                            (s/cat
                              :a (s/tuple #{"pa"} string?)
                              :b (s/? (s/tuple #{"pb"} integer?))))
                     :post (s/tuple #{:post}
                             (s/cat
                               :a (s/tuple #{"name"} string?)
                               :b (s/? (s/tuple #{"id"} integer?))))))
          :path2 (s/tuple #{"path2"}
                   (s/alt
                     :get (s/tuple #{:get}
                            (s/cat
                              :a (s/tuple #{"p2a"} string?)
                              :b (s/? (s/tuple #{"p2b"} integer?))))
                     :post (s/tuple #{:post}
                             (s/cat
                               :a (s/tuple #{"name"} string?)
                               :b (s/? (s/tuple #{"id"} integer?)))))))))


    (skey :api-call)))

(defonce serial-id (atom 0))

(def new-spec-prefix! []
  (keyword (str *ns*) (str "z" (swap! serial-id inc))))

(defn proof-of-concept-run []
  (let [definition (s/conform :swagger/definition
                     (yaml/parse-string (slurp "test/oasiege/data/hummus.yaml")))
        prefix (new-spec-prefix!)]
    (assert (clojure.string/starts-with? (:swagger definition) "2.")) ;; just in case
    (declare-specs! definition prefix)
    ;; Generate request records from hand-crafted specs corresponding to the definition
    (let [generated-records (gen/sample (s/gen ::hummus.api) 10)]
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
