;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns common-tests.types.file-test
  (:require
   [app.common.files.tokens :as cfo]
   [app.common.test-helpers.files :as thf]
   [app.common.test-helpers.ids-map :as thi]
   [app.common.test-helpers.tokens :as tht]
   [app.common.types.file :as ctf]
   [app.common.types.tokens-lib :as ctob]
   [app.common.types.tokens-status :as ctos]
   [clojure.test :as t]))

(t/deftest test-ensure-tokens-lib
  (t/testing "ensure-tokens-lib should add a tokens-lib and tokens-status to the file data if they are missing, and should not modify them if they already exist"
    (let [file       (thf/sample-file :file1)
          file-data  (ctf/file-data file)
          file-data' (cfo/ensure-tokens-lib file-data)]
      (t/is (contains? file-data' :tokens-lib))
      (t/is (ctob/tokens-lib? (:tokens-lib file-data')))
      (t/is (contains? file-data' :tokens-status))
      (t/is (ctos/tokens-status? (:tokens-status file-data'))))))

(t/deftest test-update-tokens-lib
  (t/testing "update when there is no tokens-lib has no effect"
    (let [file (thf/sample-file :file1)
          file' (ctf/update-tokens-lib file #(t/is false "This should not be called"))]
      (t/is (= file file'))))

  (t/testing "update a tokens-lib applies the changes correctly"
    (let [file (-> (thf/sample-file :file1)
                   (tht/add-tokens-lib))
          file' (ctf/update-file-data file
                                      #(ctf/update-tokens-lib
                                        %
                                        ctob/add-theme
                                        (ctob/make-token-theme :id (thi/new-id! :theme1)
                                                               :name "theme 1")))

          tokens-lib' (tht/get-tokens-lib file')]
      (t/is (= 2 (ctob/theme-count tokens-lib')))  ;; Count the hidden theme
      (t/is (ctob/token-theme? (ctob/get-theme tokens-lib' (thi/id :theme1)))))))

(t/deftest test-update-tokens-status
  (t/testing "update when there is no tokens-status has no effect"
    (let [file (thf/sample-file :file1)
          file' (ctf/update-tokens-status file #(t/is false "This should not be called"))]
      (t/is (= file file'))))

  (t/testing "update a tokens-status applies the changes correctly"
    (let [file (-> (thf/sample-file :file1)
                   (tht/add-tokens-lib)
                   (tht/update-tokens-lib
                    (fn [tokens-lib]
                      (-> tokens-lib
                          (ctob/add-theme (ctob/make-token-theme :id (thi/new-id! :theme1) :name "theme"))))))
          file' (ctf/update-file-data file
                                      #(ctf/update-tokens-status
                                        %
                                        ctos/set-tokens-status
                                        #{(thi/id :theme1)}
                                        #{}))
          tokens-status' (tht/get-tokens-status file')]
      (t/is (= 1 (count (ctos/get-active-theme-ids tokens-status'))))
      (t/is (ctos/theme-active? tokens-status' (thi/id :theme1))))))