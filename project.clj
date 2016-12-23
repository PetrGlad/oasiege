(defproject oasiege "0.1.0-SNAPSHOT"
  :description "Generate test HTTP requests (mostly) conformant with given OpenAPI specification."
  :url "https://github.com/PetrGlad/oasiege"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/test.check "0.9.0"]
                 [swagger-spec "0.1.0"]
                 [com.velisco/strgen "0.1.2"]
                 [clj-json "0.5.3"]
                 [http-kit "2.2.0"]])
