(defproject wag "0.1.0"
  :description "Word Association Game: A port from real life to clojure and the web"
  :url "http://github.com/emou/wag"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main wag.main
  :dependencies [
    [org.clojure/clojure "1.6.0"]
    [org.clojure/tools.cli "0.2.2"]
    [org.clojure/tools.logging "0.2.6"]
    [org.clojure/core.async "0.1.338.0-5c5012-alpha"]

    [log4j "1.2.17" :exclusions [javax.mail/mail
                                javax.jms/jms
                                com.sun.jdmk/jmxtools
                                com.sun.jmx/jmxri]]
    [compojure "1.1.5"]
    [ring-server "0.2.8"]
    [http-kit "2.1.5"]
    [clj-wamp "1.0.0-rc1"]

    ; Clojurescript deps
    [org.clojure/clojurescript "0.0-2280"
    :exclusions [org.apache.ant/ant]]
    [sablono "0.2.22"]
    [secretary "1.2.0"]
    [om "0.7.1"]
  ]
  :plugins [[lein-cljsbuild "1.0.3"]
            [cider/cider-nrepl "0.7.0"]
            [org.clojure/tools.nrepl "0.2.4"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds
     [{:id "dev"
       :source-paths ["src-cljs"]
       :compiler
        {:optimizations :none
         :output-to "resources/public/js/app.js"
         :output-dir "resources/public/js/"
         :pretty-print true
         :source-map "resources/public/js/app.js.map"}}
      {:id "release"
       :source-paths ["src-cljs"]
       :compiler
        {:optimizations :advanced
         :output-to "resources/public/js/app.js"
         :preamble ["react/react.min.js"]
         :externs ["react/react.js"]
         :pretty-print false
         :output-wrapper false
         :closure-warnings {:non-standard-jsdoc :off}}}]}

  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.5"]]
                   :resource-paths ["resources-dev"]}})
