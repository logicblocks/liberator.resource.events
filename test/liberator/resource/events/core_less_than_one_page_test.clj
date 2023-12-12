(ns liberator.resource.events.core-less-than-one-page-test
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.set :as set]

   [halboy.resource :as hal]
   [halboy.json :as hal-json]

   [hype.core :as hype]

   [ring.mock.request :as ring]
   [ring.middleware.keyword-params :as ring-keyword-params]
   [ring.middleware.params :as ring-params]

   [liberator.resource.events.core :as events-resource]

   [liberator.resource.events.test-support.data :as data]))

(def router
  [""
   [["/" :discovery]
    ["/events" :events]
    [["/events/" :event-id] :event]]])

(def dependencies
  {:router router})

(defn less-than-one-page-of-events-loader
  ([] (less-than-one-page-of-events-loader
        (take 5 (repeatedly data/random-event))))
  ([events]
   (events-resource/->event-loader
     {:query   (constantly events)
      :before? (constantly false)
      :after?  (constantly false)})))

(take 5 (repeatedly data/random-event))

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
                  {:event-loader (less-than-one-page-of-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)]
    (is (= 200 (:status result)))))

(deftest includes-self-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (less-than-one-page-of-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :self)))))

(deftest includes-discovery-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (less-than-one-page-of-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/"
          (hal/get-href resource :discovery)))))

(deftest includes-first-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (less-than-one-page-of-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :first)))))

(deftest does-not-include-next-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (less-than-one-page-of-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :next)))))

(deftest does-not-include-previous-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (less-than-one-page-of-events-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :previous)))))

(deftest includes-self-link-on-embedded-events
  (let [event-loader (less-than-one-page-of-events-loader)
        events (events-resource/query event-loader {})
        handler (resource-handler dependencies
                  {:event-loader event-loader})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        event-resources (hal/get-resource resource :events)]
    (is (=
          (map (fn [event] (str "https://example.com/events/" (:id event)))
            events)
          (map (fn [event-resource] (hal/get-href event-resource :self))
            event-resources)))))

(deftest includes-all-props-but-payload-on-embedded-events
  (let [event-loader (less-than-one-page-of-events-loader)
        events (events-resource/query event-loader {})
        handler (resource-handler dependencies
                  {:event-loader event-loader})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        event-resources (hal/get-resource resource :events)]
    (is (=
          (map (fn [event]
                 (-> event
                   (dissoc :payload)
                   (update :type name)
                   (update :category name)
                   (update :creator name)
                   (update :observed-at str)
                   (update :occurred-at str)
                   (set/rename-keys
                     {:observed-at :observedAt
                      :occurred-at :occurredAt})))
            events)
          (map hal/properties event-resources)))))

(deftest uses-event-transformer-to-build-embedded-event-when-provided
  (let [event-loader (less-than-one-page-of-events-loader)
        events (events-resource/query event-loader {})
        event-transformer
        (fn [{:keys [resource] :as context} event]
          (let [event-link-fn (:event-link resource)
                event-link (event-link-fn context event)
                resource (hal/new-resource event-link)
                resource (hal/add-properties resource
                           (select-keys event [:id :type]))]
            resource))
        handler (resource-handler dependencies
                  {:event-loader      event-loader
                   :event-transformer event-transformer})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        event-resources (hal/get-resource resource :events)]
    (is (=
          (map (fn [event]
                 (hal/add-properties
                   (hal/new-resource
                     (str "https://example.com/events/" (:id event)))
                   {:id   (:id event)
                    :type (name (:type event))}))
            events)
          event-resources))))

(deftest uses-event-link-function-for-event-self-link-when-provided
  (let [event-loader (less-than-one-page-of-events-loader)
        events (events-resource/query event-loader {})
        router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :events]
                   [["/events/" :api-event-id] :api-event]]]]]
        dependencies {:router router}
        handler (resource-handler dependencies
                  {:event-loader event-loader
                   :event-link
                   (fn [{:keys [request router]} event]
                     (hype/absolute-url-for request router :api-event
                       {:path-params {:api-event-id (:id event)}}))})
        request (ring/request :get "https://example.com/api/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        event-resources (hal/get-resource resource :events)]
    (is (=
          (map (fn [event]
                 (str "https://example.com/api/events/" (:id event)))
            events)
          (map (fn [event-resource]
                 (hal/get-href event-resource :self))
            event-resources)))))

(deftest uses-events-link-function-for-resource-self-link-when-provided
  (let [event-loader (less-than-one-page-of-events-loader)
        router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :api-events]
                   [["/events/" :event-id] :event]]]]]
        dependencies {:router router}
        handler (resource-handler dependencies
                  {:event-loader event-loader
                   :events-link
                   (fn [{:keys [request router]}]
                     (hype/absolute-url-for request router :api-events))})
        request (ring/request :get "https://example.com/api/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/api/events"
          (hal/get-href resource :self)))))

(deftest uses-events-link-function-for-resource-first-link-when-provided
  (let [event-loader (less-than-one-page-of-events-loader)
        router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :api-events]
                   [["/events/" :event-id] :event]]]]]
        dependencies {:router router}
        handler (resource-handler dependencies
                  {:event-loader event-loader
                   :events-link
                   (fn [{:keys [request router]}]
                     (hype/absolute-url-for request router :api-events))})
        request (ring/request :get "https://example.com/api/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/api/events"
          (hal/get-href resource :first)))))
