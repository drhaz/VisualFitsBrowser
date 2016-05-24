package org.wiyn.odi.GuideStarContainer;

public interface GuideStarReceiverStatusListener {
    final static int STATUS_ERROR = -1;
    final static int STATUS_ERROR_OPEN = -2;
    final static int STATUS_ERROR_ACCEPT = -3;
    final static int STATUS_WAITING = 0;
    final static int STATUS_OPENCONNECTION = 1;

    public void receiveGuideStarReceiverStatus (int status);

}
