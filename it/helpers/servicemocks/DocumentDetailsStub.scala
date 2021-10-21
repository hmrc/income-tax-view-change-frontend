
package helpers.servicemocks

import testConstants.BaseIntegrationTestConstants.taxYear
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}

import java.time.LocalDate

object DocumentDetailsStub {
  def docDetail(documentDescription: String): DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some(documentDescription),
    originalAmount = Some(10.34),
    outstandingAmount = Some(1.2),
    documentDate = LocalDate.of(2018, 3, 29)
  )

  def docDateDetail(dueDate: String, chargeType: String): DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetail(chargeType),
    dueDate = Some(LocalDate.parse(dueDate))
  )


  def docDetailWithInterest(documentDescription: String): DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some(documentDescription),
    originalAmount = Some(123.45),
    outstandingAmount = Some(1.2),
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = Some(2),
    interestFromDate = Some(LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29))
  )

  def docDateDetailWithInterest(dueDate: String, chargeType: String): DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetailWithInterest(chargeType),
    dueDate = Some(LocalDate.parse(dueDate))
  )
}
