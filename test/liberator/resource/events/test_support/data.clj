(ns liberator.resource.events.test-support.data
  (:require
    [tick.core :as time]
    [faker.lorem :as lorem]))

(defn random-uuid-string []
  (str (random-uuid)))

(defn random-event-type []
  (rand-nth [:event-type-1 :event-type-2 :event-type-3]))

(defn random-event-category []
  (rand-nth [:event-category-1 :event-category-2 :event-category-3]))

(defn random-url []
  (let [words (take 2 (lorem/words))
        id (random-uuid-string)]
    (format "https://%s.com/%s/%s"
      (first words) (last words) id)))

(defn random-datetime []
  (time/now))

(defn random-event
  ([] (random-event {}))
  ([overrides]
   (merge
     {:id (random-uuid-string)
      :type (random-event-type)
      :stream (random-uuid-string)
      :category (random-event-category)
      :payload {:id (random-uuid-string)
                :parent (random-uuid-string)
                :child (random-uuid-string)}
      :creator (random-url)
      :observed-at (random-datetime)
      :occurred-at (random-datetime)}
     overrides)))
