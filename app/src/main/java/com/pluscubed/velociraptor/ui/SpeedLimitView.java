package com.pluscubed.velociraptor.ui;

public interface SpeedLimitView {


    void stop();

    void changeConfig();

    void setSpeed(int speed, int speedLimitWarning);

    void setSpeeding(boolean speeding);

    void setDebuggingText(String text);

    void setLimitText(String text);

    void updatePrefs();

    void hideLimit(boolean hideLimit);
}
