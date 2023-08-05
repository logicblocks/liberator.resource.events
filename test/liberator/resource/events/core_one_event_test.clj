(ns liberator.resource.events.core-one-event-test
  (:require
    [clojure.test :refer [deftest is]]

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

(defn one-event-loader
  ([] (one-event-loader (data/random-event)))
  ([event]
   (events-resource/->event-loader
     {:query   (constantly [event])
      :before? (constantly false)
      :after?  (constantly false)})))

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
                  {:event-loader (one-event-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)]
    (is (= 200 (:status result)))))

(deftest includes-self-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (one-event-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :self)))))

(deftest includes-discovery-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (one-event-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/"
          (hal/get-href resource :discovery)))))

(deftest includes-first-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (one-event-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :first)))))

(deftest does-not-include-next-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (one-event-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :next)))))

(deftest does-not-include-previous-link-on-resource
  (let [handler (resource-handler dependencies
                  {:event-loader (one-event-loader)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :previous)))))

(deftest includes-self-link-on-embedded-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (one-event-loader event)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        event-resources (hal/get-resource resource :events)]
    (is (= 1 (count event-resources)))

    (let [event-resource (first event-resources)]
      (is (= (str "https://example.com/events/" (:id event))
            (hal/get-href event-resource :self))))))

(deftest includes-all-props-but-payload-on-embedded-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (one-event-loader event)})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        event-resources (hal/get-resource resource :events)]
    (is (= 1 (count event-resources)))

    (let [event-resource (first event-resources)]
      (is (= (:id event) (hal/get-property event-resource :id)))
      (is (= (name (:type event))
            (hal/get-property event-resource :type)))
      (is (= (:stream event) (hal/get-property event-resource :stream)))
      (is (= (name (:category event))
            (hal/get-property event-resource :category)))
      (is (= (name (:creator event))
            (hal/get-property event-resource :creator)))
      (is (= (str (:observed-at event))
            (hal/get-property event-resource :observedAt)))
      (is (= (str (:occurred-at event))
            (hal/get-property event-resource :occurredAt)))
      (is (nil? (hal/get-property event-resource :payload))))))

(deftest uses-event-transformer-to-build-embedded-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (one-event-loader event)
                   :event-transformer
                   (fn [{:keys [resource] :as context} event]
                     (let [event-link-fn (:event-link resource)
                           event-link (event-link-fn context event)
                           resource (hal/new-resource event-link)
                           resource (hal/add-properties resource
                                      (select-keys event
                                        [:id
                                         :type]))]
                       resource))})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        event-resources (hal/get-resource resource :events)]
    (is (= 1 (count event-resources)))

    (let [event-resource (first event-resources)]
      (is (= (str "https://example.com/events/" (:id event))
            (hal/get-href event-resource :self)))

      (is (= {:id   (:id event)
              :type (name (:type event))}
            (hal/properties event-resource))))))

(deftest uses-event-link-function-for-event-self-link-when-provided
  (let [event (data/random-event)
        router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :events]
                   [["/events/" :api-event-id] :api-event]]]]]
        dependencies {:router router}
        handler (resource-handler dependencies
                  {:event-loader (one-event-loader event)
                   :event-link
                   (fn [{:keys [request router]} event]
                     (hype/absolute-url-for request router :api-event
                       {:path-params {:api-event-id (:id event)}}))})
        request (ring/request :get "https://example.com/api/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        event-resources (hal/get-resource resource :events)]
    (is (= 1 (count event-resources)))

    (let [event-resource (first event-resources)]
      (is (= (str "https://example.com/api/events/" (:id event))
            (hal/get-href event-resource :self))))))

(deftest uses-events-link-function-for-resource-self-link-when-provided
  (let [event (data/random-event)
        router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :api-events]
                   [["/events/" :event-id] :event]]]]]
        dependencies {:router router}
        handler (resource-handler dependencies
                  {:event-loader (one-event-loader event)
                   :events-link
                   (fn [{:keys [request router]}]
                     (hype/absolute-url-for request router :api-events))})
        request (ring/request :get "https://example.com/api/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/api/events"
          (hal/get-href resource :self)))))

(deftest uses-events-link-function-for-resource-first-link-when-provided
  (let [event (data/random-event)
        router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :api-events]
                   [["/events/" :event-id] :event]]]]]
        dependencies {:router router}
        handler (resource-handler dependencies
                  {:event-loader (one-event-loader event)
                   :events-link
                   (fn [{:keys [request router]}]
                     (hype/absolute-url-for request router :api-events))})
        request (ring/request :get "https://example.com/api/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/api/events"
          (hal/get-href resource :first)))))
