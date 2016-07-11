package com.yunzhanghu.redpacketui.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.yunzhanghu.redpacketsdk.bean.RedPacketInfo;
import com.yunzhanghu.redpacketsdk.constant.RPConstant;
import com.yunzhanghu.redpacketui.R;
import com.yunzhanghu.redpacketui.RedPacketUtil;
import com.yunzhanghu.redpacketui.message.RongRedPacketMessage;
import com.yunzhanghu.redpacketui.ui.activity.RPRedPacketActivity;

import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.widget.provider.InputProvider;
import io.rong.imlib.RongIMClient;

/**
 * 自定义扩展栏红包提供者
 *
 * @author desert
 * @date 2016-05-17
 */
public class RongRedPacketProvider extends InputProvider.ExtendProvider {

    private static final String TAG = "RedPacketLibrary";

    HandlerThread mWorkThread;

    Handler mUploadHandler;

    public RongRedPacketProvider(RongContext context) {
        super(context);
        mWorkThread = new HandlerThread("YZHRedPacket");
        mWorkThread.start();
        mUploadHandler = new Handler(mWorkThread.getLooper());
    }

    /**
     * 设置展示的图标
     *
     * @param context
     * @return
     */
    @Override
    public Drawable obtainPluginDrawable(Context context) {
        return ContextCompat.getDrawable(context,R.drawable.yzh_chat_money_provider);
    }

    /**
     * 设置图标下的title
     *
     * @param context
     * @return
     */
    @Override
    public CharSequence obtainPluginTitle(Context context) {
        return context.getString(R.string.red_packet);
    }

    /**
     * click 事件，在这里做跳转
     *
     * @param view
     */
    @Override
    public void onPluginClick(View view) {
        final Intent intent = new Intent(getContext(), RPRedPacketActivity.class);
        final RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.fromAvatarUrl = RedPacketUtil.getInstance().getUserAvatar();//发送者头像
        redPacketInfo.fromNickName = RedPacketUtil.getInstance().getUserName();//发送者名字
        redPacketInfo.toUserId = getCurrentConversation().getTargetId(); //接受者id
        redPacketInfo.chatType = RPConstant.CHATTYPE_SINGLE;//单聊
        //跳转到发红包界面
        intent.putExtra(RPConstant.EXTRA_RED_PACKET_INFO, redPacketInfo);
        intent.putExtra(RPConstant.EXTRA_TOKEN_DATA, RedPacketUtil.getInstance().getTokenData());
        startActivityForResult(intent, RedPacketUtil.REQUEST_CODE_SEND_MONEY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //接受返回的红包信息,并发送红包消息
        if (resultCode == Activity.RESULT_OK && data != null && requestCode == RedPacketUtil.REQUEST_CODE_SEND_MONEY) {
            String greeting = data.getStringExtra(RPConstant.EXTRA_RED_PACKET_GREETING);//祝福语
            String moneyID = data.getStringExtra(RPConstant.EXTRA_RED_PACKET_ID);//红包ID
            String userId = RedPacketUtil.getInstance().getUserID();//发送者ID
            String userName = RedPacketUtil.getInstance().getUserName();//发送者名字
            String sponsor = getContext().getString(R.string.sponsor_red_packet);//XX红包
            RongRedPacketMessage message = RongRedPacketMessage.obtain(userId, userName,
                    greeting, moneyID, "1", sponsor, "", "");
            Log.e(TAG, "--发送红包返回参数--" + "-moneyID-" + moneyID + "-greeting-" + greeting);
            //发送红包消息到聊天界面
            mUploadHandler.post(new MyRunnable(message));
        }
    }

    class MyRunnable implements Runnable {

        RongRedPacketMessage mMessage;

        public MyRunnable(RongRedPacketMessage message) {
            mMessage = message;
        }

        @Override
        public void run() {
            if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {

                RongIM.getInstance().getRongIMClient().sendMessage(getCurrentConversation().getConversationType(),
                        getCurrentConversation().getTargetId(), mMessage, null, null, new RongIMClient.SendMessageCallback() {
                            @Override
                            public void onError(Integer integer, RongIMClient.ErrorCode errorCode) {
                                Log.e(TAG, "--onError--" + errorCode);
                            }

                            @Override
                            public void onSuccess(Integer integer) {
                                Log.e(TAG, "--onSuccess--" + integer);
                            }
                        }, null);
            }

        }
    }

}
