(ns user
  (:require [mount.lite :as mount]
            [ring.adapter.jetty :as jetty]))

(defn handler [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World"})

(mount/defstate server
  :start (jetty/run-jetty handler {:host "localhost"
                                   :port 3000
                                   :join? false})
  :stop (when @server (.stop @server)))

(defn go []
  (mount/stop)
  (mount/start))
