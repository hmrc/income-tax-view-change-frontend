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

import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import play.api.Logger
import play.twirl.api.Html
import uk.gov.hmrc.http.HttpGet
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.partials.HtmlPartial._
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartials, HtmlPartial}

import scala.concurrent.Future

@Singleton
class ServiceInfoPartialConnector @Inject()(http: HttpGet,
                                            headerCarrierConverter: ItvcHeaderCarrierForPartialsConverter,
                                            val config: FrontendAppConfig) extends RawResponseReads {

  lazy val btaUrl: String = config.btaService + "/business-account/partial/service-info"

  def getServiceInfoPartial()(implicit hcwc: HeaderCarrierForPartials): Future[Html] = {
    implicit val executionContext = fromLoggingDetails(hcwc.hc)
    http.GET[HtmlPartial](s"$btaUrl")(hc = hcwc.toHeaderCarrier, rds = readsPartial, ec = executionContext) recover connectionExceptionsAsHtmlPartialFailure map { p =>
      p.successfulContentOrEmpty
    } recover {
      case _ =>
        Logger.warn(s"[ServiceInfoPartialConnector][getServiceInfoPartial] - Unexpected future failed error")
        Html("")
    }
  }
}
