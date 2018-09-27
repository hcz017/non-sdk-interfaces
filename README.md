# Android P 如何访问非SDK 接口

**为什么要访问非SDK 接口？**

为了让应用能获得更多的系统属性、资源，扩展功能等。

在Android P 之前，我们可以使用反射来访问系统内部分 hide/private 的接口。

下面是一个使用反射访问非SDK 接口的例子。

## P 之前访问非SDK 接口示例

以java 反射为例

使用java 反射获得默认数据卡SubId，进而获得数据卡的运营商名称。这里是两次反射。

```JAVA
    //java reflect to access hide methods.
    public void getNetworkOperatorName(View view) throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // get defaultDataSubId @hide
        Class subscriptionManager = Class.forName("android.telephony.SubscriptionManager");
        Method getDefaultDataSubId = subscriptionManager.getMethod("getDefaultDataSubId");
        Object defaultDataSubId = getDefaultDataSubId.invoke(subscriptionManager);

        // to get getNetworkOperatorName(subId), @hide
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        Class telephonyManager = Class.forName("android.telephony.TelephonyManager");
        Method getNetworkOperatorName = telephonyManager.getMethod("getNetworkOperatorName", int.class);
        Object operatorName = getNetworkOperatorName.invoke(tm, defaultDataSubId);
        tvOpName.setText((String) operatorName);
        Log.i(TAG, "default data sub network operator name : " + operatorName);
    }
```
由于在Android M 新增getDefaultDataSubscriptionId 用以取代getDefaultDataSubId，getDefaultDataSubId 在后续版本中被移除，所以为了兼容多版本要做一下修改，否在在高版本的android 系统上会crash（稳定性问题）。

```JAVA
        Method getDefaultDataSubId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getDefaultDataSubId = subscriptionManager.getMethod("getDefaultDataSubscriptionId");
        } else {
            getDefaultDataSubId = subscriptionManager.getMethod("getDefaultDataSubId");
        }
```

**注**: 在Android P getDefaultDataSubscriptionId 已经非@hide 了，此处只是为了举例方便。

## Android P 引入对非SDK 接口访问限制

目的：通过减少对非SDK 接口的调用，提升应用的稳定性。

### 黑白灰名单

- 白名单：SDK
- 浅灰名单：仍可以访问的非 SDK 函数/字段，但不保证后续不会移到黑名单。
- 深灰名单：
  对于目标 SDK 低于 API 级别 28 的应用，允许使用深灰名单接口。
  对于目标 SDK 为 API 28 或更高级别的应用：行为与黑名单相同
- 黑名单：受限，无论目标 SDK 如何。 平台将表现为似乎接口并不存在。 例如，无论应用何时尝试使用接口，平台都会引发 NoSuchMethodError/NoSuchFieldException，即使应用想要了解某个特殊类别的字段/函数名单，平台也不会包含接口。


### 过度阶段

由于灰名单的不确定性，开发者需尽早使用其他API 代替，如果实在不能用其他API 替代，向Google 提交使用场景和说明， Google会收集使用较多的非SDK 接口，考虑做以下支持：

1. 扩展白名单
2. 新增API 功能类似的API

### 如何启用对非 SDK API 的访问（官方）？

可以使用 adb 在开发设备上启用对非 SDK API 的访问。

如果您想要在 adb logcat 输出中查看 API 访问信息，则可以更改 API 强制政策：

```shell
adb shell settings put global hidden_api_policy_pre_p_apps  1
adb shell settings put global hidden_api_policy_p_apps 1
```

要将其重置为默认设置，请执行以下操作：

```shell
adb shell settings delete global hidden_api_policy_pre_p_apps
adb shell settings delete global hidden_api_policy_p_apps
```

 这些命令**不要求已取得 root 权限**的设备。

给定整数的含义如下所示：

- 0：停用非 SDK API 使用检测。 这还会停用日志记录，并且也会破坏严格模式 API detectNonSdkApiUsage()。 不建议使用此值。
- 1：“只是警告”- 允许访问所有非 SDK API，但会在日志中保留警告。 严格模式 API 将继续工作。
- 2：不允许使用列入深灰名单和黑名单的 API。
- 3：不允许使用列入黑名单的 API，但允许使用列入深灰名单的 API。

