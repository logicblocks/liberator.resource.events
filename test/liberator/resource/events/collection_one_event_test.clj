(ns liberator.resource.events.collection-one-event-test
  (:require
   [halboy.resource :as hal]

   [hype.core :as hype]

   [eftest.runner :refer [find-tests run-tests]]
   [eftest.report.pretty :refer [report]]

   [liberator.resource.events.test-support.behaviours :as behaviours]
   [liberator.resource.events.test-support.scenarios :as scenarios]))

(def one-event-scenario
  (scenarios/make-scenario
    {:preceding-events?  false
     :subsequent-events? false
     :page-size          1}))

(let [{:keys [events options]}
      (one-event-scenario
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
  (behaviours/includes-embedded-resources-on-resource :events 1 options)
  (behaviours/includes-links-on-embedded-resources :events :self
    [(str "https://example.com/events/" (:id (first events)))]
    options))

(behaviours/when no-events-link-fn-provided
  (let [{:keys [options]}
        (one-event-scenario
          {:base-url "https://example.com"})]
    (behaviours/includes-link-on-resource :self
      "https://example.com/events"
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/events"
      options)))

(behaviours/when events-link-fn-provided
  (let [{:keys [options]}
        (one-event-scenario
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
        (one-event-scenario
          {:base-url "https://example.com"
           :router   [""
                      [["/" :discovery]
                       ["/events" :events]
                       [["/events/" :event-id] :event]]]})
        hrefs (mapv #(str "https://example.com/events/" (:id %)) events)]
    (behaviours/includes-link-on-resource :events hrefs options)
    (behaviours/includes-links-on-embedded-resources :events :self
      [(str "https://example.com/events/" (:id (first events)))]
      options)))

(behaviours/when event-link-fn-provided
  (let [{:keys [events options]}
        (one-event-scenario
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
        hrefs (mapv #(str "https://example.com/api/events/" (:id %)) events)]
    (behaviours/includes-link-on-resource :events hrefs options)
    (behaviours/includes-links-on-embedded-resources :events :self
      [(str "https://example.com/api/events/" (:id (first events)))]
      options)))

(behaviours/when no-event-transformer-fn-provided
  (let [{:keys [events options]}
        (one-event-scenario)]
    (behaviours/includes-properties-on-embedded-resources :events
      [:id :type :stream :category :creator :observedAt :occurredAt]
      [{:id         (:id (first events))
        :type       (name (:type (first events)))
        :stream     (:stream (first events))
        :category   (name (:category (first events)))
        :creator    (:creator (first events))
        :observedAt (str (:observed-at (first events)))
        :occurredAt (str (:occurred-at (first events)))}]
      options)
    (behaviours/does-not-include-properties-on-embedded-resources :events
      [:payload]
      options)))

(behaviours/when event-transformer-fn-provided
  (let [{:keys [events options]}
        (one-event-scenario
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
                resource))}})]
    (behaviours/includes-links-on-embedded-resources :events :self
      [(str "https://example.com/events/" (:id (first events)))]
      options)
    (behaviours/includes-properties-on-embedded-resources :events
      [:id :type]
      [{:id   (:id (first events))
        :type (name (:type (first events)))}]
      options)
    (behaviours/does-not-include-properties-on-embedded-resources :events
      [:stream :category :creator :observedAt :occurredAt :payload]
      options)))

(behaviours/when pick-query-param-provided
  (let [{:keys [options]}
        (one-event-scenario
          {:base-url     "https://example.com"
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
        (one-event-scenario
          {:base-url     "https://example.com"
           :query-params {:sort "desc"}})]
    (behaviours/includes-link-on-resource :self
      "https://example.com/events?sort=desc"
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/events?sort=desc"
      options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)))

(comment
  (find-tests *ns*)

  (run-tests
    [(ns-resolve *ns* 'responds-with-status-200)])

  (run-tests
    (find-tests *ns*)
    {:report report}))
