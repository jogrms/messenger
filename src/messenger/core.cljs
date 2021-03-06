(ns messenger.core
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :refer [chan put! <!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def *origin* nil)
(def *target* nil)

(defn- post [msg]
  (.postMessage *target* (pr-str msg) *origin*))

(defn- response [resp]
  (if (map? resp)
    resp
    {}))

(defn- add-listener [w f]
  (.addEventListener w "message" f false))

(defn- remove-listener [w f]
  (.removeEventListener w "message" f false))

(defn listen [f]
  (-> js/window
      (add-listener
        (fn [e]
          (when (= *origin* (.-origin e))
            (let [data (read-string e.data)]
              (when (= (:msg-kind data) :request)
                (-> data
                  (assoc :event e)
                  (f)
                  (response)
                  (merge (select-keys data [:req-id :action]))
                  (assoc :msg-kind :response)
                  (post)))))))))


(defn listen-async
  ([] (listen-async (chan)))
  ([c] (listen #(put! c %))
    c))


(defn- get-resp-handler [c action req-id]
  (fn handler [e]
    (when (= *origin* (.-origin e))
      (let [data (read-string e.data)]
        (when (and
                (= action (:action data))
                (= req-id (:req-id data))
                (= :response (:msg-kind data)))
          (-> js/window
              (remove-listener handler))
          (put! c (assoc data :event e)))))))

(defn request
  ([action] (request action {}))
  ([action data]
    (let [c (chan)
          req-id (js/Math.random)
          handler (get-resp-handler c action req-id)]
      (-> js/window
          (add-listener handler))
      (post (assoc data
              :action action
              :req-id req-id
              :msg-kind :request))
      c)))
