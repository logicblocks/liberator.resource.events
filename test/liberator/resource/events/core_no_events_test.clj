(ns liberator.resource.events.core-no-events-test
  (:require
   [hype.core :as hype]

   [eftest.runner :refer [find-tests run-tests]]
   [eftest.report.pretty :refer [report]]

   [liberator.resource.events.test-support.scenarios :as scenarios]
   [liberator.resource.events.test-support.behaviours :as behaviours]))

(def no-events-scenario
  (scenarios/make-scenario
    {:preceding-events?  false
     :subsequent-events? false
     :page-size 0}))

(let [{:keys [options]}
      (no-events-scenario
        {:base-url "https://example.com"})]
  (behaviours/responds-with-status 200 options)
  (behaviours/includes-link-on-resource :discovery
    "https://example.com/"
    options)
  (behaviours/does-not-include-link-on-resource :next options)
  (behaviours/does-not-include-link-on-resource :previous options)
  (behaviours/includes-embedded-resources-on-resource :events 0 options))

(behaviours/when no-events-link-fn-provided
  (let [{:keys [options]}
        (no-events-scenario
          {:base-url "https://example.com"})]
    (behaviours/includes-link-on-resource :self
      "https://example.com/events"
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/events"
      options)))

(behaviours/when events-link-fn-provided
  (let [{:keys [options]}
        (no-events-scenario
          {:base-url "https://example.com/api"
           :router   [""
                      [["/api"
                        [["" :discovery]
                         ["/events" :api-events]
                         [["/events/" :api-event-id] :event]]]]]
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

(behaviours/when pick-query-param-provided
  (let [{:keys [options]}
        (no-events-scenario
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
        (no-events-scenario
          {:base-url     "https://example.com"
           :query-params {:sort "descending"}})]
    (behaviours/includes-link-on-resource :self
      "https://example.com/events?sort=descending"
      options)
    (behaviours/includes-link-on-resource :first
      "https://example.com/events?sort=descending"
      options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)))

(comment
  (find-tests *ns*)

  (run-tests
    [(ns-resolve *ns*
       'includes-self-link-on-resource-when-pick-query-param-provided)])

  (run-tests
    (find-tests *ns*)
    {:report report}))
