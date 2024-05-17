(ns liberator.resource.events.collection-exactly-one-page-test
  (:require
   [halboy.resource :as hal]

   [hype.core :as hype]

   [eftest.runner :refer [find-tests run-tests]]
   [eftest.report.pretty :refer [report]]

   [liberator.resource.events.test-support.behaviours :as behaviours]
   [liberator.resource.events.test-support.scenarios :as scenarios]))

(def exactly-one-page-of-events-scenario
  (scenarios/make-scenario
    {:preceding-events?  false
     :subsequent-events? false}))

(let [{:keys [events options]}
      (exactly-one-page-of-events-scenario
        {:base-url "https://example.com"
         :router   [""
                    [["/" :discovery]
                     ["/events" :events]
                     [["/events/" :event-id] :event]]]})
      hrefs (mapv #(str "https://example.com/events/" (:id %)) events)]
  (behaviours/responds-with-status 200 options)
  (behaviours/includes-link-on-resource :discovery
    "https://example.com/"
    options)
  (behaviours/includes-link-on-resource :events hrefs options)
  (behaviours/does-not-include-link-on-resource :next options)
  (behaviours/does-not-include-link-on-resource :previous options)
  (behaviours/includes-embedded-resources-on-resource :events 10 options))

(behaviours/when no-events-link-fn-provided
  (let [{:keys [options]}
        (exactly-one-page-of-events-scenario
          {:base-url "https://example.com"})]
    (behaviours/includes-link-on-resource :self
      "https://example.com/events"
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/events"
      options)))

(behaviours/when events-link-fn-provided
  (let [{:keys [options]}
        (exactly-one-page-of-events-scenario
          {:base-url "https://example.com/api"
           :router   [""
                      [["/api"
                        [["" :discovery]
                         ["/events" :api-events]
                         [["/events/" :api-event-id] :api-event]]]]]
           :resource-definition
           {:events-link
            (fn [{:keys [request router]} params]
              (hype/absolute-url-for request router :api-events params))}})]
    (behaviours/includes-link-on-resource :self
      "https://example.com/api/events"
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/api/events"
      options)))

(behaviours/when no-event-link-fn-provided
  (let [{:keys [events options]}
        (exactly-one-page-of-events-scenario
          {:base-url "https://example.com"
           :router   [""
                      [["/" :discovery]
                       ["/events" :events]
                       [["/events/" :event-id] :event]]]})
        hrefs (map #(str "https://example.com/events/" (:id %)) events)]
    (behaviours/includes-link-on-resource :events hrefs options)
    (behaviours/includes-links-on-embedded-resources :events :self
      hrefs
      options)))

(behaviours/when event-link-fn-provided
  (let [{:keys [events options]}
        (exactly-one-page-of-events-scenario
          {:base-url "https://example.com/api"
           :router   [""
                      [["/api"
                        [["" :discovery]
                         ["/events" :api-events]
                         [["/events/" :api-event-id] :api-event]]]]]
           :resource-definition
           {:event-link
            (fn [{:keys [request router]} event params]
              (hype/absolute-url-for request router :api-event
                (merge params
                  {:path-params {:api-event-id (:id event)}})))}})
        hrefs (map #(str "https://example.com/api/events/" (:id %)) events)]
    (behaviours/includes-link-on-resource :events hrefs options)
    (behaviours/includes-links-on-embedded-resources :events :self
      hrefs
      options)))

(behaviours/when no-event-transformer-fn-provided
  (let [{:keys [events options]}
        (exactly-one-page-of-events-scenario)
        event-properties
        (map (fn [event]
               {:id         (:id event)
                :type       (name (:type event))
                :stream     (:stream event)
                :category   (name (:category event))
                :creator    (:creator event)
                :observedAt (str (:observed-at event))
                :occurredAt (str (:occurred-at event))})
          events)]
    (behaviours/includes-properties-on-embedded-resources :events
      [:id :type :stream :category :creator :observedAt :occurredAt]
      event-properties
      options)
    (behaviours/does-not-include-properties-on-embedded-resources :events
      [:payload]
      options)))

(behaviours/when event-transformer-fn-provided
  (let [{:keys [events options]}
        (exactly-one-page-of-events-scenario
          {:base-url "https://example.com"
           :resource-definition
           {:event-transformer
            (fn [{:keys [resource] :as context} event]
              (let [event-link-fn (:event-link resource)
                    event-link (event-link-fn context event {})
                    resource (hal/new-resource event-link)
                    resource (hal/add-properties resource
                               (select-keys event
                                 [:id
                                  :type]))]
                resource))}})
        event-properties
        (map (fn [event]
               {:id   (:id event)
                :type (name (:type event))})
          events)
        hrefs (map #(str "https://example.com/events/" (:id %)) events)]
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

(behaviours/when pick-query-param-provided
  (let [{:keys [options]}
        (exactly-one-page-of-events-scenario
          {:page-size    20
           :base-url     "https://example.com"
           :query-params {:pick 20}})]
    (behaviours/includes-link-on-resource :self
      "https://example.com/events?pick=20"
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/events?pick=20"
      options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)))

(behaviours/when sort-query-param-provided
  (let [{:keys [options]}
        (exactly-one-page-of-events-scenario
          {:base-url     "https://example.com"
           :query-params {:sort "asc"}})]
    (behaviours/includes-link-on-resource :self
      "https://example.com/events?sort=asc"
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/events?sort=asc"
      options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)))

(comment
  (find-tests *ns*)

  (run-tests
    [(ns-resolve *ns* 'does-not-include-next-link-on-resource)])

  (run-tests
    (find-tests *ns*)
    {:report report
     :multithread? false}))
