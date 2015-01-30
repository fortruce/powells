(ns powells.core
  (:require [net.cgrand.enlive-html :as ehtml]
            [clojure.string :as str]
            [clj-http.client :as client]
            [clojure.zip :refer [xml-zip]]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :refer [xml1-> xml-> text]]
            [slingshot.slingshot :refer [try+]]))

(def api-key "QqL7sEKSlr3hzRwsC9Hdgg")

(defn get-isbn
  "Returns isbn from a powell url."
  [powell-url]
  (re-find #"\d{13}"
           (last (str/split powell-url #"/"))))

(defn fetch-isbns
  "Returns a list of unique isbns from powells 2014 staff list."
  []
  (let [url "http://www.powells.com/staff-top-5s-2014"
        html (ehtml/html-resource (java.net.URL. url))
        books (ehtml/select html [:div.employee_box :a])]
    (->> books
         (remove #(empty? (ehtml/text %)))
         (map #(get-in % [:attrs :href]))
         (map get-isbn)
         distinct)))

(defn gr-isbn
  "Return xml response from Goodreads API for isbn."
  [isbn]
  (when-let [req (try+ (client/get "http://www.goodreads.com/book/isbn"
                                   {:query-params {:format "xml"
                                                   :key api-key
                                                   :isbn isbn}})
                       (catch [:status 404] _ nil))]
   (xml/parse-str (:body req))))

(defn get-info [isbn]
  (when-let [gr-xml (gr-isbn isbn)]
    (let [info (xml-zip gr-xml)
          book (xml1-> info :book)]
      {:title (text (xml1-> book :title))
       :image-url (text (xml1-> book :image_url))
       :num-pages (text (xml1-> book :num_pages))
       :avg-rating (text (xml1-> book :average_rating))
       :authors (map text (xml-> book :authors :author :name))
       :isbn isbn})))
       
(defn- main []
  (let [books (fetch-isbns)]
    (map get-info books)))

