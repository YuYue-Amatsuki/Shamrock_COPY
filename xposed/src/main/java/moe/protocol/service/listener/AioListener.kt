@file:OptIn(DelicateCoroutinesApi::class)
package moe.protocol.service.listener

import moe.protocol.service.HttpService
import moe.protocol.servlet.helper.MessageHelper
import com.tencent.qqnt.kernel.nativeinterface.*
import moe.protocol.servlet.msg.toCQCode
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.fuqiuluo.xposed.helper.Level
import moe.fuqiuluo.xposed.helper.LogCenter
import java.util.ArrayList
import java.util.HashMap

internal object AioListener: IKernelMsgListener {
    override fun onRecvMsg(msgList: ArrayList<MsgRecord>) {
        if (msgList.isEmpty()) return

        GlobalScope.launch {
            msgList.forEach {
                handleMsg(it)
            }
        }
    }

    private suspend fun handleMsg(record: MsgRecord) {
        try {
            val rawMsg = record.elements.toCQCode(record.chatType)
            if (rawMsg.isEmpty()) return
            val msgHash = MessageHelper.convertMsgIdToMsgHash(record.chatType, record.msgId, record.peerUin)
            when (record.chatType) {
                MsgConstant.KCHATTYPEGROUP -> {
                    LogCenter.log("群消息(group = ${record.peerName}(${record.peerUin}), uin = ${record.senderUin}, msg = $rawMsg)")
                    MessageHelper.saveMsgSeqByMsgId(record.chatType, record.msgId, record.msgSeq)
                    HttpService.pushGroupMsg(record, record.elements, rawMsg, msgHash)
                }
                MsgConstant.KCHATTYPEC2C -> {
                    LogCenter.log("私聊消息(private = ${record.senderUin}, msg = $rawMsg)")
                    MessageHelper.saveMsgSeqByMsgId(record.chatType, record.msgId, record.msgSeq)
                    HttpService.pushPrivateMsg(record, record.elements, rawMsg, msgHash)
                }
                else -> LogCenter.log("不支持PUSH事件: ${record.chatType}")
            }
        } catch (e: Throwable) {
            LogCenter.log(e.stackTraceToString(), Level.WARN)
        }
    }

    override fun onAddSendMsg(record: MsgRecord) {
        GlobalScope.launch {
            LogCenter.log("发送消息: " + record.toCQCode())
        }
    }

    override fun onRecvMsgSvrRspTransInfo(
        j2: Long,
        contact: Contact?,
        i2: Int,
        i3: Int,
        str: String?,
        bArr: ByteArray?
    ) {
        LogCenter.log("onRecvMsgSvrRspTransInfo($j2, $contact, $i2, $i3, $str)", Level.DEBUG)
    }

    override fun onRecvOnlineFileMsg(arrayList: ArrayList<MsgRecord>?) {
        LogCenter.log(("onRecvOnlineFileMsg" + arrayList?.joinToString { ", " }), Level.DEBUG)
    }

    override fun onRecvS2CMsg(arrayList: ArrayList<Byte>?) {
        LogCenter.log("onRecvS2CMsg(${arrayList.toString()})", Level.DEBUG)
    }

    override fun onRecvSysMsg(arrayList: ArrayList<Byte>?) {
        LogCenter.log("onRecvSysMsg(${arrayList.toString()})", Level.DEBUG)
    }

    override fun onBroadcastHelperDownloadComplete(broadcastHelperTransNotifyInfo: BroadcastHelperTransNotifyInfo?) {}

    override fun onBroadcastHelperProgerssUpdate(broadcastHelperTransNotifyInfo: BroadcastHelperTransNotifyInfo?) {}

    override fun onChannelFreqLimitInfoUpdate(
        contact: Contact?,
        z: Boolean,
        freqLimitInfo: FreqLimitInfo?
    ) {

    }

    override fun onContactUnreadCntUpdate(unreadMap: HashMap<Int, HashMap<String, UnreadCntInfo>>) {
        // 推送未读消息数量
    }

    override fun onCustomWithdrawConfigUpdate(customWithdrawConfig: CustomWithdrawConfig?) {
        LogCenter.log("onCustomWithdrawConfigUpdate: " + customWithdrawConfig.toString(), Level.DEBUG)
    }

    override fun onDraftUpdate(contact: Contact?, arrayList: ArrayList<MsgElement>?, j2: Long) {
        LogCenter.log("onDraftUpdate: " + contact.toString() + "|" + arrayList + "|" + j2.toString(), Level.DEBUG)
    }

    override fun onEmojiDownloadComplete(emojiNotifyInfo: EmojiNotifyInfo?) {

    }

    override fun onEmojiResourceUpdate(emojiResourceInfo: EmojiResourceInfo?) {

    }

    override fun onFeedEventUpdate(firstViewDirectMsgNotifyInfo: FirstViewDirectMsgNotifyInfo?) {

    }

    override fun onFileMsgCome(arrayList: ArrayList<MsgRecord>?) {
        LogCenter.log("onFileMsgCome: " + arrayList.toString(), Level.DEBUG)
    }

    override fun onFirstViewDirectMsgUpdate(firstViewDirectMsgNotifyInfo: FirstViewDirectMsgNotifyInfo?) {

    }

    override fun onFirstViewGroupGuildMapping(arrayList: ArrayList<FirstViewGroupGuildInfo>?) {

    }

    override fun onGrabPasswordRedBag(
        i2: Int,
        str: String?,
        i3: Int,
        recvdOrder: RecvdOrder?,
        msgRecord: MsgRecord?
    ) {

    }

    override fun onGroupFileInfoAdd(groupItem: GroupItem?) {
        LogCenter.log("onGroupFileInfoAdd: " + groupItem.toString(), Level.DEBUG)
    }

