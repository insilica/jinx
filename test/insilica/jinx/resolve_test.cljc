;; Copyright © 2019, JUXT LTD.

(ns insilica.jinx.resolve-test
  #?@(:clj
      [(:require
        [insilica.jinx.resolve :refer [resolve-uri expand-document]]
        [clojure.string :as str]
        [clojure.edn :as edn]
        [cheshire.core :as cheshire]
        [clojure.java.io :as io]
        [lambdaisland.uri :as uri]
        [clojure.test :refer [deftest is are testing]])]
      :cljs
      [(:require
        [insilica.jinx.resolve :refer [resolve-uri]]
        [clojure.string :as str]
        [cljs-node-io.file :refer [File]]
        [lambdaisland.uri :as uri]
        [cljs.test :refer-macros [deftest is are testing run-tests]])
       (:import goog.Uri)]))

(comment
  :resolvers [[:insilica.jinx.resolve/default-resolver {"http://example.com/foo" (io/resource "schemas/json-schema.org/draft-07/schema")}]
              :insilica.jinx.resolve/built-in])

(deftest built-in-resolver-test
  (is
   (resolve-uri :insilica.jinx.resolve/built-in "http://json-schema.org/draft-07/schema")))


(def example-map
  #?(:clj
     {"http://example.com/test" (io/resource "insilica/jinx/test.json")
      "http://example.com/literal-boolean-schema" false
      "http://example.com/literal-object-schema" {:type "string"}
      "http://example.com/literal-function-schema"
      (fn [_] {:type "string"
               :uri "http://example.com/literal-function-schema"})
      #"http://example.com/static/(.*)" {:type "object"}
      #"http://example.com/schemas/(.*)" (fn [match] {:type "object"
                                                      :path (second match)})}
     :cljs
     {"http://example.com/test" (File. "test/insilica/jinx/test.json")
      "http://example.com/literal-boolean-schema" false
      "http://example.com/literal-object-schema" {:type "string"}
      "http://example.com/literal-function-schema"
      (fn [_] {:type "string"
               :uri "http://example.com/literal-function-schema"})
      #"http://example.com/static/(.*)" {:type "object"}
      #"http://example.com/schemas/(.*)" (fn [match] {:type "object"
                                                      :path (second match)})}))


(deftest default-resolver-test
  (let [m example-map]
    (testing "literal-to-resource"
      (is
       (=
        {"foo" "bar"}
        (resolve-uri
         [:insilica.jinx.resolve/default-resolver m]
         "http://example.com/test"))))

    (testing "literal-to-schema"
      (is
       (=
        false
        (resolve-uri
         [:insilica.jinx.resolve/default-resolver m]
         "http://example.com/literal-boolean-schema"))))

    (testing "literal-to-object"
      (is
       (=
        {:type "string"}
        (resolve-uri
         [:insilica.jinx.resolve/default-resolver m]
         "http://example.com/literal-object-schema"))))

    (testing "literal-to-function"
      (is
       (=
        {:type "string"
         :uri "http://example.com/literal-function-schema"}
        (resolve-uri
         [:insilica.jinx.resolve/default-resolver m]
         "http://example.com/literal-function-schema"))))

    (testing "regex-to-constant"
      (is
       (=
        {:type "object"}
        (resolve-uri
         [:insilica.jinx.resolve/default-resolver example-map]
         "http://example.com/static/schema.json"))))

    (testing "regex-to-function"
      (is
       (=
        {:type "object", :path "schema1.json"}
        (resolve-uri
         [:insilica.jinx.resolve/default-resolver example-map]
         "http://example.com/schemas/schema1.json"))))))


#?(:clj
   (deftest document-expansion-test
     (let [expansion
           (expand-document
            (edn/read-string (slurp (io/resource "insilica/jinx/petstore.edn")))
            {})]
       (is
        (=
         "string"
         (get-in expansion ["paths" "/pets" "get" "responses"
                            "200" "content" "application/json"
                            "schema" "items" "properties" "name" "type"]))))))
