(ns wag.wamp-client)

(defn on-auth [{:keys [base_topic_uri, callback]} session permissions]
  (do
    (println "Authenticated!")
    (.prefix session "event" (str base_topic_uri "event#"))
    (.prefix session "rpc" (str base_topic_uri "rpc#")))
    (callback {:session session}))

(defn- error-with-type [error-type]
  {:error {:type error-type}})

(defn on-auth-error [{:keys [callback]}]
  (callback (error-with-type :auth)))

(defn on-authreq-error [{:keys [callback]}]
  (callback (error-with-type :authreq)))

(defn on-challenge [{:keys [password] :as connection-request} session challenge]
  (let [signature (.authsign session password challenge)]
    (println "Received auth challenge " challenge)
    (.then (.auth session signature)
           (partial on-auth connection-request session)
           (partial on-auth-error connection-request))))

(defn on-connect [{:keys [uri username password] :as connection-request}
                  session]
  (do
    (println "Connected to " uri
             " with session ID " (.sessionid session))
    (.then (.authreq session username)
           (partial on-challenge connection-request session)
           (partial on-authreq-error connection-request))))

(defn on-connect-error [{:keys [callback]} code reason]
  (callback (error-with-type :connect)))

(defn connect [uri base_topic_uri username password callback]
  (let [connection-request {:uri uri
                            :base_topic_url base_topic_uri
                            :username username
                            :password password
                            :callback callback}]

    (enable-console-print!)
    (println "wamp-client attempting connection to " uri)
    (.connect js/ab
              uri
              (partial on-connect connection-request)
              (partial on-connect-error connection-request))))
