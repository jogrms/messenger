(ns messenger.core
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :refer [chan put! <!]]
            [jayq.core :refer [$ on off]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def *origin* nil)
(def *target* nil)

(defn- post [msg]
  (.postMessage *target* (pr-str msg) *origin*))

(defn- response [resp]
  (if (map? resp)
    resp
    {}))

(defn listen [f]
  (-> ($ js/window)
      (on "message onmessage"
        (fn [event]
          (let [e event.originalEvent]
            (when (= *origin* (.-origin e))
              (let [data (read-string e.data)]
                (when (= (:msg-kind data) :request)
                  (-> data
                    (assoc :event e)
                    (f)
                    (response)
                    (merge (select-keys data [:req-id :action]))
                    (assoc :msg-kind :response)
                    (post))))))))))


(defn listen-async
  ([] (listen-async (chan)))
  ([c] (listen #(put! c %))
    c))


(defn- get-resp-handler [c action req-id]
  (fn handler [event]
    (let [e event.originalEvent]
      (when (= *origin* (.-origin e))
        (let [data (read-string e.data)]
          (when (and
                  (= action (:action data))
                  (= req-id (:req-id data))
                  (= :response (:msg-kind data)))
            (-> ($ js/window)
                (off "message onmessage" handler))

            (put! c (assoc data :event e))))))))

(defn request
  ([action] (request action {}))
  ([action data]
    (let [c (chan)
          req-id (js/Math.random)
          handler (get-resp-handler c action req-id)]
      (-> ($ js/window)
          (on "message onmessage" handler))
      (post (assoc data
              :action action
              :req-id req-id
              :msg-kind :request))
      c)))
