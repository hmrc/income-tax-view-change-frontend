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

package mocks.services

import assets.TestConstants.BusinessDetails.{businessIncomeModel, businessIncomeModelAlignedTaxYear}
import assets.TestConstants.PropertyIncome.propertyIncomeModel
import connectors.{BusinessDetailsConnector, PropertyDetailsConnector}
import models.IncomeSourcesModel
import org.scalatest.mockito.MockitoSugar
import services.IncomeSourceDetailsService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


trait MockIncomeSourceDetailsService extends MockitoSugar {

  object BusinessIncomeOnly extends IncomeSourceDetailsService(mock[BusinessDetailsConnector], mock[PropertyDetailsConnector]) {
    override def getIncomeSourceDetails(nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesModel] =
      Future.successful(IncomeSourcesModel(
        propertyDetails = None,
        businessDetails = Some(businessIncomeModel)
      ))
  }

  object PropertyIncomeOnly extends IncomeSourceDetailsService(mock[BusinessDetailsConnector], mock[PropertyDetailsConnector]) {
    override def getIncomeSourceDetails(nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesModel] =
      Future.successful(IncomeSourcesModel(
        propertyDetails = Some(propertyIncomeModel),
        businessDetails = None
      ))
  }

  object BothBusinessAndPropertyIncome extends IncomeSourceDetailsService(mock[BusinessDetailsConnector], mock[PropertyDetailsConnector]) {
    override def getIncomeSourceDetails(nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesModel] =
      Future.successful(IncomeSourcesModel(
        propertyDetails = Some(propertyIncomeModel),
        businessDetails = Some(businessIncomeModel)
      ))
  }

  object BothBusinessAndPropertyIncomeAlignedTaxYear extends IncomeSourceDetailsService(mock[BusinessDetailsConnector], mock[PropertyDetailsConnector]) {
    override def getIncomeSourceDetails(nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesModel] =
      Future.successful(IncomeSourcesModel(
        propertyDetails = Some(propertyIncomeModel),
        businessDetails = Some(businessIncomeModelAlignedTaxYear)
      ))
  }

  object NoIncomeSources extends IncomeSourceDetailsService(mock[BusinessDetailsConnector], mock[PropertyDetailsConnector]) {
    override def getIncomeSourceDetails(nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesModel] =
      Future.successful(IncomeSourcesModel(
        propertyDetails = None,
        businessDetails = None
      ))
  }

}