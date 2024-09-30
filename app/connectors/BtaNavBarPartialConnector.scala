/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class BtaNavBarPartialConnector @Inject()(val httpClient: HttpClientV2,
                                          val config: FrontendAppConfig) extends HttpReadsInstances {

  lazy val btaNavLinksUrl: String = config.btaService + "/business-account/partial/nav-links"

  val logger: Logger = Logger(this.getClass)

  def getNavLinks()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[NavContent]] = {
    httpClient.get(url"$btaNavLinksUrl")
      .execute[Option[NavContent]]
      .recover {
        case e =>
          logger.warn(s"Unexpected error ${e.getMessage}")
          None
      }
  }

}
