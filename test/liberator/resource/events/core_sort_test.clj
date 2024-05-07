(ns liberator.resource.events.core-sort-test
  (:require
   [eftest.runner :refer [find-tests run-tests]]
   [eftest.report.pretty :refer [report]]

   [liberator.resource.events.test-support.behaviours :as behaviours]
   [liberator.resource.events.test-support.scenarios :as scenarios]))

(def any-page-scenario
  (scenarios/make-scenario
    {:preceding-events?  false
     :subsequent-events? false}))

(behaviours/when sort-query-param-invalid
  (let [{:keys [options]}
        (any-page-scenario
          {:base-url     "https://example.com"
           :query-params {:sort "wat"}})]
    (behaviours/responds-with-status 422 options)))

(comment
  (find-tests *ns*)

  (run-tests
    [(ns-resolve *ns*
       'includes-self-link-on-resource-when-pick-query-param-provided)])

  (run-tests
    (find-tests *ns*)
    {:report report}))
