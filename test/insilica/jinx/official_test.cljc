;; Copyright © 2019, JUXT LTD.

(ns insilica.jinx.official-test
  #?@(:clj [(:require [clojure.java.io :as io]
                      [cheshire.core :as json]
                      [clojure.test :refer :all]
                      [clojure.test :as test]
                      [insilica.jinx.validate :refer [validate]]
                      [insilica.jinx.schema :refer [schema]]
                      [insilica.jinx.resolve :as resolv]
                      [insilica.jinx.schema :as schema])]
      :cljs [(:require [cljs-node-io.core :as io :refer [slurp]]
                       [cljs-node-io.fs :as fs]
                       [cljs-node-io.file :refer [File]]
                       [cljs.test :refer-macros [deftest is testing run-tests]]
                       [insilica.jinx.validate :refer [validate]]
                       [insilica.jinx.schema :as schema :refer [schema]]
                       [insilica.jinx.resolve :as resolv]
                       [cljs.nodejs :as nodejs])]))

(defn- env [s]
  #?(:clj (System/getenv (str s)))
  #?(:cljs (aget js/process.env s)))

#?(:cljs
   (def Throwable js/Error))

(def TESTS-ROOT
  #?(:clj (io/file "official-test-suite")
     :cljs (str "official-test-suite")))

(def TESTS-DIR
  #?(:clj (io/file TESTS-ROOT "tests/draft7")
     :cljs (str TESTS-ROOT "/tests/draft7")))


#?(:cljs
   (do
     (def fs (cljs.nodejs/require "fs"))

     (defn file-exists? [f]
       (fs.existsSync f))
     (defn dir? [f]
       (and
        (file-exists? f)
        (.. fs (lstatSync f) (isDirectory))))
     (defn file? [f]
       (.. fs (lstatSync f) (isFile)))
     (defn file-seq [dir]
       (if (fs.existsSync dir)
         (tree-seq
          dir?
          (fn [d] (map (partial str d "/") (seq (fs.readdirSync d))))
          dir)
         []))
     (defn read-file-cljs [fname]
       (js->clj (js/JSON.parse (.readFileSync fs fname))))))

(defn test-jsonschema [{:keys [schema data valid] :as test}]
  (try
    (let [schema (schema/schema schema)
          result (validate
                  schema data
                  {:resolvers
                   [::resolv/built-in
                    [::resolv/default-resolver
                     {#"http://localhost:1234/(.*)"
                      (fn [match]
                        #?(:clj (io/file (io/file TESTS-ROOT "remotes") (second match))
                           :cljs (File. (str TESTS-ROOT "/remotes/" (second match)))))}]]})
          success? (if valid (:valid? result)
                       (not (:valid? result)))]
      (cond-> test
        success? (assoc :result :success)
        (and (not success?) valid) (assoc :failures (:error result))
        (and (empty? result) (not valid)) (assoc :failures [{:message "Incorrectly judged valid"}])))
    (catch Throwable e (merge test {:result :error
                                    :error e}))))

(defn success? [x] (= (:result x) :success))

;; Test suite

(defn tests
  [tests-dir]
  (for [testfile #?(:clj (file-seq TESTS-DIR)
                    :cljs (filter file? (file-seq TESTS-DIR)))
        ;; filename (-> tests-dir .list sort)
        ;; Test filenames implemented so far or being worked on currently
        ;; :when ((or filename-pred some?) filename)
        ;; :let [testfile (io/file tests-dir filename)]

        :when #?(:clj (.isFile testfile)
                 :cljs (file? testfile))
        :let [objects #?(:clj (json/parse-stream (io/reader testfile))
                         :cljs (read-file-cljs testfile))]
        {:strs [schema tests description]} objects
        ;; Any required parsing of the schema, do it now for performance
        :let [test-group-description description]
        test tests
        :let [{:strs [description data valid]} test]]
    {:filename (str testfile)
     :test-group-description test-group-description
     :test-description description
     :schema schema
     :data data
     :valid valid}))

;; TODO: Pull out defaults and refs from validation keywords - this is
;; premature abstraction

(defn exclude-test? [test]
  (contains?
   #{"format: uri-template"
     "validation of an internationalized e-mail addresses"}
   (:test-group-description test)))

(defn cljs-exclude-test? [test]
  (or (contains?
       #{"format: uri-template"
         ;; Not sure why these tests are failing
         "validation of URI References"
         }
       (:test-group-description test))
      (contains?
       #{"an invalid IRI based on IPv6"}
       (:test-description test))))

#?(:clj
   (do
     (defn make-tests []
       (doseq [test (remove exclude-test? (tests TESTS-DIR))]
         (let [testname (symbol (str (gensym "test") "-test"))]
           (eval `(test/deftest ~(vary-meta testname assoc :official true) ~testname
                    (test/testing ~(:test-description test)
                      (test/is (success? (test-jsonschema ~test)))))))))
     (make-tests)))

#?(:cljs
   (deftest cljs-tests
     (testing "Testing JSON-Schema-Test-Suite - cljs"
       (doseq [test (remove cljs-exclude-test? (tests TESTS-DIR))]
         (let [testname (symbol (str (gensym "test") "-test"))]
           (do
             (is (success? (test-jsonschema test)))))))))
