(ns liberator.resource.events.core-many-pages-on-middle-page-forward-scroll-test
  (:require
   [halboy.resource :as hal]

   [hype.core :as hype]

   [eftest.runner :refer [find-tests run-tests]]
   [eftest.report.pretty :refer [report]]

   [liberator.resource.events.core :as events-resource]

   [liberator.resource.events.test-support.data :as data]
   [liberator.resource.events.test-support.behaviours :as behaviours]))

(defn many-pages-of-events-on-middle-page []
  (let [events
        (take events-resource/default-events-to-pick
          (repeatedly data/random-event))]
    (events-resource/->event-loader
      {:query   (constantly events)
       :before? (constantly true)
       :after?  (constantly true)})))

(let [event-loader (many-pages-of-events-on-middle-page)

      resource-definition {:event-loader event-loader}

      base-url "https://example.com"
      query-params {:since (data/random-uuid-string)}

      options {:base-url            base-url
               :query-params        query-params
               :resource-definition resource-definition}]
  (behaviours/responds-with-status 200 options)
  (behaviours/includes-link-on-resource :discovery
    "https://example.com/"
    options)
  (behaviours/includes-embedded-resources-on-resource :events 10 options))

(behaviours/when no-events-link-fn-provided
  (let [event-loader (many-pages-of-events-on-middle-page)
        events (events-resource/query event-loader {})
        since-event-id (data/random-uuid-string)
        first-event (first events)
        first-event-id (:id first-event)
        last-event (last events)
        last-event-id (:id last-event)

        resource-definition {:event-loader event-loader}

        base-url "https://example.com"
        query-params {:since since-event-id}

        options {:base-url            base-url
                 :query-params        query-params
                 :resource-definition resource-definition}]
    (behaviours/includes-link-on-resource :self
      (str "https://example.com/events?since=" since-event-id)
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/events"
      options)
    (behaviours/includes-link-on-resource :next
      (str "https://example.com/events?since=" last-event-id)
      options)
    (behaviours/includes-link-on-resource :previous
      (str "https://example.com/events?preceding=" first-event-id)
      options)))

(behaviours/when events-link-fn-provided
  (let [router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :api-events]
                   [["/events/" :event-id] :event]]]]]

        event-loader (many-pages-of-events-on-middle-page)
        events (events-resource/query event-loader {})
        since-event-id (data/random-uuid-string)
        first-event (first events)
        first-event-id (:id first-event)
        last-event (last events)
        last-event-id (:id last-event)

        events-link-fn
        (fn [{:keys [request router]} params]
          (hype/absolute-url-for request router :api-events params))

        resource-definition
        {:event-loader event-loader
         :events-link  events-link-fn}

        base-url "https://example.com/api"
        query-params {:since since-event-id}

        options
        {:base-url            base-url
         :query-params        query-params
         :router              router
         :resource-definition resource-definition}]

    (behaviours/includes-link-on-resource :self
      (str "https://example.com/api/events?since=" since-event-id)
      options)

    (behaviours/includes-link-on-resource :first
      "https://example.com/api/events"
      options)

    (behaviours/includes-link-on-resource :next
      (str "https://example.com/api/events?since=" last-event-id)
      options)

    (behaviours/includes-link-on-resource :previous
      (str "https://example.com/api/events?preceding=" first-event-id)
      options)))

(behaviours/when no-event-link-fn-provided
  (let [event-loader (many-pages-of-events-on-middle-page)
        events (events-resource/query event-loader {})
        since-event-id (data/random-uuid-string)

        hrefs (map #(str "https://example.com/events/" (:id %)) events)

        resource-definition {:event-loader event-loader}

        base-url "https://example.com"
        query-params {:since since-event-id}

        options {:base-url            base-url
                 :query-params        query-params
                 :resource-definition resource-definition}]
    (behaviours/includes-links-on-embedded-resources :events :self
      hrefs
      options)))

(behaviours/when event-link-fn-provided
  (let [router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :api-events]
                   [["/events/" :api-event-id] :api-event]]]]]

        event-loader (many-pages-of-events-on-middle-page)
        events (events-resource/query event-loader {})
        since-event-id (data/random-uuid-string)
        hrefs (map #(str "https://example.com/api/events/" (:id %)) events)

        event-link-fn
        (fn [{:keys [request router]} event params]
          (hype/absolute-url-for request router :api-event
            (merge params
              {:path-params {:api-event-id (:id event)}})))

        resource-definition
        {:event-loader event-loader
         :event-link   event-link-fn}

        base-url "https://example.com/api"
        query-params {:since since-event-id}

        options
        {:base-url            base-url
         :query-params        query-params
         :router              router
         :resource-definition resource-definition}]
    (behaviours/includes-links-on-embedded-resources :events :self
      hrefs
      options)))

(behaviours/when no-event-transformer-fn-provided
  (let [event-loader (many-pages-of-events-on-middle-page)
        events (events-resource/query event-loader {})
        since-event-id (data/random-uuid-string)

        event-properties
        (map (fn [event]
               {:id         (:id event)
                :type       (name (:type event))
                :stream     (:stream event)
                :category   (name (:category event))
                :creator    (:creator event)
                :observedAt (str (:observed-at event))
                :occurredAt (str (:occurred-at event))})
          events)

        resource-definition {:event-loader event-loader}

        base-url "https://example.com"
        query-params {:since since-event-id}

        options {:base-url            base-url
                 :query-params        query-params
                 :resource-definition resource-definition}]
    (behaviours/includes-properties-on-embedded-resources :events
      [:id :type :stream :category :creator :observedAt :occurredAt]
      event-properties
      options)
    (behaviours/does-not-include-properties-on-embedded-resources :events
      [:payload]
      options)))

(behaviours/when event-transformer-fn-provided
  (let [event-loader (many-pages-of-events-on-middle-page)
        events (events-resource/query event-loader {})
        since-event-id (data/random-uuid-string)

        event-properties
        (map (fn [event]
               {:id   (:id event)
                :type (name (:type event))})
          events)
        hrefs (map #(str "https://example.com/events/" (:id %)) events)

        event-transformer-fn
        (fn [{:keys [resource] :as context} event]
          (let [event-link-fn (:event-link resource)
                event-link (event-link-fn context event {})
                resource (hal/new-resource event-link)
                resource (hal/add-properties resource
                           (select-keys event
                             [:id
                              :type]))]
            resource))

        resource-definition
        {:event-loader      event-loader
         :event-transformer event-transformer-fn}

        base-url "https://example.com"
        query-params {:since since-event-id}

        options
        {:base-url            base-url
         :query-params        query-params
         :resource-definition resource-definition}]
    (behaviours/includes-links-on-embedded-resources :events :self
      hrefs
      options)
    (behaviours/includes-properties-on-embedded-resources :events
      [:id :type]
      event-properties
      options)
    (behaviours/does-not-include-properties-on-embedded-resources :events
      [:stream :category :creator :observedAt :occurredAt :payload]
      options)))

(comment
  (find-tests *ns*)

  (run-tests
    [(ns-resolve *ns* 'does-not-include-next-link-on-resource)])

  (run-tests
    (find-tests *ns*)
    {:report report}))