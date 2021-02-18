# 系统应用杀手
安卓的系统应用卸载器，开源 无广告 无收费。
# 开源协议
GPLv3.0

# 开发思路
## 界面
首先应用主页有展示系统应用的分类和具体的系统应用，分类依据为path的上一层也就是/system/app/phonesky/googleplaystore.apk的分类在/system/app，这就是垂直的分类展示了，用套娃RecyclerView就能做到。
而设置界面为bottom sheet 呼出方式为点按在屏幕上方的toolbar的menu，定义一些类似于在启动时所需要执行的shell脚本挂载system分区，是否启用`备份`功能。
导入界面则展示一个输入的框，和一个[path和删除]的列表，和一个小勾勾。
导出界面主页面展示多个版本，并且可以对版本命名、添加版本，版本点击后展示应用的[名称 path 删除的按钮]的列表，在导出时获得一串乱码。
## 功能
### 卸载时
卸载时先存储应用的名称和路径，判断是否需要用SHELL执行挂载操作，再对其进行删除或者移动，以及data分区。
##### 备份时
备份时则由用户选择是否备份在/sdcard/android/data...还是在/sdcard/的任意地方。
### 导出入
导出时从数据内获取数据并转换成为json数据并压缩，导入反道而行。
##### 关于字符压缩
我对比了对于 [卸载列表](https://github.com/ElisaMin/SystemAppsKiller/blob/7adfc99f86c305cba054553d74679ec24480fe05/app/src/main/java/me/heizi/box/packagemanager/libs/lib.kt) 的压缩后进行base64编码的长度，我打算使用GZIP。

|压缩方式|长度 |
|---:|:----|
|lz4     |1256|
|gzip    |812|
|deflate |800|
|snappy  |1048|
|bzip2   |896|
|zstd    |924|

### 判断是否可挂载
```
先挂载
写入/system/app/heizi-toolx/testRw
删除上面的路径
```
执行成功就判断成功了
### 搜索
待解决：更好用的
## 风格化
## 用户使用流程
### 第一次打开时
调起判断是否root可挂载，如果不可root、su不存在则在界面上告知用户不适合，立即退出，否则继续。
不可挂载时告之用户错误，请把挂载用的代码放进来，继续尝试挂载。
挂载成功之后进入设置页面，看看是否需要备份。弹出免责警告和用户手册。进入完整的应用。
### 导出
点击在屏幕上方工具栏的按钮进入版本导出 为空时则显示文字空，非空时展示名字和创建时间，还有一段文字解释，长按版本可复制导出这个版本。
点击上方工具栏的加号展示所有从`已经卸载的列表`，并对其进行添加和删除的操作 上方有完成按钮，点击完成后提示完成和长按版本可复制。
### 导入
界面内有按钮提示从粘贴板获取数据，读取后在下方展示所有的数据，可以进行删除单个数据，点击上方完成按钮即可完成卸载操作。
## 数据库
存放的内容其实就卸载时的记录，所以每一次卸载都会记录：
```json5
{
  id:0,
  name: "heizi_tool",
  packages: "me.heizi.tool",
  source: "/system/app/abc",
  data: null,
  isBackuped: false
}
```
对于版本信息来说
```json5
{
  id: "0",
  created_time: "",
  name: "string",
}
```
还有个中间表
```json5
{
  id: 0,
  versions_id: 0,
  uinstall_id: 0
}
```
最终效果:
```json5
[
  {
    id: 0,
    name: "一份记录",
    create_time: "*now",
    isBackup: true,
    apps: [{
      id: 0,
      name: "name",
      packageName: "me.heizi.example",
      source: "/data/app/anyway/any.apk",
      data: "null"
    },{
      id: 1,
      name: "heizi_tool",
      packages: "me.heizi.tool",
      source: "/system/app/abc",
      data: null,
    }]
  }
]
```
## 包含关系
```json5
{
  application: {
    actvities: [
      {
        id: 0,
        name: "启动器 用于跳转到1",
        type: "viewModelOnly"
      }, {
        id: 1,
        name: "展示主页和跳转",
        fragments: [
          {
            name: ""
          },
        ]
      }, {
        id: 2,
        name: "设置",
        type: "settings"
      }
    ],
    broadcast: {
      
    }
  }
}
```