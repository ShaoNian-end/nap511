package github.zerorooot.nap511.util

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.compositionLocalOf
import androidx.core.app.NotificationManagerCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogItem
import com.elvishew.xlog.XLog
import com.elvishew.xlog.flattener.ClassicFlattener
import com.elvishew.xlog.interceptor.AbstractFilterInterceptor
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy
import com.google.gson.Gson
import com.google.gson.JsonObject
import github.zerorooot.nap511.bean.AvatarBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates

val LocalDrawerState = compositionLocalOf<DrawerState> {
    error("DrawerState not provided! Ensure you wrapped your content in CompositionLocalProvider.")
}


class App : Application(), ImageLoaderFactory {
    companion object {
        lateinit var instance: App
        var cookie = ""
        var uid = "0"

        //每次请求文件数
        var requestLimitCount by Delegates.notNull<Int>()

        //缓存fileListCache文件
        lateinit var cacheFile: File
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        cookie = DataStoreUtil.getData(ConfigKeyUtil.COOKIE, "")
        uid = DataStoreUtil.getData(ConfigKeyUtil.UID, "0")
        requestLimitCount = DataStoreUtil.getData(ConfigKeyUtil.REQUEST_LIMIT_COUNT, "200").toInt()
        cacheFile = File(this.cacheDir, "fileListCache.json")

        initLog()
    }

    fun initLog() {
        //log
        val build = LogConfiguration.Builder().tag("XLOG")
            .addInterceptor(object : AbstractFilterInterceptor() {
                override fun reject(log: LogItem?): Boolean {
                    return !DataStoreUtil.getData(ConfigKeyUtil.LOG, true)
                }
            }).build()
        //todo  日志输出代码位置
        /**
         *     val stackTrace = Throwable().stackTrace
         *                     val caller = stackTrace[1] // 获取调用者信息
         *                     val logTag = "${caller.fileName}:${caller.lineNumber}" // 显示文件名和行号
         */
        val print = FilePrinter
            .Builder(this.cacheDir.absolutePath)
            .cleanStrategy(FileLastModifiedCleanStrategy(7 * 24 * 60 * 60 * 1000))
            .flattener(ClassicFlattener())
            .build()
        XLog.init(build, AndroidPrinter(true), print);
        XLog.d("-----------------------init-----------------------------------")
//        val handler = Thread.getDefaultUncaughtExceptionHandler()
//        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
//            XLog.enableStackTrace(50).e("程序崩溃退出", e)
//            handler?.uncaughtException(thread, e)
//        }
//
//        val uncaughtExceptionHandler = Thread.currentThread().uncaughtExceptionHandler
//        Thread.currentThread().uncaughtExceptionHandler = UncaughtExceptionHandler { t, e ->
//            XLog.enableStackTrace(50).e("程序崩溃退出", e)
//            uncaughtExceptionHandler?.uncaughtException(t, e)
//        }
    }

    private val toastScope = CoroutineScope(Dispatchers.Main.immediate)

    fun toast(text: String) {
        toastScope.launch {
            Toast.makeText(instance, text, Toast.LENGTH_SHORT).show()
        }
    }


    fun getStringRes(id: Int): String {
        return getString(id)
    }

