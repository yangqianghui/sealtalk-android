package cn.rongcloud.im.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import cn.rongcloud.im.App;
import cn.rongcloud.im.R;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.GetGroupResponse;
import cn.rongcloud.im.server.response.UserRelationshipResponse;
import cn.rongcloud.im.server.utils.NLog;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.widget.LoadDialog;
import cn.rongcloud.im.server.widget.SelectableRoundedImageView;
import cn.rongcloud.im.ui.widget.linkpreview.LinkPreviewCallback;
import cn.rongcloud.im.ui.widget.linkpreview.SourceContent;
import cn.rongcloud.im.ui.widget.linkpreview.TextCrawler;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.message.TextMessage;

/**
 * Created by AMing on 16/7/12.
 * Company RongCloud
 */
public class SharedReceiverActivity extends BaseActivity {

    private static final int GROUPALL = 911;  // 网络请求数据后期可换成数据库拿数据
    private static final int FRIENDALL = 912;// 网络请求数据后期可换成数据库拿数据

    private List<Conversation> conversationsList;

    private List<GetGroupResponse.ResultEntity> mGroupDatas;

    private List<UserRelationshipResponse.ResultEntity> mFriendDatas;

    private List<NewConversation> newConversationsList = new ArrayList<>();

    private ListView shareListView;

    private BaseAdapter ShareAdapter;

