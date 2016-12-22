# oasiege

A Clojure library to generate test HTTP requests conformant
with given OpenAPI (Swagger) [API description](https://www.openapis.org/).

## Usage

See `oasiege.core/example-run` for example usage
```clojure
(require 'oasiege.core)
(oasiege.core/example-run)
```

The `oasiege.core/generate-calls` generates maps that can be used to make actual
HTTP calls using existing HTTP client library. Request map looks like
```clojure
{:method :get
 :path "/bongo/abc/Lf3"}
```

## License

Copyright © 2016 Petr Gladkikh

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
