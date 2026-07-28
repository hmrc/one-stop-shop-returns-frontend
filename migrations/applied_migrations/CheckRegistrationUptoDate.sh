#!/bin/bash

echo ""
echo "Applying migration CheckRegistrationUptoDate"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/checkRegistrationUptoDate                       controllers.CheckRegistrationUptoDateController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "checkRegistrationUptoDate.title = checkRegistrationUptoDate" >> ../conf/messages.en
echo "checkRegistrationUptoDate.heading = checkRegistrationUptoDate" >> ../conf/messages.en

echo "Migration CheckRegistrationUptoDate completed"
