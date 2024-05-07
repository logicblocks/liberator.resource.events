(ns liberator.resource.events.spec
  (:require
   [clojure.spec.alpha :as spec]))

(spec/def :liberator.resource.events.params/sort
  #{"asc" "ascending" "desc" "descending"})

(spec/def :liberator.resource.events.requests.get/params
  (spec/keys :opt-un [:liberator.resource.events.params/sort]))

(spec/def :liberator.resource.events.requests.get/request
  (spec/keys :req-un [:liberator.resource.events.requests.get/params]))
