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
import models.admin.{FeatureSwitch, FeatureSwitchName}
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeatureSwitchConnector @Inject()(val appConfig: FrontendAppConfig,
                                       http: HttpClientV2)(implicit ec: ExecutionContext) extends RawResponseReads{

  def getSetSwitchStubUrl(featureFlagName: FeatureSwitchName, isEnabled: Boolean): String = {
    s"${appConfig.incomeTaxVcFsAndStubUrl}/features/${featureFlagName.name}?isEnabled=$isEnabled"
  }

  def getSetSwitchesStubUrl: String = {
    s"${appConfig.incomeTaxVcFsAndStubUrl}/features"
  }

  def setSwitch(featureFlagName: FeatureSwitchName, isEnabled: Boolean)(implicit headerCarrier: HeaderCarrier): Future[Boolean] = {

    val url = getSetSwitchStubUrl(featureFlagName, isEnabled)

    http.put(url"$url")
      .execute[HttpResponse] map { response =>
      response.status match {
        case NO_CONTENT => true
        case _ => false
      }
    }
  }

  def setSwitches(featureSwitches: Map[FeatureSwitchName, Boolean])
                 (implicit headerCarrier: HeaderCarrier): Future[Boolean] = {

    val url = getSetSwitchesStubUrl

    val featureSwitchSeq: Seq[FeatureSwitch] =
      featureSwitches.toSeq.map { case (name, isEnabled) =>
        FeatureSwitch(name, isEnabled)
      }

    val payload = Json.obj(
      "features" -> featureSwitchSeq
    )

    http.post(url"$url")
      .withBody(payload)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case NO_CONTENT => true
          case _ => false
        }
      }
  }

}