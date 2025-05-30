package com.zephyr.vbclass.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.ViewDataBinding
import com.zephyr.vbclass.ViewBindingInterface

/**
 * 功能: 显示一个禁止操作的界面, 可以用自定义布局
 *
 * @see ViewBindingInterface 注意事项
 */
open class ViewBindingDialog<VB : ViewDataBinding>(context: Context) : AlertDialog(context),
    ViewBindingInterface<VB> {

    protected val binding: VB by lazy {
        getViewBinding(LayoutInflater.from(context))
    }

    open fun VB.onInitialized() {
    }

    init {
        window?.setBackgroundDrawableResource(android.R.color.transparent) // 隐藏原生的对话框
        setCancelable(false)
        setView(binding.root)
        binding.onInitialized()
    }
}