package com.wl.turbidimetric.login.details

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentUserDetailsBinding
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.model.UserModel
import com.wl.turbidimetric.project.details.ProjectDetailsFragment
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class UserDetailsFragment :
    BaseFragment<UserDetailsViewModel, FragmentUserDetailsBinding>(R.layout.fragment_user_details) {
    override val vm: UserDetailsViewModel by viewModels { UserDetailsViewModel.UserDetailsViewModelFactory() }
    private var id: Long = 0L
    private var curUser: UserModel? = null
    override fun initViewModel() {
    }

    override fun init(savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        i("onHiddenChanged hidden=$hidden")
        if (!hidden) {
            initData()
            initView()
        }
    }

    private fun initView() {
        if (id != 0L) {
            lifecycleScope.launch {
                curUser = vm.getUser(id)
                if (curUser == null) {
                    toast("用户不存在")
                } else {
                    vd.tvTitle.text = "修改信息"
                    vd.btnSave.setText("修改信息")
                    vd.etUserName.setText(curUser!!.userName)
                    vd.etUserName.isEnabled = false
                    vd.etPsw.setText("")
                    vd.etPsw2.setText("")
                }
            }
        } else {
            vd.etUserName.isEnabled = true
            vd.tvTitle.text = "添加用户"
            vd.btnSave.setText("添加用户")
            vd.etUserName.setText("")
            vd.etPsw.setText("")
            vd.etPsw2.setText("")
        }
        vd.btnSave.setOnClickListener {
            if (!verifyParams()) {
                return@setOnClickListener
            }

            if (id != 0L) {
                //修改
                curUser!!.apply {
                    password = vd.etPsw.text.toString()
                }
                lifecycleScope.launch {
                    val ret = vm.updateUser(curUser!!)
                    if (ret) {
                        toast("修改成功")
                        close()
                    } else {
                        toast("修改失败")
                    }
                }
            } else {
                //添加
                lifecycleScope.launch {
                    val ret = vm.addUser(UserModel().apply {
                        userName = vd.etUserName.text.toString()
                        password = vd.etPsw.text.toString()
                        level = 2
                    })
                    if (ret) {
                        toast("添加成功")
                        close()
                    } else {
                        toast("添加失败")
                    }
                }
            }
        }
        vd.llBack.setOnClickListener {
            close()
        }
    }

    fun verifyParams(): Boolean {
        if (vd.etUserName.text.toString().isEmpty()) {
            toast("请输入用户名")
            return false
        }
        if (vd.etPsw.text.toString().isEmpty()) {
            toast("请输入密码")
            return false
        }
        if (vd.etPsw.text.toString() != vd.etPsw2.text.toString()) {
            toast("两次密码不一致")
            return false
        }
        return true
    }

    private fun close() {
        EventBus.getDefault().post(EventMsg<String>(EventGlobal.WHAT_USER_DETAILS_FINISH))
    }

    private fun initData() {
        id = arguments?.getLong(ID, 0) ?: 0
    }

    companion object {
        val ID = "id"
    }
}
