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

(defn- resource-attribute-as-value [{:keys [resource] :as context} attribute]
  (let [attribute-fn (attribute resource)]
    (attribute-fn context)))

(defn- resource-attribute-as-fn [{:keys [resource] :as context} attribute]
  (let [attribute-fn (attribute resource)]
    (partial attribute-fn context)))

(defn propagated-query-params [{:keys [request] :as context}]
  (let [allowed-query-params
        (resource-attribute-as-value context :allowed-query-params)
        allowed-query-params (map name allowed-query-params)
        request-query-params (:query-params request)]
    (select-keys request-query-params allowed-query-params)))

(defn excluding-query-params [query-params exclusions]
  (apply dissoc query-params (map name exclusions)))

(defn events-link
  ([context]
   (events-link context {}))
  ([context params]
   (events-link context params {}))
  ([context params options]
   (let [query-params
         (propagated-query-params context)
         query-params
         (excluding-query-params query-params
           (:query-param-exclusions options))
         events-link-fn (resource-attribute-as-fn context :events-link)]
     (events-link-fn
       (merge params
         {:query-params
          (merge query-params (:query-params params))})))))

(defn- self-link [context]
  (events-link context))

(defn- first-link [context]
  (events-link context {}
    {:query-param-exclusions #{:since :preceding}}))

(defn- next-link [context events]
  (let [last-event (last events)
        last-event-id (:id last-event)]
    (events-link context
      {:query-params {:since last-event-id}}
      {:query-param-exclusions #{:preceding}})))

(defn- previous-link [context events]
  (let [first-event (first events)
        first-event-id (:id first-event)]
    (events-link context
      {:query-params {:preceding first-event-id}}
      {:query-param-exclusions #{:since}})))

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

(def default-self-link-fn self-link)

(defn default-event-loader [_]
  (->event-loader {:query (fn [_] [])}))

(defn default-event-transformer
  [context event]
  (let [event-link (resource-attribute-as-fn context :event-link)
        resource (hal/new-resource (event-link event {}))
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

(def default-allowed-query-params #{:pick :since :preceding :sort})

(defn definitions
  ([_]
   {:event-loader              default-event-loader
    :event-transformer         default-event-transformer
    :events-to-pick-by-default default-events-to-pick

    :event-link                default-event-link-fn
    :events-link               default-events-link-fn

    :allowed-query-params      default-allowed-query-params

    :self-link                 default-self-link-fn

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
