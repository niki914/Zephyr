@file:Suppress("UNCHECKED_CAST")

package com.zephyr.vbclass

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

val ViewBinding.context: Context
    get() = root.context

/**
 * 功能 : 利用 java 反射获取 viewbinding 的类
 *
 * 请注意泛型擦除所带来的影响
 *
 * 已经用具体的 viewbinding 实现的类不能再被继承,
 * 否则会由于找不到 viewbinding 而崩溃,
 * 具体原因可以看 getTypeList 函数
 */
interface ViewBindingInterface<VB : ViewDataBinding> {
    fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToRoot: Boolean = false
    ): VB {
        val inflateMethod = getViewBindingClass().getDeclaredMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        val dataBinding =
            inflateMethod.invoke(null, inflater, container, attachToRoot) as VB
        return dataBinding
    }

    fun getViewBinding(inflater: LayoutInflater): VB {
        val inflateMethod = getViewBindingClass().getDeclaredMethod(
            "inflate",
            LayoutInflater::class.java
        )
        val dataBinding = inflateMethod.invoke(null, inflater) as VB
        return dataBinding
    }

    fun getViewBindingClass(): Class<VB> = getTypeList().getViewBindingClass()

    /**
     * 从 this.javaClass 中获取 vb 信息(即在最终的实现中),
     * 这意味着不能继承一个已经用 vb 实现过的类(除非再次在构造函数传入相同的 vb? 我没有尝试过),
     * 否则必然会因为找不到 vb 而崩溃
     */
    private fun getTypeList(): List<Class<VB>> {
        try {
            val parameterizedType = javaClass.genericSuperclass as ParameterizedType
            val arguments = parameterizedType.actualTypeArguments
            return arguments.filterIsInstance<Class<VB>>()
        } catch (e: Exception) {
            Log.e(
                this::class.simpleName.toString(),
                e.message.toString() + "\n" + e.stackTrace.toString()
            )
            return emptyList()
        }
    }

    /**
     * 从 vbClass 中找到目标类并返回
     *
     * 并不完全可靠, 不过在大部分情况下都能正确找到
     */
    private fun List<Class<VB>>.getViewBindingClass(): Class<VB> {
        // 找到包含 "binding" 的索引
        val position =
            indexOfFirst { clazz ->
                // 判定一个类是否是我们要的 binding 的逻辑
                clazz.simpleName.endsWith("Binding") &&
                        (ViewDataBinding::class.java.isAssignableFrom(clazz))
            }

        if (position == -1) {
            val builder = StringBuilder("在这个 list 中找不到名称包含 'Binding' 的项:")
            forEach {
                builder.append("\n" + it.name)
            }
            throw IllegalStateException(builder.toString())
        }
        return get(position)
    }
}
