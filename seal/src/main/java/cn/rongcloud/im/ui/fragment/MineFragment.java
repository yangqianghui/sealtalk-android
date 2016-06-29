package cn.rongcloud.im.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import cn.rongcloud.im.App;
import cn.rongcloud.im.R;
import cn.rongcloud.im.SealConst;
import cn.rongcloud.im.server.broadcast.BroadcastManager;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.widget.SelectableRoundedImageView;
import cn.rongcloud.im.ui.activity.AboutRongCloudActivity;
import cn.rongcloud.im.ui.activity.AccountSettingActivity;
import cn.rongcloud.im.ui.activity.MyAccountActivity;
import io.rong.imkit.RongIM;

/**
 * Created by AMing on 16/6/21.
 * Company RongCloud
 */
public class MineFragment extends Fragment implements View.OnClickListener {
    public static MineFragment instance = null;

    public static MineFragment getInstance() {
        if (instance == null) {
            instance = new MineFragment();
        }
        return instance;
    }

    private View mView;

    private SharedPreferences sp;

    private SelectableRoundedImageView imageView;

    private TextView mName;

    private LinearLayout mUserProfile, mMineStting, mMineService, mMineAbout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.seal_mine_fragment, null);
        initViews(mView);
        initData();
        BroadcastManager.getInstance(getActivity()).addAction(SealConst.CHANGEINFO, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String userid = sp.getString("loginid", "");
                String username = sp.getString("loginnickname", "");
                String userportrait = sp.getString("loginPortrait", "");
                mName.setText(username);
                ImageLoader.getInstance().displayImage(TextUtils.isEmpty(userportrait) ? RongGenerate.generateDefaultAvatar(username, userid) : userportrait, imageView, App.getOptions());
            }
        });
        return mView;
    }

    private void initData() {
        sp = getActivity().getSharedPreferences("config", Context.MODE_PRIVATE);
        String userid = sp.getString("loginid", "");
        String username = sp.getString("loginnickname", "");
        String userportrait = sp.getString("loginPortrait", "");
        mName.setText(username);
        ImageLoader.getInstance().displayImage(TextUtils.isEmpty(userportrait) ? RongGenerate.generateDefaultAvatar(username, userid) : userportrait, imageView, App.getOptions());
    }

    private void initViews(View mView) {
        imageView = (SelectableRoundedImageView) mView.findViewById(R.id.mine_header);
        mName = (TextView) mView.findViewById(R.id.mine_name);
        mUserProfile = (LinearLayout) mView.findViewById(R.id.start_user_profile);
        mMineStting = (LinearLayout) mView.findViewById(R.id.mine_setting);
        mMineService = (LinearLayout) mView.findViewById(R.id.mine_service);
        mMineAbout = (LinearLayout) mView.findViewById(R.id.mine_about);
        mUserProfile.setOnClickListener(this);
        mMineStting.setOnClickListener(this);
        mMineService.setOnClickListener(this);
        mMineAbout.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_user_profile:
                startActivity(new Intent(getActivity(), MyAccountActivity.class));
                break;
            case R.id.mine_setting:
                startActivity(new Intent(getActivity(), AccountSettingActivity.class));
                break;
            case R.id.mine_service:
                // KEFU146001495753714 正式  KEFU145930951497220 测试
                RongIM.getInstance().startCustomerServiceChat(getActivity(), "KEFU146001495753714", "在线客服", null);
                break;
            case R.id.mine_about:
                startActivity(new Intent(getActivity(), AboutRongCloudActivity.class));
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BroadcastManager.getInstance(getActivity()).destroy(SealConst.CHANGEINFO);
    }
}
