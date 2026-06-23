;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.rpc.effective-permissions
  "Read-permission helpers that augment normal Penpot membership with
  Nitrate organization-owner viewer access.

  Edit/admin permission providers intentionally stay membership-only."
  (:require
   [app.binfile.common :as bfc]
   [app.db :as db]
   [app.nitrate :as nitrate]))

(def viewer-role-flags
  "Role flags granted to a non-member organization owner: read-only.
  Shared so callers that build full team/file rows shape permissions the
  same way the permission lookups do."
  {:is-owner false
   :is-admin false
   :can-edit false})

(def ^:private team-viewer-permissions
  (assoc viewer-role-flags :can-read true))

(defn- file-viewer-permissions
  [profile-id]
  (assoc team-viewer-permissions
         :type :membership
         :is-logged (some? profile-id)))

(def ^:private sql:team-id-for-project
  "SELECT team_id FROM project WHERE id = ?")

(def ^:private sql:team-id-for-file
  "SELECT p.team_id
     FROM file AS f
     JOIN project AS p ON (p.id = f.project_id)
    WHERE f.id = ?")

(defn team-id-for-project
  [cfg project-id]
  (some-> (db/exec-one! cfg [sql:team-id-for-project project-id])
          (:team-id)))

(defn team-id-for-file
  [cfg file-id]
  (some-> (db/exec-one! cfg [sql:team-id-for-file file-id])
          (:team-id)))

(defn resolve-team-id
  [cfg {:keys [team-id project-id file-id]}]
  (cond
    (some? team-id)    team-id
    (some? project-id) (team-id-for-project cfg project-id)
    (some? file-id)    (team-id-for-file cfg file-id)))

(defn org-owner-team-permissions
  [cfg profile-id team-id]
  (when (nitrate/org-owner-of-team? cfg profile-id team-id)
    team-viewer-permissions))

(defn- org-owner-file-permissions
  [cfg profile-id file-id]
  (when-let [team-id (team-id-for-file cfg file-id)]
    (when (nitrate/org-owner-of-team? cfg profile-id team-id)
      (file-viewer-permissions profile-id))))

(defn get-file-read-permissions
  ([cfg profile-id file-id]
   (or (bfc/get-file-permissions cfg profile-id file-id)
       (org-owner-file-permissions cfg profile-id file-id)))

  ([cfg profile-id file-id share-id]
   (or (bfc/get-file-permissions cfg profile-id file-id share-id)
       (org-owner-file-permissions cfg profile-id file-id))))
