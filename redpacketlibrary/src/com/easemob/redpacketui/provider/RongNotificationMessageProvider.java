package com.easemob.redpacketui.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.easemob.redpacketui.R;
import com.easemob.redpacketui.RedPacketUtil;
import com.easemob.redpacketui.message.RongNotificationMessage;

import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.widget.provider.IContainerItemProvider;


/**
 * 自定义红包回执消息展示模板
 *
 * @author desert
 * @date 2016-05-22
 */
// 会话界面自定义UI注解
@ProviderTag(messageContent = RongNotificationMessage.class, showWarning = false, showPortrait = false, showProgress = false, centerInHorizontal = true)
public class RongNotificationMessageProvider extends IContainerItemProvider.MessageProvider<RongNotificationMessage> {

    private Context mContext;

    public RongNotificationMessageProvider(Context mContext) {
        super();
        this.mContext = mContext;
    }

    /**
     * 初始化View
     */
    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.yzh_row_money_message, null);
        ViewHolder holder = new ViewHolder();
        holder.message = (TextView) view.findViewById(R.id.yzh_tv_money_msg);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View v, int i, RongNotificationMessage content, UIMessage message) {
        ViewHolder holder = (ViewHolder) v.getTag();
        //群红包,自己领取了自己红包,显示"你领取了自己的红包"
        //单聊红包,自己不能领取自己的红包
        holder.message.setText(getMessage(content));
    }

    @Override
    public Spannable getContentSummary(RongNotificationMessage data) {
        if (data != null)
            return new SpannableString(getMessage(data));
        return null;
    }

    public String getMessage(RongNotificationMessage content) {
        String mContent = "";
        if (TextUtils.isEmpty(content.getSendUserID()) || TextUtils.isEmpty(content.getReceiveUserID())) {
            return "";
        }
        if (content.getSendUserID().equals(content.getReceiveUserID())) {//自己领取了自己的红包
            mContent = mContext.getString(R.string.yzh_notification_me_to_me_receive_redpacket);
        } else {

            if (content.getReceiveUserID().equals(RedPacketUtil.getInstance().getUserID())) {//接受红包者
                //你领取了XX红包
                mContent = String.format(mContext.getString(R.string.yzh_notification_me_receive_redpacket), content.getSendUserName());
            } else {//红包发送者
                //XX领取了你的红包
                mContent = String.format(mContext.getString(R.string.yzh_notification_other_receive_redpacket), content.getReceiveUserName());
            }
        }
        return mContent;
    }

    @Override
    public void onItemClick(View view, int i, RongNotificationMessage rongNotificationMessage, UIMessage uiMessage) {

    }

    @Override
    public void onItemLongClick(View view, int i, RongNotificationMessage rongNotificationMessage, UIMessage uiMessage) {

    }

    class ViewHolder {
        TextView message;
    }

}
