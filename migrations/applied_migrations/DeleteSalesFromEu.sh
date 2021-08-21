#!/bin/bash

echo ""
echo "Applying migration DeleteSalesFromEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/deleteSalesFromEu                        controllers.DeleteSalesFromEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/deleteSalesFromEu                        controllers.DeleteSalesFromEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeDeleteSalesFromEu                  controllers.DeleteSalesFromEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeDeleteSalesFromEu                  controllers.DeleteSalesFromEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "deleteSalesFromEu.title = deleteSalesFromEu" >> ../conf/messages.en
echo "deleteSalesFromEu.heading = deleteSalesFromEu" >> ../conf/messages.en
echo "deleteSalesFromEu.checkYourAnswersLabel = deleteSalesFromEu" >> ../conf/messages.en
echo "deleteSalesFromEu.error.required = Select yes if deleteSalesFromEu" >> ../conf/messages.en
echo "deleteSalesFromEu.change.hidden = DeleteSalesFromEu" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryDeleteSalesFromEuUserAnswersEntry: Arbitrary[(DeleteSalesFromEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[DeleteSalesFromEuPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryDeleteSalesFromEuPage: Arbitrary[DeleteSalesFromEuPage.type] =";\
    print "    Arbitrary(DeleteSalesFromEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(DeleteSalesFromEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration DeleteSalesFromEu completed"
