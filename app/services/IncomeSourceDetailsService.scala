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

package services

import auth.authV2.models.AuthorisedAndEnrolledRequest
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import connectors.BusinessDetailsConnector
import enums.IncomeSourceJourney.*
import enums.IncomeSourceJourney.IncomeSourceType.{ForeignProperty, SelfEmployment, UkProperty}
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class IncomeSourceDetailsService @Inject()(
                                            businessDetailsConnector: BusinessDetailsConnector
                                          )(
                                            implicit val appConfig: FrontendAppConfig,
                                            val ec: ExecutionContext
                                          ) extends FeatureSwitching {

  def getIncomeSourceDetails()(implicit hc: HeaderCarrier, mtdUser: AuthorisedAndEnrolledRequest[_]): Future[IncomeSourceDetailsResponse] = {
    businessDetailsConnector.getIncomeSources()
  }

  def getAddIncomeSourceViewModel(sources: IncomeSourceDetailsModel, displayBusinessStartDateFS: Boolean): Try[AddIncomeSourcesViewModel] = Try {
    val soleTraderBusinesses = sources.businesses.filterNot(_.isCeased)
    val ukProperty = sources.properties.filterNot(_.isCeased).find(_.isUkProperty)
    val foreignProperty = sources.properties.filterNot(_.isCeased).find(_.isForeignProperty)

    AddIncomeSourcesViewModel(
      soleTraderBusinesses = soleTraderBusinesses.map { business =>
        BusinessDetailsViewModel(
          business.tradingName,
          business.tradingStartDate
        )
      },
      ukProperty = ukProperty.map { property =>
        PropertyDetailsViewModel(property.tradingStartDate)
      },
      foreignProperty = foreignProperty.map { property =>
        PropertyDetailsViewModel(property.tradingStartDate)
      },
      ceasedBusinesses = getCeasedBusinesses(sources = sources),
      displayStartDate = displayBusinessStartDateFS
    )
  }

  def getViewIncomeSourceViewModel(
                                    sources: IncomeSourceDetailsModel,
                                    displayBusinessStartDateFS: Boolean
                                  ): Either[Throwable, ViewIncomeSourcesViewModel] = {

    val maybeSoleTraderBusinesses = sources.businesses.filterNot(_.isCeased)
    val soleTraderBusinessesExists = maybeSoleTraderBusinesses.nonEmpty

    val maybeUkProperty = sources.properties.filterNot(_.isCeased).find(_.isUkProperty)
    val ukPropertyExists = maybeUkProperty.nonEmpty

    val maybeForeignProperty = sources.properties.filterNot(_.isCeased).find(_.isForeignProperty)
    val foreignPropertyExists = maybeForeignProperty.nonEmpty

    Try {
      ViewIncomeSourcesViewModel(
        viewSoleTraderBusinesses = if (soleTraderBusinessesExists) {
          maybeSoleTraderBusinesses.map { business =>
            ViewBusinessDetailsViewModel(
              mkIncomeSourceId(business.incomeSourceId),
              business.incomeSource,
              business.tradingName,
              business.tradingStartDate
            )
          }
        } else Nil,
        viewUkProperty = if (ukPropertyExists) {
          Some(ViewPropertyDetailsViewModel(
            maybeUkProperty.flatMap(_.tradingStartDate)
          ))
        } else None,
        viewForeignProperty = if (foreignPropertyExists) {
          Some(ViewPropertyDetailsViewModel(
            maybeForeignProperty.flatMap(_.tradingStartDate)
          ))
        } else None,
        getCeasedBusinesses(sources = sources),
        displayBusinessStartDateFS
      )
    }.toEither
  }

  def getCeaseIncomeSourceViewModel(sources: IncomeSourceDetailsModel, displayBusinessStartDateFS: Boolean): Either[Throwable, CeaseIncomeSourcesViewModel] = {

    val maybeSoleTraderBusinesses = sources.businesses.filterNot(_.isCeased)
    val soleTraderBusinessesExists = maybeSoleTraderBusinesses.nonEmpty

    val maybeUkProperty = sources.properties.filterNot(_.isCeased).find(_.isUkProperty)
    val ukPropertyExists = maybeUkProperty.nonEmpty

    val maybeForeignProperty = sources.properties.filterNot(_.isCeased).find(_.isForeignProperty)
    val foreignPropertyExists = maybeForeignProperty.nonEmpty

    Try {
      CeaseIncomeSourcesViewModel(
        soleTraderBusinesses = if (soleTraderBusinessesExists) {
          maybeSoleTraderBusinesses.map { business =>
            CeaseBusinessDetailsViewModel(
              mkIncomeSourceId(business.incomeSourceId),
              business.tradingName,
              business.tradingStartDate
            )
          }
        } else Nil,
        ukProperty = if (ukPropertyExists) {
          Some(CeasePropertyDetailsViewModel(
            maybeUkProperty.flatMap(_.tradingStartDate)
          ))
        } else None,
        foreignProperty = if (foreignPropertyExists) {
          Some(CeasePropertyDetailsViewModel(
            maybeForeignProperty.flatMap(_.tradingStartDate)
          ))
        } else None,
        getCeasedBusinesses(sources = sources),
        displayBusinessStartDateFS
      )
    }.toEither
  }

  def getCheckCeaseSelfEmploymentDetailsViewModel(
                                                   sources: IncomeSourceDetailsModel,
                                                   incomeSourceId: IncomeSourceId,
                                                   businessEndDate: LocalDate
                                                 ): Either[Throwable, CheckCeaseIncomeSourceDetailsViewModel] =
    Try {
      val soleTraderBusinesses = sources.businesses.filterNot(_.isCeased)
        .find(m => mkIncomeSourceId(m.incomeSourceId) == incomeSourceId)

      soleTraderBusinesses.map { business =>
        CheckCeaseIncomeSourceDetailsViewModel(
          mkIncomeSourceId(business.incomeSourceId),
          business.tradingName,
          business.incomeSource,
          business.address,
          businessEndDate,
          incomeSourceType = SelfEmployment
        )
      }.get
    }.toEither

  def getCheckCeasePropertyIncomeSourceDetailsViewModel(
                                                         sources: IncomeSourceDetailsModel,
                                                         businessEndDate: LocalDate,
                                                         incomeSourceType: IncomeSourceType
                                                       ): Either[Throwable, CheckCeaseIncomeSourceDetailsViewModel] = {
    val propertyBusiness = incomeSourceType match {
      case UkProperty => sources.properties.filterNot(_.isCeased).find(_.isUkProperty)
      case _ => sources.properties.filterNot(_.isCeased).find(_.isForeignProperty)
    }
    Try {
      propertyBusiness.map { business =>
        CheckCeaseIncomeSourceDetailsViewModel(
          mkIncomeSourceId(business.incomeSourceId),
          tradingName = None,
          trade = None,
          address = None,
          businessEndDate,
          incomeSourceType = incomeSourceType
        )
      }.get
    }.toEither
  }

  private def getCeasedBusinesses(sources: IncomeSourceDetailsModel): List[CeasedBusinessDetailsViewModel] = {
    val viewModelsForCeasedSEBusinesses = {
      for {
        business <- sources.businesses.filter(_.isCeased)
        cessationDate <- business.cessation.flatMap(_.date)
      } yield CeasedBusinessDetailsViewModel(
        tradingName = business.tradingName,
        incomeSourceType = SelfEmployment,
        tradingStartDate = business.tradingStartDate,
        cessationDate = cessationDate
      )
    }
    val viewModelsForCeasedPropertyBusinesses = {
      for {
        property <- sources.properties.filter(_.isCeased)
        incomeSourceType <- property.incomeSourceType match {
          case Some("uk-property") => Some(UkProperty)
          case Some("foreign-property") => Some(ForeignProperty)
          case _ => None
        }
        cessationDate <- property.cessation.flatMap(_.date)
      } yield {
        CeasedBusinessDetailsViewModel(
          tradingName = None,
          incomeSourceType = incomeSourceType,
          tradingStartDate = property.tradingStartDate,
          cessationDate = cessationDate
        )
      }
    }
    viewModelsForCeasedSEBusinesses ++ viewModelsForCeasedPropertyBusinesses
  }

  def getIncomeSource(
                       incomeSourceType: IncomeSourceType,
                       incomeSourceId: IncomeSourceId,
                       incomeSourceDetailsModel: IncomeSourceDetailsModel
                     ): Option[IncomeSourceFromUser] = {
    incomeSourceType match {
      case SelfEmployment =>
        incomeSourceDetailsModel.businesses
          .find((model: BusinessDetailsModel) => IncomeSourceId(model.incomeSourceId) == incomeSourceId)
          .flatMap { addedBusiness =>
            for {
              businessName <- addedBusiness.tradingName
              startDate <- addedBusiness.tradingStartDate
            } yield IncomeSourceFromUser(startDate, Some(businessName))
          }
      case UkProperty =>
        for {
          newlyAddedProperty <-
            incomeSourceDetailsModel.properties.find(incomeSource =>
              mkIncomeSourceId(incomeSource.incomeSourceId) == incomeSourceId && incomeSource.isUkProperty
            )
          startDate <- newlyAddedProperty.tradingStartDate
        } yield IncomeSourceFromUser(startDate, None)
      case ForeignProperty =>
        for {
          newlyAddedProperty <- incomeSourceDetailsModel.properties.find(incomeSource =>
            mkIncomeSourceId(incomeSource.incomeSourceId) == incomeSourceId && incomeSource.isForeignProperty
          )
          startDate <- newlyAddedProperty.tradingStartDate
        } yield IncomeSourceFromUser(startDate, None)
      case _ =>
        None
    }
  }

  def getReportingMethod(maybeAddIncomeSourceData: Option[AddIncomeSourceData]): ChosenReportingMethod = {
    val (reportingMethodTaxYear1, reportingMethodTaxYear2) =
      (
        maybeAddIncomeSourceData.flatMap(_.reportingMethodTaxYear1).orElse(None),
        maybeAddIncomeSourceData.flatMap(_.reportingMethodTaxYear2).orElse(None)
      )

    (reportingMethodTaxYear1, reportingMethodTaxYear2) match {
      case (Some("A"), Some("A")) | (None, Some("A")) => ChosenReportingMethod.Annual
      case (Some("Q"), Some("Q")) | (None, Some("Q")) => ChosenReportingMethod.Quarterly
      case (Some("A"), Some("Q")) | (Some("Q"), Some("A")) => ChosenReportingMethod.Hybrid
      case (None, None) => ChosenReportingMethod.DefaultAnnual
      case _ => ChosenReportingMethod.Unknown
    }
  }

  def getLatencyDetailsFromUser(incomeSourceType: IncomeSourceType, incomeSourceDetailsModel: IncomeSourceDetailsModel): Option[LatencyDetails] = {
    incomeSourceType match {
      case SelfEmployment => incomeSourceDetailsModel.businesses.flatMap(_.latencyDetails).headOption
      case UkProperty => incomeSourceDetailsModel.getUKProperty.flatMap(_.latencyDetails)
      case ForeignProperty => incomeSourceDetailsModel.getForeignProperty.flatMap(_.latencyDetails)
      case _ => None
    }
  }
}

