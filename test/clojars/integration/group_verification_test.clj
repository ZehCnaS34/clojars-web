(ns clojars.integration.group-verification-test
  (:require
   [clj-http.client :as http]
   [clojars.integration.steps :refer [register-as]]
   [clojars.test-helper :as help]
   [clojure.test :refer [deftest use-fixtures]]
   [kerodon.core :refer [fill-in follow-redirect
                         press session visit within]]
   [kerodon.test :refer [has some-text?]]))

(use-fixtures :each
  help/default-fixture
  help/with-clean-database)

(deftest user-can-verify-group
  (help/with-TXT ["clojars dantheman"]
    (-> (session (help/app))
        (register-as "dantheman" "test@example.org" "password")
        (follow-redirect)
        (visit "/verify/group")
        (within [:div.via-txt]
          (fill-in "Group name" "com.example")
          (fill-in "Domain with TXT record" "example.com")
          (press "Verify Group"))
        (follow-redirect)
        (within [:div.info]
          (has (some-text? "The group 'com.example' has been verified"))))))

(deftest user-cannot-verify-non-corresponding-group
  (help/with-TXT ["clojars dantheman"]
    (-> (session (help/app))
        (register-as "dantheman" "test@example.org" "password")
        (follow-redirect)
        (visit "/verify/group")
        (within [:div.via-txt]
          (fill-in "Group name" "com.example")
          (fill-in "Domain with TXT record" "example.org")
          (press "Verify Group"))
        (follow-redirect)
        (within [:div.error]
          (has (some-text? "Group and domain do not correspond with each other"))))))

(deftest user-can-verify-sub-group
  (help/with-TXT ["clojars dantheman"]
    (-> (session (help/app))
        (register-as "dantheman" "test@example.org" "password")
        (follow-redirect)
        (visit "/verify/group")
        (within [:div.via-txt]
          (fill-in "Group name" "com.example")
          (fill-in "Domain with TXT record" "example.com")
          (press "Verify Group"))
        (follow-redirect)
        (within [:div.info]
          (has (some-text? "The group 'com.example' has been verified")))

        (within [:div.via-parent]
          (fill-in "Group name" "com.example.ham")
          (press "Verify Group"))
        (follow-redirect)
        (within [:div.info]
          (has (some-text? "The group 'com.example.ham' has been verified"))))))

(deftest user-cannot-verify-subgroup-with-non-verified-parent
  (-> (session (help/app))
      (register-as "dantheman" "test@example.org" "password")
      (follow-redirect)
      (visit "/verify/group")
      (within [:div.via-parent]
        (fill-in "Group name" "com.example")
        (press "Verify Group"))
      (follow-redirect)
      (within [:div.error]
        (has (some-text? "The group is not a subgroup of a verified group")))))

(deftest user-can-verify-vcs-groups
  (with-redefs [http/head (constantly {:status 200})]
    (-> (session (help/app))
        (register-as "dantheman" "test@example.org" "password")
        (follow-redirect)
        (visit "/verify/group")
        (within [:div.via-vcs]
          (fill-in "Verification Repository URL"
                   "https://github.com/example/clojars-dantheman")
          (press "Verify Groups"))
        (follow-redirect)
        (within [:div.info]
          (has (some-text? "The groups 'com.github.example' & 'net.github.example' have been verified"))))))

(deftest user-cannot-verify-vcs-groups-with-missing-repo
  (with-redefs [http/head (constantly {:status 404})]
    (-> (session (help/app))
        (register-as "dantheman" "test@example.org" "password")
        (follow-redirect)
        (visit "/verify/group")
        (within [:div.via-vcs]
          (fill-in "Verification Repository URL"
                   "https://github.com/example/clojars-dantheman")
          (press "Verify Groups"))
        (follow-redirect)
        (within [:div.error]
          (has (some-text? "The verification repo does not exist"))))))
