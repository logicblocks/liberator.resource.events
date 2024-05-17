(ns liberator.resource.events.collection-validation-test
  (:require
   [clojure.spec.alpha :as spec]
   [eftest.runner :refer [find-tests run-tests]]
   [eftest.report.pretty :refer [report]]

   [spec.definition.core :as sd]

   [liberator.resource.events.test-support.behaviours :as behaviours]
   [liberator.resource.events.test-support.scenarios :as scenarios]))

(def any-page-scenario
  (scenarios/make-scenario
    {:preceding-events?  false
     :subsequent-events? false}))

(behaviours/when pick-query-param-invalid
  (let [{:keys [options]}
        (any-page-scenario
          {:base-url     "https://example.com"
           :query-params {:pick "wat"}})]
    (behaviours/responds-with-status 422 options)
    (behaviours/includes-properties-on-resource
      [:errorId :errorContext]
      {:errorId some?
       :errorContext
       [{:field        ["params" "pick"]
         :requirements ["must-be-a-positive-number"]
         :subject      "request"
         :type         "invalid"}]}
      options)
    (behaviours/includes-link-on-resource :discovery
      "https://example.com/"
      options)
    (behaviours/includes-link-on-resource :self
      "https://example.com/events?pick=wat"
      options)
    (behaviours/does-not-include-link-on-resource :first options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)
    (behaviours/does-not-include-embedded-resources-on-resource :events
      options)))

(behaviours/when sort-query-param-invalid
  (let [{:keys [options]}
        (any-page-scenario
          {:base-url     "https://example.com"
           :query-params {:sort "wat"}})]
    (behaviours/responds-with-status 422 options)
    (behaviours/includes-properties-on-resource
      [:errorId :errorContext]
      {:errorId some?
       :errorContext
       [{:field        ["params" "sort"]
         :requirements ["must-be-a-valid-sort-order"]
         :subject      "request"
         :type         "invalid"}]}
      options)
    (behaviours/includes-link-on-resource :discovery
      "https://example.com/"
      options)
    (behaviours/includes-link-on-resource :self
      "https://example.com/events?sort=wat"
      options)
    (behaviours/does-not-include-link-on-resource :first options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)
    (behaviours/does-not-include-embedded-resources-on-resource :events
      options)))

(behaviours/when since-query-param-invalid
  (let [{:keys [options]}
        (any-page-scenario
          {:base-url     "https://example.com"
           :query-params {:since "not-a-uuid"}})]
    (behaviours/responds-with-status 422 options)
    (behaviours/includes-properties-on-resource
      [:errorId :errorContext]
      {:errorId some?
       :errorContext
       [{:field        ["params" "since"]
         :requirements ["must-be-a-uuid-string"]
         :subject      "request"
         :type         "invalid"}]}
      options)
    (behaviours/includes-link-on-resource :discovery
      "https://example.com/"
      options)
    (behaviours/includes-link-on-resource :self
      "https://example.com/events?since=not-a-uuid"
      options)
    (behaviours/does-not-include-link-on-resource :first options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)
    (behaviours/does-not-include-embedded-resources-on-resource :events
      options)))

(behaviours/when preceding-query-param-invalid
  (let [{:keys [options]}
        (any-page-scenario
          {:base-url     "https://example.com"
           :query-params {:preceding "not-a-uuid"}})]
    (behaviours/responds-with-status 422 options)
    (behaviours/includes-properties-on-resource
      [:errorId :errorContext]
      {:errorId some?
       :errorContext
       [{:field        ["params" "preceding"]
         :requirements ["must-be-a-uuid-string"]
         :subject      "request"
         :type         "invalid"}]}
      options)
    (behaviours/includes-link-on-resource :discovery
      "https://example.com/"
      options)
    (behaviours/includes-link-on-resource :self
      "https://example.com/events?preceding=not-a-uuid"
      options)
    (behaviours/does-not-include-link-on-resource :first options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)
    (behaviours/does-not-include-embedded-resources-on-resource :events
      options)))

