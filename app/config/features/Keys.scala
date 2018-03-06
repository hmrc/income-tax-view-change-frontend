/*
 * Copyright 2018 HM Revenue & Customs
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

object Keys {
  private val prefix = "features"
  val homePageEnabled = s"$prefix.homePageEnabled"
  val propertyDetailsEnabled = s"$prefix.propertyDetailsEnabled"
  val propertyEopsEnabled = s"$prefix.propertyEopsEnabled"
  val businessEopsEnabled = s"$prefix.businessEopsEnabled"
  val paymentEnabled = s"$prefix.paymentEnabled"
  val estimatesEnabled = s"$prefix.estimatesEnabled"
  val billsEnabled = s"$prefix.billsEnabled"
  val reportDeadlinesEnabled = s"$prefix.reportDeadlinesEnabled"
}
