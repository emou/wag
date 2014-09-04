(ns wag.core
  (:require [clojure.browser.repl]
            [clojure.walk :refer [keywordize-keys]]
            [wag.routes :as routes]
            [wag.views :as views]))

(def app-state (atom nil))

(def wamp-session (atom nil))

(defn handle-event! [js-event]
  (let [event (keywordize-keys (js->clj js-event))]
    (when (= (:type event) "games")
      (swap! app-state assoc :events event))))

(defn set-wamp-session! [session]
  (reset! wag.core/wamp-session session))

(defn get-wamp-session []
  @wamp-session)

(defn init []
  (do
    (enable-console-print!)
    (routes/init)
    (println "Application initialized")
    (println "Dispatching /login")
    (routes/dispatch! "/login")
    (views/attempt-login "guest" "1") ; Auto-login. For easier testing.
    ))

(comment
  (ns wag.core)
  (swap! app-state assoc :text ":)")
  (js/alert "hm!")
  (println "x!")
  )
