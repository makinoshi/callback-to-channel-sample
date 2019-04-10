(ns core
  (:require [clj-http.client :as http]
            [clojure.core.async :as a]))

(defn- async-get-request [channel]
  (http/get "http://localhost:3000"
            {:async? true}
            (fn [res] (a/put! channel {:res res :exp nil}))
            (fn [exp] (a/put! channel {:res nil :exp exp}))))

(defn- do-something [{:keys [res exp]}]
  (if-not exp
    (println (:status res))
    (println "Exception thrown")))

(defn one-request
  "Print following order
  1. start
  2. request sent
  3. end
  4. 200"
  []
  (let [c (a/chan)]
    (println "start")
    (async-get-request c)
    (println "request sent")
    (a/go
      (when-let [ret (a/<! c)]
        (do-something ret)))
    (println "end")))

#_(one-request)

(defn one-request-and-wait
  "Print following order
  1. start
  2. request sent
  3. 200
  4. end"
  []
  (let [c (a/chan)]
    (println "start")
    (async-get-request c)
    (println "request sent")
    (let [c' (a/go
               (when-let [ret (a/<! c)]
                 (do-something ret)))]
      (a/<!! c'))
    (println "end")))

#_(one-request-and-wait)

(defn one-request-using-go-loop
  "Print following order
  1. start
  2. request sent
  3. end
  4. 200"
  []
  (let [c (a/chan)
        nreq 1]
    (println "start")
    ;; Run a process that receives http response
    (a/go-loop [i 0]
      (if (< i nreq)
        (when-let [ret (a/<! c)]
          (do-something ret)
          (recur (inc i)))
        (a/close! c)))
    (async-get-request c)
    (println "request sent")
    (println "end")))

#_(one-request-using-go-loop)

(defn some-requests-using-go-loop
  "Print following order
  1. start
  2. 200...
  3. request sent
  4. 200...
  5. end
  6. 200..."
  []
  (let [c (a/chan 10)
        nreq 10]
    (println "start")
    ;; Run a process that receives http responses
    (a/go-loop [i 0]
      (if (< i nreq)
        (when-let [ret (a/<! c)]
          (do-something ret)
          (recur (inc i)))
        (a/close! c)))
    ;; Some request send asynchronously
    (dotimes [_ nreq]
      (async-get-request c))
    (println "request sent")
    (println "end")))

#_(some-requests-using-go-loop)

(defn some-requests-and-wait-using-go-loop
  "Print following order
  1. start
  2. 200...
  3. request sent
  4. end"
  []
  (let [c (a/chan 10)
        nreq 10
        ;; Run a process that receives http responses
        receive-chan (a/go-loop [i 0]
                       (if (< i nreq)
                         (when-let [ret (a/<! c)]
                           (do-something ret)
                           (recur (inc i)))
                         (a/close! c)))]
    (println "start")
    ;; Some request send asynchronously
    (dotimes [_ nreq]
      (async-get-request c))
    (println "request sent")
    (a/<!! receive-chan)
    (println "end")))

#_(some-requests-and-wait-using-go-loop)
