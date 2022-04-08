/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.predicates

import auth.MtdItUserBase
import config.featureswitch.FeatureSwitching
import forms.utils.SessionKeys
import models.OriginEnum
import play.api.i18n.I18nSupport
import play.api.mvc.Result
import play.api.mvc.Results.Redirect

import scala.concurrent.Future

trait SaveOriginAndRedirect extends I18nSupport with FeatureSwitching {

  def redirectToHome: Result = Redirect(controllers.routes.HomeController.show())

  def saveOriginAndReturnToHomeWithoutQueryParams[A](request: MtdItUserBase[A], navBarFsDisabled: Boolean = true): Future[Result] = {
    val originStringOpt: Option[String] = request.getQueryString(SessionKeys.origin)
    val redirectToOriginalCall: Result = Redirect(request.path)

    if (navBarFsDisabled) {
      Future.successful(redirectToOriginalCall)
    } else {
      originStringOpt.fold[Future[Result]](ifEmpty = Future.successful(redirectToOriginalCall))(originString =>
        (OriginEnum(originString), request.session.get(SessionKeys.origin)) match {
          case (Some(originStringEnum), Some(sessionOrigin)) if originStringEnum.toString != sessionOrigin =>
            Future.successful(
              redirectToOriginalCall.removingFromSession("origin")(request).addingToSession(("origin", originStringEnum.toString))(request)
            )
          case (Some(originStringEnum), None) =>
            Future.successful(redirectToOriginalCall.addingToSession(("origin", originStringEnum.toString))(request))
          case _ => Future.successful(redirectToOriginalCall)
        }
      )
    }
  }

}
