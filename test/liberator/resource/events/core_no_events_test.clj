(ns liberator.resource.events.core-no-events-test
  (:require
   [clojure.test :refer [deftest is]]

   [halboy.resource :as hal]
   [halboy.json :as hal-json]

   [hype.core :as hype]

   [ring.mock.request :as ring]
   [ring.middleware.keyword-params :as ring-keyword-params]
   [ring.middleware.params :as ring-params]

   [liberator.resource.events.core :as events-resource]))

(def router
  [""
   [["/" :discovery]
    ["/events" :events]
    [["/events/" :event-id] :event]]])

(def dependencies
  {:router router})

(defn no-events-loader []
  (events-resource/->event-loader
    {:query   (constantly [])
     :before? (constantly false)
     :after?  (constantly false)}))

(defn resource-handler
  ([dependencies] (resource-handler dependencies {}))
  ([dependencies overrides]
   (let [handler (events-resource/handler dependencies overrides)
         handler (-> handler
                   ring-keyword-params/wrap-keyword-params
                   ring-params/wrap-params)]
     handler)))

(deftest responds-with-status-200
  (let [handler (resource-handler dependencies
                  {:event-loader (no-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)]
    (is (= 200 (:status result)))))

(deftest includes-self-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (no-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :self)))))

(deftest includes-discovery-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (no-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/"
          (hal/get-href resource :discovery)))))

(deftest includes-first-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (no-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :first)))))

(deftest does-not-include-next-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (no-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :next)))))

(deftest does-not-include-previous-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (no-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :previous)))))

(deftest embeds-empty-events-resource-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (no-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= [] (hal/get-resource resource :events)))))
(deftest uses-events-link-function-for-resource-self-link-when-provided
  (let [router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :api-events]
                   [["/events/" :event-id] :event]]]]]
        dependencies {:router router}
        handler (resource-handler dependencies
                  {:event-loader (no-events-loader)
                   :events-link
                   (fn [{:keys [request router]}]
                     (hype/absolute-url-for request router :api-events))})
        request (ring/request :get "https://example.com/api/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/api/events"
          (hal/get-href resource :self)))))

(deftest uses-events-link-function-for-resource-first-link-when-provided
  (let [router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :api-events]
                   [["/events/" :event-id] :event]]]]]
        dependencies {:router router}
        handler (resource-handler dependencies
                  {:event-loader (no-events-loader)
                   :events-link
                   (fn [{:keys [request router]}]
                     (hype/absolute-url-for request router :api-events))})
        request (ring/request :get "https://example.com/api/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/api/events"
          (hal/get-href resource :first)))))
