(ns liberator.resource.events.core
  (:require
   [halboy.resource :as hal]
   [hype.core :as hype]
   [liberator.mixin.core :as mixin]
   [liberator.mixin.json.core :as json-mixin]
   [liberator.mixin.hypermedia.core :as hypermedia-mixin]
   [liberator.mixin.hal.core :as hal-mixin]))

(defprotocol EventLoader
  (query [loader context])
  (before? [loader context])
  (after? [loader context]))

(defrecord FnBackedEventLoader [fns]
  EventLoader
  (query [_ context]
    ((:query fns) context))
  (before? [_ context]
    ((get fns :before? (constantly true)) context))
  (after? [_ context]
    ((get fns :after? (constantly true)) context)))

(defn ->event-loader [fns]
  (->FnBackedEventLoader fns))

(defn default-event-link-fn [{:keys [request router]} event params]
  {:href
   (hype/absolute-url-for request router :event
     (merge params
       {:path-params {:event-id (:id event)}}))})

(defn default-events-link-fn [{:keys [request router]} params]
  {:href
   (hype/absolute-url-for request router :events params)})

(defn default-event-loader [_]
  (->event-loader {:query (fn [_] [])}))

(def default-events-to-pick 10)

(defn default-event-transformer
  [{:keys [resource] :as context} event]
  (let [event-link-fn (:event-link resource)
        event-link (event-link-fn context event {})
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

(defn- self-link [{:keys [resource] :as context}]
  (let [events-link-fn (:events-link resource)
        self-link (events-link-fn context {})]
    self-link))

(defn- first-link [{:keys [resource] :as context}]
  (let [events-link-fn (:events-link resource)
        events-link (events-link-fn context {})]
    events-link))

(defn- next-link [{:keys [resource] :as context} events]
  (let [last-event (last events)
        last-event-id (:id last-event)
        events-link-fn (:events-link resource)
        events-link (events-link-fn context
                      {:query-params {:since last-event-id}})]
    events-link))

(defn- previous-link [{:keys [resource] :as context} events]
  (let [first-event (first events)
        first-event-id (:id first-event)
        events-link-fn (:events-link resource)
        events-link (events-link-fn context
                      {:query-params {:preceding first-event-id}})]
    events-link))

(defn definitions
  ([_]
   {:event-loader              default-event-loader
    :event-transformer         default-event-transformer
    :events-to-pick-by-default default-events-to-pick

    :event-link                default-event-link-fn
    :events-link               default-events-link-fn

    :self-link                 (fn [{:keys [resource] :as context}]
                                 (let [events-link-fn (:events-link resource)
                                       events-link (events-link-fn context {})]
                                   events-link))

    :handle-ok
    (fn [{:keys [resource] :as context}]
      (let [event-loader-fn (:event-loader resource)
            event-loader (event-loader-fn)
            event-transformer-fn (:event-transformer resource)
            event-transformer (partial event-transformer-fn context)

            events (query event-loader context)
            before? (before? event-loader context)
            after? (after? event-loader context)

            event-resources (mapv event-transformer events)
            events-resource
            (-> (hal/new-resource (self-link context))
              (hal/add-href :first (first-link context))
              (hal/add-resource :events event-resources))
            events-resource
            (if after?
              (hal/add-href events-resource
                :next (next-link context events))
              events-resource)
            events-resource
            (if before?
              (hal/add-href events-resource
                :previous (previous-link context events))
              events-resource)]
        events-resource))}))

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
