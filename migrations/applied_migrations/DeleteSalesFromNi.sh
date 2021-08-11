#!/bin/bash

echo ""
echo "Applying migration DeleteSalesFromNi"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /deleteSalesFromNi                        controllers.DeleteSalesFromNiController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /deleteSalesFromNi                        controllers.DeleteSalesFromNiController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeDeleteSalesFromNi                  controllers.DeleteSalesFromNiController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeDeleteSalesFromNi                  controllers.DeleteSalesFromNiController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "deleteSalesFromNi.title = deleteSalesFromNi" >> ../conf/messages.en
echo "deleteSalesFromNi.heading = deleteSalesFromNi" >> ../conf/messages.en
echo "deleteSalesFromNi.checkYourAnswersLabel = deleteSalesFromNi" >> ../conf/messages.en
echo "deleteSalesFromNi.error.required = Select yes if deleteSalesFromNi" >> ../conf/messages.en
echo "deleteSalesFromNi.change.hidden = DeleteSalesFromNi" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryDeleteSalesFromNiUserAnswersEntry: Arbitrary[(DeleteSalesFromNiPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[DeleteSalesFromNiPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryDeleteSalesFromNiPage: Arbitrary[DeleteSalesFromNiPage.type] =";\
    print "    Arbitrary(DeleteSalesFromNiPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(DeleteSalesFromNiPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration DeleteSalesFromNi completed"
