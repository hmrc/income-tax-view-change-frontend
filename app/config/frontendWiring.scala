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

package config

import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import javax.inject.{Inject, Singleton}


@Singleton
class FrontendAuthConnector @Inject()(config: ServicesConfig,
                                      val WSHttp: HttpClientV2) extends PlayAuthConnector {
  lazy val serviceUrl: String = config.baseUrl("auth")
  lazy val httpClientV2 = WSHttp
}

@Singleton
class ItvcHeaderCarrierForPartialsConverter @Inject() extends HeaderCarrierForPartialsConverter {}