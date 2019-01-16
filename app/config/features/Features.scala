/*
 * Copyright 2019 HM Revenue & Customs
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

package config.features

import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class Features @Inject()(config: Configuration) {
  val propertyDetailsEnabled: Feature = new Feature(Keys.propertyDetailsEnabled, config)
  val propertyEopsEnabled: Feature = new Feature(Keys.propertyEopsEnabled, config)
  val businessEopsEnabled: Feature = new Feature(Keys.businessEopsEnabled, config)
  val paymentEnabled: Feature = new Feature(Keys.paymentEnabled, config)
  val statementsEnabled: Feature = new Feature(Keys.statementsEnabled, config)
  val estimatesEnabled: Feature = new Feature(Keys.estimatesEnabled, config)
  val billsEnabled: Feature = new Feature(Keys.billsEnabled, config)
  val reportDeadlinesEnabled: Feature = new Feature(Keys.reportDeadlinesEnabled, config)
  val accountDetailsEnabled: Feature = new Feature(Keys.accountDetailsEnabled, config)
  val calcBreakdownEnabled: Feature = new Feature(Keys.calcBreakdownEnabled, config)
  val calcDataApiEnabled: Feature = new Feature(Keys.calcDataApiEnabled, config)
}