    private TextCrawler textCrawler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_receiver);
        initVies();
        LoadDialog.show(mContext);

        /** 异步取数据的部分 包括群组数据 和 单聊数据 **/
        if (RongIM.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)) {
            getConversations();
        } else {
            String cacheToken = getSharedPreferences("config", MODE_PRIVATE).getString("loginToken", "");
            if (!TextUtils.isEmpty(cacheToken)) {
                RongIM.connect(cacheToken, new RongIMClient.ConnectCallback() {
                    @Override
                    public void onTokenIncorrect() {

                    }

                    @Override
                    public void onSuccess(String s) {
                        getConversations();
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                });
            }
        }

        Intent intent = getIntent();
        if (intent != null) {
            /** 截获 Intent 部分 **/

            textCrawler = new TextCrawler();

            /** --- From ShareVia Intent */
            if (getIntent().getExtras() != null) {
                String shareVia = (String) getIntent().getExtras().get(Intent.EXTRA_TEXT);
                if (shareVia != null) {
                    textCrawler.makePreview(callback, shareVia);
                }
            }


        }


    }

    private void initVies() {
        shareListView = (ListView) findViewById(R.id.share_listview);
        shareListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() || !networkInfo.isAvailable()) {
                    Toast.makeText(mContext, getString(R.string.network_not_available), Toast.LENGTH_SHORT).show();
                    return;
                }


                if (newConversationsList != null) {


                    final AlertDialog dlg = new AlertDialog.Builder(mContext).create();
                    dlg.show();
                    dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                    dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    Window window = dlg.getWindow();
                    window.setContentView(R.layout.share_dialog);
                    Button ok = (Button) window.findViewById(R.id.share_ok);
                    Button cancel = (Button) window.findViewById(R.id.share_cancel);
                    TextView title = (TextView) window.findViewById(R.id.share_title);
                    TextView content = (TextView) window.findViewById(R.id.share_cotent);
                    ImageView image = (ImageView) window.findViewById(R.id.share_image);
                    TextView  from = (TextView) window.findViewById(R.id.share_from);
                    final EditText say = (EditText) window.findViewById(R.id.share_say);

                    if (!TextUtils.isEmpty(titleString)) {
                        title.setText(titleString);
                    }

                    if (!TextUtils.isEmpty(imageString)) {
                        ImageLoader.getInstance().displayImage(imageString, image);
                    }

                    if (!TextUtils.isEmpty(description)) {
                        content.setText(description);
                    }

                    if (!TextUtils.isEmpty(fromString)) {
                        from.setText("来自:" + fromString);
                    } else {
                        from.setVisibility(View.GONE);
                    }



                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Conversation.ConversationType conversationType = newConversationsList.get(position).getmConversationType();
                            String targetId = newConversationsList.get(position).getTargetId();
                            LoadDialog.show(mContext);

                            if (!TextUtils.isEmpty(say.getText().toString().trim())) {
                                RongIM.getInstance().sendMessage(conversationType, targetId, TextMessage.obtain(say.getText().toString().trim()), null, null, new RongIMClient.SendMessageCallback() {
                                    @Override
                                    public void onError(Integer messageId, RongIMClient.ErrorCode e) {

                                    }

                                    @Override
                                    public void onSuccess(Integer integer) {

                                    }
                                });
                            }
                            NLog.e("share", "分享:" + titleString + "\n" + finalUri + "\n" + "来自:" + fromString);
                            RongIM.getInstance().sendMessage(conversationType, targetId, TextMessage.obtain("分享:" + titleString + "\n" + finalUri + "\n" + "来自:" + fromString), null, null, new RongIMClient.SendMessageCallback() {
                                @Override
                                public void onError(Integer messageId, RongIMClient.ErrorCode e) {

                                }

                                @Override
                                public void onSuccess(Integer integer) {

                                    LoadDialog.dismiss(mContext);
                                    NToast.shortToast(mContext, "分享成功");
                                }
                            });



                            dlg.cancel();
                        }
                    });
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dlg.cancel();
                        }
                    });
                }
            }
        });
    }


    class ShareAdapter extends BaseAdapter {

        private List<NewConversation> list;

        private Context context;

        private ViewHolder holder;


        public ShareAdapter(List<NewConversation> newConversationsList, Context mContext) {
            this.list = newConversationsList;
            this.context = mContext;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.share_item, null);
                holder.mImageView = (SelectableRoundedImageView) convertView.findViewById(R.id.share_icon);
                holder.title = (TextView) convertView.findViewById(R.id.share_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            NewConversation bean = list.get(position);
            ImageLoader.getInstance().displayImage(bean.getPortraitUri(), holder.mImageView, App.getOptions());
            holder.title.setText(bean.getTitle());
            return convertView;
        }
    }


    final static class ViewHolder {
        /**
         * 头像
         */
        SelectableRoundedImageView mImageView;

        TextView title;
    }


    private void getConversations() {
        final Conversation.ConversationType[] conversationTypes = {
            Conversation.ConversationType.PRIVATE,
            Conversation.ConversationType.GROUP,
        };
        if (RongIM.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)) {
            RongIM.getInstance().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                @Override
                public void onSuccess(List<Conversation> conversations) {
                    conversationsList = conversations;
                    request(GROUPALL);

                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            }, conversationTypes);
        }

    }


    @Override
    public Object doInBackground(int requsetCode, String id) throws HttpException {
        switch (requsetCode) {
            case GROUPALL:
                return action.getGroups();
            case FRIENDALL:
                return action.getAllUserRelationship();
        }
        return null;
    }


    @Override
    public void onSuccess(int requestCode, Object result) {
        switch (requestCode) {
            case GROUPALL:
                GetGroupResponse response = (GetGroupResponse) result;
                if (response.getCode() == 200) {
                    mGroupDatas = response.getResult();
                    if (mGroupDatas != null) {
                        request(FRIENDALL);
                    }
                }
                break;
            case FRIENDALL:
                UserRelationshipResponse urRes = (UserRelationshipResponse) result;
                if (urRes.getCode() == 200) {
                    mFriendDatas = urRes.getResult();

                    /** Start 双重循环过滤已经被解散或者退出的群组数据  **/
                    List<Conversation> tempList = new ArrayList<>();
                    for (Conversation c : conversationsList) {
                        if (c.getConversationType().equals(Conversation.ConversationType.GROUP)) {
                            for (GetGroupResponse.ResultEntity gr : mGroupDatas) {
                                if (gr.getGroup().getId().equals(c.getTargetId())) {
                                    tempList.add(c);
                                }
                            }
                        } else { // 后期如果做删除好友接口后也可能需要处理 private 类型的数据
                            tempList.add(c);
                        }
                    }
                    /** End **/

                    if (tempList.size() > 0) {
                        for (Conversation conversation : tempList) {
                            if (conversation.getConversationType().equals(Conversation.ConversationType.PRIVATE)) {
                                newConversationsList.add(new NewConversation(Conversation.ConversationType.PRIVATE, conversation.getTargetId(), TextUtils.isEmpty(getUserInfoById(conversation.getTargetId()).getUser().getPortraitUri()) ? RongGenerate.generateDefaultAvatar(getUserInfoById(conversation.getTargetId()).getUser().getNickname(), getUserInfoById(conversation.getTargetId()).getUser().getId()) : getUserInfoById(conversation.getTargetId()).getUser().getPortraitUri(), getUserInfoById(conversation.getTargetId()).getUser().getNickname()));
                            } else {
                                newConversationsList.add(new NewConversation(Conversation.ConversationType.GROUP, conversation.getTargetId(), TextUtils.isEmpty(getGroupInfoById(conversation.getTargetId()).getGroup().getPortraitUri()) ? RongGenerate.generateDefaultAvatar(getGroupInfoById(conversation.getTargetId()).getGroup().getName(), getGroupInfoById(conversation.getTargetId()).getGroup().getId()) : getGroupInfoById(conversation.getTargetId()).getGroup().getPortraitUri(), getGroupInfoById(conversation.getTargetId()).getGroup().getName()));
                            }
                        }
                        if (newConversationsList != null && newConversationsList.size() > 0) {
                            ShareAdapter = new ShareAdapter(newConversationsList, mContext);
                            shareListView.setAdapter(ShareAdapter);
                            LoadDialog.dismiss(mContext);
                        }
                    }

                }
                break;
        }
    }

    class NewConversation {
        Conversation.ConversationType mConversationType;
        String targetId;
        String portraitUri;
        String title;

        public NewConversation(Conversation.ConversationType mConversationType, String targetId, String portraitUri, String title) {
            this.mConversationType = mConversationType;
            this.targetId = targetId;
            this.portraitUri = portraitUri;
            this.title = title;
        }

        public Conversation.ConversationType getmConversationType() {
            return mConversationType;
        }

        public void setmConversationType(Conversation.ConversationType mConversationType) {
            this.mConversationType = mConversationType;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public String getPortraitUri() {
            return portraitUri;
        }

        public void setPortraitUri(String portraitUri) {
            this.portraitUri = portraitUri;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }


    private UserRelationshipResponse.ResultEntity getUserInfoById(String userId) {
        if (mFriendDatas != null) {
            for (UserRelationshipResponse.ResultEntity ur : mFriendDatas) {
                if (ur.getUser().getId().equals(userId)) {
                    return ur;
                }
            }
        }
        return null;
    }

    private GetGroupResponse.ResultEntity getGroupInfoById(String groupId) {
        if (mGroupDatas != null) {
            for (GetGroupResponse.ResultEntity gr : mGroupDatas) {
                if (gr.getGroup().getId().equals(groupId)) {
                    return gr;
                }
            }
        }
        return null;
    }

    private String imageString;

    private String fromString;

    private String description;

    private String titleString;

    private String finalUri;
    /** Callback to update your view. Totally customizable. */
    /** onPre() will be called before the crawling. onPos() after. */
    /**
     * You can customize this to update your view
     */
    private LinkPreviewCallback callback = new LinkPreviewCallback() {

        @Override
        public void onPre() {
            NLog.e("share", "onPre");
            LoadDialog.show(mContext);
        }

        @Override
        public void onPos(SourceContent sourceContent, boolean isNull) {
            if (sourceContent != null) {
                NLog.e("share", sourceContent.getImages().size());
                NLog.e("share", sourceContent.getCannonicalUrl());
                NLog.e("share", sourceContent.getDescription());
                NLog.e("share", sourceContent.getFinalUrl());
                NLog.e("share", sourceContent.getTitle());

                if (sourceContent.getImages().size() > 0) {
                    imageString = sourceContent.getImages().get(0);
                }
                fromString = sourceContent.getCannonicalUrl();
                description = sourceContent.getDescription();
                titleString = sourceContent.getTitle();
                finalUri = sourceContent.getFinalUrl();
                LoadDialog.dismiss(mContext);
            }
        }
    };

}
