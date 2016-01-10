(ns wfh.handler
  (:use 
        korma.db)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response status]]
	    [cheshire.generate :refer [add-encoder encode-str]]
	    [korma.core :as korma]
	    [dire.core :refer [with-handler!]]))

(def date-formatter (. java.time.format.DateTimeFormatter ISO_LOCAL_DATE))

(add-encoder java.time.LocalDate 
	(fn [date jg]
		(encode-str (.format date-formatter date) jg)))

(defn parse-date [datestr]
	(->> datestr
	     (.parse date-formatter)
	     (java.time.LocalDate/from)))


(defdb data (mysql {
    :subname "//localhost:3306/wfhdb"
    :user "wfh"
    :password "wfh"
    }))

(korma/defentity entry
    (korma/pk :id)
    (korma/table :entries)
    (korma/entity-fields :email :date :duration :type))

(defn fetch-all [request]
    (response (korma/select entry)))

(defn insert-entry 
    [body]
    (let [key (get (korma/insert entry (korma/values body)) :generated_key)]
        (response 
            (assoc body :key key))))

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

(defroutes app-routes
  (GET "/" [] fetch-all)
  (POST "/" request post-entry)
  (route/not-found "Not Found"))

(def app
    (-> app-routes
        (wrap-json-response)
        (wrap-json-body {:keywords true})))
