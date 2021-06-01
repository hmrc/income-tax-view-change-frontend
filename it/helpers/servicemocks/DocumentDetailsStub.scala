
package helpers.servicemocks

import assets.BaseIntegrationTestConstants.taxYear
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}

import java.time.LocalDate

object DocumentDetailsStub {
  def docDetail(documentDescription:String): DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some(documentDescription),
    originalAmount = Some(10.34),
    outstandingAmount = Some(1.2),
    documentDate = "2018-03-29"
  )

  def docDateDetail(dueDate: String, chargeType: String): DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetail(chargeType),
    dueDate = Some(LocalDate.parse(dueDate))
  )
}
