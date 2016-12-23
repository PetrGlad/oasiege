(ns oasiege.core-test
  (:require [clojure.test :refer :all]
            [oasiege.core :refer :all]
            [clojure.spec :as s]))

(deftest a-test
  (let [api (load-api (slurp "test/oasiege/data/hummus.yaml"))
        rspec (request-spec api)]
    (testing "Request generation."
      (doseq [call (generate-calls api rspec 200)]
        (prn call)))

    (testing "Match against the spec."
      (prn (s/conform rspec
             (map-to-speced {:method :get
                             :path "/quality-label/{key}"
                             :params [{:name "key"
                                       :in :path
                                       :value "wGHi3LCS9Q8a3"}]}))))))
