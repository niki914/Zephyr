# 快速开始

## settings.gradle.kts

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        ...
        maven { url = uri("https://jitpack.io") } // 添加 jitpack 的 Maven
    }
}
```

## 模块 build.gradle.kts 依赖导入

```kotlin
dependencies {
    // 按需导入
    val version = "3.1.0" // 根据最新 release 来设置
    implementation("com.github.niki914.Zephyr:datastore:$version")
    implementation("com.github.niki914.Zephyr:log:$version")
    implementation("com.github.niki914.Zephyr:net:$version")
    implementation("com.github.niki914.Zephyr:scaling-layout:$version")
    implementation("com.github.niki914.Zephyr:tools:$version")
    implementation("com.github.niki914.Zephyr:vbclass:$version")
}
```

# 模块介绍

## vbclass 模块

基于反射实现的 View binding 封装, 使用起来就像:
```kotlin
class MainActivity : ViewBindingActivity<ActivityMainBinding>() {
    override fun ActivityMainBinding.onCreate() {
        textView.setText("xxx")
    }
}
```

此模块有大量 UI 的 viewbinding 封装, 包括但不限于 activity、fragment、recyclerview-adapter

---

## log 模块

日志模块支持动态配置日志参数、文件存储、任意线程的异常捕获以及日志清理策略

### 配置日志

```kotlin
// 类似 DSL 语法的设置
LogConfig.edit {
    logLevel = LogLevel.DEBUG // 设置日志级别
    writeToFile = true // 开启文件存储
    fileFolder = "logs" // 设置存储路径
    logFileHeader = "log_" // 设置日志文件名前缀
    retainedDays = 7 // 日志保留7天
}
```

### 记录日志

```kotlin
logD("MainActivity", "Application started") // 记录调试日志
logE("Network", "Failed to connect") // 记录错误日志
```

### 异常捕获配置

可以防止大部分的崩溃情况

```kotlin
setOnCaughtListener { thread, throwable ->
    startExceptionActivity(throwable) // 自定义异常处理, 比如打开一个崩溃界面
}
```

---

## tools 模块

一些工具类和扩展函数集

### 图像处理(ImageTools.kt)
提供 Drawable 和 Bitmap 的便捷操作, 例如复制 Drawable、生成圆角占位图等。支持自定义颜色和尺寸, 适用于动态生成 UI 占位符

### 视图操作(LPTools.kt)
扩展了 View 类的功能, 支持快速设置尺寸、边距、处理 WindowInsets, 以及查找点击位置的 View。特别适合处理复杂布局和动态 UI 调整

### 窗口参数获取(WinParamsHelper.kt)
提供获取状态栏高度、导航栏高度、屏幕宽高等功能, 兼容 Android R 及以下版本, 确保在不同设备上的适配性

### RecyclerView 增强(RecyclerViewHelper.kt)
封装了 RecyclerView 的常见功能, 包括 PagerSnapHelper 吸附效果、原生分割线添加, 以及触底/触边加载更多监听器, 简化分页加载实现

### Toast 提示(Toaster.kt)
提供简洁的 Toast 封装, 支持取消上一次 Toast、自定义显示时长, 并确保安全调用, 适用于快速提示用户

---

## net 模块

基于 retrofit 的封装, 包含同步、挂起、异步的网络请求函数, 支持流式传输请求

---

## scaling layout 模块

为  (scaling-layout)[https://github.com/iammert/ScalingLayout]  在新版本安卓上进行适配

---

## datastore 模块

对 jetpack datastore 的封装
