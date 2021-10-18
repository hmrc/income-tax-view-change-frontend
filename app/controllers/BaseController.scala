/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import javax.inject.Inject
import play.api.http.HeaderNames
import play.api.mvc.{MessagesControllerComponents, RequestHeader, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseController @Inject()(implicit mcc: MessagesControllerComponents) extends FrontendController(mcc) {

  implicit val executionContext: ExecutionContext

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = {
    rh.headers.headers.find(_._1 == HeaderNames.REFERER) match {
      case Some(referrer) => super.hc.withExtraHeaders(referrer)
      case _ => super.hc
    }
  }
  def redirectToHome: Result = Redirect(controllers.routes.HomeController.home())
  def fRedirectToHome: Future[Result] = Future.successful(redirectToHome)
}