package com.tencent.qqnt.kernel.nativeinterface;

import java.util.ArrayList;
import java.util.HashMap;

public interface IKernelMsgService {
    void deleteMsg(Contact contact, ArrayList<Long> msgIdList, IOperateCallback callback);

    void fetchLongMsg(Contact contact, long msgId);

    void fetchLongMsgWithCb(Contact contact, long msgId, IOperateCallback back);

    void getRecallMsgsByMsgId(Contact contact, ArrayList<Long> msgIdList, IMsgOperateCallback callback);

    void recallMsg(Contact contact, ArrayList<Long> msgIdList, IOperateCallback callback);

    void addLocalRecordMsg(Contact contact, long msgId, MsgElement elem, HashMap<Integer, MsgAttributeInfo> hashMap, boolean z, IOperateCallback callback);
    //void recallMsgs(Contact contact, ArrayList<GProRecallReqItem> arrayList, IGProRecallCallback iGProRecallCallback);
}
