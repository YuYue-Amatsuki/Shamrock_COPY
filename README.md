![logo](logo.png)

☘ 基于Xposed实现的Onebot11/12标准即时通讯Bot框架（OneBot12未完全实现）

> 本项目仅提供学习与交流用途，请在24小时内删除，本项目目的是研究Xposed和Lsposed框架的使用，以及Epic框架开发相关知识，如有违反法律，请联系删除。

> 学习交流群：758533243（二次元）| 333425831（正常人）

# Go-CqHttp无缝衔接性

本项目基于Go-CqHttp的文档进行开发实现，未来也将允许在docker运行本项目(现在也可以)，但在qsign死亡(报废)情况下Shamrock支持作为NTR提供sign api服务。

# 部署教程 / API文档

-> [点我直达](https://linxinrao.github.io/Shamrock)

# 权限声明

未在此处声明的权限，请警惕是否为正版Shamrock。

- 联网权限: 为了让Shamrock进程使用HTTP API进行一些操作。
- [Hook**系统框架**](https://github.com/fuqiuluo/Shamrock/wiki/perm_hook_android): 为了保证息屏状态下仍能维持服务后台运行。
- 后台启动Activity，自动唤醒QQ需要。

# 语音解码器支持

语音转换器已经模块化，如果不加入指定的模块，则无法发送mp3/flac/wav/ogg等格式的语音。

为了完整支持，您需要下载[AudioLibrary](https://raw.githubusercontent.com/fuqiuluo/Shamrock/master/AudioLibrary.zip)并将里面的`so文件`全部解压到`目标应用数据目录/Tencent/Shamrock/lib`文件夹。

**目标应用数据目录**一般在`/storage/emulated/0/Android/data/com.tencent.mobileqq`

如果没有`lib`文件夹，则创建一个，`lib`文件夹内只能有格式为`*.so`文件，不能有目录存在，否则无法正常加载。



