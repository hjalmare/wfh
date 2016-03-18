(ns wfh.handler
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response status header]]
	    [cheshire.generate :refer [add-encoder encode-str]]
	    [clojure.java.jdbc :as jdbc] 
        [dire.core :refer [with-handler!]]))

;;DB Pooling etc
;;==========================================================
(def db-spec {
    :classname "com.mysql.jdbc.Driver"
    :subprotocol "mysql"
    :subname "//localhost:3306/wfhdb"
    :user "wfh"
    :password "wfh"
    })

(defn pool
  [spec]
    (let [cpds (doto (ComboPooledDataSource.)
                    (.setDriverClass (:classname spec)) 
                    (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
                    (.setUser (:user spec))
                    (.setPassword (:password spec))
                    ;; expire excess connections after 30 minutes of inactivity:
                    (.setMaxIdleTimeExcessConnections (* 30 60))
                    ;; expire connections after 3 hours of inactivity:
                    (.setMaxIdleTime (* 3 60 60)))] 
                        {:datasource cpds}))
(def pooled-db (delay (pool db-spec)))
(defn db-conn [] @pooled-db)

(defn update-or-insert!
  "Updates columns or inserts a new row in the specified table"
    [db table row where-clause]
      (jdbc/with-db-transaction [t-con db]
          (let [result (jdbc/update! t-con table row where-clause)]
                (if (zero? (first result))
                        (jdbc/insert! t-con table row)
                                result))))

;;Utils
;;=========================================================
(def date-formatter (. java.time.format.DateTimeFormatter ISO_LOCAL_DATE))

(add-encoder java.time.LocalDate 
	(fn [date jg]
		(encode-str (.format date-formatter date) jg)))

(defn parse-date [datestr]
	(->> datestr
	     (.parse date-formatter)
	     (java.time.LocalDate/from)))

;; Teh code
;;========================================================
(defn fetch-all [request]
    (-> (response (map  
                    (fn [ent] (update ent :date #(.format date-formatter (.toLocalDate %1))))
                    (jdbc/query (db-conn)  ["SELECT * from entries"])))))

(defn insert-entry 
    [body]
    (let [key (update-or-insert! (db-conn) :entries body ["handle = ? and date = ?", (get body "handle") , (get body"date")] )]
        (println "##############")
        (prn key)
        (response body)))

(defn post-entry 
    [request]
	(do (prn request)
	    (let [body (update (get request :body) "date" parse-date)]
		(insert-entry body))))


(with-handler! #'post-entry java.lang.Exception 
	(fn [ex & args]
	(.printStackTrace ex) 
	(-> (response {:message (.getMessage ex)})
	    (status 400)))) 

;; Routes
;;=======================================================
(defn allow-origin 
    [func]
    (fn [re] (-> (func re) 
        (header  "Access-Control-Allow-Origin" "*")
        (header  "Access-Control-Allow-Headers" "Content-Type")
        (header  "Access-Control-Allow-Methods" "GET,PUT,OPTIONS"))))


(defroutes app-routes
  (GET "/" [] (-> fetch-all allow-origin))
  (PUT "/" [] (-> post-entry allow-origin ))
  (OPTIONS "/" [] (-> (fn [_] {}) allow-origin))
  (route/not-found "Not Found"))

(def app
    (-> app-routes
        (wrap-json-response)
        (wrap-json-body {:keywords true})))
