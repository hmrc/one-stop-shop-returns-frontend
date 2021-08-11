#!/bin/bash

echo ""
echo "Applying migration NetValueOfSalesFromNi"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /netValueOfSalesFromNi                  controllers.NetValueOfSalesFromNiController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /netValueOfSalesFromNi                  controllers.NetValueOfSalesFromNiController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeNetValueOfSalesFromNi                        controllers.NetValueOfSalesFromNiController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeNetValueOfSalesFromNi                        controllers.NetValueOfSalesFromNiController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "netValueOfSalesFromNi.title = NetValueOfSalesFromNi" >> ../conf/messages.en
echo "netValueOfSalesFromNi.heading = NetValueOfSalesFromNi" >> ../conf/messages.en
echo "netValueOfSalesFromNi.checkYourAnswersLabel = NetValueOfSalesFromNi" >> ../conf/messages.en
echo "netValueOfSalesFromNi.error.nonNumeric = Enter your netValueOfSalesFromNi using numbers" >> ../conf/messages.en
echo "netValueOfSalesFromNi.error.required = Enter your netValueOfSalesFromNi" >> ../conf/messages.en
echo "netValueOfSalesFromNi.error.wholeNumber = Enter your netValueOfSalesFromNi using whole numbers" >> ../conf/messages.en
echo "netValueOfSalesFromNi.error.outOfRange = NetValueOfSalesFromNi must be between {0} and {1}" >> ../conf/messages.en
echo "netValueOfSalesFromNi.change.hidden = NetValueOfSalesFromNi" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryNetValueOfSalesFromNiUserAnswersEntry: Arbitrary[(NetValueOfSalesFromNiPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[NetValueOfSalesFromNiPage.type]";\
    print "        value <- arbitrary[Int].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryNetValueOfSalesFromNiPage: Arbitrary[NetValueOfSalesFromNiPage.type] =";\
    print "    Arbitrary(NetValueOfSalesFromNiPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(NetValueOfSalesFromNiPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration NetValueOfSalesFromNi completed"
