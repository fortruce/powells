(ns powells.core
  (:require [powells.extractor :as ex]))

(defn- main []
  (let [books (ex/fetch-isbns)]
    (doall (pmap ex/info books))))

