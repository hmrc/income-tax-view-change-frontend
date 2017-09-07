/*
 * Copyright 2017 HM Revenue & Customs
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

package services
import javax.inject.{Inject, Singleton}

import auth.MtdItUser
import config.AppConfig
import connectors.ServiceInfoPartialConnector
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.twirl.api.Html
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials
import views.html.helpers.renderServiceInfoHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ServiceInfoPartialService @Inject()(implicit val appConfig: AppConfig,
                                          implicit val messagesApi: MessagesApi,
                                          val serviceInfoPartialConnector: ServiceInfoPartialConnector
                                         ) extends I18nSupport {

  def serviceInfoPartial()(implicit hc: HeaderCarrierForPartials, user: MtdItUser): Future[Html] = {
    serviceInfoPartialConnector.getServiceInfoPartial().map { htmlResult =>
      if (htmlResult.body.isEmpty) {
        Logger.warn("[ServiceInfoPartialService][serviceInfoPartial] - could not retrieve BTA Service Info Partial")
        renderServiceInfoHelper(Some(user))
      }
      else
        Logger.debug("[ServiceInfoPartialService][serviceInfoPartial] - retrieved BTA Service Info Partial")
        htmlResult
    }
  }
}
