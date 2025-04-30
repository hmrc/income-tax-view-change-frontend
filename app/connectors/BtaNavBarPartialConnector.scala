/*
 * Copyright 2023 HM Revenue & Customs
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

import config.FrontendAppConfig
import models.btaNavBar.NavContent
import play.api.Logger
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpReadsInstances, HttpResponse}

import java.net.{URI, URL}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class BtaNavBarPartialConnector @Inject()(val http: HttpClientV2,
                                          val config: FrontendAppConfig) extends HttpReadsInstances {

  private lazy val btaNavLinksUrl: URL = new URI(config.btaService + "/business-account/partial/nav-links").toURL

  val logger: Logger = Logger(this.getClass)

  implicit val hr: HttpReads.Implicits.type = HttpReads.Implicits
  implicit val legacyRawReads: HttpReads[HttpResponse] = HttpReads.Implicits.throwOnFailure(HttpReads.Implicits.readEitherOf(HttpReads.Implicits.readRaw))


  def getNavLinks()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[NavContent]] = {
    http
      .get(btaNavLinksUrl)
      .execute[Option[NavContent]]
      .recover {
        case e =>
          logger.warn(s"Unexpected error ${e.getMessage}")
          None
      }
  }

}
