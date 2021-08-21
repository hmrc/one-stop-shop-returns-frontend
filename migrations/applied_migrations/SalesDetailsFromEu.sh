#!/bin/bash

echo ""
echo "Applying migration SalesDetailsFromEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/salesDetailsFromEu                        controllers.SalesDetailsFromEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/salesDetailsFromEu                        controllers.SalesDetailsFromEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeSalesDetailsFromEu                  controllers.SalesDetailsFromEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeSalesDetailsFromEu                  controllers.SalesDetailsFromEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "salesDetailsFromEu.title = salesDetailsFromEu" >> ../conf/messages.en
echo "salesDetailsFromEu.heading = salesDetailsFromEu" >> ../conf/messages.en
echo "salesDetailsFromEu.netValueOfSales = netValueOfSales" >> ../conf/messages.en
echo "salesDetailsFromEu.vatOnSales = vatOnSales" >> ../conf/messages.en
echo "salesDetailsFromEu.checkYourAnswersLabel = SalesDetailsFromEu" >> ../conf/messages.en
echo "salesDetailsFromEu.error.netValueOfSales.required = Enter netValueOfSales" >> ../conf/messages.en
echo "salesDetailsFromEu.error.vatOnSales.required = Enter vatOnSales" >> ../conf/messages.en
echo "salesDetailsFromEu.error.netValueOfSales.length = netValueOfSales must be 100 characters or less" >> ../conf/messages.en
echo "salesDetailsFromEu.error.vatOnSales.length = vatOnSales must be 100 characters or less" >> ../conf/messages.en
echo "salesDetailsFromEu.netValueOfSales.change.hidden = netValueOfSales" >> ../conf/messages.en
echo "salesDetailsFromEu.vatOnSales.change.hidden = vatOnSales" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySalesDetailsFromEuUserAnswersEntry: Arbitrary[(SalesDetailsFromEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[SalesDetailsFromEuPage.type]";\
    print "        value <- arbitrary[SalesDetailsFromEu].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySalesDetailsFromEuPage: Arbitrary[SalesDetailsFromEuPage.type] =";\
    print "    Arbitrary(SalesDetailsFromEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySalesDetailsFromEu: Arbitrary[SalesDetailsFromEu] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        netValueOfSales <- arbitrary[String]";\
    print "        vatOnSales <- arbitrary[String]";\
    print "      } yield SalesDetailsFromEu(netValueOfSales, vatOnSales)";\
    print "    }";\
    next }1' ../test/generators/ModelGenerators.scala > tmp && mv tmp ../test/generators/ModelGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(SalesDetailsFromEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration SalesDetailsFromEu completed"
