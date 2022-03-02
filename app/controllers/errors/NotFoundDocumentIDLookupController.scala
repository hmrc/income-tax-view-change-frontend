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

package controllers.errors

import com.google.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.errorPages.CustomNotFoundError

import scala.concurrent.Future

@Singleton
class NotFoundDocumentIDLookupController @Inject()(customNotFoundError: CustomNotFoundError)
                                                  (implicit mcc: MessagesControllerComponents) extends FrontendController(mcc) {
  val show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(customNotFoundError()))
  }
}
