(ns wag.main
  (:gen-class)
  (:use [clojure.tools.logging :only [info]]
        [clojure.tools.cli :only [cli]]
        [org.httpkit.server :only [run-server]]
        [ring.middleware.reload :only [wrap-reload]]
        [wag.config :only [reset-conf!]]
        [wag.routes :only [app]]))

(defn- to-int [s] (Integer/parseInt s))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (info "stopping server...")
    (@server) ; contains the stop-server callback fn
    (reset! server nil)
    ;; other cleanup stuff here ...
    ))

(defn start-server
  [& [options]]
    ;; stop it if started, for running multi-times in repl
    (when-not (nil? @server) (stop-server))
    ;; other init stuff here, like init-db, init-redis, ...
    (let [cfg        (reset-conf!)
          server-cfg (merge (:http-kit cfg) options)]
      (reset! server
        (run-server (if (:hot-reload cfg) (wrap-reload (app)) (app)) server-cfg))
      (info "server started. listen on" (:ip server-cfg) "@" (:port server-cfg))))

(defn -main [& args]
  (let [[options _ banner]
          (cli args
            ["-i" "--ip"     "The ip address to bind to"]
            ["-p" "--port"   "Port to listen"            :parse-fn to-int]
            ["-t" "--thread" "Http worker thread count"  :parse-fn to-int]
            ["--[no-]help"   "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    ; Shutdown hook
    (. (Runtime/getRuntime) (addShutdownHook (Thread. stop-server)))
    (start-server options)))

(comment
  ;; 1) $ lein repl
  ;; 2) Eval this in vim-fireplace
  ;; 3) Open the app in a browser
  ;; 4) Open a cljs file
  ;; 5) :Piggieback (cemerick.austin/repl-env)
  ;; 6) Eval away in the context of the app!
  (-main)
  (def repl-env (reset!
                  cemerick.austin.repls/browser-repl-env
                  (cemerick.austin/repl-env)))
  (cemerick.austin.repls/cljs-repl repl-env))
