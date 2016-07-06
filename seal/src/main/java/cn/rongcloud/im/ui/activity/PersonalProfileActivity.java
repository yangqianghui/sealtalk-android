package cn.rongcloud.im.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import cn.rongcloud.im.App;
import cn.rongcloud.im.R;
import cn.rongcloud.im.SealAppContext;
import cn.rongcloud.im.db.DBManager;
import cn.rongcloud.im.db.FriendDao;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.FriendInvitationResponse;
import cn.rongcloud.im.server.response.GetGroupInfoResponse;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.widget.DialogWithYesOrNoUtils;
import cn.rongcloud.im.server.widget.LoadDialog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

/**
 * Created by AMing on 16/6/22.
 * Company RongCloud
 */
public class PersonalProfileActivity extends BaseActivity implements View.OnClickListener {

    private static final int ADDFRIEND = 10086;
    private ImageView mPersonalPortrait;

    private TextView mPersonalName;

    private Button mSendMessage, mAddFriend;

    private UserInfo userInfo;

    private String mySelf, addMessage;

    private Conversation.ConversationType mConversationType;

    private GetGroupInfoResponse.ResultEntity mGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal);
        getSupportActionBar().setTitle(R.string.user_details);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.de_actionbar_back);
        initViews();
        userInfo = getIntent().getParcelableExtra("userinfo");
        mGroup = (GetGroupInfoResponse.ResultEntity) getIntent().getSerializableExtra("groupinfo");
        int type = getIntent().getIntExtra("conversationType", 0);
        mConversationType = Conversation.ConversationType.setValue(type);
        initData(userInfo);


    }

    private void initData(UserInfo userInfo) {
        mPersonalName.setText(userInfo.getName());
        ImageLoader.getInstance().displayImage(userInfo.getPortraitUri().toString(), mPersonalPortrait, App.getOptions());
        if (userInfo != null && !TextUtils.isEmpty(userInfo.getUserId())) {
            mySelf = getSharedPreferences("config", MODE_PRIVATE).getString("loginid", "");
            if (mySelf.equals(userInfo.getUserId())) {
                mSendMessage.setVisibility(View.VISIBLE);
                mSendMessage.setOnClickListener(this);
                return;
            }
            if (getFriendShip(userInfo.getUserId())) {
                mSendMessage.setVisibility(View.VISIBLE);
            } else {
                mAddFriend.setVisibility(View.VISIBLE);
            }

            mSendMessage.setOnClickListener(this);
            mAddFriend.setOnClickListener(this);
        }
    }

    private void initViews() {
        mPersonalPortrait = (ImageView) findViewById(R.id.per_friend_header);
        mPersonalName = (TextView) findViewById(R.id.per_friend_name);
        mSendMessage = (Button) findViewById(R.id.per_start_friend_chat);
        mAddFriend = (Button) findViewById(R.id.per_add_friend);
    }

    /**
     * 从本地缓存的数据库中查询是否存在好友关系
     * @param userid
     * @return
     */
    private boolean getFriendShip(String userid) {
        if (DBManager.getInstance(mContext).getDaoSession().getFriendDao().queryBuilder().where(FriendDao.Properties.UserId.eq(userid)).unique() != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.per_start_friend_chat:
                if (SealAppContext.getInstance().containsInQue(mConversationType, userInfo.getUserId())) {
                    finish();
                } else {
                    RongIM.getInstance().startPrivateChat(mContext, userInfo.getUserId(), userInfo.getName());
                }
                break;
            case R.id.per_add_friend:
                DialogWithYesOrNoUtils.getInstance().showEditDialog(mContext, getString(R.string.add_text), getString(R.string.confirm), new DialogWithYesOrNoUtils.DialogCallBack() {
                    @Override
                    public void exectEvent() {

                    }

                    @Override
                    public void exectEditEvent(String editText) {
                        if (TextUtils.isEmpty(editText)) {
                            if (mGroup != null && !TextUtils.isEmpty(mGroup.getName())) {
                                addMessage = "我是" + mGroup.getName() + "群的" + getSharedPreferences("config", MODE_PRIVATE).getString("loginnickname", "");
                            }else {
                                addMessage = "我是" + getSharedPreferences("config", MODE_PRIVATE).getString("loginnickname", "");
                            }
                        } else {
                            addMessage = editText;
                        }
                        LoadDialog.show(mContext);
                        request(ADDFRIEND);
                    }

                    @Override
                    public void updatePassword(String oldPassword, String newPassword) {

                    }
                });
                break;
        }
    }

    @Override
    public Object doInBackground(int requsetCode, String id) throws HttpException {
        return action.sendFriendInvitation(userInfo.getUserId(), addMessage);
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            FriendInvitationResponse response = (FriendInvitationResponse) result;
            if (response.getCode() == 200) {
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, getString(R.string.request_success));
                this.finish();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}
