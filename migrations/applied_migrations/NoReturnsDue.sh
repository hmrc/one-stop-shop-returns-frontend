#!/bin/bash

echo ""
echo "Applying migration NoReturnsDue"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/noReturnsDue                       controllers.NoReturnsDueController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "noReturnsDue.title = noReturnsDue" >> ../conf/messages.en
echo "noReturnsDue.heading = noReturnsDue" >> ../conf/messages.en

echo "Migration NoReturnsDue completed"
