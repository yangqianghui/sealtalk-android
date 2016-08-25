package cn.rongcloud.im.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.yunzhanghu.redpacketui.RedPacketUtil;

import java.util.List;

import cn.rongcloud.im.R;
import cn.rongcloud.im.db.DBManager;
import cn.rongcloud.im.db.Friend;
import cn.rongcloud.im.db.Groups;
import cn.rongcloud.im.server.network.async.AsyncTaskManager;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.GetGroupResponse;
import cn.rongcloud.im.server.response.GetTokenResponse;
import cn.rongcloud.im.server.response.GetUserInfoByIdResponse;
import cn.rongcloud.im.server.response.LoginResponse;
import cn.rongcloud.im.server.response.UserRelationshipResponse;
import cn.rongcloud.im.server.utils.AMUtils;
import cn.rongcloud.im.server.utils.NLog;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.widget.ClearWriteEditText;
import cn.rongcloud.im.server.widget.LoadDialog;
import cn.rongcloud.im.utils.SharedPreferencesContext;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.UserInfo;

/**
 * Created by AMing on 16/1/15.
 * Company RongCloud
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private static final int LOGIN = 5;
    private static final int GETTOKEN = 6;
    private static final int SYNCUSERINFO = 9;
    private static final int SYNCGROUP = 17;
    private static final int AUTOLOGIN = 19;
    private static final int SYNCFRIEND = 14;
    private ImageView mImgBackgroud;

    private ClearWriteEditText mPhoneEdit, mPasswordEdit;

    private Button mConfirm;

    private TextView mRegist, forgetPassword;

    private String phoneString, passwordString, loginToken, connectResultId;

    private SharedPreferences sp;

    private SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        sp = getSharedPreferences("config", MODE_PRIVATE);
        editor = sp.edit();

        initView();
    }

    private void initView() {
        mPhoneEdit = (ClearWriteEditText) findViewById(R.id.de_login_phone);
        mPasswordEdit = (ClearWriteEditText) findViewById(R.id.de_login_password);
        mConfirm = (Button) findViewById(R.id.de_login_sign);
        mRegist = (TextView) findViewById(R.id.de_login_register);
        forgetPassword = (TextView) findViewById(R.id.de_login_forgot);
        forgetPassword.setOnClickListener(this);
        mConfirm.setOnClickListener(this);
        mRegist.setOnClickListener(this);
        mImgBackgroud = (ImageView) findViewById(R.id.de_img_backgroud);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.translate_anim);
                mImgBackgroud.startAnimation(animation);
            }
        }, 200);
        mPhoneEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 11) {
                    AMUtils.onInactive(mContext, mPhoneEdit);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        String oldPhone = sp.getString("loginphone", "");
        String oldPassword = sp.getString("loginpassword", "");
        if (oldPhone.equals(mPhoneEdit.getText().toString().trim())) {//和上次登录账户一致

        } else {
            //和上次登录账户不一致 或者 换设备登录  重新网络拉取好友 和 群组数据
            DBManager.getInstance(mContext).getDaoSession().getFriendDao().deleteAll();//清空上个用户的数据库
            DBManager.getInstance(mContext).getDaoSession().getGroupsDao().deleteAll();
        }
        if (!TextUtils.isEmpty(oldPhone) && !TextUtils.isEmpty(oldPassword)) {
            mPhoneEdit.setText(oldPhone);
            mPasswordEdit.setText(oldPassword);
        }


        if (!sp.getBoolean("exit", false) && !TextUtils.isEmpty(oldPhone) && !TextUtils.isEmpty(oldPassword)) {
            editor.putBoolean("exit", false);
            editor.apply();
            phoneString = oldPhone;
            passwordString = oldPassword;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    LoadDialog.show(mContext);
                    request(AUTOLOGIN);
                }
            }, 100);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.de_login_sign:
                phoneString = mPhoneEdit.getText().toString().trim();
                passwordString = mPasswordEdit.getText().toString().trim();

                if (TextUtils.isEmpty(phoneString)) {
                    NToast.shortToast(mContext, R.string.phone_number_is_null);
                    mPhoneEdit.setShakeAnimation();
                    return;
                }

                if (!AMUtils.isMobile(phoneString)) {
                    NToast.shortToast(mContext, R.string.Illegal_phone_number);
                    mPhoneEdit.setShakeAnimation();
                    return;
                }

                if (TextUtils.isEmpty(passwordString)) {
                    NToast.shortToast(mContext, R.string.password_is_null);
                    mPasswordEdit.setShakeAnimation();
                    return;
                }
                if (passwordString.contains(" ")) {
                    NToast.shortToast(mContext, R.string.password_cannot_contain_spaces);
                    mPasswordEdit.setShakeAnimation();
                    return;
                }
                LoadDialog.show(mContext);
                editor.putBoolean("exit", false);
                editor.apply();
                request(LOGIN);
                break;
            case R.id.de_login_register:
                startActivityForResult(new Intent(this, RegisterActivity.class), 1);
                break;
            case R.id.de_login_forgot:
                startActivityForResult(new Intent(this, ForgetPasswordActivity.class), 2);
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && data != null) {
            String phone = data.getStringExtra("phone");
            String password = data.getStringExtra("password");
            mPhoneEdit.setText(phone);
            mPasswordEdit.setText(password);
        } else if (data != null && requestCode == 1) {
            String phone = data.getStringExtra("phone");
            String password = data.getStringExtra("password");
            String id = data.getStringExtra("id");
            String nickname = data.getStringExtra("nickname");
            if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(id) && !TextUtils.isEmpty(nickname)) {
                mPhoneEdit.setText(phone);
                mPasswordEdit.setText(password);
                editor.putString("loginphone", phone);
                editor.putString("loginpassword", password);
                editor.putString("loginid", id);
                editor.putString("loginnickname", nickname);
                editor.apply();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        switch (requestCode) {
            case LOGIN:
                return action.login("86", phoneString, passwordString);
            case AUTOLOGIN:
                return action.login("86", phoneString, passwordString);
            case GETTOKEN:
                return action.getToken();
            case SYNCUSERINFO:
                return action.getUserInfoById(connectResultId);
            case SYNCGROUP:
                return action.getGroups();
            case SYNCFRIEND:
                return action.getAllUserRelationship();
        }
        return null;
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            switch (requestCode) {
                case LOGIN:
                    LoginResponse lrres = (LoginResponse) result;
                    if (lrres.getCode() == 200) {
                        loginToken = lrres.getResult().getToken();
                        if (!TextUtils.isEmpty(loginToken)) {
                            editor.putString("loginToken", loginToken);
                            editor.putString("loginphone", phoneString);
                            editor.putString("loginpassword", passwordString);
                            editor.apply();

                            RongIM.connect(loginToken, new RongIMClient.ConnectCallback() {
                                @Override
                                public void onTokenIncorrect() {
                                    NLog.e("connect", "onTokenIncorrect");
                                    reGetToken();
                                }

                                @Override
                                public void onSuccess(String s) {
                                    connectResultId = s;
                                    NLog.e("connect", "onSuccess userid:" + s);
                                    editor.putString("loginid", s);
                                    editor.apply();

                                    request(SYNCUSERINFO, true);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    NLog.e("connect", "onError errorcode:" + errorCode.getValue());
                                }
                            });
                        }
                    } else if (lrres.getCode() == 100) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    } else if (lrres.getCode() == 1000) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    }
                    break;
                case AUTOLOGIN:
                    LoginResponse autolrres = (LoginResponse) result;
                    if (autolrres.getCode() == 200) {
                        loginToken = autolrres.getResult().getToken();
                        if (!TextUtils.isEmpty(loginToken)) {
                            editor.putString("loginToken", loginToken);
                            editor.putString("loginphone", phoneString);
                            editor.putString("loginpassword", passwordString);
                            editor.apply();

                            RongIM.connect(loginToken, new RongIMClient.ConnectCallback() {
                                @Override
                                public void onTokenIncorrect() {
                                    reGetToken();
                                    NLog.e("connect", "onTokenIncorrect");
                                }

                                @Override
                                public void onSuccess(String s) {
                                    connectResultId = s;
                                    NLog.e("connect", "onSuccess userid:" + s);
                                    editor.putString("loginid", s);
                                    editor.apply();

                                    request(SYNCUSERINFO, true);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    NLog.e("connect", "onError errorcode:" + errorCode.getValue());
                                }
                            });
                        }
                    } else if (autolrres.getCode() == 100) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    } else if (autolrres.getCode() == 1000) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    }
                    break;
                case SYNCUSERINFO:
                    GetUserInfoByIdResponse guRes = (GetUserInfoByIdResponse) result;
                    if (guRes.getCode() == 200) {
                        editor.putString("loginnickname", guRes.getResult().getNickname());
                        editor.putString("loginPortrait", guRes.getResult().getPortraitUri());
                        editor.apply();

                        if (TextUtils.isEmpty(guRes.getResult().getPortraitUri())) {
                            guRes.getResult().setPortraitUri(RongGenerate.generateDefaultAvatar(guRes.getResult().getNickname(), guRes.getResult().getId()));
                        }
                        //初始化用户信息
                        RedPacketUtil.getInstance().initUserInfo(guRes.getResult().getId(), guRes.getResult().getNickname(), guRes.getResult().getPortraitUri());

                        RongIM.getInstance().setCurrentUserInfo(new UserInfo(guRes.getResult().getId(), guRes.getResult().getNickname(), Uri.parse(guRes.getResult().getPortraitUri())));
                        RongIM.getInstance().setMessageAttachedUserInfo(true);

                        List<Groups> groupList = DBManager.getInstance(mContext).getDaoSession().getGroupsDao().loadAll();
                        if (groupList.size() == 0 || groupList == null) {
                            request(SYNCGROUP);
                        } else {
                            LoadDialog.dismiss(mContext);
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            NToast.shortToast(mContext, R.string.login_success);
                            finish();
                        }
                    }
                    break;
                case SYNCGROUP:
                    GetGroupResponse ggRes = (GetGroupResponse) result;
                    if (ggRes.getCode() == 200) {
                        List<GetGroupResponse.ResultEntity> list = ggRes.getResult();
                        if (list.size() > 0 && list != null) {
                            for (GetGroupResponse.ResultEntity g : list) {
                                DBManager.getInstance(mContext).getDaoSession().getGroupsDao().insertOrReplace(
                                        new Groups(g.getGroup().getId(), g.getGroup().getName(), g.getGroup().getPortraitUri(), String.valueOf(g.getRole()))
                                );
                                NLog.e("sync_group", "-id-" + g.getGroup().getId() + "-num-" + g.getGroup().getMemberCount());
                                SharedPreferencesContext.getInstance().getSharedPreferences().edit().putInt(g.getGroup().getId(), g.getGroup().getMemberCount()).commit();
                            }
                        }
                        request(SYNCFRIEND);
                    }
                    break;
                case SYNCFRIEND:
                    UserRelationshipResponse urRes = (UserRelationshipResponse) result;
                    if (urRes.getCode() == 200) {
                        List<UserRelationshipResponse.ResultEntity> list = urRes.getResult();
                        if (list != null && list.size() > 0) {
                            for (UserRelationshipResponse.ResultEntity friend : list) {
                                if (friend.getStatus() == 20) {
                                    DBManager.getInstance(mContext).getDaoSession().getFriendDao().insertOrReplace(new Friend(
                                            friend.getUser().getId(),
                                            friend.getUser().getNickname(),
                                            friend.getUser().getPortraitUri(),
                                            friend.getDisplayName(),
                                            null,
                                            null
                                    ));
                                }
                            }

                        }
                        LoadDialog.dismiss(mContext);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        NToast.shortToast(mContext, R.string.login_success);
                        finish();
                    }
                    break;
                case GETTOKEN:
                    GetTokenResponse response = (GetTokenResponse) result;
                    if (response.getCode() == 200) {
                        String token = response.getResult().getToken();
                        if (!TextUtils.isEmpty(token)) {
                            RongIM.connect(token, new RongIMClient.ConnectCallback() {
                                @Override
                                public void onTokenIncorrect() {

                                }

                                @Override
                                public void onSuccess(String s) {
                                    connectResultId = s;
                                    NLog.e("connect", "onSuccess userid:" + s);
                                    editor.putString("loginid", s);
                                    editor.apply();

                                    request(SYNCUSERINFO, true);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {

                                }
                            });
                        }
                    }

                    break;
            }
        }
    }

    private void reGetToken() {
        request(GETTOKEN);
    }

    @Override
    public void onFailure(int requestCode, int state, Object result) {
        if (state == AsyncTaskManager.HTTP_NULL_CODE || state == AsyncTaskManager.HTTP_ERROR_CODE) {
            LoadDialog.dismiss(mContext);
            NToast.shortToast(mContext, R.string.network_not_available);
            return;
        }
        switch (requestCode) {
            case LOGIN:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, R.string.login_api_fail);
                break;
            case SYNCUSERINFO:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, R.string.sync_userinfo_api_fail);
                break;
            case GETTOKEN:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, R.string.get_token_api_fail);
                break;
            case SYNCGROUP:
                NToast.shortToast(mContext, R.string.sync_group_api_fail);
                break;
        }
    }


}
