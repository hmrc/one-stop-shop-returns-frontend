#!/bin/bash

echo ""
echo "Applying migration NotRegistered"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /notRegistered                       controllers.NotRegisteredController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "notRegistered.title = notRegistered" >> ../conf/messages.en
echo "notRegistered.heading = notRegistered" >> ../conf/messages.en

echo "Migration NotRegistered completed"
