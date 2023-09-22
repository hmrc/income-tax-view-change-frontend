//import audit.Utilities
//import auth.MtdItUser
//import models.core.{AccountingPeriodModel, AddressModel}
//import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
//import play.api.libs.json.{JsObject, JsValue, Json}
//import play.api.mvc.{Request, RequestHeader}
//import uk.gov.hmrc.auth.core.AffinityGroup.Individual
//import uk.gov.hmrc.auth.core.retrieve.Name
//import play.api.test._
//
//import java.time.LocalDate
//
//val detail: JsObject = {
//  Json.obj(
//    Utilities.userAuditDetails(MtdItUser(
//      mtditid = "fdsgfsdfgsd",
//      nino = "fdfdasfsda",
//      userName = Some(Name(Some("namefdsfsd"), Some("Smith"))),
//      incomeSources = IncomeSourceDetailsModel(
//        "fdasfdsaf",
//        Some("2021"),
//        businesses = List(
//          BusinessDetailsModel(
//            incomeSourceId = "testSelfEmploymentId",
//            accountingPeriod = Some(AccountingPeriodModel(
//              start = LocalDate.of(2021, 1, 1),
//              end = LocalDate.of(2022, 1, 1)
//            )),
//            tradingName = Some("b1TradingName"),
//            firstAccountingPeriodEndDate = Some(LocalDate.of(2023, 1, 1)),
//            tradingStartDate = Some(LocalDate.of(2023, 1, 1)),
//            cessation = None,
//            address = Some(AddressModel("dsadsa", None, None, None, None, "GB"))
//          )
//        ),
//        properties = List(
//          PropertyDetailsModel(
//            incomeSourceId = "testPropertyIncomeId",
//            accountingPeriod = Some(AccountingPeriodModel(
//              start = LocalDate.of(2022, 1, 1),
//              end = LocalDate.of(2023, 1, 1)
//            )),
//            firstAccountingPeriodEndDate = Some(LocalDate.of(2023, 1, 1)),
//            Some("propertyIncomeType"),
//            Some(LocalDate.of(2023, 1, 1)),
//            None
//          )
//        )
//      ),
//      btaNavPartial = None,
//      saUtr = Some("1234567890"),
//      credId = Some("credId"),
//      userType = Some(Individual),
//      None
//    )(Request(rh = , body = ???)))
//  )
//
//}
//    detail ++ Json.obj(
//      "soleTraderBusinesses" ->
//        soleTraderBusinesses.map(business =>
//          (
//            "businessName" -> business.tradingName,
//            "dateStarted" -> business.tradingStartDate
//          )
//        ),
//      "ukProperty" ->
//        ukProperty.map(property =>
//          "dateStarted" -> property.tradingStartDate
//        ),
//      "foreignProperty" ->
//        foreignProperty.map(property =>
//          "dateStarted" -> property.tradingStartDate
//        ),
//      "ceasedBusinesses" ->
//        ceasedBusinesses.map(business =>
//          (
//            "businessName" -> business.tradingName,
//            "dateStarted" -> business.tradingStartDate,
//            "dateEnded" -> business.cessationDate
//          )
//        )
//    )
//
//
//