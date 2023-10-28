package moe.protocol.servlet.protocol

import com.tencent.mobileqq.qroute.QRoute
import moe.protocol.servlet.helper.ContactHelper
import moe.protocol.servlet.helper.MessageHelper
import com.tencent.qqnt.kernel.nativeinterface.Contact
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback
import com.tencent.qqnt.kernel.nativeinterface.MsgConstant
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.qqnt.msg.api.IMsgService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import moe.fuqiuluo.xposed.helper.LogCenter
import moe.fuqiuluo.utils.MMKVFetcher
import moe.fuqiuluo.xposed.helper.NTServiceFetcher
import kotlin.coroutines.resume

internal object MsgSvc: BaseSvc() {
    suspend fun getMsg(msgId: Long): MsgRecord? {
        val chatType = MessageHelper.getChatType(msgId)
        val peerId = MessageHelper.getPeerIdByMsgId(msgId)
        val contact = MessageHelper.generateContact(chatType, peerId.toString())
        return withTimeout(5000) {
            val service = QRoute.api(IMsgService::class.java)
            suspendCancellableCoroutine { continuation ->
                service.getMsgsByMsgId(contact, arrayListOf(msgId)) { code, _, msgRecords ->
                    if (code == 0 && msgRecords.isNotEmpty()) {
                        continuation.resume(msgRecords.first())
                    } else {
                        continuation.resume(null)
                    }
                }
                continuation.invokeOnCancellation {
                    continuation.resume(null)
                }
            }
        }
    }

    suspend fun getMsgBySeq(
        chatType: Int,
        peerId: String,
        seq: Long
    ): MsgRecord? {
        val contact = MessageHelper.generateContact(chatType, peerId)
        return withTimeoutOrNull(60 * 1000) {
            val service = QRoute.api(IMsgService::class.java)
            suspendCancellableCoroutine { continuation ->
                service.getMsgsBySeqs(contact, arrayListOf(seq)) { code, _, msgRecords ->
                    //LogCenter.log("getMsgBySeq: $code, ${msgRecords?.firstOrNull()?.msgId}")
                    continuation.resume(msgRecords?.firstOrNull())
                }
                continuation.invokeOnCancellation {
                    continuation.resume(null)
                }
            }
        }
    }

    suspend fun deleteMsgASync(msgId: Long) {
        val kernelService = NTServiceFetcher.kernelService
        val sessionService = kernelService.wrapperSession
        val msgService = sessionService.msgService

        val contact = internalGenerateContact(msgId)
        msgService.recallMsg(contact, arrayListOf(msgId)) { code, why ->
            LogCenter.log("撤回消息(code = $code, reason = $why)")
        }
    }

    suspend fun recallMsg(msgId: Long): Pair<Int, String> {
        val kernelService = NTServiceFetcher.kernelService
        val sessionService = kernelService.wrapperSession
        val msgService = sessionService.msgService

        val contact = internalGenerateContact(msgId)
        return suspendCancellableCoroutine { continuation ->
            msgService.recallMsg(contact, arrayListOf(msgId)) { code, why ->
                continuation.resume(code to why)
            }
        }
    }

    suspend fun sendToAIO(chatType: Int, peedId: String, message: JsonArray): Pair<Long, Int> {
        val callback = MessageCallback(peedId, 0)
        val result = MessageHelper.sendMessageWithoutMsgId(chatType, peedId, message, callback)
        callback.hashCode = result.second
        return result
    }

    private class MessageCallback(
        private val peerId: String,
        var hashCode: Int
    ): IOperateCallback {
        override fun onResult(code: Int, reason: String?) {
            if (code != 0 && hashCode != 0) {
                MessageHelper.removeMsgByHashCode(hashCode)
            }
            when (code) {
                120 -> LogCenter.log("消息发送: $peerId, 禁言状态无法发送。")
                5 -> LogCenter.log("消息发送: $peerId, 当前不支持该消息类型。")
                else -> LogCenter.log("消息发送: $peerId, code: $code $reason")
            }
        }
    }

    private suspend fun internalGenerateContact(msgId: Long): Contact {
        val chatType = MessageHelper.getChatType(msgId)
        val mmkv = MMKVFetcher.mmkvWithId("hash2id")
        if (chatType == MsgConstant.KCHATTYPEGROUP) {
            val key = "grp$msgId"
            val groupId = mmkv.getLong(key, 0)
            mmkv.remove(key)
            return MessageHelper.generateContact(chatType, groupId.toString())
        } else if (chatType == MsgConstant.KCHATTYPEC2C) {
            val key = "c2c$msgId"
            val friendId = mmkv.getLong(key, 0)
            mmkv.remove(key)
            return MessageHelper.generateContact(chatType, ContactHelper.getUidByUinAsync(friendId))
        }
        error("暂时不支持该类型消息: $chatType")
    }
}