package com.chenyue404.logaccessdialogblocklist

import android.content.Context
import android.os.Handler
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClassIfExists
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Created by chenyue on 2023/4/16 0016.
 */
class Hook : IXposedHookLoadPackage {
    companion object {
        const val PREF_NAME = "android"
        const val KEY = "key"
        const val KEY_ALLOW = "key_allow"
        val pref: XSharedPreferences? by lazy {
            val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, PREF_NAME)
            if (pref.file.canRead()) pref else null
        }
    }

    private val TAG = "LogAccessDialogActivity-hook-"

    private fun log(str: String) {
        XposedBridge.log("$TAG$str")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        val classLoader = lpparam.classLoader

//        val clazz =
//            findClassIfExists("com.android.server.logcat.LogAccessDialogActivity", classLoader)
//        log("clazz exit: ${clazz != null}, packageName=$packageName")
//        try {
//            findMethodBestMatch(
//                clazz,
//                "readIntentInfo",
//                Intent::class.java,
//                object : XC_MethodHook() {
//                    override fun afterHookedMethod(param: MethodHookParam) {
//                        val pkgName = getObjectField(param.thisObject, "mPackageName") as String?
//                        log("readIntentInfo: pkg=$pkgName")
//                        if (param.result == false || TextUtils.isEmpty(pkgName)) {
//                            return
//                        }
//                        if (pref?.getStringSet(KEY, setOf())?.any {
//                                pkgName!!.contains(it, true)
//                            } == true) {
//                            log("block: $pkgName")
//                            param.result = false
//                        }
//                    }
//                })
//        } catch (e: NullPointerException) {
//            e.printStackTrace()
//        } catch (e: NoSuchMethodException) {
//            e.printStackTrace()
//        } catch (e: NoClassDefFoundError) {
//            e.printStackTrace()
//        }

        val clazzLogAccessClient = findClassIfExists(
            "com.android.server.logcat.LogcatManagerService.LogAccessClient",
            classLoader
        )
        try {
            findAndHookMethod("com.android.server.logcat.LogcatManagerService", classLoader,
                "processNewLogAccessRequest",
                clazzLogAccessClient,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val logAccessClient = param.args.first()
                        val mPackageName = getObjectField(logAccessClient, "mPackageName") as String
                        log("mPackageName=$mPackageName")

                        pref?.reload()
                        val stringSet = pref?.getStringSet(KEY, setOf())
                        val allowSet = pref?.getStringSet(KEY_ALLOW, setOf())
                        log(
                            "blockSet=${stringSet?.joinToString(",").toString()}\n" +
                                    "allowSet=${allowSet?.joinToString(",").toString()}"
                        )

                        if (stringSet?.any {
                                mPackageName.contains(it.trim(), true)
                            } == true) {
                            log("block: $mPackageName")
                            val mContext = (getObjectField(
                                param.thisObject,
                                "mContext"
                            ) as Context).applicationContext
                            Handler(mContext.mainLooper).post {
                                Toast.makeText(
                                    mContext,
                                    "Blocked LogAccessRequest of $mPackageName",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            callMethod(
                                param.thisObject,
                                "onAccessDeclinedForClient",
                                logAccessClient
                            )
                            param.result = null
                        }

                        if (allowSet?.any {
                                mPackageName.contains(it.trim(), true)
                            } == true) {
                            log("Approved: $mPackageName")
                            val mContext = (getObjectField(
                                param.thisObject,
                                "mContext"
                            ) as Context).applicationContext
                            Handler(mContext.mainLooper).post {
                                Toast.makeText(
                                    mContext,
                                    "Approved LogAccessRequest of $mPackageName",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            callMethod(
                                param.thisObject,
                                "onAccessApprovedForClient",
                                logAccessClient
                            )
                            param.result = null
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}