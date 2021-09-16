#!/bin/bash

echo ""
echo "Applying migration NetValueOfSalesFromEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/netValueOfSalesFromEu                  controllers.NetValueOfSalesFromEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/netValueOfSalesFromEu                  controllers.NetValueOfSalesFromEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeNetValueOfSalesFromEu                        controllers.NetValueOfSalesFromEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeNetValueOfSalesFromEu                        controllers.NetValueOfSalesFromEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "netValueOfSalesFromEu.title = NetValueOfSalesFromEu" >> ../conf/messages.en
echo "netValueOfSalesFromEu.heading = NetValueOfSalesFromEu" >> ../conf/messages.en
echo "netValueOfSalesFromEu.checkYourAnswersLabel = NetValueOfSalesFromEu" >> ../conf/messages.en
echo "netValueOfSalesFromEu.error.nonNumeric = Enter your netValueOfSalesFromEu using numbers" >> ../conf/messages.en
echo "netValueOfSalesFromEu.error.required = Enter your netValueOfSalesFromEu" >> ../conf/messages.en
echo "netValueOfSalesFromEu.error.wholeNumber = Enter your netValueOfSalesFromEu using whole numbers" >> ../conf/messages.en
echo "netValueOfSalesFromEu.error.outOfRange = NetValueOfSalesFromEu must be between {0} and {1}" >> ../conf/messages.en
echo "netValueOfSalesFromEu.change.hidden = NetValueOfSalesFromEu" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryNetValueOfSalesFromEuUserAnswersEntry: Arbitrary[(NetValueOfSalesFromEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[NetValueOfSalesFromEuPage.type]";\
    print "        value <- arbitrary[Int].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryNetValueOfSalesFromEuPage: Arbitrary[NetValueOfSalesFromEuPage.type] =";\
    print "    Arbitrary(NetValueOfSalesFromEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(NetValueOfSalesFromEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration NetValueOfSalesFromEu completed"