    fun checkLogin(cookie: String): Pair<Boolean, String> {
        val gson = Gson()
        val okHttpClient = OkHttpClient().newBuilder()
            .addInterceptor(Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("Cookie", cookie)
                        .addHeader("Content-Type", "application/json; Charset=UTF-8")
                        .addHeader(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36 115Browser/23.9.3.6"
                        )
                        .build()
                );
            }).build()
        val avatarUrl = "https://my.115.com/?ct=ajax&ac=nav&_${System.currentTimeMillis() / 1000}"
        val avatarUrlRequest: Request = Request.Builder().url(avatarUrl).get().build()
        val avatarUrlRespBody = okHttpClient.newCall(avatarUrlRequest).execute().body.string()
        Log.d("nap511 checkLogin avatarBean", avatarUrlRespBody)

        //{"state":true,"data":{"expire":1,"user_name":"Test","face":"face","user_id":11}}
        val avatarBean = run {
            try {
                gson.fromJson(
                    gson.fromJson(avatarUrlRespBody, JsonObject::class.java).get("data"),
                    AvatarBean::class.java
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        if (avatarBean == null) {
            return Pair(false, "验证失败，请重试")
        }

        avatarBean.expireString = Instant.ofEpochSecond(avatarBean.expire)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        DataStoreUtil.putData(ConfigKeyUtil.COOKIE, cookie)
        DataStoreUtil.putData(ConfigKeyUtil.UID, avatarBean.userId)
        DataStoreUtil.putData(ConfigKeyUtil.AVATAR_BEAN, gson.toJson(avatarBean))
        return Pair(true, "登陆成功,重启中～")
    }

    /**
     * 通过账号密码登录115网盘
     * @param username 账号（手机号或用户名）
     * @param password 密码
     * @return Pair<成功标志, 消息>
     */
    fun accountLogin(username: String, password: String): Pair<Boolean, String> {
        try {
            // RSA 加密密码
            val rsaUtil = MyRsaUtil()
            val encryptedPassword = rsaUtil.encrypt(password)

            val jsonBody = """{"login_name":"$username","login_pass":"$encryptedPassword"}"""
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val client = OkHttpClient().newBuilder()
                .followRedirects(false)
                .build()

            val request = Request.Builder()
                .url("https://passportapi.115.com/app/1.0/web/1.0/login")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36 115Browser/23.9.3.6"
                )
                .addHeader("Referer", "https://passport.115.com/")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body.string()

            Log.d("nap511 accountLogin", "response: $responseBody")
            Log.d("nap511 accountLogin", "code: ${response.code}")

            // 从 Set-Cookie 头中提取所有 cookie
            val cookieHeaders = response.headers("Set-Cookie")
            val cookieBuilder = StringBuilder()
            for (cookieHeader in cookieHeaders) {
                val cookiePart = cookieHeader.split(";")[0].trim()
                if (cookiePart.isNotEmpty()) {
                    if (cookieBuilder.isNotEmpty()) cookieBuilder.append("; ")
                    cookieBuilder.append(cookiePart)
                }
            }
            val cookieString = cookieBuilder.toString()

            Log.d("nap511 accountLogin", "cookies: $cookieString")

            if (cookieString.isEmpty()) {
                // 尝试从响应 JSON 中获取 cookie
                return try {
                    val jsonObject = Gson().fromJson(responseBody, JsonObject::class.java)
                    val state = jsonObject.get("state")?.asBoolean ?: false
                    if (!state) {
                        val msg = jsonObject.get("message")?.asString ?: jsonObject.get("msg")?.asString ?: "登录失败"
                        Pair(false, msg)
                    } else {
                        // 尝试从 data 中获取 cookie
                        val data = jsonObject.getAsJsonObject("data")
                        val cookieObj = data?.getAsJsonObject("cookie")
                        if (cookieObj != null) {
                            val cookieStr = cookieObj.entrySet().joinToString("; ") { "${it.key}=${it.value.asString}" }
                            checkLogin(cookieStr)
                        } else {
                            Pair(false, "登录失败：无法获取Cookie，请尝试通过网页登录")
                        }
                    }
                } catch (e: Exception) {
                    Pair(false, "登录失败：${e.message}")
                }
            }

            return checkLogin(cookieString)
        } catch (e: Exception) {
            Log.e("nap511 accountLogin", "error", e)
            return Pair(false, "登录异常：${e.message}")
        }
    }

    /**
     * 判断允许通知，是否已经授权
     * 返回值为true时，通知栏打开，false未打开。
     * @param context 上下文
     */
    fun isNotificationEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * 跳转到app的设置界面--开启通知
     * @param context
     */
    fun goToNotificationSetting(context: Context) {
        val intent = Intent()
        // android 8.0引导
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }


    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // 注册 GIF 解码器
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            // 你也可以在这里配置全局的淡入淡出效果、默认占位图等
            .crossfade(true)
            .build()
    }
}