package me.heizi.box.packagemanager.libs

import dev.utils.app.ShellUtils


infix fun ShellUtils.CommandResult.success(doSTH:ShellUtils.CommandResult.()->Unit): ShellUtils.CommandResult {
    if (this.isSuccess){
        doSTH()
    }
    return this
}
infix fun ShellUtils.CommandResult.fall(doSTH: ShellUtils.CommandResult.() -> Unit): ShellUtils.CommandResult {
    if (!this.isSuccess){
        doSTH()
    }
    return this
}