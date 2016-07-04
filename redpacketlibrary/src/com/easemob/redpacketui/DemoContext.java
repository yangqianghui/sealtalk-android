package com.easemob.redpacketui;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;

import io.rong.imkit.RongIM;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;

/**
 * Created by Bob on 2015/1/30.
 */
public class DemoContext {

    private static DemoContext mDemoContext;
    public Context mContext;
    private HashMap<String, String> myGroupMap;
    private HashMap<String, List<UserInfo>> groupMemberMap;
    private HashMap<String, Group> groupMap;
    private SharedPreferences mPreferences;
    private RongIM.LocationProvider.LocationCallback mLastLocationCallback;


//    public static void init(Context context) {
//        mDemoContext = new DemoContext(context);
//    }

    public static DemoContext getInstance() {

        if (mDemoContext == null) {
            mDemoContext = new DemoContext();
        }
        return mDemoContext;
    }

    private DemoContext() {

    }


    /**
     * 通过groupid 获得groupname
     *
     * @param groupid
     * @return
     */
    public String getGroupNameById(String groupid) {
        Group groupReturn = null;
        if (!TextUtils.isEmpty(groupid) && groupMap != null) {

            if (groupMap.containsKey(groupid)) {
                groupReturn = groupMap.get(groupid);
            } else
                return null;

        }
        if (groupReturn != null)
            return groupReturn.getName();
        else
            return null;
    }

    /**
     * 通过群ID获取群里面的当前人的个数
     *
     * @param groupID
     * @return
     */
    public String getGroupNumberById(String groupID) {

        if (TextUtils.isEmpty(groupID)) {
            return null;
        } else if (myGroupMap != null && myGroupMap.containsKey(groupID)) {
            return myGroupMap.get(groupID);
        }
        return null;
    }

    /**
     * 缓存群成员个数信息
     *
     * @param groupID
     * @return
     */

    public void putGroupNumber(String groupID, String number) {

        if (myGroupMap == null) {
            myGroupMap = new HashMap<String, String>();
        }
        myGroupMap.put(groupID, number);

    }

    /**
     * 根据群ID删除群组信息
     *
     * @param groupID
     * @return
     */
    public void removeGroupNumberById(String groupID) {

        if (myGroupMap == null && myGroupMap.containsKey(groupID)) {
            myGroupMap.remove(groupID);
        }
    }

    /**
     * 通过群ID获取群成员信息
     *
     * @param groupID
     * @return
     */
    public List<UserInfo> getGroupMemberById(String groupID) {

        if (TextUtils.isEmpty(groupID)) {
            return null;
        } else if (groupMemberMap != null && groupMemberMap.containsKey(groupID)) {
            return groupMemberMap.get(groupID);
        }
        return null;
    }

    /**
     * 缓存群成员信息
     *
     * @param groupID
     * @return
     */

    public void putGroupMember(String groupID, List<UserInfo> data) {

        if (groupMemberMap == null) {
            groupMemberMap = new HashMap<>();
        }
        groupMemberMap.put(groupID, data);

    }

    /**
     * 根据群ID删除群成员信息
     *
     * @param groupID
     * @return
     */
    public void removeGroupMemberById(String groupID) {

        if (groupMemberMap == null && groupMemberMap.containsKey(groupID)) {
            groupMemberMap.remove(groupID);
        }
    }
}
