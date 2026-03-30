/*
 * Copyright 2026 HM Revenue & Customs
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
import models.admin.FeatureSwitchName
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeatureSwitchConnector @Inject()(val appConfig: FrontendAppConfig,
                                       http: HttpClientV2)(implicit ec: ExecutionContext) extends RawResponseReads{

  def getIncomeTaxVcFsAndStubUrl(featureFlagName: FeatureSwitchName, isEnabled: Boolean): String = {
    s"${appConfig.incomeTaxVcFsAndStubUrl}/features/${featureFlagName.name}?isEnabled=$isEnabled"
  }

  def setSwitch(featureFlagName: FeatureSwitchName, isEnabled: Boolean)(implicit headerCarrier: HeaderCarrier): Future[Boolean] = {

    val url = getIncomeTaxVcFsAndStubUrl(featureFlagName, isEnabled)

    println(s"Hello, World! $featureFlagName, $isEnabled ")
    println(url)

    http.put(url"$url")
      .execute[HttpResponse] map { response =>
      response.status match {
        case NO_CONTENT => true
        case _ => false
      }
    }
  }

}