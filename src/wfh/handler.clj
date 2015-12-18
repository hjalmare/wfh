(ns wfh.handler
  (:use 
        korma.db
        korma.core)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response status]]))

(defdb data (mysql {
    :subname "//localhost:3306/wfhdb"
    :user "wfh"
    :password "wfh"
    }))

(defentity entry
    (pk :id)
    (table :entries)
    (entity-fields :email :date :duration :type))

(defn fetch-all [request]
    (response (select entry)))

(defn insert-entry 
    [request]
    (let [key (get (insert entry (values (request :body))) :generated_key)]
        (response 
            (assoc (request :body) :key key))))

(defn validate-entry 
    [body]
    (remove nil? [
        (if (true? false) "true") 
        (if (false? true) "false")
    ]))

(defn post-entry 
    [request]
    (let [      body (get request :body) 
                validation-errors (validate-entry (get request :body))]
    (if (empty? validation-errors)
        (insert-entry request)
        (-> (response {:error validation-errors})
            (status 400)))))


(defroutes app-routes
  (GET "/" [] fetch-all)
  (POST "/" request post-entry)
  (route/not-found "Not Found"))

(def app
    (-> app-routes
        (wrap-json-response)
        (wrap-json-body {:keywords true})))
