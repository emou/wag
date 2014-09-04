(ns wag.actions
  (:require [wag.views :as views]
            [wag.wamp-client :as wamp-client]
            [wag.core]))

(enable-console-print!)

(defn login []
  {:template views/login})

(defn dashboard []
  {:template views/dashboard})

(defn new-game []
  (wamp-client/rpc-call (wag.core/get-wamp-session)
                        "new-game"
                        (fn [result]
                          (printlin "new-game returned: " result)))
  {:template views/new-game})

(defn join-game []
  {:template views/join-game})
