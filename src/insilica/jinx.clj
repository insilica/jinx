;; Copyright © 2019, JUXT LTD.

(ns insilica.jinx
  (:require
   [insilica.jinx.schema :as schema]
   [insilica.jinx.validate :as validate]
   [insilica.jinx.clj-transform :as transform]))

(defn schema
  "Build a JSON Schema from a map (or boolean). Must conform to
  rules. Returns a map that can be used in validation."
  ([s] (schema/schema s))
  ([s options] (schema/schema s options)))

(defn validate
  "Validate a map (or boolean) according to the given schema."
  ([schema instance] (validate/validate schema instance))
  ([schema instance options] (validate/validate schema instance options)))

(defn ^:jinx/experimental clj->jsch
  "Transform a Clojure syntax shorthand into JSON Schema and build it."
  [clj]
  (schema/schema (transform/clj->jsch clj)))
