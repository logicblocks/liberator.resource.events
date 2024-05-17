(disable-warning
  {:linter       :constant-test
   :for-macro    'clojure.core/and
   :if-inside-macroexpansion-of
   #{'liberator.resource.events.test-support.behaviours/includes-link-on-resource}
   :within-depth 10})

(disable-warning
  {:linter       :constant-test
   :for-macro    'clojure.core/cond
   :if-inside-macroexpansion-of
   #{'liberator.resource.events.test-support.behaviours/includes-link-on-resource}
   :within-depth 10})

(disable-warning
  {:linter       :constant-test
   :for-macro    'clojure.core/if
   :if-inside-macroexpansion-of
   #{'liberator.resource.events.test-support.behaviours/includes-link-on-resource}
   :within-depth 10})