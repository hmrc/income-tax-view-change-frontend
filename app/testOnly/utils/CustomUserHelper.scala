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

package testOnly.utils

object CustomUserHelper {
  final val customIncomeSourceUsers            = Seq("TR000001A", "AS000000A", "AS000001A")
  final val customReportingObligationsUsers    = Seq("OP000001A", "OP000002A", "OP000003A", "OP000005A", "OP000006A", "NE000000A", "NE000001A", "NE000002A", "HP000000A")
  final val latentBusinessUser                 = "AS000002A"
  final val recentActivityUser                 = "HP000000A"
  final val customTaxCalculationUser           = "PP000003A"
  final val revenueAmendmentAndCorrectionsUser = "RA000000A"
}
