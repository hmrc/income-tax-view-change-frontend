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

package config

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

@Singleton
class FrontendAuditConnector @Inject()() extends Auditing with AppName {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

@Singleton
class FrontendAuthConnector @Inject()(val WSHttp: WSHttp) extends PlayAuthConnector with ServicesConfig {
  lazy val serviceUrl: String = baseUrl("auth")
  lazy val http = WSHttp
}

@Singleton
class ItvcHeaderCarrierForPartialsConverter extends HeaderCarrierForPartialsConverter {
  val crypto: String => String = cookie => SessionCookieCryptoFilter.encrypt(cookie)
}

object FrontendAuditConnector extends FrontendAuditConnector

object FrontendAuthConnector extends FrontendAuthConnector(WSHttp)
