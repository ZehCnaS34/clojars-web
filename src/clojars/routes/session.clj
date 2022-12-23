(ns clojars.routes.session
  (:require
   [cemerick.friend :as friend]
   [clojars.web.login :as view]
   [compojure.core :refer [GET ANY defroutes]]
   [ring.util.response :as response]))

(defroutes routes
  (GET "/login" {:keys [flash params]}
       (let [{:keys [login_failed username]} params]
         (view/login-form login_failed username flash)))
  (friend/logout
   (ANY "/logout" _ (response/redirect "/"))))
