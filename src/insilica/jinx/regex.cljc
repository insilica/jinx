;; Copyright © 2019, JUXT LTD.

(ns insilica.jinx.regex
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [insilica.jinx.patterns :as patterns]))

(def addr-spec patterns/addr-spec)
(comment
  (re-matches addr-spec "mal@juxt.pro"))

(def iaddr-spec patterns/iaddr-spec)

(defn hostname? [s]
  (and
   (re-matches patterns/subdomain s)
   ;; "Labels must be 63 characters or less." -- RFC 1034, Section 3.5
   (<= (apply max (map count (str/split s #"\."))) 63)
   ;; "To simplify implementations, the total number of octets that
   ;; represent a domain name (i.e., the sum of all label octets and
   ;; label lengths) is limited to 255." -- RFC 1034, Section 3.1
   (<= (count s) 255)))


(defn idn-hostname? [s]
  (when-let [ace #?(:bb (throw (Exception. "idn-hostname support is not implemented in babashka"))
                    :clj (try
                           (java.net.IDN/toASCII s)
                           (catch IllegalArgumentException e
                             ;; Catch an error indicating this is not valid
                             ;; idn-hostname
                             ))
                    :cljs s)]
    (and
     ;; Ensure no illegal chars
     (empty? (set/intersection (set (seq s))
                               #{\u302E ; Hangul single dot tone mark
                                 }))
     ;; Ensure ASCII version is a valid hostname
     (hostname? ace))))

(def IPv4address patterns/IPv4address)
(def IPv6address patterns/IPv6address)

(def URI patterns/URI)
(def relative-ref patterns/relative-ref)

(def IRI patterns/IRI)
(def irelative-ref patterns/irelative-ref)

(def json-pointer patterns/json-pointer)
(def relative-json-pointer patterns/relative-json-pointer)
