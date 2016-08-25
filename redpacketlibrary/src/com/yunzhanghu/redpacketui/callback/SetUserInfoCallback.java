package com.yunzhanghu.redpacketui.callback;

/**
 * Created by desert on 16/6/28.
 */
public interface SetUserInfoCallback {
    void setUserInfo(String userName, String userAvatar);

    void UserInfoError(String msg);
}
