package cn.rongcloud.im.ui.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import cn.rongcloud.im.R;
import cn.rongcloud.im.SealConst;
import cn.rongcloud.im.server.broadcast.BroadcastManager;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.SetNameResponse;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.widget.ClearWriteEditText;
import cn.rongcloud.im.server.widget.LoadDialog;
import io.rong.imkit.RongIM;
import io.rong.imlib.model.UserInfo;

/**
 * Created by AMing on 16/6/23.
 * Company RongCloud
 */
public class UpdateNameActivity extends BaseActivity {

    private static final int UPDATENAME = 7;

    private ClearWriteEditText mNameEditText;

    private String newName;

    private SharedPreferences sp;

    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.de_actionbar_back);
        getSupportActionBar().setTitle(R.string.nickname);
        mNameEditText = (ClearWriteEditText) findViewById(R.id.update_name);
        sp = getSharedPreferences("config", MODE_PRIVATE);
        editor = sp.edit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.de_select_ok, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.ok) {
            newName = mNameEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(newName)) {
                LoadDialog.show(mContext);
                request(UPDATENAME, true);
            } else {
                NToast.shortToast(mContext, "昵称不能为空");
                mNameEditText.setShakeAnimation();
            }
        } else {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Object doInBackground(int requsetCode, String id) throws HttpException {
        return action.setName(newName);
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        SetNameResponse sRes = (SetNameResponse) result;
        if (sRes.getCode() == 200) {
            editor.putString("loginnickname", newName);
            editor.commit();

            BroadcastManager.getInstance(mContext).sendBroadcast(SealConst.CHANGEINFO);

            RongIM.getInstance().refreshUserInfoCache(new UserInfo(sp.getString("loginid", ""), newName, Uri.parse(sp.getString("loginPortrait", ""))));
            RongIM.getInstance().setCurrentUserInfo(new UserInfo(sp.getString("loginid", ""), newName, Uri.parse(sp.getString("loginPortrait", ""))));

            LoadDialog.dismiss(mContext);
            NToast.shortToast(mContext, "昵称更改成功");
            finish();
        }
    }
}
