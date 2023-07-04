(ns liberator.resource.events.core-test
  (:require
   [clojure.test :refer [deftest is]]

   [halboy.resource :as hal]
   [halboy.json :as hal-json]

   [ring.mock.request :as ring]
   [ring.middleware.keyword-params :as ring-keyword-params]
   [ring.middleware.params :as ring-params]

   [liberator.resource.events.core :as events-resource]))

(def router
  [""
   [["/" :discovery]
    ["/events" :events]]])

(def dependencies
  {:router router})

(defn resource-handler
  ([dependencies] (resource-handler dependencies {}))
  ([dependencies overrides]
   (let [handler (events-resource/handler dependencies overrides)
         handler (-> handler
                   ring-keyword-params/wrap-keyword-params
                   ring-params/wrap-params)]
     handler)))

(deftest has-status-200
  (let [handler (resource-handler dependencies)
        request (ring/request :get "/events")
        result (handler request)]
    (is (= 200 (:status result)))))
