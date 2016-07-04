package com.yunzhanghu.redpacketui;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.yunzhanghu.redpacketsdk.bean.AuthData;
import com.yunzhanghu.redpacketsdk.bean.RedPacketInfo;
import com.yunzhanghu.redpacketsdk.constant.RPConstant;
import com.yunzhanghu.redpacketui.callback.GetSignInfoCallback;
import com.yunzhanghu.redpacketui.callback.GetUserInfoCallback;
import com.yunzhanghu.redpacketui.message.RongEmptyMessage;
import com.yunzhanghu.redpacketui.message.RongNotificationMessage;
import com.yunzhanghu.redpacketui.message.RongRedPacketMessage;
import com.yunzhanghu.redpacketui.provider.RongNotificationMessageProvider;
import com.yunzhanghu.redpacketui.provider.RongRedPacketMessageProvider;
import com.yunzhanghu.redpacketui.ui.activity.RPChangeActivity;

import org.json.JSONException;
import org.json.JSONObject;

import io.rong.imkit.RongIM;
import io.rong.imlib.model.Message;

/**
 * Created by yunyu on 16/5/29.
 */
public class RedPacketUtil implements Response.Listener<JSONObject>, Response.ErrorListener {

    public static final int REQUEST_CODE_SEND_MONEY = 15;

    public static final String CHAT_GROUP = "chat_group";

    public static final String CHAT_DISCUSSION = "chat_discussion";

    private String userName;

    private String userAvatar;

    private String userID;

    private String chatType;

    private AuthData mAuthData;

    private GetSignInfoCallback mGetSignInfoCallback;

    private GetUserInfoCallback mGetUserInfoCallback;

    private static RedPacketUtil mRedPacketUtil;

    private RedPacketUtil() {

    }

    public static RedPacketUtil getInstance() {
        if (mRedPacketUtil == null) {
            synchronized (RedPacketUtil.class) {
                if (mRedPacketUtil == null) {
                    mRedPacketUtil = new RedPacketUtil();
                }

            }
        }
        return mRedPacketUtil;
    }

    /**
     * 初始化Token
     *
     * @param authPartner
     * @param authUserId
     * @param authTimestamp
     * @param authSign
     */
    public void initAuthData(String authPartner, String authUserId, String authTimestamp, String authSign) {
        mAuthData = new AuthData();
        mAuthData.authPartner = authPartner;
        mAuthData.authUserId = authUserId;
        mAuthData.authTimestamp = authTimestamp;
        mAuthData.authSign = authSign;
    }

    public AuthData getAuthData() {
        return mAuthData;
    }

    /**
     * 初始化用户信息
     *
     * @param userID     用户ID
     * @param userName   用户名字
     * @param userAvatar 用户头像
     */

    public void initUserInfo(String userID, String userName, String userAvatar) {
        this.userID = userID;
        this.userName = userName;
        this.userAvatar = userAvatar;
        if (TextUtils.isEmpty(userID)) {
            this.userID = "default";
        }
        if (TextUtils.isEmpty(userName)) {
            this.userName = "default";
        }
        if (TextUtils.isEmpty(userAvatar)) {
            this.userAvatar = "default";
        }
    }

    /**
     * 注册消息类型以及消息展示模板
     */
    public void registerMsgTypeAndTemplate(Context mContext) {
        RongIM.registerMessageType(RongRedPacketMessage.class);
        RongIM.registerMessageType(RongNotificationMessage.class);
        RongIM.registerMessageType(RongEmptyMessage.class);
        RongIM.registerMessageTemplate(new RongRedPacketMessageProvider(mContext));
        RongIM.registerMessageTemplate(new RongNotificationMessageProvider(mContext));
    }

    /**
     * 插入消息体
     *
     * @param message 消息类型
     */
    public void insertMessage(Message message) {
        RongEmptyMessage content = (RongEmptyMessage) message.getContent();
        if (TextUtils.isEmpty(userID)) {
            userID = "default";
        }
        RongNotificationMessage rongNotificationMessage = RongNotificationMessage.obtain(content.getSendUserID(), content.getSendUserName(), content.getReceiveUserID(), content.getReceiveUserName(), content.getIsOpenMoney());
        if (content.getSendUserID().equals(userID)) {//如果当前用户是发送红包者,插入一条"XX领取了你的红包"
            RongIM.getInstance().getRongIMClient().insertMessage(message.getConversationType(), message.getTargetId(), content.getReceiveUserID(), rongNotificationMessage, null);
        }
    }

    /**
     * 跳转到零钱页
     *
     * @param mContext
     */
    public void toChangeActivity(Context mContext) {
        Intent intent = new Intent(mContext, RPChangeActivity.class);
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.fromNickName = userName;
        redPacketInfo.fromAvatarUrl = userAvatar;
        intent.putExtra(RPConstant.EXTRA_MONEY_INFO, redPacketInfo);
        intent.putExtra(RPConstant.EXTRA_AUTH_INFO, getAuthData());
        mContext.startActivity(intent);
    }

    public GetUserInfoCallback getGetUserInfoCallback() {
        return mGetUserInfoCallback;
    }

    public void setGetUserInfoCallback(GetUserInfoCallback mGetUserInfoCallback) {
        this.mGetUserInfoCallback = mGetUserInfoCallback;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getChatType() {
        return chatType;
    }

    public void setChatType(String chatType) {
        this.chatType = chatType;
    }

    public void requestSign(Context mContext, String url, final GetSignInfoCallback mCallback) {
        mGetSignInfoCallback = mCallback;
        RequestQueue mRequestQueue = Volley.newRequestQueue(mContext);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, this, this);
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(3000, 2, 2));
        mRequestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        Log.e("dxf", "--" + volleyError.toString());
        mGetSignInfoCallback.signInfoError(volleyError.toString());
    }

    @Override
    public void onResponse(JSONObject jsonObject) {
        if (jsonObject != null && jsonObject.length() > 0) {
            try {
                String partner = jsonObject.getString("partner");
                String userId = jsonObject.getString("user_id");
                String timestamp = jsonObject.getString("timestamp");
                String sign = jsonObject.getString("sign");
                //初始化红包Token
                initAuthData(partner, userId, timestamp, sign);
                mGetSignInfoCallback.signInfoSuccess();
            } catch (JSONException e) {
                e.printStackTrace();
                mGetSignInfoCallback.signInfoError(e.getMessage());
            }

        } else {
            mGetSignInfoCallback.signInfoError("sign data is  null");
        }
    }
}
