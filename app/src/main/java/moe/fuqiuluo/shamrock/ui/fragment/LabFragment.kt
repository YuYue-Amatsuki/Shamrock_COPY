package moe.fuqiuluo.shamrock.ui.fragment

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.fuqiuluo.shamrock.R
import moe.fuqiuluo.shamrock.ui.app.AppRuntime
import moe.fuqiuluo.shamrock.ui.app.ShamrockConfig
import moe.fuqiuluo.shamrock.ui.theme.LocalString
import moe.fuqiuluo.shamrock.ui.theme.TabUnSelectedColor
import moe.fuqiuluo.shamrock.ui.tools.NoticeTextDialog
import moe.fuqiuluo.shamrock.ui.tools.toast

@Composable
fun LabFragment() {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val showNoticeDialog = remember { mutableStateOf(false) }

        NoticeBox(text = LocalString.labWarning) {
            showNoticeDialog.value = true
        }
        NoticeTextDialog(
            openDialog = showNoticeDialog,
            title = "温馨提示",
            text = "实验室功能会导致一些奇怪的问题，请谨慎使用！"
        )

        ActionBox(
            modifier = Modifier.padding(top = 12.dp),
            painter = painterResource(id = R.drawable.baseline_preview_24),
            title = "显示设置"
        ) {
            Column {
                Divider(
                    modifier = Modifier,
                    color = TabUnSelectedColor,
                    thickness = 0.2.dp
                )

                Function(
                    title = "中二病模式",
                    desc = "也许会导致奇怪的问题，大抵就是你看不懂罢了。",
                    descColor = it,
                    isSwitch = ShamrockConfig.is2B(ctx)
                ) {
                    ShamrockConfig.set2B(ctx, it)
                    scope.toast(ctx, "重启生效哦！")
                    return@Function true
                }

                Function(
                    title = "显示调试日志",
                    desc = "会导致日志刷屏。",
                    descColor = it,
                    isSwitch = ShamrockConfig.isDebug(ctx)
                ) {
                    ShamrockConfig.setDebug(ctx, it)
                    ShamrockConfig.pushUpdate(ctx)
                    return@Function true
                }
            }
        }

        ActionBox(
            modifier = Modifier.padding(top = 12.dp),
            painter = painterResource(id = R.drawable.round_logo_dev_24),
            title = "实验功能"
        ) {
            Column {
                Divider(
                    modifier = Modifier,
                    color = TabUnSelectedColor,
                    thickness = 0.2.dp
                )

                Function(
                    title = "自动清理QQ垃圾",
                    desc = "也许会导致奇怪的问题。",
                    descColor = it,
                    isSwitch = ShamrockConfig.isAutoClean(ctx)
                ) {
                    ShamrockConfig.setAutoClean(ctx, it)
                    ShamrockConfig.pushUpdate(ctx)
                    return@Function false
                }

                Function(
                    title = "拦截QQ无用发包",
                    desc = "测试阶段，可能导致网络异常。",
                    descColor = it,
                    isSwitch = ShamrockConfig.isInjectPacket(ctx)
                ) {
                    ShamrockConfig.setInjectPacket(ctx, it)
                    ShamrockConfig.pushUpdate(ctx)
                    return@Function true
                }
            }

        }

        ActionBox(
            modifier = Modifier.padding(top = 12.dp),
            painter = painterResource(id = R.drawable.round_logo_dev_24),
            title = "语音编解码器"
        ) {
            Column {
                Divider(
                    modifier = Modifier,
                    color = TabUnSelectedColor,
                    thickness = 0.2.dp
                )

                Function(
                    title = "语音流支持器",
                    desc = "请按照Wiki提示安装语音转换器。",
                    descColor = it,
                    isSwitch = AppRuntime.state.supportVoice.value
                ) {
                        scope.toast(ctx, "请按照Github提示手动操作。")
                    return@Function false
                }
            }

        }
    }
}

@Composable
private fun Function(
    title: String,
    desc: String,
    descColor: Color,
    isSwitch: Boolean,
    onClick: (Boolean) -> Boolean
) {
    Column(modifier = Modifier
        .absolutePadding(left = 8.dp, right = 8.dp, top = 12.dp, bottom = 0.dp)
    ) {
        Text(
            modifier = Modifier.padding(2.dp),
            text = desc,
            color = descColor,
            fontSize = 11.sp
        )
        ActionSwitch(text = title, isSwitch = isSwitch) {
            onClick(it)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabPreView() {
    LabFragment()
}