    override fun onGroupFileInfoUpdate(groupFileListResult: GroupFileListResult?) {
        LogCenter.log("onGroupFileInfoUpdate: " + groupFileListResult.toString(), Level.DEBUG)
    }

    override fun onGroupGuildUpdate(groupGuildNotifyInfo: GroupGuildNotifyInfo?) {
        LogCenter.log("onGroupGuildUpdate: " + groupGuildNotifyInfo.toString(), Level.DEBUG)
    }

    override fun onGroupTransferInfoAdd(groupItem: GroupItem?) {
        LogCenter.log("onGroupTransferInfoAdd: " + groupItem.toString(), Level.DEBUG)
    }

    override fun onGroupTransferInfoUpdate(groupFileListResult: GroupFileListResult?) {
        LogCenter.log("onGroupTransferInfoUpdate: " + groupFileListResult.toString(), Level.DEBUG)
    }

    override fun onHitCsRelatedEmojiResult(downloadRelateEmojiResultInfo: DownloadRelateEmojiResultInfo?) {

    }

    override fun onHitEmojiKeywordResult(hitRelatedEmojiWordsResult: HitRelatedEmojiWordsResult?) {

    }

    override fun onHitRelatedEmojiResult(relatedWordEmojiInfo: RelatedWordEmojiInfo?) {

    }

    override fun onImportOldDbProgressUpdate(importOldDbMsgNotifyInfo: ImportOldDbMsgNotifyInfo?) {

    }

    override fun onInputStatusPush(inputStatusInfo: InputStatusInfo?) {

    }

    override fun onKickedOffLine(kickedInfo: KickedInfo?) {
        LogCenter.log("onKickedOffLine($kickedInfo)")
    }

    override fun onLineDev(devList: ArrayList<DevInfo>?) {
        //LogCenter.log("onLineDev($arrayList)")
    }

    override fun onLogLevelChanged(j2: Long) {

    }

    override fun onMsgAbstractUpdate(arrayList: ArrayList<MsgAbstract>?) {

    }

    override fun onMsgBoxChanged(arrayList: ArrayList<ContactMsgBoxInfo>?) {

    }

    override fun onMsgDelete(contact: Contact?, arrayList: ArrayList<Long>?) {

    }

    override fun onMsgEventListUpdate(hashMap: HashMap<String, ArrayList<Long>>?) {

    }

    override fun onMsgInfoListAdd(arrayList: ArrayList<MsgRecord>?) {

    }

    override fun onMsgInfoListUpdate(arrayList: ArrayList<MsgRecord>?) {

    }

    override fun onMsgQRCodeStatusChanged(i2: Int) {

    }

    override fun onMsgRecall(i2: Int, str: String?, j2: Long) {
        LogCenter.log("onMsgRecall($i2, $str, $j2)")
    }

    override fun onMsgSecurityNotify(msgRecord: MsgRecord?) {
        LogCenter.log("onMsgSecurityNotify($msgRecord)")
    }

    override fun onMsgSettingUpdate(msgSetting: MsgSetting?) {

    }

    override fun onNtFirstViewMsgSyncEnd() {

    }

    override fun onNtMsgSyncEnd() {
        LogCenter.log("NTKernel同步消息完成", Level.DEBUG)
    }

    override fun onNtMsgSyncStart() {
        LogCenter.log("NTKernel同步消息开始", Level.DEBUG)
    }

    override fun onReadFeedEventUpdate(firstViewDirectMsgNotifyInfo: FirstViewDirectMsgNotifyInfo?) {

    }

    override fun onRecvGroupGuildFlag(i2: Int) {

    }

    override fun onRecvUDCFlag(i2: Int) {
        LogCenter.log("onRecvUDCFlag($i2)", Level.DEBUG)
    }

    override fun onRichMediaDownloadComplete(fileTransNotifyInfo: FileTransNotifyInfo?) {
        LogCenter.log("onRichMediaDownloadComplete($fileTransNotifyInfo)", Level.DEBUG)
    }

    override fun onRichMediaProgerssUpdate(fileTransNotifyInfo: FileTransNotifyInfo?) {

    }

    override fun onRichMediaUploadComplete(fileTransNotifyInfo: FileTransNotifyInfo?) {

    }

    override fun onSearchGroupFileInfoUpdate(searchGroupFileResult: SearchGroupFileResult?) {

    }

    override fun onSendMsgError(j2: Long, contact: Contact?, i2: Int, str: String?) {
        LogCenter.log("onSendMsgError($j2, $contact, $j2, $str)", Level.DEBUG)
    }

    override fun onSysMsgNotification(i2: Int, j2: Long, j3: Long, arrayList: ArrayList<Byte>?) {
        LogCenter.log("onSysMsgNotification($i2, $j2, $j3, $arrayList)", Level.DEBUG)
    }

    override fun onTempChatInfoUpdate(tempChatInfo: TempChatInfo?) {

    }

    override fun onUnreadCntAfterFirstView(hashMap: HashMap<Int, ArrayList<UnreadCntInfo>>?) {

    }

    override fun onUnreadCntUpdate(hashMap: HashMap<Int, ArrayList<UnreadCntInfo>>?) {

    }

    override fun onUserChannelTabStatusChanged(z: Boolean) {

    }

    override fun onUserOnlineStatusChanged(z: Boolean) {

    }

    override fun onUserTabStatusChanged(arrayList: ArrayList<TabStatusInfo>?) {
        LogCenter.log("onUserTabStatusChanged($arrayList)", Level.DEBUG)
    }

    override fun onlineStatusBigIconDownloadPush(i2: Int, j2: Long, str: String?) {

    }

    override fun onlineStatusSmallIconDownloadPush(i2: Int, j2: Long, str: String?) {

    }
}