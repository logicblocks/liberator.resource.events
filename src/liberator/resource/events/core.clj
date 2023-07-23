(ns liberator.resource.events.core
  (:require
    [halboy.resource :as hal]
    [hype.core :as hype]
    [liberator.mixin.core :as mixin]
    [liberator.mixin.json.core :as json-mixin]
    [liberator.mixin.hypermedia.core :as hypermedia-mixin]
    [liberator.mixin.hal.core :as hal-mixin]))

(defn default-event-link-fn [{:keys [request router]} event]
  {:href
   (hype/absolute-url-for request router :event
     {:path-params {:event-id (:id event)}})})

(defn default-events-link-fn [{:keys [request router]}]
  {:href
   (hype/absolute-url-for request router :events)})

(defn default-event-loader [_] [])

(def default-events-to-pick 10)

(defn default-events-before-check [_ _]
  true)

(defn default-events-after-check [_ _]
  true)

(defn default-event-transformer
  [{:keys [resource] :as context} event]
  (let [event-link-fn (:event-link resource)
        event-link (event-link-fn context event)
        resource (hal/new-resource event-link)
        resource (hal/add-properties resource
                   (select-keys event
                     [:id
                      :type
                      :stream
                      :category
                      :creator
                      :observed-at
                      :occurred-at]))]
    resource))

(defn- first-link [{:keys [resource] :as context}]
  (let [events-link-fn (:events-link resource)
        first-link (events-link-fn context)]
    first-link))

(defn definitions
  ([_]
   {:event-loader              default-event-loader
    :event-transformer         default-event-transformer
    :events-to-pick-by-default default-events-to-pick
    :events-after?             default-events-after-check
    :events-before?            default-events-before-check

    :event-link                default-event-link-fn
    :events-link               default-events-link-fn

    :self-link                 (fn [{:keys [resource] :as context}]
                                 (let [events-link-fn (:events-link resource)
                                       events-link (events-link-fn context)]
                                   events-link))

    :handle-ok
    (fn [{:keys [resource] :as context}]
      (let [event-loader (:event-loader resource)
            events (event-loader context)

            event-transformer (:event-transformer resource)

            self-link-fn (:self-link resource)
            self-link (self-link-fn context)

            event-resources
            (mapv (partial event-transformer context) events)]
        (-> (hal/new-resource self-link)
          (hal/add-href :first (first-link context))
          (hal/add-resource :events event-resources))))}))

(defn handler
  ([dependencies]
   (handler dependencies {}))
  ([dependencies overrides]
   (mixin/build-resource
     (json-mixin/with-json-mixin dependencies)
     (hypermedia-mixin/with-hypermedia-mixin dependencies)
     (hal-mixin/with-hal-mixin dependencies)
     (definitions dependencies)
     overrides)))
