package cn.rongcloud.im.ui.activity;

import android.os.Bundle;

import cn.rongcloud.im.R;

/**
 * Created by AMing on 16/7/1.
 * Company RongCloud
 */
public class TotalGroupMemberActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toatl_member);
        getSupportActionBar().setTitle(R.string.total_member);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
