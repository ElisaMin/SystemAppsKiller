package me.heizi.box.package_manager.workers

// work manager is shit on this project

//class UninstallWorker(
//    private val uninstallInfo: UninstallInfo,
//    private val mountString: String,
//    private val backupType: BackupType,
//    appContext: Context, params: WorkerParameters
//) : CoroutineWorker(appContext, params) {
//
//    override suspend fun doWork(): Result = withContext(IO) {
//        val message = StringBuilder()
//        val data = Data.Builder()
//        var isSuccess: Boolean? = null
//        try {
//            val commandLines = getUninstallCommand(
//                    mountString = mountString,
//                    backupType = backupType,
//                    sDir = uninstallInfo.sourceDirectory,
//                    packageName = uninstallInfo.packageName
//            ).replace("\n"," && ")
//            data.putString(COMMAND,commandLines)
//            launch(IO) {
//                shell(commandLines = arrayOf(commandLines),prefix = arrayOf("su")).collect {
//                    when(it) {
//                        is ProcessingResults.CODE -> {
//                            isSuccess = it.code == 0
//                            data.putInt(CODE, it.code)
//                        }
//                        is ProcessingResults.Error -> message.line { "ERROR!:'${it.message}'" }
//                        is ProcessingResults.Message -> message.line { it.message }
//                        ProcessingResults.Closed -> cancel()
//                    }
//                }
//            }.join()
//            if (isSuccess == null) throw IOException("未知错误")
//            data.putString(MESSAGE,message.toString())
//        } catch (e:Exception) {
//            data.putString(EXCEPTION,"${e.javaClass.name}:${e.message}")
//            Log.e(TAG, "doWork: failed", e)
//            isSuccess = false
//        }
//        val outputData = data.build()
//        when (isSuccess) {
//            true -> {
//                updateDB {
//                    UninstallRecord (
//                            name = uninstallInfo.applicationName,
//                            packageName = uninstallInfo.packageName,
//                            source =  uninstallInfo.sourceDirectory,
//                            data = uninstallInfo.dataDirectory,
//                            isBackups = backupType !is BackupType.JustRemove
//                    ).add()
//                }
//                Result.success(outputData)
//            }
//            false -> Result.failure(outputData)
//            null -> throw IOException("未知错误")
//        }
//    }
//    companion object {
//        const val COMMAND = "$PACKAGE_NAME.result.command"
//        const val MESSAGE = "$PACKAGE_NAME.result.message"
//        const val EXCEPTION = "$PACKAGE_NAME.result.exception"
//        const val CODE = "$PACKAGE_NAME.result.code"
//    }
//
//}