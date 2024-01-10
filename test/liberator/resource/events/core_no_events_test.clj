(ns liberator.resource.events.core-no-events-test
  (:require
   [hype.core :as hype]

   [eftest.runner :refer [find-tests run-tests]]
   [eftest.report.pretty :refer [report]]

   [liberator.resource.events.core :as events-resource]

   [liberator.resource.events.test-support.behaviours :as behaviours]))

(defn no-events-loader []
  (events-resource/->event-loader
    {:query   (constantly [])
     :before? (constantly false)
     :after?  (constantly false)}))

(let [base-url "https://example.com"
      event-loader (no-events-loader)
      resource-definition {:event-loader event-loader}
      options {:base-url            base-url
               :resource-definition resource-definition}]
  (behaviours/responds-with-status 200 options)
  (behaviours/includes-link-on-resource :discovery
    "https://example.com/"
    options)
  (behaviours/does-not-include-link-on-resource :next options)
  (behaviours/does-not-include-link-on-resource :previous options)
  (behaviours/includes-embedded-resources-on-resource :events 0 options))

(behaviours/when no-events-link-fn-provided
  (let [base-url "https://example.com"
        event-loader (no-events-loader)
        resource-definition {:event-loader event-loader}
        options {:base-url            base-url
                 :resource-definition resource-definition}]
    (behaviours/includes-link-on-resource :self
      "https://example.com/events"
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/events"
      options)))

(behaviours/when events-link-fn-provided
  (let [base-url "https://example.com/api"
        router [""
                [["/api"
                  [["" :discovery]
                   ["/events" :api-events]
                   [["/events/" :event-id] :event]]]]]
        event-loader (no-events-loader)
        events-link-fn
        (fn [{:keys [request router]} params]
          (hype/absolute-url-for request router :api-events params))

        resource-definition
        {:event-loader event-loader
         :events-link  events-link-fn}

        options
        {:base-url            base-url
         :router              router
         :resource-definition resource-definition}]

    (behaviours/includes-link-on-resource :self
      "https://example.com/api/events"
      options)

    (behaviours/includes-link-on-resource :first
      "https://example.com/api/events"
      options)))

(comment
  (find-tests *ns*)

  (run-tests
    [(ns-resolve *ns* 'does-not-include-next-link-on-resource)])

  (run-tests
    (find-tests *ns*)
    {:report report}))
