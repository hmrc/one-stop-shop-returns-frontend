#!/bin/bash

echo ""
echo "Applying migration VatOnSalesFromEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/vatOnSalesFromEu                  controllers.VatOnSalesFromEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/vatOnSalesFromEu                  controllers.VatOnSalesFromEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeVatOnSalesFromEu                        controllers.VatOnSalesFromEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeVatOnSalesFromEu                        controllers.VatOnSalesFromEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "vatOnSalesFromEu.title = VatOnSalesFromEu" >> ../conf/messages.en
echo "vatOnSalesFromEu.heading = VatOnSalesFromEu" >> ../conf/messages.en
echo "vatOnSalesFromEu.checkYourAnswersLabel = VatOnSalesFromEu" >> ../conf/messages.en
echo "vatOnSalesFromEu.error.nonNumeric = Enter your vatOnSalesFromEu using numbers" >> ../conf/messages.en
echo "vatOnSalesFromEu.error.required = Enter your vatOnSalesFromEu" >> ../conf/messages.en
echo "vatOnSalesFromEu.error.wholeNumber = Enter your vatOnSalesFromEu using whole numbers" >> ../conf/messages.en
echo "vatOnSalesFromEu.error.outOfRange = VatOnSalesFromEu must be between {0} and {1}" >> ../conf/messages.en
echo "vatOnSalesFromEu.change.hidden = VatOnSalesFromEu" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatOnSalesFromEuUserAnswersEntry: Arbitrary[(VatOnSalesFromEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[VatOnSalesFromEuPage.type]";\
    print "        value <- arbitrary[Int].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatOnSalesFromEuPage: Arbitrary[VatOnSalesFromEuPage.type] =";\
    print "    Arbitrary(VatOnSalesFromEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(VatOnSalesFromEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration VatOnSalesFromEu completed"
