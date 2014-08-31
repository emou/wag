(ns wag.wamp-client)

(defn on-auth [{:keys [base_topic_uri]} session permissions]
  (do
    (println "Authenticated!")
    (.prefix session "event" (str base_topic_uri "event#"))
    (.prefix session "rpc" (str base_topic_uri "rpc#"))))

(defn on-auth-error []
  (println "Authentication failed"))

(defn on-challenge [{:keys [password] :as connection-request} session challenge]
  (let [signature (.authsign session password challenge)]
    (println "Received auth challenge " challenge)
    (.then (.auth session signature)
           (partial on-auth connection-request session)
           on-auth-error)))

(defn on-connect [{:keys [uri username password] :as connection-request}
                  session]
  (do
    (println "Connected to " uri
             " with session ID " (.sessionid session))
    (.then (.authreq session username)
           (partial on-challenge connection-request session)
           (fn [] (println "Auth request failed")))))

(defn on-connect-error [code reason]
  (.log js/console "code " code " reason" reason))

(defn connect [uri base_topic_uri username password]
  (let [connection-request {:uri uri
                            :base_topic_url base_topic_uri
                            :username username
                            :password password}]

    (enable-console-print!)
    (println "wamp-client attempting connection to " uri)
    (.connect js/ab
              uri
              (partial on-connect connection-request)
              on-connect-error)))
