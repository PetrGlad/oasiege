# oasiege

A Clojure library to generate test HTTP requests conformant
with given OpenAPI (Swagger) [API description](https://www.openapis.org/).
Note that not all possible API specifications are supported.
Patches that implement missing parts are welcome.


## Usage

To run a test:
```bash
lein uberjar
java -cp target/oasiege-0.1.0-SNAPSHOT-standalone.jar clojure.main -m oasiege.main \
     "api/myservice.yaml" \
     "https://my-service.there/" \
     "deadbeef-dead-beef-feeb-abbafeedda4a"
```

The `oasiege.core/generate-calls` generates maps that can be used to make actual
HTTP calls using existing HTTP client library. Request map looks like
```clojure
{:method :get
 :path "/bongo/abc/Lf3"}
```
See `oasiege.core-test` namespace for example usage of requests generator.


## License

Copyright © 2016 Petr Gladkikh

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
