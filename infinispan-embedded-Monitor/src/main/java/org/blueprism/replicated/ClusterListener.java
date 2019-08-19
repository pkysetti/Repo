package org.blueprism.replicated;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

@Listener//(clustered=true,sync=false)
public class ClusterListener {
    @ViewChanged
    public void viewChanged(ViewChangedEvent event) {
        MyLogger.log("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        MyLogger.log("%s", event);
        MyLogger.log("-- Old Memebers = %s", event.getOldMembers());
        MyLogger.log("-- New Memebers = %s", event.getNewMembers());
        MyLogger.log("-- MergeView = %s", event.isMergeView());
        MyLogger.log("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    @Merged
    public void MergeView(ViewChangedEvent event) {
        MyLogger.log("<<<<<<+++++++++++++++++++++++MERGED CLUSTER+++++++++++++++++++++++++++++++++>>>>>>>>>>>");
        MyLogger.log("%s", event);
        MyLogger.log("-- Old Memebers = %s", event.getOldMembers());
        MyLogger.log("-- New Memebers = %s", event.getNewMembers());
        MyLogger.log("-- MergeView = %s", event.isMergeView());
        MyLogger.log("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        MyLogger.log(">>>> MERGE END" + event.getCacheManager().getCoordinator());

    }

}