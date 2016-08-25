package com.yunzhanghu.redpacketui.callback;

/**
 * Created by yunyu on 16/6/28.
 */
public interface GetUserInfoCallback {
    void getUserInfo(String userID, SetUserInfoCallback mCallback);
}
