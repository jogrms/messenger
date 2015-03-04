# messenger
[![Clojars Project](http://clojars.org/messenger/latest-version.svg)](http://clojars.org/messenger)

A ClojureScript wrapper for window.postMessage API. Uses `core.async` for asynchronous message sending and handling.

## Usage

```clojure
(ns example.core
  (:require [messenger.core :as msg]
            [cljs.core.async :refer [<!]])
  (:require-macros [messenger.macros :refer [deflistener]]
                   [cljs.core.async.macros :refer [go]]))

; Define an iframe to communicate with.
(def iframe (js/document.getElementById "iframe"))

; Define a listener function that handles requests.
(deflistener frame-listener [req]
  
  ; Declares :hello action - get :name from the request map and display the hello alert.
  ; Return empty response map.
  (:hello
    (js/alert (str "hello, " (:name req)))
    {})
    
  ; Declares :get-time action - return current time to the calling window.
  (:get-time
    {:time (.getTime (Date.))}))

(go
  ; Initialize messaging target and origin.
  (set! msg/*target* (or iframe.contentWindow iframe))
  (set! msg/*origin* "http://example.com"))
  
  ; Set up the listener.
  (msg/listen frame-listener)
  
  ; Send :get-some-info request to the target (the iframe) and then display the response.
  (js/alert (<! (msg/request :get-some-info {:another-info "test"}))))
```

## License

Copyright © 2015 Ivan Babushkin

Distributed under the Eclipse Public License, the same as Clojure.
