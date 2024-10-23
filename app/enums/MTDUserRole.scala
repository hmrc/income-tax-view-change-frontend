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

package enums

sealed trait MTDUserRole {
  val isAgent: Boolean
  val isSupportingAgent: Boolean
}

case object MTDPrimaryAgent extends MTDUserRole {
  lazy val isAgent: Boolean = true
  lazy val isSupportingAgent: Boolean = false
}

case object MTDSupportingAgent extends MTDUserRole {
  lazy val isAgent: Boolean = true
  lazy val isSupportingAgent: Boolean = true
}

case object MTDIndividual extends MTDUserRole {
  lazy val isAgent: Boolean = false
  lazy val isSupportingAgent: Boolean = false
}