(ns liberator.resource.events.test-support.behaviours
  (:refer-clojure :exclude [when])
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.string :as string]

   [halboy.resource :as hal]
   [halboy.json :as hal-json]

   [ring.mock.request :as ring]

   [liberator.resource.events.test-support.handlers :as handlers]))

(def ^:dynamic *when* nil)

(defn fetch-events [{:keys [resource-definition router base-url query-params]
                     :or   {base-url     "https://example.com"
                            query-params {}
                            router       handlers/default-router}}]
  (let [handler (handlers/resource-handler
                  {:router router}
                  resource-definition)
        request (ring/request :get (str base-url "/events") query-params)]
    (handler request)))

(defn ->resource [response]
  (hal-json/json->resource (:body response)))

(defn get-resource-href [response rel]
  (hal/get-href (->resource response) rel))

(defn get-embedded-resource [response key]
  (hal/get-resource (->resource response) key))

(defn properties-description [property-keys]
  (let [property-key-names (map name property-keys)
        property-key-descriptions
        (if (= (count property-key-names) 1)
          (first property-key-names)
          (str
            (string/join "-" (butlast property-key-names)) "-and-"
            (last property-key-names)))
        description-suffix
        (if (= (count property-key-names) 1)
          "property"
          "properties")]
    (str property-key-descriptions "-" description-suffix)))

(defn test-name [& pieces]
  (let [suffix (if *when* (str "when-" (name *when*)))
        pieces (if *when* (concat pieces ["-" suffix]) pieces)]
    (symbol (apply str pieces))))

(defmacro when [scenario & body]
  `(do
     (alter-var-root #'*when* (constantly (quote ~scenario)))
     ~@body
     (alter-var-root #'*when* (constantly nil))))

(defmacro responds-with-status
  [status-code options]
  (let [name (test-name "responds-with-status-" status-code)]
    `(deftest ~name
       (let [response# (fetch-events ~options)]
         (is (= ~status-code (:status response#)))))))

(defmacro includes-link-on-resource
  [rel href options]
  (let [name (test-name "includes-" (name rel) "-link-on-resource")]
    `(deftest ~name
       (let [response# (fetch-events ~options)]
         (is (= ~href (get-resource-href response# ~rel)))))))

(defmacro does-not-include-link-on-resource
  [rel options]
  (let [name (test-name "does-not-include-" (name rel) "-link-on-resource")]
    `(deftest ~name
       (let [response# (fetch-events ~options)]
         (is (nil? (get-resource-href response# ~rel)))))))

(defmacro includes-embedded-resources-on-resource
  [embed-key count options]
  (let [name
        (test-name "embeds-" count "-" (name embed-key)
          "-resources-on-resource")]
    `(deftest ~name
       (let [response# (fetch-events ~options)]
         (is (= ~count
               (count (get-embedded-resource response# ~embed-key))))))))

(defmacro includes-links-on-embedded-resources
  [embed-key rel hrefs options]
  (let [name (test-name "includes-" (name rel)
               "-links-on-embedded-" (name embed-key) "-resources")]
    `(deftest ~name
       (let [response# (fetch-events ~options)
             event-resources# (get-embedded-resource response# ~embed-key)]
         (is (= ~hrefs
               (map
                 (fn [event-resource#]
                   (hal/get-href event-resource# ~rel))
                 event-resources#)))))))

(defmacro includes-properties-on-embedded-resources
  [embed-key property-keys values options]
  (let [name (test-name "includes-" (properties-description property-keys)
               "-on-embedded-event-resources")]
    `(deftest ~name
       (let [response# (fetch-events ~options)
             event-resources# (get-embedded-resource response# ~embed-key)]
         (is (= ~values
               (map (fn [event-resource#]
                      (select-keys
                        (hal/properties event-resource#)
                        ~property-keys))
                 event-resources#)))))))

(defmacro does-not-include-properties-on-embedded-resources
  [embed-key property-keys options]
  (let [name (test-name "does-not-include-"
               (properties-description property-keys)
               "-on-embedded-event-resources")]
    `(deftest ~name
       (let [response# (fetch-events ~options)
             event-resources# (get-embedded-resource response# ~embed-key)]
         (is (= (repeat (count event-resources#)
                  (repeat (count ~property-keys) nil))
               (map (fn [event-resource#]
                      (map
                        #(hal/get-property event-resource# %)
                        ~property-keys))
                 event-resources#)))))))
