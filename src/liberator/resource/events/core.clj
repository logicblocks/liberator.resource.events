(ns liberator.resource.events.core
  (:require
   [halboy.resource :as hal]
   [hype.core :as hype]
   [liberator.mixin.core :as mixin]
   [liberator.mixin.json.core :as json-mixin]
   [liberator.mixin.hypermedia.core :as hypermedia-mixin]
   [liberator.mixin.hal.core :as hal-mixin]))

(defn- resource-attribute-as-value [{:keys [resource] :as context} attribute]
  (let [attribute-fn (attribute resource)]
    (attribute-fn context)))

(defn- resource-attribute-as-fn [{:keys [resource] :as context} attribute]
  (let [attribute-fn (attribute resource)]
    (partial attribute-fn context)))

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

(def default-events-to-pick 10)

(def default-allowed-query-params #{:pick :since})

(defn propagated-query-params [{:keys [request] :as context}]
  (let [allowed-query-params
        (resource-attribute-as-value context :allowed-query-params)
        allowed-query-params (map name allowed-query-params)
        request-query-params (:query-params request)]
    (select-keys request-query-params allowed-query-params)))

(defn excluding-query-params [query-params exclusions]
  (apply dissoc query-params (map name exclusions)))

(defn- self-link [context]
  (let [query-params (propagated-query-params context)
        events-link-fn (resource-attribute-as-fn context :events-link)
        self-link (events-link-fn {:query-params query-params})]
    self-link))

(defn- first-link [context]
  (let [query-params (excluding-query-params
                       (propagated-query-params context)
                       #{:since})
        events-link-fn (resource-attribute-as-fn context :events-link)
        events-link (events-link-fn {:query-params query-params})]
    events-link))

(defn- next-link [context events]
  (let [last-event (last events)
        last-event-id (:id last-event)
        query-params (merge (propagated-query-params context)
                       {:since last-event-id})
        events-link-fn (resource-attribute-as-fn context :events-link)
        events-link (events-link-fn {:query-params query-params})]
    events-link))

(defn- previous-link [context events]
  (let [first-event (first events)
        first-event-id (:id first-event)
        query-params (-> (propagated-query-params context)
                       (excluding-query-params #{:since})
                       (merge {:preceding first-event-id}))
        events-link-fn (resource-attribute-as-fn context :events-link)
        events-link (events-link-fn {:query-params query-params})]
    events-link))

(defn definitions
  ([_]
   {:event-loader              default-event-loader
    :event-transformer         default-event-transformer
    :events-to-pick-by-default default-events-to-pick

    :event-link                default-event-link-fn
    :events-link               default-events-link-fn

    :allowed-query-params      default-allowed-query-params

    :self-link
    (fn [context]
      (let [query-params (propagated-query-params context)
            events-link-fn
            (resource-attribute-as-fn context :events-link)
            events-link (events-link-fn {:query-params query-params})]
        events-link))

    :handle-ok
    (fn [context]
      (let [event-loader
            (resource-attribute-as-value context :event-loader)
            event-transformer
            (resource-attribute-as-fn context :event-transformer)

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