(behaviours/when multiple-params-invalid
  (let [{:keys [options]}
        (any-page-scenario
          {:base-url     "https://example.com"
           :query-params {:pick      "spinach"
                          :since     "not-a-uuid"
                          :preceding "not-a-uuid"}})]
    (behaviours/responds-with-status 422 options)
    (behaviours/includes-properties-on-resource
      [:errorId :errorContext]
      {:errorId some?
       :errorContext
       [{:field        ["params" "pick"]
         :requirements ["must-be-a-positive-number"]
         :subject      "request"
         :type         "invalid"}
        {:field        ["params" "since"]
         :requirements ["must-be-a-uuid-string"]
         :subject      "request"
         :type         "invalid"}
        {:field        ["params" "preceding"]
         :requirements ["must-be-a-uuid-string"]
         :subject      "request"
         :type         "invalid"}]}
      options)
    (behaviours/includes-link-on-resource :discovery
      "https://example.com/"
      options)
    (behaviours/includes-link-on-resource :self
      (str "https://example.com/events?"
        "pick=spinach&"
        "since=not-a-uuid&"
        "preceding=not-a-uuid")
      options)
    (behaviours/does-not-include-link-on-resource :first options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)
    (behaviours/does-not-include-embedded-resources-on-resource :events
      options)))

(behaviours/when validator-options-provided
  (let [{:keys [options]}
        (any-page-scenario
          {:base-url     "https://example.com"
           :query-params {:pick "spinach"
                          :sort "wat"}
           :resource-definition
           {:validator-options
            {:problem-subject "events-resource"
             :problem-transformer
             (fn [problem]
               (select-keys problem [:subject :field :requirements]))}}})]
    (behaviours/responds-with-status 422 options)
    (behaviours/includes-properties-on-resource
      [:errorId :errorContext]
      {:errorId some?
       :errorContext
       [{:subject      "events-resource"
         :field        ["params" "pick"]
         :requirements ["must-be-a-positive-number"]}
        {:subject      "events-resource"
         :field        ["params" "sort"]
         :requirements ["must-be-a-valid-sort-order"]}]}
      options)
    (behaviours/includes-link-on-resource :discovery
      "https://example.com/"
      options)
    (behaviours/includes-link-on-resource :self
      (str "https://example.com/events?pick=spinach&sort=wat")
      options)
    (behaviours/does-not-include-link-on-resource :first options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)
    (behaviours/does-not-include-embedded-resources-on-resource :events
      options)))

(def sort-order-string?
  #{"ASC" "DESC"})

(sd/extend-pred-with-requirement
  'liberator.resource.events.collection-validation-test/sort-order-string?
  :must-be-ASC-or-DESC)

(spec/def ::sort sort-order-string?)

(spec/def ::params
  (spec/keys :opt-un [::sort]))

(spec/def ::request
  (spec/keys :req-un [::params]))

(behaviours/when validator-spec-provided
  (let [{:keys [options]}
        (any-page-scenario
          {:base-url            "https://example.com"
           :query-params        {:sort "asc"}
           :resource-definition {:validator-spec (constantly ::request)}})]
    (behaviours/responds-with-status 422 options)
    (behaviours/includes-properties-on-resource
      [:errorId :errorContext]
      {:errorId some?
       :errorContext
       [{:field        ["params" "sort"]
         :requirements ["must-be-ASC-or-DESC"]
         :subject      "request"
         :type         "invalid"}]}
      options)
    (behaviours/includes-link-on-resource :discovery
      "https://example.com/"
      options)
    (behaviours/includes-link-on-resource :self
      (str "https://example.com/events?sort=asc")
      options)
    (behaviours/does-not-include-link-on-resource :first options)
    (behaviours/does-not-include-link-on-resource :next options)
    (behaviours/does-not-include-link-on-resource :previous options)
    (behaviours/does-not-include-embedded-resources-on-resource :events
      options)))

(comment
  (find-tests *ns*)

  (run-tests
    [(ns-resolve *ns*
       'ns/test)])

  (run-tests
    (find-tests *ns*)
    {:report report}))
