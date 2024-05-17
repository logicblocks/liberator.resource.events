(ns liberator.resource.events.test-support.handlers
  (:require
   [ring.middleware.keyword-params :as ring-keyword-params]
   [ring.middleware.params :as ring-params]

   [liberator.resource.events.collection :as events-resource]))

(def default-router
  [""
   [["/" :discovery]
    ["/events" :events]
    [["/events/" :event-id] :event]]])

(defn resource-handler
  ([dependencies] (resource-handler dependencies {}))
  ([dependencies overrides]
   (let [handler (events-resource/handler dependencies overrides)
         handler (-> handler
                   ring-keyword-params/wrap-keyword-params
                   ring-params/wrap-params)]
     handler)))
