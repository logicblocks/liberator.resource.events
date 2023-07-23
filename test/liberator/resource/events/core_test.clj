(ns liberator.resource.events.core-test
  (:require
    [clojure.test :refer [deftest is testing]]

    [halboy.resource :as hal]
    [halboy.json :as hal-json]

    [hype.core :as hype]

    [ring.mock.request :as ring]
    [ring.middleware.keyword-params :as ring-keyword-params]
    [ring.middleware.params :as ring-params]

    [liberator.resource.events.core :as events-resource]

    [liberator.resource.events.test-support.data :as data]))

;; Ideas
;; -----
;; by ID pointer: since, preceding, from, to
;; by date: observed-before, observed-after, occurred-before, occurred-after
;; by content: type, stream, category,
;; sorting: sort (ascending/asc, descending/desc)
;; filtering: filter?
;; pagination: pick, per-page, page
;; links: first, last, next, previous
;;   - first easy, no since / page / preceding,
;;     - retain pick / per-page and filters
;;   - last hard as requires count
;;     - does knowing last event help?
;;     - could find (pick + 1) events back from last and use since
;;
;; paging modes:
;;   - since/preceding + pick, excluding event with id, guaranteed not to skip
;;   - from/to + pick, including event with id, guaranteed not to skip
;;   - page + per-page, may involve skips
;;
;; { id, type, stream, category, creator, observed-at, occurred-at }
;;
;; Plan
;; ----
;; only support [{since, preceding}, {pick}], not [{page}, {per-page}]
;; only support [{first, next, previous}, {sort}], not [{last}, {}]
;; support, type, stream, category, {observed|occurred}-{before|after}
;;

(def router
  [""
   [["/" :discovery]
    ["/events" :events]
    [["/events/" :event-id] :event]]])

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

(deftest responds-with-status-200
  (testing "when no events"
    (let [handler (resource-handler dependencies
                    {:event-loader (fn [_] [])})
          request (ring/request :get "https://example.com/events")
          result (handler request)]
      (is (= 200 (:status result)))))

  (testing "when single event"
    (let [event (data/random-event)
          handler (resource-handler dependencies
                    {:event-loader (fn [_] [event])})
          request (ring/request :get "https://example.com/events")
          result (handler request)]
      (is (= 200 (:status result)))))

  (testing "when many events but less than default pick"
    (let [event-1 (data/random-event)
          event-2 (data/random-event)
          event-3 (data/random-event)
          handler (resource-handler dependencies
                    {:event-loader (fn [_] [event-1 event-2 event-3])})
          request (ring/request :get "https://example.com/events")
          result (handler request)]
      (is (= 200 (:status result)))))

  (testing "when many events and equal to default pick"
    (let [event-stream (repeatedly data/random-event)
          events (take 10 event-stream)

          handler (resource-handler dependencies
                    {:event-loader (fn [_] events)})
          request (ring/request :get "https://example.com/events")
          result (handler request)]
      (is (= 200 (:status result))))))

(deftest includes-self-link-on-resource-when-no-events
  (let [handler (resource-handler dependencies
                  {:event-loader (fn [_] [])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :self)))))

(deftest includes-discovery-link-on-resource-when-no-events
  (let [handler (resource-handler dependencies
                  {:event-loader (fn [_] [])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/"
          (hal/get-href resource :discovery)))))

(deftest includes-first-link-on-resource-when-no-events
  (let [handler (resource-handler dependencies
                  {:event-loader (fn [_] [])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :first)))))

(deftest does-not-include-next-link-on-resource-when-no-events
  (let [handler (resource-handler dependencies
                  {:event-loader (fn [_] [])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :next)))))

(deftest does-not-include-previous-link-on-resource-when-no-events
  (let [handler (resource-handler dependencies
                  {:event-loader (fn [_] [])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :previous)))))

(deftest embeds-empty-events-resource-on-resource-when-no-events
  (let [handler (resource-handler dependencies
                  {:event-loader (fn [_] [])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= [] (hal/get-resource resource :events)))))

(deftest includes-self-link-on-resource-when-single-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (fn [_] [event])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :self)))))

(deftest includes-discovery-link-on-resource-when-single-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (fn [_] [event])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/"
          (hal/get-href resource :discovery)))))

(deftest includes-first-link-on-resource-when-single-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (fn [_] [event])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/events"
          (hal/get-href resource :first)))))

(deftest does-not-include-next-link-on-resource-when-single-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (fn [_] [event])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :next)))))

(deftest does-not-include-previous-link-on-resource-when-single-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (fn [_] [event])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-href resource :previous)))))

(deftest includes-self-link-on-embedded-event-when-single-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (fn [_] [event])})
        request (ring/request :get "https://example.com/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        event-resources (hal/get-resource resource :events)]
    (is (= 1 (count event-resources)))

    (let [event-resource (first event-resources)]
      (is (= (str "https://example.com/events/" (:id event))
            (hal/get-href event-resource :self))))))

(deftest
  includes-all-props-but-payload-on-embedded-event-when-single-event
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (fn [_] [event])})
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

(deftest uses-event-transformer-to-build-embedded-event-when-provided
  (let [event (data/random-event)
        handler (resource-handler dependencies
                  {:event-loader (fn [_] [event])
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

      (is (= {:id (:id event)
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
                  {:event-loader (fn [_] [event])
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
                  {:event-loader (fn [_] [event])
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
                  {:event-loader (fn [_] [event])
                   :events-link
                   (fn [{:keys [request router]}]
                     (hype/absolute-url-for request router :api-events))})
        request (ring/request :get "https://example.com/api/events")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "https://example.com/api/events"
          (hal/get-href resource :first)))))
