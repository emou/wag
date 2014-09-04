(ns wag.wamp-client
  (:require [wag.log :as log]))

(defn on-auth [{:keys [callback, username]} session permissions]
  (do
    (log/debug "Authenticated!")
    (callback {:session session
               :username username})))

(defn- error-with-type [error-type]
  {:error {:type error-type}})

(defn on-auth-error [{:keys [callback]}]
  (callback (error-with-type :auth)))

(defn on-authreq-error [{:keys [callback]}]
  (callback (error-with-type :authreq)))

(defn on-challenge [{:keys [password] :as connection-request} session challenge]
  (let [signature (.authsign session password challenge)]
    (log/debug "Received auth challenge " challenge)
    (.then (.auth session signature)
           (partial on-auth connection-request session)
           (partial on-auth-error connection-request))))

(defn on-connect [{:keys [uri username password] :as connection-request}
                  session]
  (do
    (log/debug "Connected to " uri
               " with session ID " (.sessionid session))
    (.then (.authreq session username)
           (partial on-challenge connection-request session)
           (partial on-authreq-error connection-request))))

(defn on-connect-error [{:keys [callback]} code reason]
  (callback (error-with-type :connect)))

(defn connect [uri username password callback]
  (let [connection-request {:uri uri
                            :username username
                            :password password
                            :callback callback}]
    (log/info)
    (.connect js/ab
              uri
              (partial on-connect connection-request)
              (partial on-connect-error connection-request))))

(defn rpc-call [session args callback]
  (.apply
    (.-call session)
    session
    (clj->js args)))
