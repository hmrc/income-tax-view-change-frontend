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

package connectors

import javax.inject.{Inject, Singleton}

import config.ItvcHeaderCarrierForPartialsConverter
import play.api.Logger
import play.twirl.api.Html
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.partials.HtmlPartial._
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartials, HtmlPartial}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ServiceInfoPartialConnector @Inject()(http: HttpGet,
                                            headerCarrierConverter: ItvcHeaderCarrierForPartialsConverter
                                           ) extends ServicesConfig with RawResponseReads {


  lazy val btaUrl: String = baseUrl("business-account") + "/business-account/partial/service-info"

  def getServiceInfoPartial()(implicit hcwc: HeaderCarrierForPartials): Future[Html] = {
    http.GET[HtmlPartial](s"$btaUrl")(hc = hcwc.toHeaderCarrier, rds = readsPartial) recover connectionExceptionsAsHtmlPartialFailure map { p =>
      p.successfulContentOrEmpty
    }  recoverWith {
      case _ =>
        Logger.warn(s"[ServiceInfoPartialConnector][getServiceInfoPartial] - Unexpected future failed error")
        Future.successful(Html(""))
    }
  }
}
