(ns wag.routes
  (:use [compojure.core :only [defroutes GET]]
        (ring.middleware [keyword-params :only [wrap-keyword-params]]
                         [params :only [wrap-params]]
                         [session :only [wrap-session]])
        [wag.websocket :only [wamp-handler]])
  (:require [compojure.route :as route]
            [clojure.tools.logging :as log]
            [cemerick.austin.repls :refer (browser-connected-repl-js)]))

(defroutes app-routes*
  (GET "/ws" [:as req] (wamp-handler req))
  ;; If there's no REPL running, return empty string;
  ;; otherwise return code that connects to the running REPL.
  (GET "/browser-connected-repl.js" [] (or (browser-connected-repl-js) ""))
  (route/resources "/")
  (route/not-found "<p>Page not found.</p>"))

(defn wrap-failsafe [handler]
  (fn [req]
    (try (handler req)
      (catch Exception e
        (log/error e "error handling request" req)
        {:status 500 :body "Sorry, an error occured."}))))

(defn wrap-root-url
  "Rewrite requests of / to /index.html"
  [handler]
  (fn [req]
    (handler
      (update-in req [:uri]
        #(if (= "/" %) "/index.html" %)))))

(defn wrap-logging
  "Log each request"
  [handler]
  (fn [req]
    (log/debug "Processing request " req)
    (handler req)))

(defn app [] (-> #'app-routes*
              wrap-logging
              wrap-session
              wrap-keyword-params
              wrap-params
              wrap-failsafe
              wrap-root-url))