## 绕过SDK 接口限制
### 1. 选择较低的target SDK

> In cases where a migration to SDK methods will be possible but is likely to be technically challenging, we'll allow continued usage until your app is updated to target the latest API level.

上面内容取自Google 官方博客，大意是：考虑到更换SDK 接口在技术上可能有难度，我们允许大家在target 最新的API level 之前继续访问非SDK 接口。

举例，访问深灰名单的接口，target API 低于P 的时候可以访问，target API 为 P的时候会出现闪退或其他错误。

### 2. 设置属性

前面已经提到
```shell
  adb shell settings put global hidden_api_policy_pre_p_apps  1
  adb shell settings put global hidden_api_policy_p_apps 1
```
### 3. 第三方工具

[FreeReflection](https://github.com/tiann/FreeReflection)

> ## Usage
>
> 1. Add dependency to your project(jcenter):
>
> ```
> implementation 'me.weishu:free_reflection:1.2.0'
> ```
>
> 1. Add one line to your `Application.attachBaseContext` :
>
> ```
> @Override
> protected void attachBaseContext(Context base) {
>     super.attachBaseContext(base);
>     Reflection.unseal(base);
> }
> ```

用这个方法log 中不会有warning log 打印。
博客介绍[free-reflection-above-android-p](http://www.weishu.me/2018/06/07/free-reflection-above-android-p/)

### 4. 添加系统classes.jar/frameworks.jar

SDK 中提供的framework.jar 包含的都是公开的接口，源码编译生成的classes.jar 中含有hide 接口，添加classes.jar 到依赖可以访问非公开接口。

参考:

1. [How to use Android Studio to build system application?](http://www.31mins.com/android-studio-build-system-application/)

### 小结

1. 选择较低的target SDK -> 适用于对target SDK 没有要求的app。推荐
2. 设置属性 –> 不需要改应用代码，但需要adb 环境活着获得root权限后应用去设置属性。不推荐。
3. 第三方工具 –> 简单，不用做额外配置。缺点是如果后续google 更改了限制访问非SDK 接口的实现，第三方工具可能会更新不及时（或不更新），有风险。不是特别推荐。
4. 添加系统classes.jar/frameworks.jar ->不用写反射代码，但需要有源码编译生成的classes.jar，考虑到国内有诸多定制系统，可能会存在兼容性问题。不推荐。

## 补充

### 灰名单/黑名单位于什么地方？

它们作为平台的一部分构建。 您可以在[此处](https://android.googlesource.com/platform/prebuilts/runtime/+/master/appcompat)找到预构建条目：

- platform/prebuilts/runtime/appcompat/hiddenapi-light-greylist.txt：浅灰 API 的 AOSP 名单

- platform/prebuilts/runtime/appcompat/hiddenapi-dark-greylist.txt：深灰 API 的 AOSP 名单

笔者注：实测灰名单/黑名单已过时，我在demo 中使用的一个非浅灰名单的接口被证实为灰名单接口，一个黑名单接口被证实为非名单内接口。

此外，此[名单](https://android.googlesource.com/platform/frameworks/base/+/master/config/hiddenapi-p-light-greylist.txt)还包含浅灰 API 的 SDK 28 名单。

黑名单和深灰名单在构建时衍生而来。 我们已添加了一个可以在 AOSP 上生成名单的构建规则。 它与 P 中的黑名单不同，但是重叠比较合理。 开发者可以下载 AOSP，然后使用以下命令生成黑名单：

```
make hiddenapi-aosp-blacklist
```

然后，可以在以下位置找到文件：

```
out/target/common/obj/PACKAGING/hiddenapi-aosp-blacklist.txt
```

笔者注：实测该make 命令编译报错。

### 黑名单/灰名单在采用相同 Android 版本的原始设备制造商设备上是否相同？

相同，原始设备制造商可以向黑名单中添加自己的 API，但无法从原始的/AOSP 黑名单或灰名单中移除条目。 CDD 可以防止此类变化，CTS (Compatibility Test Suite) 测试可以确保 Android 运行时强制执行名单。

Demo 地址 [non-sdk-interfaces
](https://github.com/hcz017/non-sdk-interfaces)