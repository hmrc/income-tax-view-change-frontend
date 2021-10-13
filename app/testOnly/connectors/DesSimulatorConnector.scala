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

package testOnly.connectors

import connectors.RawResponseReads
import javax.inject.Inject
import testOnly.TestOnlyAppConfig
import testOnly.models.UserModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class DesSimulatorConnector @Inject()(val appConfig: TestOnlyAppConfig,
                                      val http: HttpClient)(implicit ec: ExecutionContext) extends RawResponseReads {

  def stubUser(userModel: UserModel)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = s"${appConfig.desSimulatorUrl}/test-users/individuals"
    val fixHeaders = hc.copy(otherHeaders = hc.otherHeaders.filterNot(header => header._1 == "Content-Type") ++ Seq("Content-Type" -> "application/json"))
    http.POST[UserModel, HttpResponse](url, userModel)(implicitly, implicitly, fixHeaders, implicitly)
  }
}
