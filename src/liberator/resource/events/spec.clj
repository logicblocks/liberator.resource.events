(ns liberator.resource.events.spec
  (:require
   [clojure.spec.alpha :as spec]
   [spec.definition.core :as sd]
   [spec.definition.number.core]
   [spec.definition.uuid.core]))

(def sort-order-string?
  #{"asc" "ascending" "desc" "descending"})

(sd/extend-pred-with-requirement
  'liberator.resource.events.spec/sort-order-string?
  :must-be-a-valid-sort-order)

(spec/def :liberator.resource.events.params/pick
  :datatype.number/positive-number)

(spec/def :liberator.resource.events.params/sort
  sort-order-string?)

(spec/def :liberator.resource.events.params/since
  :datatype.uuid/uuid-string)

(spec/def :liberator.resource.events.params/preceding
  :datatype.uuid/uuid-string)

(spec/def :liberator.resource.events.requests.get/params
  (spec/keys :opt-un [:liberator.resource.events.params/sort
                      :liberator.resource.events.params/pick
                      :liberator.resource.events.params/since
                      :liberator.resource.events.params/preceding]))

(spec/def :liberator.resource.events.requests.get/request
  (spec/keys :req-un [:liberator.resource.events.requests.get/params]))
