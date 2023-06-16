#!/bin/bash

echo ""
echo "Applying migration NiToNiIntercept"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/niToNiIntercept                       controllers.NiToNiInterceptController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "niToNiIntercept.title = niToNiIntercept" >> ../conf/messages.en
echo "niToNiIntercept.heading = niToNiIntercept" >> ../conf/messages.en

echo "Migration NiToNiIntercept completed"
