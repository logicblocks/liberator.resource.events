(ns liberator.resource.events.test-support.scenarios
  (:require
   [liberator.resource.events.collection :as events-resource]
   [liberator.resource.events.test-support.data :as data]))

(defn make-scenario
  ([{mandatory-query-params :query-params
     default-page-size      :page-size
     :keys                  [preceding-events? subsequent-events?]
     :or
     {mandatory-query-params (fn [_] {})
      default-page-size      events-resource/default-events-to-pick}}]
   (fn scenario-fn
     ([] (scenario-fn {}))
     ([{:keys [base-url router resource-definition query-params page-size]
        :or   {base-url  (data/random-base-url)
               page-size default-page-size}}]
      (let [events (take page-size (repeatedly data/random-event))

            since-event-id
            (when preceding-events? (data/random-uuid-string))
            preceding-event-id
            (when subsequent-events? (data/random-uuid-string))
            first-event-id (:id (first events))
            last-event-id (:id (last events))

            data {:events             events
                  :since-event-id     since-event-id
                  :preceding-event-id preceding-event-id
                  :first-event-id     first-event-id
                  :last-event-id      last-event-id}

            event-loader
            (events-resource/->event-loader
              {:load-events
               (fn [_]
                 {:events  events
                  :before? preceding-events?
                  :after?  subsequent-events?})})

            query-params (merge query-params (mandatory-query-params data))

            resource-definition
            (merge resource-definition {:event-loader event-loader})

            options {:base-url            base-url
                     :query-params        query-params
                     :router              router
                     :resource-definition resource-definition}]
        (merge data {:options options}))))))
