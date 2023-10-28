package moe.protocol.servlet.transfile

import com.tencent.mobileqq.data.MessageForShortVideo
import com.tencent.mobileqq.transfile.FileMsg
import com.tencent.mobileqq.transfile.TransferRequest
import moe.protocol.servlet.transfile.ContactType.*
import java.io.File
import moe.protocol.servlet.transfile.ResourceType.*
import oicq.wlogin_sdk.tools.MD5

internal object Transfer: FileTransfer() {
    private val ROUTE = mapOf<ContactType, Map<ResourceType, suspend TransTarget.(Resource) -> Boolean>>(
        TROOP to mapOf(
            Picture to { uploadGroupPic(id, (it as PictureResource).src) },
            Voice to { uploadGroupVoice(id, (it as VoiceResource).src) },
            Video to { uploadGroupVideo(id, (it as VideoResource).src, it.thumb) },

        ),
        PRIVATE to mapOf(
            Picture to { uploadC2CPic(id, (it as PictureResource).src) },
            Voice to { uploadC2CVoice(id, (it as VoiceResource).src) },
            Video to { uploadC2CVideo(id, (it as VideoResource).src, it.thumb) },

        )
    )

    suspend fun uploadC2CVideo(
        groupId: String,
        file: File,
        thumb: File,
        wait: Boolean = true
    ): Boolean {
        return transC2CResource(groupId, file, FileMsg.TRANSFILE_TYPE_SHORT_VIDEO_C2C, BUSI_TYPE_SHORT_VIDEO, wait) {
            it.mSourceVideoCodecFormat = VIDEO_FORMAT_MP4
            it.mRec = MessageForShortVideo().also {
                it.busiType = BUSI_TYPE_SHORT_VIDEO
            }
            it.mThumbPath = thumb.absolutePath
            it.mThumbMd5 = MD5.getFileMD5(thumb)
        }
    }

    suspend fun uploadGroupVideo(
        groupId: String,
        file: File,
        thumb: File,
        wait: Boolean = true
    ): Boolean {
        return transTroopResource(groupId, file, FileMsg.TRANSFILE_TYPE_SHORT_VIDEO_TROOP, BUSI_TYPE_SHORT_VIDEO, wait) {
            it.mSourceVideoCodecFormat = VIDEO_FORMAT_MP4
            it.mRec = MessageForShortVideo().also {
                it.busiType = BUSI_TYPE_SHORT_VIDEO
            }
            it.mThumbPath = thumb.absolutePath
            it.mThumbMd5 = MD5.getFileMD5(thumb)
        }
    }

    suspend fun uploadC2CVoice(
        groupId: String,
        file: File,
        wait: Boolean = true
    ): Boolean {
        return transC2CResource(groupId, file, FileMsg.TRANSFILE_TYPE_PTT, 1002, wait) {
            it.mPttUploadPanel = 3
            it.mPttCompressFinish = true
            it.mIsPttPreSend = true
        }
    }

    suspend fun uploadGroupVoice(
        groupId: String,
        file: File,
        wait: Boolean = true
    ): Boolean {
        return transTroopResource(groupId, file, FileMsg.TRANSFILE_TYPE_PTT, 1002, wait) {
            it.mPttUploadPanel = 3
            it.mPttCompressFinish = true
            it.mIsPttPreSend = true
        }
    }

    suspend fun uploadC2CPic(
        peerId: String,
        file: File,
        wait: Boolean = true
    ): Boolean {
        return transC2CResource(peerId, file, FileMsg.TRANSFILE_TYPE_PIC, SEND_MSG_BUSINESS_TYPE_PIC_SHARE, wait) {
            val picUpExtraInfo = TransferRequest.PicUpExtraInfo()
            picUpExtraInfo.mIsRaw = true
            it.mPicSendSource = 8
            it.mExtraObj = picUpExtraInfo
        }
    }

    suspend fun uploadGroupPic(
        groupId: String,
        file: File,
        wait: Boolean = true
    ): Boolean {
        return transTroopResource(groupId, file, FileMsg.TRANSFILE_TYPE_PIC, SEND_MSG_BUSINESS_TYPE_PIC_SHARE, wait) {
            val picUpExtraInfo = TransferRequest.PicUpExtraInfo()
            picUpExtraInfo.mIsRaw = true
            it.mPicSendSource = 8
            it.mExtraObj = picUpExtraInfo
        }
    }

    operator fun get(contactType: ContactType, resourceType: ResourceType): suspend TransTarget.(Resource) -> Boolean {
        return (ROUTE[contactType] ?: error("unsupported contact type: $contactType"))[resourceType]
            ?: error("Unsupported resource type: $resourceType")
    }
}

internal suspend infix fun TransferTaskBuilder.trans(res: Resource): Boolean {
    return Transfer[contact.type, res.type](contact, res)
}

internal class TransferTaskBuilder {
    lateinit var contact: TransTarget
}

internal infix fun Transfer.with(contact: TransTarget): TransferTaskBuilder {
    return TransferTaskBuilder().also {
        it.contact = contact
    }
}